package com.winsongr.saga.common.headers

/**
 * Kafka header constants shared between orchestrator and step services.
 * Every command/reply message carries these headers for correlation.
 */
object KafkaHeaders {
    const val SAGA_ID = "saga_id"
    const val STEP_NAME = "step_name"
    const val IDEMPOTENCY_KEY = "idempotency_key"
    const val REPLY_TOPIC = "saga.replies"
}
