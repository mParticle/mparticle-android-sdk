package com.mparticle.commerce

import com.mparticle.MParticle
import com.mparticle.MockMParticle
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ProductTest {
    @Before
    fun before() {
        MParticle.setInstance(MockMParticle())
    }

    @Test
    fun testDefaultEqualityComparator() {
        Product.setEqualityComparator(null)
        val product1 = Product.Builder("name", "sku", 2.0).brand("cool brand!").build()
        val product2 =
            Product.Builder("cool brand!", "sku", 2.0).brand("cool brand!adsflkjh").build()
        val product2Copy = Product.Builder(product2).build()
        product2Copy.mTimeAdded = product2.mTimeAdded
        Assert.assertNotEquals(product2, product1)
        Assert.assertEquals(product1, product1)
        Assert.assertEquals(product2, product2Copy)
        Assert.assertNotEquals(product1, null)
        Assert.assertNotEquals(null, product1)
    }

    @Test
    fun testEqualityComparator() {
        Product.setEqualityComparator { product1, product2 -> product1?.name == product2?.brand }
        val product1 = Product.Builder("name", "sku", 2.0).brand("cool brand!").build()
        val product2 =
            Product.Builder("cool brand!", "sku", 2.0).brand("cool brand!adsflkjh").build()
        Assert.assertEquals(product2, product1)
    }

    @Test
    fun testSerializationDeserialization() {
        Product.setEqualityComparator { product1, product2 -> product1.toString() == product2.toString() }
        val product = Product.Builder("product name", "product sku", 301.45)
            .brand("product brand")
            .category("product category")
            .couponCode("product coupon code")
            .name("product name")
            .position(4)
            .variant("product variant")
            .quantity(12.1)
            .build()
        val productJson = product.toJson()
        val product2 = Product.fromJson(productJson)
        Assert.assertEquals(product, product2)
        product2?.quantity = 10000.0
        Assert.assertNotEquals(product, product2)
    }
}
