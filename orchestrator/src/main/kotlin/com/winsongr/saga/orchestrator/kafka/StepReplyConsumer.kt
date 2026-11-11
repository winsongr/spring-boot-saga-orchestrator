package com.winsongr.saga.orchestrator.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winsongr.saga.common.events.StepReply
import com.winsongr.saga.common.events.StepReplyStatus
import com.winsongr.saga.orchestrator.engine.SagaManager
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Consumes all step replies from the single `saga.replies` topic.
 *
 * Routes replies to the SagaManager based on:
 * - Whether this is a compensation reply (header "compensation" = "true")
 * - Whether the step succeeded or failed
 *
 * Errors are handled by Spring Kafka's DefaultErrorHandler → DLQ after 3 retries.
 */
@Component
class StepReplyConsumer(
    private val sagaManager: SagaManager,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(StepReplyConsumer::class.java)

    @KafkaListener(topics = ["saga.replies"], groupId = "saga-orchestrator")
    fun onReply(record: ConsumerRecord<String, String>) {
        val sagaIdHeader = extractHeader(record, "saga_id")
        MDC.put("saga_id", sagaIdHeader)

        try {
            val reply: StepReply = objectMapper.readValue(record.value())
            val sagaId = UUID.fromString(reply.sagaId)
            val isCompensation = extractHeader(record, "compensation") == "true"

            log.info("received reply: sagaId={}, step={}, status={}, compensation={}",
                sagaId, reply.stepName, reply.status, isCompensation)

            when {
                isCompensation && reply.status == StepReplyStatus.SUCCESS ->
                    sagaManager.handleCompensationSuccess(sagaId, reply.stepName)

                isCompensation && reply.status == StepReplyStatus.FAILED ->
                    sagaManager.handleCompensationFailure(sagaId, reply.stepName, reply.errorMessage)

                reply.status == StepReplyStatus.SUCCESS ->
                    sagaManager.handleStepSuccess(sagaId, reply.stepName, reply.payload)

                reply.status == StepReplyStatus.FAILED ->
                    sagaManager.handleStepFailure(sagaId, reply.stepName, reply.errorMessage)
            }
        } catch (e: Exception) {
            log.error("error processing reply: partition={}, offset={}", record.partition(), record.offset(), e)
            throw e  // let DefaultErrorHandler handle retry → DLQ
        } finally {
            MDC.remove("saga_id")
        }
    }

    private fun extractHeader(record: ConsumerRecord<String, String>, key: String): String? {
        return record.headers().lastHeader(key)?.value()?.let { String(it, Charsets.UTF_8) }
    }
}
