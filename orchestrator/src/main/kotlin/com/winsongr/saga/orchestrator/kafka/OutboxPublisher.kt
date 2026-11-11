package com.winsongr.saga.orchestrator.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winsongr.saga.orchestrator.metrics.SagaMetrics
import com.winsongr.saga.orchestrator.persistence.OutboxRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Pull-based outbox publisher.
 *
 * Every 50ms, reads up to 100 unsent rows from the outbox table,
 * publishes each to Kafka, and marks it sent on ack.
 *
 * On failure, increments the attempt counter — the row stays unsent
 * and will be retried on the next tick.
 */
@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val metrics: SagaMetrics,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OutboxPublisher::class.java)

    @Value("\${saga.outbox.batch-size:100}")
    private var batchSize: Int = 100

    @Scheduled(fixedDelayString = "\${saga.outbox.publish-interval-ms:50}")
    fun publishBatch() {
        val startTime = System.nanoTime()
        val batch = outboxRepository.fetchUnsent(batchSize)
        if (batch.isEmpty()) return

        batch.forEach { row ->
            try {
                val headers: Map<String, String> = objectMapper.readValue(row.headers)
                val record = ProducerRecord(row.topic, row.key, row.payload)
                headers.forEach { (k, v) ->
                    record.headers().add(k, v.toByteArray(Charsets.UTF_8))
                }

                kafkaTemplate.send(record).get(2, TimeUnit.SECONDS)
                outboxRepository.markSent(row.id)
            } catch (e: Exception) {
                outboxRepository.incrementAttempts(row.id)
                metrics.outboxPublishFailure()
                log.warn("outbox publish failed, will retry: outbox_id={}, topic={}, attempts={}",
                    row.id, row.topic, row.attempts + 1, e)
            }
        }

        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        metrics.outboxPublishDuration(durationMs)
        log.debug("outbox batch published: count={}, durationMs={}", batch.size, durationMs)
    }
}
