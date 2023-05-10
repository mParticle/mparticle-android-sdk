package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.mock.MockContext
import com.mparticle.mock.utils.RandomUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.TreeMap

class MParticleJSInterfaceTest : MParticleJSInterface() {

    private lateinit var mProduct1: Product
    private lateinit var mProduct2: Product
    private lateinit var jsInterfaceInstance: MParticleJSInterface

    private val mProduct1Json =
        "{\"Name\":\"iPhone\",\"Sku\":\"12345\",\"Price\":400,\"Quantity\":1,\"TotalAmount\":400}"
    private val mProduct2Json =
        "{\"Name\":\"Android\",\"Sku\":\"98765\",\"Price\":\"600\",\"Quantity\":4,\"Brand\":\"Samsung\",\"Variant\":\"SuperDuper\",\"Category\":\"CellPhones\",\"Position\":2,\"CouponCode\":\"my-coupon-code-2\",\"TotalAmount\":2400,\"Attributes\":{\"customkey\":\"customvalue\"}}"
    private val mProduct3Json =
        "{\"Name\":\"iPhone\",\"Sku\":\"123456\",\"Price\":\"400\",\"Quantity\":2,\"Brand\":\"Apple\",\"Variant\":\"Plus\",\"Category\":\"Phones\",\"Position\":1,\"CouponCode\":\"my-coupon-code\",\"TotalAmount\":800,\"Attributes\":{\"customkey\":\"customvalue\"}}"
    private val mProduct4Json =
        "{\"Name\":\"iPhone\",\"Sku\":\"1234567\",\"Price\":null,\"Quantity\":\"2-foo\",\"Brand\":\"Apple\",\"Variant\":\"Plus\",\"Category\":\"Phones\",\"Position\":\"1-foo\",\"CouponCode\":\"my-coupon-code\",\"TotalAmount\":null,\"Attributes\":{\"customkey\":\"customvalue\"}}"
    private val mProduct5Json =
        "{\"Name\":\"iPhone\",\"Sku\":\"SKU1\",\"Price\":1,\"Quantity\":1,\"TotalAmount\":1},{\"Name\":\"Android\",\"Sku\":\"SKU2\",\"Price\":1,\"Quantity\":1,\"TotalAmount\":1}"
    private val mPromotion1String =
        "{\"Id\":\"12345\",\"Creative\":\"my-creative\",\"Name\":\"creative-name\",\"Position\":1}"
    private val sampleCommerceEvent1 =
        "{\"EventName\":\"eCommerce - ViewDetail\",\"EventCategory\":15,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"a37943f1-c9b9-452f-8cc4-0d2eca2ef002\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029994680,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":6,\"ProductList\":[$mProduct1Json]}}"
    private val sampleCommerceEvent2 =
        "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"64563b6b-0ece-41e1-a68c-675025be2a57\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492026476362,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"8b658328-f6bf-4bf6-a9bc-6dcc7b9a9e44\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[$mProduct2Json],\"TransactionId\":\"5050505\",\"Affiliation\":\"test-affiliation2\",\"CouponCode\":\"coupon-cod2e\",\"TotalAmount\":43234,\"ShippingAmount\":100,\"TaxAmount\":400}}"
    private val sampleCommerceEvent3 =
        "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"67480717-6e06-4d7f-9326-75ee7bf7e6a8\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029690858,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[$mProduct3Json],\"TransactionId\":\"12345\",\"Affiliation\":\"test-affiliation\",\"CouponCode\":\"coupon-code\",\"TotalAmount\":44334,\"ShippingAmount\":600,\"TaxAmount\":200}}"
    private val sampleCommerceEvent4 =
        "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"80db6a3b-4e4c-4ce1-8930-ca7eebdcbb06\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029758226,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[$mProduct4Json],\"TransactionId\":\"12345\",\"Affiliation\":\"test-affiliation\",\"CouponCode\":\"coupon-code\",\"TotalAmount\":\"44334-foo\",\"ShippingAmount\":\"600-foo\",\"TaxAmount\":\"200-foo\"}}"
    private val sampleCommerceEvent5 =
        "{\"EventName\":\"eCommerce - Purchase\",\"EventCategory\":16,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"cba9e5fe-31f3-431a-ba15-cdd887a298b7\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029826952,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":7,\"ProductList\":[$mProduct5Json],\"TransactionId\":\"12345\"}}"
    private val sampleCommerceEvent6 =
        "{\"EventName\":\"eCommerce - Refund\",\"EventCategory\":17,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"7c24e0b1-6686-4e08-9d8a-94235cb7e34d\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029858097,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":8,\"ProductList\":[$mProduct5Json],\"TransactionId\":\"12345\"}}"
    private val sampleCommerceEvent7 =
        "{\"EventName\":\"eCommerce - PromotionClick\",\"EventCategory\":19,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"bb81adfd-cd23-492d-a333-c453fbf1b255\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029882614,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"PromotionAction\":{\"PromotionActionType\":2,\"PromotionList\":[$mPromotion1String]}}"
    private val sampleCommerceEvent8 =
        "{\"EventName\":\"eCommerce - Impression\",\"EventCategory\":22,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"7cb945f2-8281-4483-ab41-cd10e9faac53\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029916774,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductImpressions\":[{\"ProductImpressionList\":\"impression-name\",\"ProductList\":[$mProduct1Json]}]}"
    private val sampleCommerceEvent9 =
        "{\"EventName\":\"eCommerce - Refund\",\"EventCategory\":17,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"257aaa99-e1c6-4b50-bb6d-dcdcdcfc0abc\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029941497,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":8,\"ProductList\":[],\"TransactionId\":\"12345\"}}"
    private val sampleCommerceEvent10 =
        "{\"EventName\":\"eCommerce - Checkout\",\"EventCategory\":12,\"UserAttributes\":{},\"SessionAttributes\":{},\"UserIdentities\":[],\"Store\":null,\"SDKVersion\":\"1.8.7\",\"SessionId\":\"a2401cd1-1f2b-446a-a37c-5ed3ffc2b2d1\",\"EventDataType\":16,\"Debug\":false,\"Timestamp\":1492029969607,\"OptOut\":null,\"ExpandedEventCount\":0,\"AppVersion\":null,\"ClientGeneratedId\":\"543fbfd0-f6f0-4ec5-a8a7-efc218251105\",\"CurrencyCode\":null,\"ShoppingCart\":{},\"ProductAction\":{\"ProductActionType\":3,\"CheckoutStep\":1,\"CheckoutOptions\":\"Visa\",\"ProductList\":[]}}"

    @Before
    @Throws(Exception::class)
    fun setup() {
        val mockMp = Mockito.mock(MParticle::class.java)
        Mockito.`when`(mockMp.Internal()).thenReturn(
            Mockito.mock(
                MParticle.Internal::class.java
            )
        )
        Mockito.`when`(mockMp.Internal().configManager).thenReturn(ConfigManager(MockContext()))
        val mockCurrentUser = Mockito.mock(MParticleUser::class.java)
        val mockIdentity = Mockito.mock(IdentityApi::class.java)
        Mockito.`when`(mockIdentity.currentUser).thenReturn(mockCurrentUser)
        Mockito.`when`(mockMp.Identity()).thenReturn(mockIdentity)
        Mockito.`when`(mockMp.environment).thenReturn(MParticle.Environment.Development)
        MParticle.setInstance(mockMp)
        jsInterfaceInstance = MParticleJSInterface()
        Mockito.`when`(MParticle.getInstance()?.environment)
            .thenReturn(MParticle.Environment.Development)
        mProduct1 = Product.Builder("iPhone", "12345", 400.0)
            .quantity(1.0)
            .build()
        val customAttributes: MutableMap<String, String> = TreeMap()
        customAttributes["customkey"] = "customvalue"
        mProduct2 = Product.Builder("Android", "98765", 600.0)
            .quantity(4.0)
            .couponCode("my-coupon-code-2")
            .variant("SuperDuper")
            .brand("Samsung")
            .position(2)
            .category("CellPhones")
            .customAttributes(customAttributes)
            .build()
    }

    @Test
    @Throws(Exception::class)
    fun unpackSimpleCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent1)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.productAction, Product.DETAIL)
        Assert.assertEquals(commerceEvent.products?.size, 1)
        val product = commerceEvent.products?.get(0)
        product?.let {
            Assert.assertEquals(commerceEvent.eventName, "eCommerce - ViewDetail")
            Assert.assertEquals(product.name, mProduct1.name)
            Assert.assertEquals(product.unitPrice, mProduct1.unitPrice, .01)
            Assert.assertEquals(product.quantity, mProduct1.quantity, .01)
            Assert.assertEquals(product.totalAmount, mProduct1.totalAmount, .01)
            Assert.assertEquals(product.sku, mProduct1.sku)
        }
    }

    @Test
    @Throws(Exception::class)
    fun unpackCommerceEventWithTransactions() {
        val `object` = JSONObject(sampleCommerceEvent2)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Purchase")
        Assert.assertEquals(commerceEvent.products?.size, 1)
        val product = commerceEvent.products?.get(0)
        product?.let {
            Assert.assertEquals(product.name, mProduct2.name)
            Assert.assertEquals(product.sku, mProduct2.sku)
            Assert.assertEquals(product.customAttributes?.size, 1)
            val key = product.customAttributes?.keys?.toTypedArray()?.get(0) as String
            Assert.assertEquals(key, "customkey")
            Assert.assertEquals(product.customAttributes?.get(key), "customvalue")
            Assert.assertEquals(product.couponCode, mProduct2.couponCode)
            Assert.assertEquals(product.quantity, mProduct2.quantity, .01)
            Assert.assertEquals(product.position, 2)
            Assert.assertEquals(product.variant, mProduct2.variant)
            val transactionAttributes = commerceEvent.transactionAttributes
            transactionAttributes?.let {
                Assert.assertEquals(transactionAttributes.affiliation, "test-affiliation2")
                Assert.assertEquals(transactionAttributes.revenue!!, 43234.0, .01)
                Assert.assertEquals(transactionAttributes.shipping!!, 100.0, .01)
                Assert.assertEquals(transactionAttributes.couponCode, "coupon-cod2e")
                Assert.assertEquals(transactionAttributes.id, "5050505")
            }
        }
    }

    /**
     * tests how we respond to String values where we should have Numbers
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun unpackMalformedCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent4)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.transactionAttributes?.shipping, null)
        Assert.assertEquals(commerceEvent.transactionAttributes?.tax, null)
        Assert.assertEquals(commerceEvent.transactionAttributes?.revenue!!.toDouble(), 0.0, 0.0)
        Assert.assertEquals(commerceEvent.products?.size, 1)
        val product2 = commerceEvent.products?.get(0)
        Assert.assertEquals(product2?.unitPrice!!, 0.0, .001)
        Assert.assertEquals(product2.position!!.toInt(), 0)
    }

    @Test
    @Throws(Exception::class)
    fun unpackMultipleProductsCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent5)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.products?.size, 2)
        val product3 = commerceEvent.products?.get(0)
        val product4 = commerceEvent.products?.get(1)
        Assert.assertEquals(product3?.name, "iPhone")
        Assert.assertEquals(product4?.name, "Android")
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Purchase")
        Assert.assertEquals(commerceEvent.productAction, Product.PURCHASE)
        Assert.assertEquals(commerceEvent.transactionAttributes?.id, "12345")
    }

    @Test
    @Throws(Exception::class)
    fun unpackRefundMultipleProductsCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent6)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Refund")
        Assert.assertEquals(commerceEvent.productAction, Product.REFUND)
        Assert.assertEquals(commerceEvent.products?.size, 2)
    }

    @Test
    @Throws(Exception::class)
    fun unpackPromotionCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent7)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.promotionAction, Promotion.CLICK)
        Assert.assertEquals(commerceEvent.promotions?.size, 1)
        val promotion = commerceEvent.promotions?.get(0)
        promotion?.let {
            Assert.assertEquals(promotion.creative, "my-creative")
            Assert.assertEquals(promotion.id, "12345")
            Assert.assertEquals(promotion.name, "creative-name")
            Assert.assertEquals(promotion.position, "1")
        }
    }

    @Test
    @Throws(Exception::class)
    fun unpackImpressionCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent8)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Impression")
        Assert.assertEquals(commerceEvent.impressions?.size, 1)
        val impression = commerceEvent.impressions?.get(0)
        Assert.assertEquals(impression?.listName, "impression-name")
        Assert.assertEquals(impression?.products?.size, 1)
        val product5 = impression?.products?.get(0)
        product5?.let {
            Assert.assertEquals(product5.name, "iPhone")
            Assert.assertEquals(product5.sku, "12345")
            Assert.assertEquals(product5.unitPrice, 400.0, .01)
            Assert.assertEquals(product5.quantity, 1.0, .01)
        }
    }

    /**
     * tests how we respond to an empty product array, with a product action
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun unpackRefundEmptyProductArrayCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent9)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Refund")
        Assert.assertEquals(commerceEvent.productAction, Product.REFUND)
        Assert.assertEquals(commerceEvent.transactionAttributes?.id, "12345")
        Assert.assertEquals(commerceEvent.products?.size, 0)
    }

    @Test
    @Throws(Exception::class)
    fun unpackCheckoutCommerceEvent() {
        val `object` = JSONObject(sampleCommerceEvent10)
        val commerceEvent = toCommerceEvent(`object`)
        Assert.assertEquals(commerceEvent.eventName, "eCommerce - Checkout")
        Assert.assertEquals(commerceEvent.productAction, Product.CHECKOUT)
        Assert.assertEquals(commerceEvent.checkoutStep, 1)
        Assert.assertEquals(commerceEvent.checkoutOptions, "Visa")
    }

    @Test
    @Throws(Exception::class)
    fun testProductEqualityComparator() {
        val product2Json = JSONObject(mProduct2Json)
        var product2 = toProduct(product2Json)
        Assert.assertTrue(isEqual(product2, mProduct2))
        product2Json.put("Sku", "00000")
        product2 = toProduct(product2Json)
        Assert.assertFalse(isEqual(product2, mProduct2))
    }

    @Test
    @Throws(Exception::class)
    fun testLogin() {
        val identities = RandomUtils.getInstance().randomUserIdentities
        val identityArray = JSONArray()
        for (entry in identities) {
            identityArray.put(
                JSONObject()
                    .put(TYPE, entry.key.name)
                    .put(IDENTITY, entry.value)
            )
        }
        val jsonObject = JSONObject()
            .put(USER_IDENTITIES, identityArray)
        login(jsonObject.toString())
        Mockito.verify(MParticle.getInstance()?.Identity(), Mockito.times(1))?.login(
            Mockito.any(
                IdentityApiRequest::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        val identities = RandomUtils.getInstance().randomUserIdentities
        val identityArray = JSONArray()
        for (entry in identities) {
            identityArray.put(
                JSONObject()
                    .put(TYPE, entry.key.name)
                    .put(IDENTITY, entry.value)
            )
        }
        val jsonObject = JSONObject()
            .put(USER_IDENTITIES, identityArray)
        logout(jsonObject.toString())
        Mockito.verify(MParticle.getInstance()?.Identity(), Mockito.times(1))?.logout(
            Mockito.any(
                IdentityApiRequest::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testModify() {
        val identities = RandomUtils.getInstance().randomUserIdentities
        val identityArray = JSONArray()
        for (entry in identities) {
            identityArray.put(
                JSONObject()
                    .put(TYPE, entry.key.name)
                    .put(IDENTITY, entry.value)
            )
        }
        val jsonObject = JSONObject()
            .put(USER_IDENTITIES, identityArray)
        modify(jsonObject.toString())
        Mockito.verify(MParticle.getInstance()?.Identity(), Mockito.times(1))?.modify(
            Mockito.any(
                IdentityApiRequest::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testParsing() {
        val `val` =
            "{\"UserIdentities\":[{\"Type\":1,\"Identity\":\"123\"},{\"Type\":7,\"Identity\":\"test@gmail.com\"}]}"
        val request = getIdentityApiRequest(JSONObject(`val`))
        request.userIdentities.toString()
    }

    @Test
    fun testEventTypeParsing() {
        for (i in MParticle.EventType.values().indices) {
            Assert.assertEquals(
                i.toString() + "",
                MParticle.EventType.values()[i],
                jsInterfaceInstance.convertEventType(i)
            )
        }
    }

    fun isEqual(product1: Product, product2: Product?): Boolean {
        return try {
            val object1 = JSONObject(product1.toString())
            val object2 = JSONObject(product2.toString())
            object1.remove("act")
            object2.remove("act")
            object1.toString() == object2.toString()
        } catch (ignore: JSONException) {
            false
        }
    }
}
