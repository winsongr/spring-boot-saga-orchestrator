package com.winsongr.saga.orchestrator.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.winsongr.saga.common.events.OrderItem
import com.winsongr.saga.common.events.ShippingAddress

data class CreateOrderRequest(
    @JsonProperty("customer_id") val customerId: String,
    @JsonProperty("items") val items: List<OrderItem>,
    @JsonProperty("payment_method") val paymentMethod: String,
    @JsonProperty("shipping_address") val shippingAddress: ShippingAddress
)
