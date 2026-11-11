package com.winsongr.saga.orchestrator.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.stereotype.Component
import org.springframework.util.backoff.FixedBackOff

/**
 * DLQ error handler configuration.
 *
 * After 3 failed processing attempts, the message is forwarded
 * to saga.replies.dlq with all original headers preserved.
 */
@Component
class DlqHandler(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(DlqHandler::class.java)

    /**
     * Creates a DefaultErrorHandler that retries 3 times with 1s backoff,
     * then publishes to the DLQ topic.
     */
    fun createErrorHandler(): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition("saga.replies.dlq", record.partition() % 3)
        }

        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3L)).also {
            it.setRetryListeners({ record, ex, deliveryAttempt ->
                log.warn("retry attempt {} for record: topic={}, partition={}, offset={}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(), ex)
            })
        }
    }
}
