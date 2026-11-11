package com.winsongr.saga.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winsongr.saga.common.events.StepReply
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis-backed idempotency service.
 *
 * First execution stores its response in Redis keyed by saga:idem:{sagaId}:{stepName}.
 * Subsequent executions return the cached response without re-executing.
 *
 * Caveat: GET-then-SET is not atomic. For payment-grade idempotency,
 * the handler itself must be idempotent at the DB level. Redis is the fast path.
 */
@Component
class IdempotencyService(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(IdempotencyService::class.java)

    fun executeIdempotent(
        sagaId: String,
        stepName: String,
        handler: () -> StepReply
    ): StepReply {
        val key = "saga:idem:$sagaId:$stepName"
        val cached = redis.opsForValue().get(key)
        if (cached != null) {
            log.info("idempotent cache hit: key={}", key)
            return objectMapper.readValue(cached, StepReply::class.java)
        }

        val result = handler()
        redis.opsForValue().set(key, objectMapper.writeValueAsString(result), Duration.ofHours(24))
        return result
    }
}
