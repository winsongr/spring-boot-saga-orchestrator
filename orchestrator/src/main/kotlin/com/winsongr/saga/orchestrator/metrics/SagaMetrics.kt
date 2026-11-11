package com.winsongr.saga.orchestrator.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Micrometer instruments for saga observability.
 * All metrics follow the catalog from the PRD.
 */
@Component
class SagaMetrics(private val registry: MeterRegistry) {

    // --- Saga lifecycle ---

    fun sagaStarted(type: String) {
        Counter.builder("saga.started.total")
            .tag("type", type)
            .register(registry)
            .increment()
    }

    fun sagaCompleted(type: String, createdAt: Instant) {
        Counter.builder("saga.completed.total")
            .tag("type", type)
            .tag("result", "completed")
            .register(registry)
            .increment()

        Timer.builder("saga.duration.seconds")
            .tag("type", type)
            .tag("result", "completed")
            .register(registry)
            .record(Duration.between(createdAt, Instant.now()))
    }

    fun sagaFailed(type: String, createdAt: Instant) {
        Counter.builder("saga.completed.total")
            .tag("type", type)
            .tag("result", "failed")
            .register(registry)
            .increment()

        Timer.builder("saga.duration.seconds")
            .tag("type", type)
            .tag("result", "failed")
            .register(registry)
            .record(Duration.between(createdAt, Instant.now()))
    }

    fun sagaStuck(type: String) {
        Counter.builder("saga.stuck.total")
            .tag("type", type)
            .register(registry)
            .increment()
    }

    // --- Step lifecycle ---

    fun stepCompleted(type: String, stepName: String) {
        Counter.builder("saga.step.completed.total")
            .tag("type", type)
            .tag("step", stepName)
            .register(registry)
            .increment()
    }

    fun stepFailed(type: String, stepName: String) {
        Counter.builder("saga.step.failed.total")
            .tag("type", type)
            .tag("step", stepName)
            .register(registry)
            .increment()
    }

    fun stepTimedOut(stepName: String) {
        Counter.builder("saga.step.timeouts.total")
            .tag("step", stepName)
            .register(registry)
            .increment()
    }

    // --- Compensation ---

    fun compensationStarted(type: String, stepName: String) {
        Counter.builder("saga.compensations.total")
            .tag("type", type)
            .tag("step", stepName)
            .tag("result", "started")
            .register(registry)
            .increment()
    }

    fun compensationCompleted(type: String, stepName: String) {
        Counter.builder("saga.compensations.total")
            .tag("type", type)
            .tag("step", stepName)
            .tag("result", "completed")
            .register(registry)
            .increment()
    }

    // --- Outbox ---

    fun outboxPublishFailure() {
        Counter.builder("outbox.publish.failures.total")
            .register(registry)
            .increment()
    }

    fun outboxPublishDuration(durationMs: Double) {
        Timer.builder("outbox.publish.duration.seconds")
            .register(registry)
            .record(Duration.ofNanos((durationMs * 1_000_000).toLong()))
    }

    // --- DLQ ---

    fun dlqMessage() {
        Counter.builder("dlq.messages.total")
            .register(registry)
            .increment()
    }
}
