package com.mparticle.internal

import android.location.Location
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.internal.messages.BaseMPMessageBuilder
import com.mparticle.internal.messages.MPCommerceMessage
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Location::class, BaseMPMessageBuilder::class)
class BaseMPMessageTest {
    @Mock
    lateinit var location: Location
    private lateinit var builder: BaseMPMessageBuilder

    @Before
    fun setUp() {
        // Initialize PowerMockito mocks
        PowerMockito.mockStatic(Location::class.java)
        builder = PowerMockito.mock(BaseMPMessageBuilder::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testEventLength() {
        val event = MPEvent.Builder("test name", MParticle.EventType.Navigation).build()
        val session = InternalSession()
        val message = BaseMPMessage.Builder(Constants.MessageType.EVENT)
            .name(event.eventName)
            .timestamp(1235)
            .length(event.length)
            .attributes(MPUtility.enforceAttributeConstraints(event.customAttributeStrings))
            .build(session, null, 1)
        Assert.assertNull(message.opt("el"))
        Assert.assertNull(message.attributes)
        val info: MutableMap<String, String?> = HashMap(1)
        info["EventLength"] = "321"
        val event2 = MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123.0)
            .customAttributes(info).build()
        val message2 = BaseMPMessage.Builder(Constants.MessageType.EVENT)
            .name(event2.eventName)
            .timestamp(1235)
            .length(event2.length)
            .attributes(MPUtility.enforceAttributeConstraints(event2.customAttributeStrings))
            .build(session, null, 1)
        Assert.assertEquals(message2.attributes?.getString("EventLength"), "321")
        val event3 =
            MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123.0).build()
        val message3 = BaseMPMessage.Builder(Constants.MessageType.EVENT)
            .name(event3.eventName)
            .timestamp(1235)
            .length(event3.length)
            .attributes(MPUtility.enforceAttributeConstraints(event.customAttributeStrings))
            .build(session, null, 1)
        Assert.assertEquals(message3.attributes?.getString("EventLength"), "123")
    }

    @Test
    fun testNotNullOnTimestampAndSessionId() {
        val location = mock(Location::class.java)
        val event = MPEvent.Builder("test name", MParticle.EventType.Navigation).build()
        val session = InternalSession()
        val message = BaseMPMessage.Builder(Constants.MessageType.SESSION_START)
            .name(event.eventName)
            .length(event.length)
            .timestamp(System.currentTimeMillis())
            .attributes(MPUtility.enforceAttributeConstraints(event.customAttributeStrings))
            .build(session, location, 1)
        Assert.assertNull(message.opt("el"))
        Assert.assertNotNull(message.timestamp)
        Assert.assertNotNull(message.typeNameHash)
        Assert.assertNotNull(message.sessionId)
    }

    @Test
    @Throws(Exception::class)
    fun testNullCartOnCommerceEvent() {
        MParticle.setInstance(MockMParticle())
        val event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("foo", "bar", 10.0).build())
                .build()
        val builder = MPCommerceMessage.Builder(event)
            .timestamp(12345) as MPCommerceMessage.Builder
        val message = builder.build(InternalSession(), null, 0)
        Assert.assertNotNull(message)
    }

    @Test
    @Throws(Exception::class)
    fun testNullCartOnCommerceEventWithImpression() {
        MParticle.setInstance(MockMParticle())
        val impression = Impression("Test Products List", Product.Builder("foo", "bar", 10.0).build())

        val event =
            CommerceEvent.Builder(impression)
                .screen("TestScreen")
                .nonInteraction(true)
                .customAttributes(
                    mapOf(
                        "size" to "XL",
                        "color" to "RED"
                    )
                )
                .currency("USD")
                .build()
        val builder = MPCommerceMessage.Builder(event)
            .timestamp(12345) as MPCommerceMessage.Builder
        val message = builder.build(InternalSession(), null, 0)

        Assert.assertNotNull(message)
    }

    @Test
    @Throws(Exception::class)
    fun testNullCartOnCommerceEventWithPromotion() {
        MParticle.setInstance(MockMParticle())

        val event = Promotion().apply {
            id = "1234"
            creative = "test_banner_1"
            name = "50% off sale"
            position = "test_button"
        }.let {
            CommerceEvent.Builder(Promotion.CLICK, it)
                .productListName("Testing Product List Name")
                .productListSource("Testing source")
                .checkoutStep(2)
                .customAttributes(mapOf("Promotion ATT" to "Promotion Testing")).build()
        }

        val builder = MPCommerceMessage.Builder(event)
            .timestamp(12345) as MPCommerceMessage.Builder
        val message = builder.build(InternalSession(), null, 0)

        Assert.assertNotNull(message)
    }
}
