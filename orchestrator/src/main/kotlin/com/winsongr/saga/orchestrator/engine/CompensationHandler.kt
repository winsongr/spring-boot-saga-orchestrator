package com.winsongr.saga.orchestrator.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.orchestrator.domain.StepStatus
import com.winsongr.saga.orchestrator.domain.definitions.SagaDefinitionRegistry
import com.winsongr.saga.orchestrator.metrics.SagaMetrics
import com.winsongr.saga.orchestrator.persistence.OutboxRepository
import com.winsongr.saga.orchestrator.persistence.OutboxRow
import com.winsongr.saga.orchestrator.persistence.SagaStepRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Walks completed forward steps in reverse order and enqueues
 * compensation commands into the outbox.
 *
 * Only steps with a non-null compensationTopic get compensated.
 * Notification (best-effort, no compensation) is skipped.
 */
@Component
class CompensationHandler(
    private val stepRepository: SagaStepRepository,
    private val outboxRepository: OutboxRepository,
    private val definitionRegistry: SagaDefinitionRegistry,
    private val metrics: SagaMetrics,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(CompensationHandler::class.java)

    /**
     * Enqueue compensation commands for all completed steps of the given saga.
     * Must be called within a @Transactional context.
     */
    fun compensate(sagaId: UUID, sagaType: String) {
        val definition = definitionRegistry.get(sagaType)
        val completedSteps = stepRepository.findCompletedSteps(sagaId) // already DESC by sequence_no

        if (completedSteps.isEmpty()) {
            log.info("no completed steps to compensate: sagaId={}", sagaId)
            return
        }

        for (step in completedSteps) {
            val stepDef = definition.stepByName(step.stepName)
            if (stepDef?.compensationTopic == null) {
                log.info("skipping compensation for step without compensation topic: step={}", step.stepName)
                continue
            }

            // Mark step as COMPENSATING
            stepRepository.updateStatusToCompensating(sagaId, step.stepName)

            // Enqueue compensation command to outbox
            val headers = objectMapper.writeValueAsString(
                mapOf(
                    "saga_id" to sagaId.toString(),
                    "step_name" to step.stepName,
                    "compensation" to "true"
                )
            )

            outboxRepository.insert(
                OutboxRow(
                    sagaId = sagaId,
                    topic = stepDef.compensationTopic,
                    key = sagaId.toString(),
                    payload = step.requestPayload,   // original payload for the compensation handler
                    headers = headers
                )
            )

            metrics.compensationStarted(sagaType, step.stepName)
            log.info("enqueued compensation: sagaId={}, step={}, topic={}",
                sagaId, step.stepName, stepDef.compensationTopic)
        }
    }
}
