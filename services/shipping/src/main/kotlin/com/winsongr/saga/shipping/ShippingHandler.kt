package com.winsongr.saga.shipping

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.common.events.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ShippingHandler(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(ShippingHandler::class.java)

    fun book(command: StepCommand): StepReply {
        log.info("booking shipment: sagaId={}", command.sagaId)
        Thread.sleep(80)

        val result = ShipmentBookResult(
            shipmentId = UUID.randomUUID().toString(),
            trackingNumber = "TRK-${UUID.randomUUID().toString().take(8).uppercase()}"
        )

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = objectMapper.writeValueAsString(result)
        )
    }

    fun cancel(command: StepCommand): StepReply {
        log.info("cancelling shipment (compensation): sagaId={}", command.sagaId)
        Thread.sleep(30)

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = """{"cancelled": true}"""
        )
    }
}
