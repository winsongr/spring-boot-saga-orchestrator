package com.winsongr.saga.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.common.events.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationHandler(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(NotificationHandler::class.java)

    fun send(command: StepCommand): StepReply {
        log.info("sending notification: sagaId={}", command.sagaId)
        Thread.sleep(30)

        val result = NotificationSendResult(
            notificationId = UUID.randomUUID().toString(),
            channel = "email"
        )

        return StepReply(
            sagaId = command.sagaId,
            stepName = command.stepName,
            status = StepReplyStatus.SUCCESS,
            payload = objectMapper.writeValueAsString(result)
        )
    }
}
