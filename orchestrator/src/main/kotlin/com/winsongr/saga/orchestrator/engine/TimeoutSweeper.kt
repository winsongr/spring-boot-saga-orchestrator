package com.winsongr.saga.orchestrator.engine

import com.winsongr.saga.orchestrator.domain.StepStatus
import com.winsongr.saga.orchestrator.metrics.SagaMetrics
import com.winsongr.saga.orchestrator.persistence.SagaStepRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Background sweeper that detects steps past their deadline.
 *
 * Runs every 1s (configurable). For each timed-out step:
 * 1. Marks it TIMED_OUT
 * 2. Triggers compensation via SagaManager.handleStepFailure
 *
 * Uses the partial index idx_saga_steps_running_deadline for O(log N) lookups.
 */
@Component
class TimeoutSweeper(
    private val stepRepository: SagaStepRepository,
    private val sagaManager: SagaManager,
    private val metrics: SagaMetrics
) {
    private val log = LoggerFactory.getLogger(TimeoutSweeper::class.java)

    @Value("\${saga.timeout-sweeper.batch-size:100}")
    private var batchSize: Int = 100

    @Scheduled(fixedDelayString = "\${saga.timeout-sweeper.interval-ms:1000}")
    fun sweep() {
        val timedOut = stepRepository.fetchTimedOut(batchSize)
        if (timedOut.isEmpty()) return

        log.info("timeout sweep found {} steps past deadline", timedOut.size)

        timedOut.forEach { step ->
            try {
                stepRepository.updateStatus(step.sagaId, step.stepName, StepStatus.TIMED_OUT)
                sagaManager.handleStepFailure(step.sagaId, step.stepName, "deadline exceeded")
                metrics.stepTimedOut(step.stepName)
            } catch (e: Exception) {
                log.error("error processing timed-out step: sagaId={}, step={}",
                    step.sagaId, step.stepName, e)
            }
        }
    }
}
