package com.winsongr.saga.orchestrator.domain

import java.time.Instant
import java.util.UUID

enum class SagaStatus {
    PENDING,        // created, not yet advanced
    RUNNING,        // at least one step in flight
    COMPENSATING,   // forward step failed, running compensations
    COMPLETED,      // all forward steps succeeded
    FAILED,         // compensations completed, saga ended in failure
    STUCK           // compensation failed, manual intervention required
}

data class Saga(
    val id: UUID,
    val type: String,
    val status: SagaStatus,
    val payload: String,          // JSONB — the initial input
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
)
