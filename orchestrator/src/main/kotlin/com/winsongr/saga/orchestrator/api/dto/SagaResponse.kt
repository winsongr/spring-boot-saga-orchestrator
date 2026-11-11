package com.winsongr.saga.orchestrator.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SagaResponse(
    @JsonProperty("saga_id") val sagaId: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("steps") val steps: List<StepResponse>? = null,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("completed_at") val completedAt: Instant? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StepResponse(
    @JsonProperty("name") val name: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("started_at") val startedAt: Instant? = null,
    @JsonProperty("completed_at") val completedAt: Instant? = null,
    @JsonProperty("deadline_at") val deadlineAt: Instant? = null,
    @JsonProperty("error_message") val errorMessage: String? = null
)
