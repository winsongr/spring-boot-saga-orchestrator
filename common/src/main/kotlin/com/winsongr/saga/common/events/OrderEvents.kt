package com.winsongr.saga.common.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Order-specific command/reply payload types.
 * These are serialized into the `payload` field of StepCommand/StepReply as JSON strings.
 */

data class OrderPayload(
    @JsonProperty("customer_id") val customerId: String,
    @JsonProperty("items") val items: List<OrderItem>,
    @JsonProperty("payment_method") val paymentMethod: String,
    @JsonProperty("shipping_address") val shippingAddress: ShippingAddress
)

data class OrderItem(
    @JsonProperty("sku") val sku: String,
    @JsonProperty("qty") val qty: Int,
    @JsonProperty("unit_price") val unitPrice: Long   // in smallest currency unit (paise/cents)
)

data class ShippingAddress(
    @JsonProperty("line1") val line1: String,
    @JsonProperty("line2") val line2: String? = null,
    @JsonProperty("city") val city: String,
    @JsonProperty("state") val state: String,
    @JsonProperty("postal_code") val postalCode: String,
    @JsonProperty("country") val country: String = "IN"
)

// Reply payloads from step services
data class InventoryReserveResult(
    @JsonProperty("reservation_id") val reservationId: String,
    @JsonProperty("reserved_items") val reservedItems: List<String>
)

data class PaymentChargeResult(
    @JsonProperty("transaction_id") val transactionId: String,
    @JsonProperty("amount_charged") val amountCharged: Long
)

data class ShipmentBookResult(
    @JsonProperty("shipment_id") val shipmentId: String,
    @JsonProperty("tracking_number") val trackingNumber: String
)

data class NotificationSendResult(
    @JsonProperty("notification_id") val notificationId: String,
    @JsonProperty("channel") val channel: String = "email"
)
