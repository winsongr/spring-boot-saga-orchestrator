package com.winsongr.saga.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winsongr.saga.common.events.StepReply
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

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
