package com.winsongr.saga.orchestrator.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.winsongr.saga.common.events.OrderPayload
import com.winsongr.saga.orchestrator.api.dto.CreateOrderRequest
import com.winsongr.saga.orchestrator.api.dto.SagaResponse
import com.winsongr.saga.orchestrator.domain.definitions.OrderSagaDefinition
import com.winsongr.saga.orchestrator.engine.SagaManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/v1/orders")
class OrderController(
    private val sagaManager: SagaManager,
    private val objectMapper: ObjectMapper
) {

    /**
     * POST /v1/orders
     *
     * Creates an order saga. Returns 202 Accepted with a Location header
     * pointing to the saga status endpoint.
     *
     * Why 202 not 201: the order isn't created yet — a saga to create it
     * has been enqueued. The client can poll the Location to track progress.
     */
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Map<String, String>> {
        val payload = objectMapper.writeValueAsString(
            OrderPayload(
                customerId = request.customerId,
                items = request.items,
                paymentMethod = request.paymentMethod,
                shippingAddress = request.shippingAddress
            )
        )

        val sagaId = sagaManager.startSaga(OrderSagaDefinition.TYPE, payload)

        return ResponseEntity
            .accepted()
            .location(URI.create("/v1/sagas/$sagaId"))
            .body(mapOf(
                "saga_id" to sagaId.toString(),
                "status" to "PENDING"
            ))
    }
}
