package com.braze.models.recommended.ecommerce

import com.braze.models.outgoing.BrazeProperties

/**
 * Test doubles for Braze's recommended eCommerce event API (com.braze.models.recommended.ecommerce).
 *
 * The kit's production code is compiled against the real Braze AAR, so these doubles must declare
 * constructor parameters in the same order, with the same types and the same defaults as Braze 43.
 * Any divergence changes the generated constructor descriptor and fails at runtime with
 * NoSuchMethodError rather than at compile time.
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
    val subtotalValue: Double? = null,
    val tax: Double? = null,
    val shipping: Double? = null,
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
    val subtotalValue: Double? = null,
    val tax: Double? = null,
    val shipping: Double? = null,
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
    val subtotalValue: Double? = null,
    val tax: Double? = null,
    val shipping: Double? = null,
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
    val type: List<String>? = null,
) : EcommerceEvent() {
    override val eventName: String = "ecommerce.product_viewed"
}
