package com.mparticle.kits

import com.braze.Braze
import com.braze.models.recommended.ecommerce.CartUpdatedAction
import com.braze.models.recommended.ecommerce.CartUpdatedEvent
import com.braze.models.recommended.ecommerce.CheckoutStartedEvent
import com.braze.models.recommended.ecommerce.OrderPlacedEvent
import com.braze.models.recommended.ecommerce.ProductViewedEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.identity.IdentityApi
import com.mparticle.kits.mocks.MockAppboyKit
import com.mparticle.kits.mocks.MockKitConfiguration
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

/**
 * Tests for the opt-in Braze recommended eCommerce event forwarding (useEcommerceRecommendedEvents).
 * Covers the mapping of each supported product action and the legacy fallback paths.
 */
class RecommendedEcommerceTests {
    private val kit: MockAppboyKit
        get() =
            MockAppboyKit().apply {
                configuration = MockKitConfiguration()
                useEcommerceRecommendedEvents = true
            }

    @Before
    fun setup() {
        Braze.clearPurchases()
        Braze.clearEvents()
        Braze.clearEcommerceEvents()
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()!!.Identity()).thenReturn(
            Mockito.mock(IdentityApi::class.java),
        )
    }

    private fun productWithUrls(): Product =
        Product
            .Builder("product name", "sku1", 4.5)
            .quantity(2.0)
            .brand("testBrand")
            .variant("testVariant")
            .position(3)
            .category("testCategory")
            .customAttributes(
                hashMapOf(
                    "image_url" to "https://example.com/image.jpg",
                    "product_url" to "https://example.com/product",
                    "customProductKey" to "customProductValue",
                ),
            ).build()

    @Test
    fun testAddToCartLogsCartUpdatedAddEvent() {
        kit.logEvent(
            CommerceEvent
                .Builder(Product.ADD_TO_CART, productWithUrls())
                .currency("USD")
                .customAttributes(hashMapOf("cart_id" to "cart-123"))
                .build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CartUpdatedEvent
        Assert.assertEquals("ecommerce.cart_updated", event.eventName)
        Assert.assertEquals(CartUpdatedAction.ADD, event.action)
        Assert.assertEquals("cart-123", event.cartId)
        Assert.assertEquals("USD", event.currency)
        Assert.assertEquals("android", event.source)
        Assert.assertEquals(1, event.products.size)
        val lineItem = event.products[0]
        Assert.assertEquals("sku1", lineItem.productId)
        Assert.assertEquals("product name", lineItem.productName)
        Assert.assertEquals("testVariant", lineItem.variantId)
        Assert.assertEquals(2L, lineItem.quantity)
        Assert.assertEquals("https://example.com/image.jpg", lineItem.imageUrl)
        Assert.assertEquals("https://example.com/product", lineItem.productUrl)
        // Product-level custom props are nested in metadata, never at the top level.
        Assert.assertEquals("testBrand", lineItem.metadata?.properties?.get("brand"))
        Assert.assertEquals("customProductValue", lineItem.metadata?.properties?.get("customProductKey"))
    }

    @Test
    fun testRemoveFromCartLogsCartUpdatedRemoveEvent() {
        kit.logEvent(
            CommerceEvent
                .Builder(Product.REMOVE_FROM_CART, productWithUrls())
                .currency("USD")
                .customAttributes(hashMapOf("cart_id" to "cart-123"))
                .build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CartUpdatedEvent
        Assert.assertEquals(CartUpdatedAction.REMOVE, event.action)
    }

    @Test
    fun testCheckoutLogsCheckoutStartedEvent() {
        kit.logEvent(
            CommerceEvent
                .Builder(Product.CHECKOUT, productWithUrls())
                .currency("USD")
                .customAttributes(hashMapOf("checkout_id" to "checkout-9", "cart_id" to "cart-123"))
                .build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CheckoutStartedEvent
        Assert.assertEquals("ecommerce.checkout_started", event.eventName)
        Assert.assertEquals("checkout-9", event.checkoutId)
        Assert.assertEquals("cart-123", event.cartId)
        Assert.assertEquals(9.0, event.totalValue, 0.001)
    }

    @Test
    fun testViewDetailLogsProductViewedEventPerProduct() {
        val secondProduct =
            Product
                .Builder("second", "sku2", 10.0)
                .quantity(1.0)
                .build()
        kit.logEvent(
            CommerceEvent
                .Builder(Product.DETAIL, productWithUrls())
                .addProduct(secondProduct)
                .currency("USD")
                .build(),
        )
        Assert.assertEquals(2, Braze.ecommerceEvents.size)
        val first = Braze.ecommerceEvents[0] as ProductViewedEvent
        Assert.assertEquals("ecommerce.product_viewed", first.eventName)
        Assert.assertEquals("sku1", first.productId)
        Assert.assertEquals("https://example.com/image.jpg", first.imageUrl)
        val second = Braze.ecommerceEvents[1] as ProductViewedEvent
        Assert.assertEquals("sku2", second.productId)
        // variantId falls back to sku when the product has no variant.
        Assert.assertEquals("sku2", second.variantId)
    }

    @Test
    fun testPurchaseLogsOrderPlacedEvent() {
        val transactionAttributes =
            TransactionAttributes("order-42")
                .setRevenue(99.0)
                .setTax(5.0)
                .setShipping(7.0)
                .setAffiliation("the affiliation")
        kit.logEvent(
            CommerceEvent
                .Builder(Product.PURCHASE, productWithUrls())
                .currency("USD")
                .transactionAttributes(transactionAttributes)
                .customAttributes(hashMapOf("total_discounts" to "3.5"))
                .build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as OrderPlacedEvent
        Assert.assertEquals("ecommerce.order_placed", event.eventName)
        Assert.assertEquals("order-42", event.orderId)
        Assert.assertEquals(99.0, event.totalValue, 0.001)
        Assert.assertEquals(3.5, event.totalDiscounts!!, 0.001)
        // tax/shipping/affiliation have no typed field; they are preserved in metadata.
        Assert.assertEquals(5.0, event.metadata?.properties?.get("tax"))
        Assert.assertEquals(7.0, event.metadata?.properties?.get("shipping"))
        Assert.assertEquals("the affiliation", event.metadata?.properties?.get("affiliation"))
    }

    @Test
    fun testRefundLogsOrderRefundedCustomEvent() {
        val transactionAttributes =
            TransactionAttributes("order-42").setRevenue(99.0)
        kit.logEvent(
            CommerceEvent
                .Builder(Product.REFUND, productWithUrls())
                .currency("USD")
                .transactionAttributes(transactionAttributes)
                .build(),
        )
        // Refund has no typed Braze event; it is forwarded as a custom event.
        Assert.assertTrue(Braze.ecommerceEvents.isEmpty())
        val refund = Braze.events["ecommerce.order_refunded"]
        Assert.assertNotNull(refund)
        Assert.assertEquals("order-42", refund?.properties?.get("order_id"))
        Assert.assertEquals(99.0, refund?.properties?.get("total_value"))
        Assert.assertEquals("android", refund?.properties?.get("source"))
        Assert.assertNotNull(refund?.properties?.get("products"))
    }

    @Test
    fun testToggleOffFallsBackToLegacyPurchase() {
        val kit =
            MockAppboyKit().apply {
                // useEcommerceRecommendedEvents defaults to false
                configuration = MockKitConfiguration()
            }
        kit.logEvent(
            CommerceEvent
                .Builder(Product.PURCHASE, productWithUrls())
                .currency("USD")
                .transactionAttributes(TransactionAttributes("order-42").setRevenue(99.0))
                .build(),
        )
        Assert.assertTrue(Braze.ecommerceEvents.isEmpty())
        Assert.assertEquals(1, Braze.purchases.size)
    }

    @Test
    fun testUnsupportedActionFallsBackToLegacy() {
        kit.logEvent(
            CommerceEvent
                .Builder(Product.ADD_TO_WISHLIST, productWithUrls())
                .currency("USD")
                .build(),
        )
        // add_to_wishlist is not a recommended eCommerce event; it must fall back to legacy forwarding.
        Assert.assertTrue(Braze.ecommerceEvents.isEmpty())
        Assert.assertTrue(Braze.events.isNotEmpty())
    }

    @Test
    fun testNoProductsFallsBackToLegacy() {
        val promotion = Promotion().setId("promo1").setName("promo name")
        kit.logEvent(
            CommerceEvent
                .Builder(Promotion.VIEW, promotion)
                .build(),
        )
        // Promotion events carry no products, so the recommended path must defer to legacy forwarding.
        Assert.assertTrue(Braze.ecommerceEvents.isEmpty())
    }
}
