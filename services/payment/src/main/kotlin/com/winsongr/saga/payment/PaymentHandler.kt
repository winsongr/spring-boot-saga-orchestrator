package com.winsongr.saga.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.common.events.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Mock payment handler.
 * Has a configurable failure mode for testing compensation flows.
 *
 * Set PAYMENT_SHOULD_FAIL=true to make charge() return FAILED.
 * Set PAYMENT_SHOULD_TIMEOUT=true to make charge() hang (never reply).
 */
@Component
class PaymentHandler(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(PaymentHandler::class.java)

    @Value("\${payment.should-fail:false}")
    private var shouldFail: Boolean = false

    @Value("\${payment.should-timeout:false}")
    private var shouldTimeout: Boolean = false

    fun charge(command: StepCommand): StepReply {
        log.info("charging payment: sagaId={}", command.sagaId)

        // Simulate timeout — hang forever (deadline will fire)
        if (shouldTimeout) {
            log.info("simulating payment timeout: sagaId={}", command.sagaId)
            Thread.sleep(60_000)  // sleep longer than any timeout
        }

        // Simulate failure
        if (shouldFail) {
            log.info("simulating payment failure: sagaId={}", command.sagaId)
            return StepReply(
                sagaId = command.sagaId,
                stepName = command.stepName,
                status = StepReplyStatus.FAILED,
                errorMessage = "Insufficient funds"
            )
        }

        // Simulate work
        Thread.sleep(100)

        val result = PaymentChargeResult(
            transactionId = UUID.randomUUID().toString(),
            amountCharged = 99800
        )

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = objectMapper.writeValueAsString(result)
        )
    }

    fun refund(command: StepCommand): StepReply {
        log.info("refunding payment (compensation): sagaId={}", command.sagaId)
        Thread.sleep(50)

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = """{"refunded": true}"""
        )
    }
}
