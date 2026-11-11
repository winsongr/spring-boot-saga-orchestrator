package com.winsongr.saga.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.common.events.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Mock inventory handler.
 * Simulates reserve/release with a small delay.
 */
@Component
class InventoryHandler(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(InventoryHandler::class.java)

    fun reserve(command: StepCommand): StepReply {
        log.info("reserving inventory: sagaId={}", command.sagaId)

        // Simulate work
        Thread.sleep(50)

        val result = InventoryReserveResult(
            reservationId = UUID.randomUUID().toString(),
            reservedItems = listOf("WIDGET-1")
        )

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = objectMapper.writeValueAsString(result)
        )
    }

    fun release(command: StepCommand): StepReply {
        log.info("releasing inventory (compensation): sagaId={}", command.sagaId)

        // Simulate work
        Thread.sleep(30)

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = """{"released": true}"""
        )
    }
}
