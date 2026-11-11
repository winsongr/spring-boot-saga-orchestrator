package com.winsongr.saga.orchestrator.domain

/**
 * Fluent DSL for declaring saga step sequences.
 *
 * Usage:
 *   SagaDefinition("order_saga")
 *       .step("reserve_inventory", commandTopic = "...", compensationTopic = "...", timeoutMs = 5000)
 *       .step("charge_payment", ...)
 *       .build()
 */
class SagaDefinition(private val type: String) {
    private val steps = mutableListOf<StepDef>()

    fun step(
        name: String,
        commandTopic: String,
        compensationTopic: String? = null,   // null = no compensation (e.g. notification)
        timeoutMs: Long = 30_000
    ): SagaDefinition {
        steps += StepDef(
            name = name,
            sequenceNo = steps.size,
            commandTopic = commandTopic,
            compensationTopic = compensationTopic,
            timeoutMs = timeoutMs
        )
        return this
    }

    fun build(): ImmutableSagaDefinition = ImmutableSagaDefinition(type, steps.toList())
}

data class StepDef(
    val name: String,
    val sequenceNo: Int,
    val commandTopic: String,
    val compensationTopic: String?,
    val timeoutMs: Long
)

/**
 * Immutable, thread-safe saga definition registered at startup.
 */
data class ImmutableSagaDefinition(
    val type: String,
    val steps: List<StepDef>
) {
    fun stepByName(name: String): StepDef? = steps.find { it.name == name }

    fun nextStep(afterSequence: Int): StepDef? =
        steps.find { it.sequenceNo == afterSequence + 1 }

    fun firstStep(): StepDef = steps.first()

    fun completedStepsReversed(completedNames: Set<String>): List<StepDef> =
        steps.filter { it.name in completedNames && it.compensationTopic != null }
            .sortedByDescending { it.sequenceNo }
}
