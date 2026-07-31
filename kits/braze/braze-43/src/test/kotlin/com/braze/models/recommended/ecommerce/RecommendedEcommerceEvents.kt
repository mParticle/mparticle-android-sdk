package com.braze.models.recommended.ecommerce

import com.braze.models.outgoing.BrazeProperties

/**
 * Test doubles for Braze's recommended eCommerce event API (com.braze.models.recommended.ecommerce).
 * These mirror the real SDK's public constructors so the kit compiles and its forwarding behavior
 * can be asserted in unit tests without the Braze AAR on the classpath.
 */
enum class CartUpdatedAction(
    val wireValue: String,
) {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace"),
}

abstract class EcommerceEvent {
    abstract val eventName: String
}

data class EcommerceProduct(
    val productId: String,
    val productName: String,
    val variantId: String,
    val price: Double,
    val quantity: Long,
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val metadata: BrazeProperties? = null,
)

class CartUpdatedEvent(
    val cartId: String,
    val currency: String,
    val source: String,
    val totalValue: Double? = null,
    val products: List<EcommerceProduct>,
    val metadata: BrazeProperties? = null,
    val action: CartUpdatedAction = CartUpdatedAction.REPLACE,
) : EcommerceEvent() {
    override val eventName: String = "ecommerce.cart_updated"
}

class CheckoutStartedEvent(
    val checkoutId: String,
    val currency: String,
    val source: String,
    val totalValue: Double,
    val products: List<EcommerceProduct>,
    val cartId: String? = null,
    val metadata: BrazeProperties? = null,
) : EcommerceEvent() {
    override val eventName: String = "ecommerce.checkout_started"
}

class OrderPlacedEvent(
    val orderId: String,
    val currency: String,
    val source: String,
    val totalValue: Double,
    val products: List<EcommerceProduct>,
    val cartId: String? = null,
    val totalDiscounts: Double? = null,
    val discounts: List<Any>? = null,
    val metadata: BrazeProperties? = null,
) : EcommerceEvent() {
    override val eventName: String = "ecommerce.order_placed"
}

class ProductViewedEvent(
    val productId: String,
    val productName: String,
    val variantId: String,
    val price: Double,
    val currency: String,
    val source: String,
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val metadata: BrazeProperties? = null,
) : EcommerceEvent() {
    override val eventName: String = "ecommerce.product_viewed"
}
