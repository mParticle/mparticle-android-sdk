package com.mparticle.kits

import com.mparticle.MParticle.EventType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import org.junit.Assert
import org.junit.Test

class CommerceEventUtilsTest {
    @Test
    @Throws(Exception::class)
    fun testNullProductExpansion() {
        Assert.assertNotNull(CommerceEventUtils.expand(null))
        Assert.assertEquals(0, CommerceEventUtils.expand(null).size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_AddToCart() {
        val product = Product.Builder("Double Room - Econ Rate", "econ-1", 100.00)
            .quantity(4.0)
            .build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.AddToCart, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_CLICK() {
        val product = Product.Builder("Double Room - Econ Rate", "econ-1", 100.00)
            .quantity(4.0)
            .build()
        val event = CommerceEvent.Builder(Product.CLICK, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.Click, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_ADD_TO_WISHLIST() {
        val product = Product.Builder("Double Room - Econ Rate", "econ-1", 100.00)
            .quantity(4.0)
            .build()
        val event = CommerceEvent.Builder(Product.ADD_TO_WISHLIST, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.AddToWishlist, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_CHECKOUT() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.CHECKOUT, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.Checkout, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_CHECKOUT_OPTION() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.CHECKOUT_OPTION, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.CheckoutOption, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_DETAIL() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.DETAIL, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.ViewDetail, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_PURCHASE() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.Transaction, events.get(0).eventType)
        Assert.assertEquals(EventType.Purchase, events.get(1).eventType)
        Assert.assertEquals(2, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_REFUND() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.REFUND, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.Transaction, events.get(0).eventType)
        Assert.assertEquals(EventType.Refund, events.get(1).eventType)
        Assert.assertEquals(2, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_REMOVE_FROM_CART() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.REMOVE_FROM_CART, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.RemoveFromCart, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }

    @Test
    @Throws(Exception::class)
    fun testProductExpansion_REMOVE_FROM_WISHLIST() {
        val product = Product.Builder("Unisex Tee", "128747", 18.00)
            .quantity(3.0)
            .build()
        val event = CommerceEvent.Builder(Product.REMOVE_FROM_WISHLIST, product)
            .build()

        val events = CommerceEventUtils.expand(event)
        Assert.assertNotNull(CommerceEventUtils.expand(event))
        Assert.assertEquals(EventType.RemoveFromWishlist, events.get(0).eventType)
        Assert.assertEquals(1, events.size)
    }
}
