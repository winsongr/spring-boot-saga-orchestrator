package com.winsongr.saga.common.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Reply sent from a step service back to the orchestrator.
 */
data class StepReply(
    @JsonProperty("saga_id") val sagaId: String,
    @JsonProperty("step_name") val stepName: String,
    @JsonProperty("status") val status: StepReplyStatus,
    @JsonProperty("payload") val payload: String? = null,     // step-specific result data
    @JsonProperty("error_message") val errorMessage: String? = null
)

enum class StepReplyStatus {
    SUCCESS,
    FAILED
}
