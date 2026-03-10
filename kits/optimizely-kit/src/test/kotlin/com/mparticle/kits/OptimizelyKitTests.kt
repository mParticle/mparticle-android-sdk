package com.mparticle.kits

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.TypedUserAttributeListener
import com.mparticle.UserAttributeListener
import com.mparticle.UserAttributeListenerType
import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.AudienceTask
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.ConsentState
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.OptimizelyKit.Companion.optimizelyClient
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockKitConfiguration
import com.mparticle.testutils.RandomUtils
import com.mparticle.testutils.TestingUtils
import com.optimizely.ab.android.sdk.OptimizelyClient
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.HashMap
import java.util.Random
import java.util.UUID

class OptimizelyKitTests {
    var randomUtils = RandomUtils()
    private val kit: KitIntegration
        get() = OptimizelyKit()

    @Before
    fun before() {
        val mockMParticle = Mockito.mock(MParticle::class.java)
        val mockIdentityApi = Mockito.mock(IdentityApi::class.java)
        val mockUser: MParticleUser = EmptyMParticleUser()
        Mockito.`when`(mockMParticle.Identity()).thenReturn(mockIdentityApi)
        Mockito.`when`(mockMParticle.environment).thenReturn(MParticle.Environment.Development)
        Mockito.`when`(mockIdentityApi.currentUser).thenReturn(mockUser)
        MParticle.setInstance(mockMParticle)
    }

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name != null && name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        val optimizelyKit = OptimizelyKit()
        val client = Mockito.mock(OptimizelyClient::class.java)
        Mockito.`when`(client.isValid).thenReturn(true)
        optimizelyClient = client
        val minimalSettings: MutableMap<String, String> = HashMap()
        var e: Exception? = null
        minimalSettings["projectId"] = "2234"
        try {
            optimizelyKit.onKitCreate(minimalSettings, MockContext())
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNull(e)
        val badSettings: MutableMap<String, String> = HashMap()
        badSettings["projectId"] = "2234"
        badSettings[OptimizelyKit.DATAFILE_INTERVAL] = "foo"
        badSettings[OptimizelyKit.EVENT_INTERVAL] = "bar"
        try {
            optimizelyKit.onKitCreate(badSettings, MockContext())
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNull(e)
        val goodSettings: MutableMap<String, String> = HashMap()
        goodSettings["projectId"] = "2234"
        goodSettings[OptimizelyKit.DATAFILE_INTERVAL] = "3"
        goodSettings[OptimizelyKit.EVENT_INTERVAL] = "5"
        try {
            optimizelyKit.onKitCreate(goodSettings, MockContext())
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNull(e)
    }

    /**
     * Test that the correct value for userId is being set, based on the settings value. default userId
     * will be null
     */
    @Test
    @Throws(JSONException::class)
    fun testUserId() {
        val product1 = Product.Builder("product1", "1234", 0.0).build()
        val product2 = Product.Builder("product2", "9876", 1.0).build()
        val product3 = Product.Builder("product3", "3333", 300.0).build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.ADD_TO_CART, product1)
                .addProduct(product2)
                .addProduct(product3)
                .build()
        val mpid = Random().nextLong()
        val customerId = randomUtils.getAlphaNumericString(20)
        val email = randomUtils.getAlphaNumericString(10)
        val identities: MutableMap<IdentityType, String> = HashMap()
        identities[IdentityType.CustomerId] = customerId
        identities[IdentityType.Email] = email
        val user: MParticleUser =
            object : EmptyMParticleUser() {
                override fun getUserIdentities(): Map<IdentityType, String> = identities

                override fun getId(): Long = mpid
            }
        Mockito.`when`(MParticle.getInstance()?.Identity()?.currentUser).thenReturn(user)
        val expectedUserId = Mutable("")
        val count = Mutable(0)
        val settings = Mutable(JSONObject())
        val optimizelyKit: OptimizelyKit =
            object : OptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    count.value++
                    Assert.assertEquals(expectedUserId.value, trackEvent.userId)
                }

                override fun getConfiguration(): KitConfiguration? =
                    try {
                        MockKitConfiguration.createKitConfiguration(
                            JSONObject()
                                .put("id", MParticle.ServiceProviders.OPTIMIZELY)
                                .put("as", settings.value),
                        )
                    } catch (e: JSONException) {
                        null
                    }
            }
        val expectedEventCount = CommerceEventUtils.expand(commerceEvent).size
        settings.value =
            JSONObject().put(OptimizelyKit.USER_ID_FIELD_KEY, OptimizelyKit.USER_ID_EMAIL_VALUE)
        expectedUserId.value = email
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(expectedEventCount, count.value)
        count.value = 0
        settings.value =
            JSONObject().put(
                OptimizelyKit.USER_ID_FIELD_KEY,
                OptimizelyKit.USER_ID_CUSTOMER_ID_VALUE,
            )
        expectedUserId.value = customerId
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(expectedEventCount, count.value)
        count.value = 0
        settings.value =
            JSONObject().put(OptimizelyKit.USER_ID_FIELD_KEY, OptimizelyKit.USER_ID_MPID_VALUE)
        expectedUserId.value = mpid.toString()
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(expectedEventCount, count.value)
        count.value = 0
        val das = UUID.randomUUID().toString()
        Mockito.`when`(MParticle.getInstance()?.Identity()?.deviceApplicationStamp).thenReturn(das)
        settings.value =
            JSONObject().put(OptimizelyKit.USER_ID_FIELD_KEY, OptimizelyKit.USER_ID_DAS_VALUE)
        expectedUserId.value = das
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(expectedEventCount, count.value)
        count.value = 0

        // test default, should be das
        settings.value = JSONObject()
        expectedUserId.value = das
        optimizelyKit.logEvent(commerceEvent)
        // Don't log events if there is no userId type present
        Assert.assertEquals(3, count.value.toLong())
        count.value = 0
        Mockito.`when`(MParticle.getInstance()!!.Identity().currentUser).thenReturn(null)
        // test default when no user is present, default to das
        settings.value = JSONObject()
        expectedUserId.value = das
        optimizelyKit.logEvent(MPEvent.Builder("an event", MParticle.EventType.Location).build())
        optimizelyKit.logEvent(commerceEvent)
        // log events with das if there is no userId type present
        Assert.assertEquals(4, count.value.toLong())
    }

    /**
     * Test that when the Client is explicitly set via the static getClient() method, the Kit will not
     * override it, even if a client request is in progress
     */
    @Test
    fun testClientSetClientNotOverriden() {
        val optimizelyClient = Mockito.mock(OptimizelyClient::class.java)
        OptimizelyKit.optimizelyClient = optimizelyClient
        val rejectedClient = Mockito.mock(OptimizelyClient::class.java)
        Mockito.`when`(rejectedClient.isValid).thenReturn(true)
        val optimizelyKit = OptimizelyKit()
        optimizelyKit.onStart(rejectedClient)
        Assert.assertTrue(optimizelyClient === OptimizelyKit.optimizelyClient)
        OptimizelyKit.optimizelyClient = null
        optimizelyKit.onStart(rejectedClient)
        Assert.assertTrue(rejectedClient === OptimizelyKit.optimizelyClient)
    }

    @Test
    fun testLogEventNoCustom() {
        val event = MPEvent.Builder("An event", MParticle.EventType.Location).build()
        val called = Mutable(false)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    if (called.value) {
                        Assert.fail("multiple events, expecting 1")
                    }
                    Assert.assertEquals(trackEvent.eventName, "An event")
                    Assert.assertNull(trackEvent.eventAttributes)
                    called.value = true
                }
            }
        optimizelyKit.logEvent(event)
        Assert.assertTrue(called.value)
    }

    /**
     * test that all Product custom attributes are properly translated into Optimizely event attributes
     * test that default event name is correct
     */
    @Test
    fun testCommerceEventNoCustom() {
        val userAttributes = randomUtils.getRandomAttributes(4)
        val user: MParticleUser =
            object : EmptyMParticleUser() {
                override fun getUserAttributes(userAttributeListener: UserAttributeListenerType?): Map<String, Any>? {
                    (userAttributeListener as TypedUserAttributeListener?)!!.onUserAttributesReceived(
                        HashMap(userAttributes),
                        HashMap(),
                        1L,
                    )
                    return null
                }
            }
        Mockito.`when`(MParticle.getInstance()!!.Identity().currentUser).thenReturn(user)
        val product1 = Product.Builder("product1", "1234", 0.0).build()
        val product2 = Product.Builder("product2", "9876", 1.0).build()
        val product3 = Product.Builder("product3", "3333", 300.0).build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.ADD_TO_CART, product1)
                .addProduct(product2)
                .addProduct(product3)
                .build()
        val expectedCount = CommerceEventUtils.expand(commerceEvent).size
        val count = Mutable(0)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    count.value++
                    Assert.assertEquals(
                        String.format(
                            "eCommerce - %s - Item",
                            commerceEvent.productAction,
                        ),
                        trackEvent.eventName,
                    )
                    Assert.assertEquals(userAttributes, trackEvent.userAttributes)
                }
            }
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(expectedCount.toLong(), count.value.toLong())
    }

    /**
     * test that the "revenue" reserved keyword is being populated when a CommerceEvent has revenue
     */
    @Test
    fun testCommerceEventRevenue() {
        val userAttributes = randomUtils.getRandomAttributes(4)
        val user: MParticleUser =
            object : EmptyMParticleUser() {
                override fun getUserAttributes(userAttributeListener: UserAttributeListenerType?): Map<String, Any>? {
                    (userAttributeListener as TypedUserAttributeListener?)!!.onUserAttributesReceived(
                        HashMap(userAttributes),
                        HashMap(),
                        1L,
                    )
                    return null
                }
            }
        Mockito.`when`(MParticle.getInstance()!!.Identity().currentUser).thenReturn(user)
        val customAttributes = randomUtils.getRandomAttributes(3)
        customAttributes[OptimizelyKit.OPTIMIZELY_EVENT_NAME] = "myCustomName1"
        val customAttributes1 = randomUtils.getRandomAttributes(5)
        customAttributes1[OptimizelyKit.OPTIMIZELY_EVENT_NAME] = "myCustomName2"
        val product1 =
            Product.Builder("product1", "1234", 0.0).customAttributes(customAttributes).build()
        val product2 =
            Product.Builder("product2", "9876", 1.0).customAttributes(customAttributes1).build()
        val product3 = Product.Builder("product3", "3333", 300.0).build()
        val transactionAttributes = TransactionAttributes("999").setRevenue(45.5)
        val commerceEvent =
            CommerceEvent
                .Builder(Product.PURCHASE, product1)
                .transactionAttributes(transactionAttributes)
                .addProduct(product2)
                .addProduct(product3)
                .build()
        val count = Mutable(0)
        val revenueEventFound = Mutable(false)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    count.value++
                    if (String.format(
                            CommerceEventUtils.PLUSONE_NAME,
                            commerceEvent.productAction,
                        ) == trackEvent.eventName
                    ) {
                        // make sure it is our revenue * 100 (we our dollars, they are cents)
                        Assert.assertEquals(
                            trackEvent.eventAttributes!!["revenue"].toString().toDouble(),
                            4550.0,
                            0.0,
                        )
                        Assert.assertFalse(revenueEventFound.value)
                        revenueEventFound.value = true
                    } else {
                        Assert.assertEquals(
                            String.format(
                                "eCommerce - %s - Item",
                                commerceEvent.productAction,
                            ),
                            trackEvent.eventName,
                        )
                    }
                    Assert.assertEquals(userAttributes, trackEvent.userAttributes)
                }
            }
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(
            CommerceEventUtils.expand(commerceEvent).size.toLong(),
            count.value.toLong(),
        )
        Assert.assertTrue(revenueEventFound.value)
    }

    /**
     * Ensure that the custom name flag is being applied when it should be
     */
    @Test
    fun testCommerceEventProductCustomName() {
        val eventName = "custom name"
        val product1 = Product.Builder("product1", "1234", 0.0).build()
        val transactionAttributes = TransactionAttributes("999").setRevenue(20.5)
        val commerceEvent =
            CommerceEvent
                .Builder(Product.PURCHASE, product1)
                .transactionAttributes(transactionAttributes)
                .addCustomFlag(OptimizelyKit.OPTIMIZELY_EVENT_NAME, eventName)
                .build()
        val count = Mutable(0)
        val customNameFound = Mutable(false)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    count.value++
                    if (trackEvent.eventName == eventName) {
                        Assert.assertEquals(
                            trackEvent.eventAttributes!!["revenue"].toString().toFloat(),
                            2050f,
                            0f,
                        )
                        Assert.assertFalse(customNameFound.value)
                        customNameFound.value = true
                    }
                }
            }
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(
            CommerceEventUtils.expand(commerceEvent).size.toLong(),
            count.value.toLong(),
        )
        Assert.assertTrue(customNameFound.value)
    }

    @Test
    fun testCustomValue() {
        val randomEvent =
            MPEvent
                .Builder(TestingUtils.getInstance().randomMPEventRich)
                .addCustomFlag(OptimizelyKit.OPTIMIZELY_VALUE_KEY, "40.1")
                .build()
        val eventFound = Mutable(false)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    Assert.assertFalse(eventFound.value)
                    eventFound.value = true
                    Assert.assertEquals((trackEvent.eventAttributes?.get("value") as Double), 40.1, 0.0)
                }
            }
        optimizelyKit.logEvent(randomEvent)
        Assert.assertTrue(eventFound.value)
    }

    /**
     * Make sure the queueing is working. When the OptimizelyClient is not present, we should be queueing
     * events. The queue should be emptied when the OptimizelyClient becomes available
     */
    @Test
    fun testQueueWorking() {
        val userAttributes = randomUtils.getRandomAttributes(4)
        val user: MParticleUser =
            object : EmptyMParticleUser() {
                override fun getUserAttributes(userAttributeListener: UserAttributeListenerType?): Map<String, Any>? {
                    (userAttributeListener as TypedUserAttributeListener?)!!.onUserAttributesReceived(
                        HashMap(userAttributes),
                        HashMap(),
                        1L,
                    )
                    return null
                }
            }
        Mockito.`when`(MParticle.getInstance()!!.Identity().currentUser).thenReturn(user)
        val product1 = Product.Builder("product1", "1234", 0.0).build()
        val product2 = Product.Builder("product2", "9876", 1.0).build()
        val product3 = Product.Builder("product3", "3333", 300.0).build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.ADD_TO_CART, product1)
                .build()
        val commerceEvent1 =
            CommerceEvent
                .Builder(Product.CLICK, product2)
                .addProduct(product3)
                .build()
        val optimizelyKit: OptimizelyKit = MockOptimizelyKit()
        optimizelyKit.logEvent(commerceEvent)
        optimizelyKit.logEvent(commerceEvent1)
        val expectedEvents =
            CommerceEventUtils.expand(commerceEvent).size + CommerceEventUtils.expand(commerceEvent1).size
        Assert.assertEquals(expectedEvents.toLong(), optimizelyKit.mEventQueue.size.toLong())
        val optimizelyClient = Mockito.mock(OptimizelyClient::class.java)
        Mockito.`when`(optimizelyClient.isValid).thenReturn(false)
        optimizelyKit.onStart(optimizelyClient)

        // Events should NOT be dequeued if the OptimizelyClient is not valid
        Assert.assertEquals(expectedEvents.toLong(), optimizelyKit.mEventQueue.size.toLong())
        var count = invocationCount(optimizelyClient, "track")
        Assert.assertEquals(0, count.toLong())
        Mockito.`when`(optimizelyClient.isValid).thenReturn(true)
        optimizelyKit.onStart(optimizelyClient)
        Mockito.verify(optimizelyClient, Mockito.times(expectedEvents)).track(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any<MutableMap<String, *>>(),
            Mockito.any<MutableMap<String, *>>(),
        )
        count = invocationCount(optimizelyClient, "track")
        Assert.assertEquals(expectedEvents.toLong(), count.toLong())
        Assert.assertEquals(0, optimizelyKit.mEventQueue.size.toLong())
    }

    @Test
    fun testCustomUserId() {
        val event =
            MPEvent
                .Builder("Event Name", MParticle.EventType.Search)
                .addCustomFlag(OptimizelyKit.OPTIMIZELY_USER_ID, "44")
                .build()
        val count = Mutable(0)
        val optimizelyKit: OptimizelyKit =
            object : MockOptimizelyKit() {
                override fun getUserId(user: MParticleUser?): String = "should not be this userId"

                override fun logOptimizelyEvent(trackEvent: OptimizelyEvent) {
                    count.value++
                    Assert.assertEquals("44", trackEvent.userId)
                }
            }
        optimizelyKit.logEvent(event)
        Assert.assertEquals(1, count.value.toLong())
        count.value = 0
        val product = Product.Builder("My Product", "12345", 1.0).build()
        val product1 = Product.Builder("My Other Product", "2356", 2.0).build()
        val impression = Impression("name", product).addProduct(product1)
        val transactionAttributes = TransactionAttributes("123").setRevenue(43.2)
        val commerceEvent =
            CommerceEvent
                .Builder(Product.DETAIL, product)
                .addProduct(product1)
                .addImpression(impression)
                .addCustomFlag(OptimizelyKit.OPTIMIZELY_USER_ID, "44")
                .transactionAttributes(transactionAttributes) //                .addPromotion(promotion)
                .build()
        optimizelyKit.logEvent(commerceEvent)
        Assert.assertEquals(4, count.value.toLong())
    }

    @Test
    fun onCreateAfterDestroy() {
        val optimizelyKit = OptimizelyKit()
        val client = Mockito.mock(OptimizelyClient::class.java)
        Mockito.`when`(client.isValid).thenReturn(true)
        optimizelyClient = client
        val minimalSettings: MutableMap<String, String> = HashMap()
        val e: Exception? = null
        minimalSettings["projectId"] = "2234"
        optimizelyKit.onKitCreate(minimalSettings, MockContext())
        optimizelyKit.onKitDestroy()
        optimizelyKit.onKitCreate(minimalSettings, MockContext())
    }

    private fun invocationCount(
        `object`: Any,
        methodName: String,
    ): Int {
        val invocationList = Mockito.mockingDetails(`object`).invocations
        var invocationCount = 0
        for (invocation in invocationList) {
            if (invocation.method.name == methodName) {
                invocationCount++
            }
        }
        return invocationCount
    }

    internal open inner class MockOptimizelyKit : OptimizelyKit() {
        override fun getConfiguration(): KitConfiguration? =
            try {
                MockKitConfiguration.createKitConfiguration(
                    JSONObject()
                        .put("id", MParticle.ServiceProviders.OPTIMIZELY)
                        .put("as", JSONObject().put(USER_ID_FIELD_KEY, USER_ID_MPID_VALUE)),
                )
            } catch (e: JSONException) {
                null
            }
    }

    class Mutable<T>(
        var value: T,
    )

    internal open inner class EmptyMParticleUser : MParticleUser {
        override fun getId(): Long = 1L

        override fun getUserAttributes(): Map<String, Any> = HashMap()

        override fun getUserAttributes(userAttributeListener: UserAttributeListenerType?): Map<String, Any>? {
            if (userAttributeListener is TypedUserAttributeListener) {
                userAttributeListener.onUserAttributesReceived(
                    HashMap(),
                    HashMap(),
                    id,
                )
            }
            if (userAttributeListener is UserAttributeListener) {
                userAttributeListener.onUserAttributesReceived(
                    HashMap(),
                    HashMap(),
                    id,
                )
            }
            return null
        }

        override fun setUserAttributes(map: Map<String, Any>): Boolean = false

        override fun getUserIdentities(): Map<IdentityType, String> = mapOf()

        override fun setUserAttribute(
            s: String,
            o: Any,
        ): Boolean = false

        override fun setUserAttributeList(
            s: String,
            o: Any,
        ): Boolean = false

        override fun incrementUserAttribute(
            s: String,
            i: Number,
        ): Boolean = false

        override fun removeUserAttribute(s: String): Boolean = false

        override fun setUserTag(s: String): Boolean = false

        override fun getConsentState(): ConsentState = consentState

        override fun setConsentState(consentState: ConsentState?) {}

        override fun isLoggedIn(): Boolean = false

        override fun getFirstSeenTime(): Long = 0

        override fun getLastSeenTime(): Long = 0

        override fun getUserAudiences(): AudienceTask<AudienceResponse> = throw NotImplementedError("getUserAudiences() is not implemented")
    }
}
