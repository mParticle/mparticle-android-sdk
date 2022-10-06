package com.mparticle.commerce

import com.mparticle.MParticle
import com.mparticle.internal.Logger
import com.mparticle.internal.Logger.DefaultLogHandler
import com.mparticle.mock.utils.RandomUtils
import com.mparticle.testutils.AndroidUtils
import org.junit.Assert
import org.junit.Test


class CommerceEventTest {
    @Test
    @Throws(Exception::class)
    fun testScreen() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val event =
            CommerceEvent.Builder(Product.ADD_TO_CART, product).screen("some screen name").build()
        Assert.assertEquals("some screen name", event.screen)
    }

    @Test
    @Throws(Exception::class)
    fun testAddProduct() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val product2 = Product.Builder("name 2", "sku 2", 0.0).build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).addProduct(product2).build()
        Assert.assertEquals("name 2", event.products?.get(1)?.name)
        val errorMessage = AndroidUtils.Mutable<String?>(null)
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable?, messages: String) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages
                }
            }
        })
        CommerceEvent.Builder(Promotion.VIEW, Promotion().setId("whatever")).addProduct(product2)
            .build()
        Assert.assertNotNull("Should have logged Error", errorMessage.value)
        errorMessage.value = null
        CommerceEvent.Builder(Impression("name", product)).addProduct(product2).build()
        Assert.assertNotNull("Should have loggedError", errorMessage.value)
        errorMessage.value = null
    }

    @Test
    @Throws(Exception::class)
    fun testTransactionAttributes() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product)
            .transactionAttributes(TransactionAttributes().setId("the id")).build()
        Assert.assertEquals("the id", event.transactionAttributes?.id)
        val errorMessage = AndroidUtils.Mutable<String?>(null)
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable?, messages: String) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages
                }
            }
        })
        CommerceEvent.Builder(Product.PURCHASE, product).build()
        Assert.assertNotNull("Should have logged Error", errorMessage.value)
    }

    @Test
    @Throws(Exception::class)
    fun testCurrency() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).currency("test").build()
        Assert.assertEquals("test", event.currency)
    }

    @Test
    @Throws(Exception::class)
    fun testNonInteraction() {
        val product = Product.Builder("name", "sku", 0.0).build()
        var event = CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(true).build()
        event.nonInteraction?.let { Assert.assertTrue(it) }
        event = CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(false).build()
        event.nonInteraction?.let { Assert.assertFalse(it) }
    }

    @Test
    @Throws(Exception::class)
    fun testCustomAttributes() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val attributes = HashMap<String, String>()
        attributes["cool attribute key"] = "cool attribute value"
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).customAttributes(attributes)
            .nonInteraction(true).build()
        Assert.assertEquals(
            "cool attribute value",
            event.customAttributeStrings?.get("cool attribute key")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testAddPromotion() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val event:CommerceEvent =
            CommerceEvent.Builder("promo", Promotion().setId("promo id")).nonInteraction(true)
                .build()
        Assert.assertEquals("promo id", event.promotions?.get(0)?.id)
        val errorMessage = AndroidUtils.Mutable<String?>(null)
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable?, messages: String) {
                if (priority == MParticle.LogLevel.ERROR) {
                    errorMessage.value = messages
                }
            }
        })
        CommerceEvent.Builder(Product.ADD_TO_CART, product).nonInteraction(true)
            .addPromotion(Promotion().setId("promo id")).build()
        Assert.assertNotNull("Should have logged Error", errorMessage.value)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckoutStep() {
        val event = CommerceEvent.Builder("promo", Promotion().setId("promo id")).checkoutStep(100)
            .nonInteraction(true).build()
        Assert.assertEquals(100, event.checkoutStep)
    }

    @Test
    @Throws(Exception::class)
    fun testAddImpression() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val event = CommerceEvent.Builder(Impression("name", product))
            .addImpression(Impression("name 2", product)).nonInteraction(true).build()
        Assert.assertEquals("name 2", event.impressions?.get(1)?.listName)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckoutOptions() {
        val event = CommerceEvent.Builder("promo", Promotion().setId("promo id")).checkoutStep(100)
            .checkoutOptions("some checkout options").nonInteraction(true).build()
        Assert.assertEquals("some checkout options", event.checkoutOptions)
    }

    @Test
    @Throws(Exception::class)
    fun testProductListName() {
        val event = CommerceEvent.Builder("promo", Promotion().setId("promo id"))
            .productListName("the list name").nonInteraction(true).build()
        Assert.assertEquals("the list name", event.productListName)
    }

    @Test
    @Throws(Exception::class)
    fun testProductListSource() {
        val event = CommerceEvent.Builder("promo", Promotion().setId("promo id"))
            .productListSource("the list source").nonInteraction(true).build()
        Assert.assertEquals("the list source", event.productListSource)
    }

    @Test
    @Throws(Exception::class)
    fun testCustomFlags() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val builder = CommerceEvent.Builder(Product.CLICK, product)
        var event = builder.build()
        Assert.assertNull(event.customFlags)

        val attributes = RandomUtils.getInstance().getRandomCustomFlags(RandomUtils.getInstance().randomInt(1, 10))
        for (attribute in attributes.entries) {
            for (value in attribute.value) {
                builder.addCustomFlag(attribute.key, value)
            }
        }
        event = builder.build()
        attributes.remove(null)
        event.customFlags?.size?.let { Assert.assertEquals(attributes.size.toLong(), it.toLong()) }
        for (entry in attributes) {
            if (entry.value != null) {
                entry.value.sort()
                val customFlagValues = event.customFlags?.get(entry.key)
                customFlagValues?.sort()
                Assert.assertEquals(entry.value, customFlagValues)
            } else {
                Assert.assertEquals(entry.value, event.customFlags?.get(entry.key))
            }
        }
        Assert.assertEquals(attributes, event.customFlags)
    }
}