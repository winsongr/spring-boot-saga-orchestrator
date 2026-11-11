package com.winsongr.saga.orchestrator.domain

import java.time.Instant
import java.util.UUID

enum class StepStatus {
    PENDING,                // not yet started
    RUNNING,                // command published, awaiting reply
    COMPLETED,              // forward step succeeded
    FAILED,                 // forward step replied failure
    TIMED_OUT,              // no reply by deadline
    COMPENSATING,           // compensation in flight
    COMPENSATED,            // compensation succeeded
    COMPENSATION_FAILED     // compensation replied failure → saga STUCK
}

data class SagaStep(
    val sagaId: UUID,
    val stepName: String,
    val sequenceNo: Int,
    val status: StepStatus,
    val requestPayload: String,
    val responsePayload: String? = null,
    val errorMessage: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val deadlineAt: Instant? = null,
    val attemptCount: Int = 0
)
