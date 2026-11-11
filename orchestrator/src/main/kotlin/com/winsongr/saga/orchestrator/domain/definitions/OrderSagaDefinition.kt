package com.winsongr.saga.orchestrator.domain.definitions

import com.winsongr.saga.orchestrator.domain.ImmutableSagaDefinition
import com.winsongr.saga.orchestrator.domain.SagaDefinition
import org.springframework.stereotype.Component

/**
 * Order saga: reserve → charge → ship → notify.
 * Registered as a Spring bean so it's discoverable by the definition registry.
 */
@Component
class OrderSagaDefinition {

    companion object {
        const val TYPE = "order_saga"
    }

    val definition: ImmutableSagaDefinition = SagaDefinition(TYPE)
        .step(
            name = "reserve_inventory",
            commandTopic = "saga.commands.inventory.reserve",
            compensationTopic = "saga.commands.inventory.release",
            timeoutMs = 5_000
        )
        .step(
            name = "charge_payment",
            commandTopic = "saga.commands.payment.charge",
            compensationTopic = "saga.commands.payment.refund",
            timeoutMs = 10_000
        )
        .step(
            name = "book_shipment",
            commandTopic = "saga.commands.shipping.book",
            compensationTopic = "saga.commands.shipping.cancel",
            timeoutMs = 5_000
        )
        .step(
            name = "send_notification",
            commandTopic = "saga.commands.notification.send",
            compensationTopic = null,       // best-effort, no compensation
            timeoutMs = 5_000
        )
        .build()
}
