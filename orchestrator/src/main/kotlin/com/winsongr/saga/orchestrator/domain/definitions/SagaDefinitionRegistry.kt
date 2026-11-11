package com.winsongr.saga.orchestrator.domain.definitions

import com.winsongr.saga.orchestrator.domain.ImmutableSagaDefinition
import org.springframework.stereotype.Component

/**
 * Registry of all saga definitions. Add new saga types by injecting more definition beans.
 */
@Component
class SagaDefinitionRegistry(
    orderSagaDefinition: OrderSagaDefinition
) {
    private val definitions: Map<String, ImmutableSagaDefinition> = mapOf(
        OrderSagaDefinition.TYPE to orderSagaDefinition.definition
    )

    fun get(type: String): ImmutableSagaDefinition =
        definitions[type] ?: throw IllegalArgumentException("Unknown saga type: $type")

    fun allTypes(): Set<String> = definitions.keys
}
