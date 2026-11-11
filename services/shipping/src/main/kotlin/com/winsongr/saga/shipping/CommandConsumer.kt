package com.winsongr.saga.shipping

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winsongr.saga.common.events.StepCommand
import com.winsongr.saga.common.events.StepReply
import com.winsongr.saga.common.events.StepReplyStatus
import com.winsongr.saga.common.headers.KafkaHeaders
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class CommandConsumer(
    private val shippingHandler: ShippingHandler,
    private val idempotencyService: IdempotencyService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(CommandConsumer::class.java)

    @KafkaListener(
        topics = ["saga.commands.shipping.book", "saga.commands.shipping.cancel"],
        groupId = "shipping-service"
    )
    fun onCommand(record: ConsumerRecord<String, String>) {
        val command: StepCommand = objectMapper.readValue(record.value())
        val isCompensation = record.topic().contains("cancel")

        log.info("received command: sagaId={}, step={}, topic={}", command.sagaId, command.stepName, record.topic())

        val reply = idempotencyService.executeIdempotent(command.sagaId, command.stepName) {
            if (isCompensation) {
                shippingHandler.cancel(command)
            } else {
                shippingHandler.book(command)
            }
        }

        publishReply(reply, record, isCompensation)
    }

    private fun publishReply(reply: StepReply, original: ConsumerRecord<String, String>, isCompensation: Boolean) {
        val record = ProducerRecord<String, String>(
            KafkaHeaders.REPLY_TOPIC,
            original.key(),
            objectMapper.writeValueAsString(reply)
        )
        record.headers().add("saga_id", reply.sagaId.toByteArray(Charsets.UTF_8))
        record.headers().add("step_name", reply.stepName.toByteArray(Charsets.UTF_8))
        if (isCompensation) {
            record.headers().add("compensation", "true".toByteArray(Charsets.UTF_8))
        }

        kafkaTemplate.send(record)
        log.info("published reply: sagaId={}, step={}, status={}", reply.sagaId, reply.stepName, reply.status)
    }
}
