package com.winsongr.saga.common.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Base command sent from orchestrator to a step service.
 * Serialized as JSON into Kafka message value.
 */
data class StepCommand(
    @JsonProperty("saga_id") val sagaId: String,
    @JsonProperty("step_name") val stepName: String,
    @JsonProperty("payload") val payload: String   // JSON string — step-specific data
)
