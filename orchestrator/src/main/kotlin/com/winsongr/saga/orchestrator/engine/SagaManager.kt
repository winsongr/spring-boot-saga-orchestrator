package com.winsongr.saga.orchestrator.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.orchestrator.domain.*
import com.winsongr.saga.orchestrator.domain.definitions.SagaDefinitionRegistry
import com.winsongr.saga.orchestrator.metrics.SagaMetrics
import com.winsongr.saga.orchestrator.persistence.OutboxRepository
import com.winsongr.saga.orchestrator.persistence.OutboxRow
import com.winsongr.saga.orchestrator.persistence.SagaRepository
import com.winsongr.saga.orchestrator.persistence.SagaStepRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Central saga orchestration engine.
 * All state mutations happen inside @Transactional boundaries —
 * saga row, step row, and outbox row are committed atomically.
 */
@Component
class SagaManager(
    private val sagaRepository: SagaRepository,
    private val stepRepository: SagaStepRepository,
    private val outboxRepository: OutboxRepository,
    private val definitionRegistry: SagaDefinitionRegistry,
    private val compensationHandler: CompensationHandler,
    private val metrics: SagaMetrics,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(SagaManager::class.java)

    /**
     * Create a new saga and kick off the first step.
     * Returns the saga ID for the 202 response.
     */
    @Transactional
    fun startSaga(type: String, payload: String): UUID {
        val definition = definitionRegistry.get(type)
        val now = Instant.now()
        val sagaId = UUID.randomUUID()

        MDC.put("saga_id", sagaId.toString())
        try {
            val saga = Saga(
                id = sagaId,
                type = type,
                status = SagaStatus.PENDING,
                payload = payload,
                createdAt = now,
                updatedAt = now
            )
            sagaRepository.insert(saga)

            // Insert the first step and its outbox command
            val firstStep = definition.firstStep()
            insertStepAndOutbox(sagaId, firstStep, payload)

            sagaRepository.updateStatus(sagaId, SagaStatus.RUNNING)

            metrics.sagaStarted(type)
            log.info("saga started: type={}, sagaId={}", type, sagaId)
            return sagaId
        } finally {
            MDC.remove("saga_id")
        }
    }

    /**
     * Advance a saga after a successful step reply.
     * Called by StepReplyConsumer on SUCCESS.
     */
    @Transactional
    fun handleStepSuccess(sagaId: UUID, stepName: String, responsePayload: String?) {
        MDC.put("saga_id", sagaId.toString())
        try {
            val saga = sagaRepository.findForUpdate(sagaId)
                ?: throw IllegalStateException("Saga not found: $sagaId")

            // Guard: if saga is not RUNNING, this is a duplicate/late reply — ignore
            if (saga.status != SagaStatus.RUNNING) {
                log.warn("ignoring step success for non-RUNNING saga: status={}", saga.status)
                return
            }

            val step = stepRepository.findByKey(sagaId, stepName)
                ?: throw IllegalStateException("Step not found: $sagaId/$stepName")

            // Guard: only advance if step is RUNNING (idempotency at orchestrator level)
            if (step.status != StepStatus.RUNNING) {
                log.warn("ignoring duplicate step success: step={}, status={}", stepName, step.status)
                return
            }

            stepRepository.updateStatus(sagaId, stepName, StepStatus.COMPLETED, responsePayload)
            metrics.stepCompleted(saga.type, stepName)

            // Find next step from definition
            val definition = definitionRegistry.get(saga.type)
            val nextStepDef = definition.nextStep(step.sequenceNo)

            if (nextStepDef == null) {
                // All steps completed — saga is done
                val now = Instant.now()
                sagaRepository.updateStatus(sagaId, SagaStatus.COMPLETED, now)
                metrics.sagaCompleted(saga.type, saga.createdAt)
                log.info("saga completed: sagaId={}", sagaId)
            } else {
                // Advance to next step
                insertStepAndOutbox(sagaId, nextStepDef, saga.payload)
                log.info("saga advanced: sagaId={}, nextStep={}", sagaId, nextStepDef.name)
            }
        } finally {
            MDC.remove("saga_id")
        }
    }

    /**
     * Handle a failed step — trigger compensation cascade.
     * Called by StepReplyConsumer on FAILED, and by TimeoutSweeper.
     */
    @Transactional
    fun handleStepFailure(sagaId: UUID, stepName: String, errorMessage: String?) {
        MDC.put("saga_id", sagaId.toString())
        try {
            val saga = sagaRepository.findForUpdate(sagaId)
                ?: throw IllegalStateException("Saga not found: $sagaId")

            if (saga.status != SagaStatus.RUNNING) {
                log.warn("ignoring step failure for non-RUNNING saga: status={}", saga.status)
                return
            }

            stepRepository.updateStatus(sagaId, stepName, StepStatus.FAILED, errorMessage = errorMessage)
            sagaRepository.updateStatus(sagaId, SagaStatus.COMPENSATING)
            metrics.stepFailed(saga.type, stepName)

            log.info("step failed, starting compensation: sagaId={}, step={}, error={}",
                sagaId, stepName, errorMessage)

            compensationHandler.compensate(sagaId, saga.type)
        } finally {
            MDC.remove("saga_id")
        }
    }

    /**
     * Handle a successful compensation reply.
     * When all compensations complete, mark saga as FAILED.
     */
    @Transactional
    fun handleCompensationSuccess(sagaId: UUID, stepName: String) {
        MDC.put("saga_id", sagaId.toString())
        try {
            val saga = sagaRepository.findForUpdate(sagaId)
                ?: throw IllegalStateException("Saga not found: $sagaId")

            if (saga.status != SagaStatus.COMPENSATING) {
                log.warn("ignoring compensation success for non-COMPENSATING saga: status={}", saga.status)
                return
            }

            stepRepository.updateStatus(sagaId, stepName, StepStatus.COMPENSATED)
            metrics.compensationCompleted(saga.type, stepName)

            // Check if all compensating steps are done
            val steps = stepRepository.findBySagaId(sagaId)
            val allCompensated = steps
                .filter { it.status == StepStatus.COMPENSATING }
                .isEmpty()

            if (allCompensated) {
                val now = Instant.now()
                sagaRepository.updateStatus(sagaId, SagaStatus.FAILED, now)
                metrics.sagaFailed(saga.type, saga.createdAt)
                log.info("saga fully compensated, marked FAILED: sagaId={}", sagaId)
            }
        } finally {
            MDC.remove("saga_id")
        }
    }

    /**
     * Handle a failed compensation — saga is STUCK, needs manual intervention.
     */
    @Transactional
    fun handleCompensationFailure(sagaId: UUID, stepName: String, errorMessage: String?) {
        MDC.put("saga_id", sagaId.toString())
        try {
            val saga = sagaRepository.findForUpdate(sagaId)
                ?: throw IllegalStateException("Saga not found: $sagaId")

            stepRepository.updateStatus(sagaId, stepName, StepStatus.COMPENSATION_FAILED, errorMessage = errorMessage)
            sagaRepository.updateStatus(sagaId, SagaStatus.STUCK)
            metrics.sagaStuck(saga.type)

            log.error("compensation failed, saga STUCK: sagaId={}, step={}, error={}",
                sagaId, stepName, errorMessage)
        } finally {
            MDC.remove("saga_id")
        }
    }

    /**
     * Insert a step row + outbox command row in the current transaction.
     */
    private fun insertStepAndOutbox(sagaId: UUID, stepDef: StepDef, sagaPayload: String) {
        val now = Instant.now()

        stepRepository.insert(
            SagaStep(
                sagaId = sagaId,
                stepName = stepDef.name,
                sequenceNo = stepDef.sequenceNo,
                status = StepStatus.RUNNING,
                requestPayload = sagaPayload,
                startedAt = now,
                deadlineAt = now.plusMillis(stepDef.timeoutMs)
            )
        )

        val headers = objectMapper.writeValueAsString(
            mapOf("saga_id" to sagaId.toString(), "step_name" to stepDef.name)
        )

        outboxRepository.insert(
            OutboxRow(
                sagaId = sagaId,
                topic = stepDef.commandTopic,
                key = sagaId.toString(),
                payload = sagaPayload,
                headers = headers
            )
        )
    }
}
