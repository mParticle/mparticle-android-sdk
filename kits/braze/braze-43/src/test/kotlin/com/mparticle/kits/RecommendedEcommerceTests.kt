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
import com.mparticle.kits.mocks.MockBrazeKit
import com.mparticle.kits.mocks.MockKitConfiguration
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

/**
 * Tests for the opt-in Braze recommended eCommerce event forwarding (useEcommerceRecommendedEvents).
 * Covers the mapping of each supported product action and the legacy fallback paths.
 */
class RecommendedEcommerceTests {
    private fun eventAttributeMapping(attributeName: String): String =
        "[{\"jsmap\":null,\"map\":null,\"maptype\":\"EventAttributeClass.Name\",\"value\":\"$attributeName\"}]"

    private fun productAttributeMapping(attributeName: String): String =
        "[{\"jsmap\":null,\"map\":null,\"maptype\":\"ProductAttributeSelector.Name\",\"value\":\"$attributeName\"}]"

    private fun kitWithMappings(vararg settings: Pair<String, String>): MockBrazeKit {
        val settingsMap = hashMapOf(*settings)
        return MockBrazeKit().apply {
            configuration =
                KitConfiguration.createKitConfiguration(
                    JSONObject().put("as", JSONObject(settingsMap as Map<*, *>)),
                )
            useEcommerceRecommendedEvents = true
        }
    }

    /** Default mappings matching common attribute names used by most tests. */
    private val kit: MockBrazeKit
        get() =
            kitWithMappings(
                BrazeKit.CART_ID_ATTRIBUTE_MAPPING to eventAttributeMapping("cart_id"),
                BrazeKit.CHECKOUT_ID_ATTRIBUTE_MAPPING to eventAttributeMapping("checkout_id"),
                BrazeKit.SUBTOTAL_VALUE_ATTRIBUTE_MAPPING to eventAttributeMapping("subtotal_value"),
                BrazeKit.IMAGE_URL_ATTRIBUTE_MAPPING to productAttributeMapping("image_url"),
                BrazeKit.PRODUCT_URL_ATTRIBUTE_MAPPING to productAttributeMapping("product_url"),
            )

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

    private fun productWithUrls(
        imageKey: String = "image_url",
        productUrlKey: String = "product_url",
    ): Product =
        Product
            .Builder("product name", "sku1", 4.5)
            .quantity(2.0)
            .brand("testBrand")
            .variant("testVariant")
            .position(3)
            .category("testCategory")
            .customAttributes(
                hashMapOf(
                    imageKey to "https://example.com/image.jpg",
                    productUrlKey to "https://example.com/product",
                    "customProductKey" to "customProductValue",
                ),
            ).build()

    @Test
    fun testAddToCartLogsCartUpdatedAddEvent() {
        val transactionAttributes =
            TransactionAttributes()
                .setTax(5.0)
                .setShipping(7.0)
        kit.logEvent(
            CommerceEvent
                .Builder(Product.ADD_TO_CART, productWithUrls())
                .currency("USD")
                .transactionAttributes(transactionAttributes)
                .customAttributes(
                    hashMapOf(
                        "cart_id" to "cart-123",
                        "subtotal_value" to "80.0",
                    ),
                ).build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CartUpdatedEvent
        Assert.assertEquals("ecommerce.cart_updated", event.eventName)
        Assert.assertEquals(CartUpdatedAction.ADD, event.action)
        Assert.assertEquals("cart-123", event.cartId)
        Assert.assertEquals("USD", event.currency)
        Assert.assertEquals("android", event.source)
        Assert.assertEquals(80.0, event.subtotalValue!!, 0.001)
        Assert.assertEquals(5.0, event.tax!!, 0.001)
        Assert.assertEquals(7.0, event.shipping!!, 0.001)
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
        // Promoted typed fields must not be duplicated in metadata.
        Assert.assertNull(event.metadata?.properties?.get("cart_id"))
        Assert.assertNull(event.metadata?.properties?.get("subtotal_value"))
        Assert.assertNull(event.metadata?.properties?.get("tax"))
        Assert.assertNull(event.metadata?.properties?.get("shipping"))
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
        val transactionAttributes =
            TransactionAttributes()
                .setTax(2.5)
                .setShipping(3.5)
        kit.logEvent(
            CommerceEvent
                .Builder(Product.CHECKOUT, productWithUrls())
                .currency("USD")
                .transactionAttributes(transactionAttributes)
                .customAttributes(
                    hashMapOf(
                        "checkout_id" to "checkout-9",
                        "cart_id" to "cart-123",
                        "subtotal_value" to "9.0",
                    ),
                ).build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CheckoutStartedEvent
        Assert.assertEquals("ecommerce.checkout_started", event.eventName)
        Assert.assertEquals("checkout-9", event.checkoutId)
        Assert.assertEquals("cart-123", event.cartId)
        // CommerceEvent derives revenue as products (4.5 x 2) + tax + shipping when revenue is unset.
        Assert.assertEquals(15.0, event.totalValue, 0.001)
        Assert.assertEquals(9.0, event.subtotalValue!!, 0.001)
        Assert.assertEquals(2.5, event.tax!!, 0.001)
        Assert.assertEquals(3.5, event.shipping!!, 0.001)
        Assert.assertNull(event.metadata?.properties?.get("subtotal_value"))
        Assert.assertNull(event.metadata?.properties?.get("tax"))
        Assert.assertNull(event.metadata?.properties?.get("shipping"))
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
                .customAttributes(
                    hashMapOf(
                        "total_discounts" to "3.5",
                        "subtotal_value" to "91.0",
                    ),
                ).build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as OrderPlacedEvent
        Assert.assertEquals("ecommerce.order_placed", event.eventName)
        Assert.assertEquals("order-42", event.orderId)
        Assert.assertEquals(99.0, event.totalValue, 0.001)
        Assert.assertEquals(3.5, event.totalDiscounts!!, 0.001)
        Assert.assertEquals(91.0, event.subtotalValue!!, 0.001)
        Assert.assertEquals(5.0, event.tax!!, 0.001)
        Assert.assertEquals(7.0, event.shipping!!, 0.001)
        // affiliation has no typed field; it is preserved in metadata.
        Assert.assertEquals("the affiliation", event.metadata?.properties?.get("affiliation"))
        // Promoted typed fields must not be duplicated in metadata.
        Assert.assertNull(event.metadata?.properties?.get("total_discounts"))
        Assert.assertNull(event.metadata?.properties?.get("subtotal_value"))
        Assert.assertNull(event.metadata?.properties?.get("tax"))
        Assert.assertNull(event.metadata?.properties?.get("shipping"))
    }

    @Test
    fun testRefundLogsOrderRefundedCustomEvent() {
        val transactionAttributes =
            TransactionAttributes("order-42")
                .setRevenue(99.0)
                .setTax(5.0)
                .setShipping(7.0)
        kit.logEvent(
            CommerceEvent
                .Builder(Product.REFUND, productWithUrls())
                .currency("USD")
                .transactionAttributes(transactionAttributes)
                .customAttributes(hashMapOf("subtotal_value" to "87.0"))
                .build(),
        )
        // Refund has no typed Braze event; it is forwarded as a custom event.
        Assert.assertTrue(Braze.ecommerceEvents.isEmpty())
        val refund = Braze.events["ecommerce.order_refunded"]
        Assert.assertNotNull(refund)
        Assert.assertEquals("order-42", refund?.properties?.get("order_id"))
        Assert.assertEquals(99.0, refund?.properties?.get("total_value"))
        Assert.assertEquals("android", refund?.properties?.get("source"))
        Assert.assertEquals(87.0, refund?.properties?.get("subtotal_value"))
        Assert.assertEquals(5.0, refund?.properties?.get("tax"))
        Assert.assertEquals(7.0, refund?.properties?.get("shipping"))
        Assert.assertNotNull(refund?.properties?.get("products"))
    }

    @Test
    fun testMappedCustomAttributesAndConfiguredSource() {
        val mappedKit =
            kitWithMappings(
                BrazeKit.CART_ID_ATTRIBUTE_MAPPING to eventAttributeMapping("mapped_cart_id"),
                BrazeKit.CHECKOUT_ID_ATTRIBUTE_MAPPING to eventAttributeMapping("mapped_checkout_id"),
                BrazeKit.SUBTOTAL_VALUE_ATTRIBUTE_MAPPING to eventAttributeMapping("order_subtotal"),
                BrazeKit.IMAGE_URL_ATTRIBUTE_MAPPING to productAttributeMapping("mapped_image_url"),
                BrazeKit.PRODUCT_URL_ATTRIBUTE_MAPPING to productAttributeMapping("mapped_product_url"),
                BrazeKit.ECOMMERCE_SOURCE_SETTING to "mobile_app",
            )
        val product =
            Product
                .Builder("product name", "sku1", 13.0)
                .quantity(1.0)
                .customAttributes(
                    hashMapOf(
                        "mapped_image_url" to "https://example.com/mapped-image.png",
                        "mapped_product_url" to "https://example.com/mapped-product",
                        "image_url" to "https://example.com/unmapped-image.png",
                    ),
                ).build()
        mappedKit.logEvent(
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .currency("USD")
                .customAttributes(
                    hashMapOf(
                        "mapped_cart_id" to "mapped-cart-1",
                        "mapped_checkout_id" to "mapped-checkout-1",
                        "order_subtotal" to "13.0",
                    ),
                ).build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CheckoutStartedEvent
        Assert.assertEquals("mapped-cart-1", event.cartId)
        Assert.assertEquals("mapped-checkout-1", event.checkoutId)
        Assert.assertEquals("mobile_app", event.source)
        Assert.assertEquals(13.0, event.subtotalValue!!, 0.001)
        Assert.assertEquals("https://example.com/mapped-image.png", event.products[0].imageUrl)
        Assert.assertEquals("https://example.com/mapped-product", event.products[0].productUrl)
        // Mapped attribute names must not be duplicated in metadata.
        Assert.assertNull(event.metadata?.properties?.get("mapped_cart_id"))
        Assert.assertNull(event.metadata?.properties?.get("mapped_checkout_id"))
        Assert.assertNull(event.metadata?.properties?.get("order_subtotal"))
        // Unmapped image_url stays in product metadata (not promoted).
        Assert.assertEquals(
            "https://example.com/unmapped-image.png",
            event.products[0].metadata?.properties?.get("image_url"),
        )
    }

    @Test
    fun testMissingUrlMappingsYieldNullUrls() {
        val kitWithoutUrlMappings =
            kitWithMappings(
                BrazeKit.CART_ID_ATTRIBUTE_MAPPING to eventAttributeMapping("cart_id"),
            )
        kitWithoutUrlMappings.logEvent(
            CommerceEvent
                .Builder(Product.ADD_TO_CART, productWithUrls())
                .currency("USD")
                .customAttributes(hashMapOf("cart_id" to "cart-123"))
                .build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CartUpdatedEvent
        Assert.assertEquals("cart-123", event.cartId)
        Assert.assertNull(event.products[0].imageUrl)
        Assert.assertNull(event.products[0].productUrl)
        // Without URL mappings, image_url/product_url remain in product metadata.
        Assert.assertEquals(
            "https://example.com/image.jpg",
            event.products[0].metadata?.properties?.get("image_url"),
        )
        Assert.assertEquals(
            "https://example.com/product",
            event.products[0].metadata?.properties?.get("product_url"),
        )
    }

    @Test
    fun testPlainStringMappingConfigIsAccepted() {
        val plainKit =
            kitWithMappings(
                BrazeKit.CART_ID_ATTRIBUTE_MAPPING to "cart_id",
                BrazeKit.SUBTOTAL_VALUE_ATTRIBUTE_MAPPING to "cart_subtotal",
            )
        plainKit.logEvent(
            CommerceEvent
                .Builder(Product.ADD_TO_CART, productWithUrls())
                .currency("USD")
                .customAttributes(
                    hashMapOf(
                        "cart_id" to "cart-plain",
                        "cart_subtotal" to "26.0",
                    ),
                ).build(),
        )
        Assert.assertEquals(1, Braze.ecommerceEvents.size)
        val event = Braze.ecommerceEvents[0] as CartUpdatedEvent
        Assert.assertEquals("cart-plain", event.cartId)
        Assert.assertEquals(26.0, event.subtotalValue!!, 0.001)
    }

    @Test
    fun testToggleOffFallsBackToLegacyPurchase() {
        val kit =
            MockBrazeKit().apply {
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
