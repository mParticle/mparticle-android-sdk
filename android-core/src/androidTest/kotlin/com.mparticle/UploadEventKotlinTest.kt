package com.mparticle

import android.os.Looper
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.messages.events.BaseEvent
import com.mparticle.messages.events.BatchMessage
import com.mparticle.messages.events.CommerceEventMessage
import com.mparticle.messages.events.MPEventMessage
import com.mparticle.messages.events.ScreenViewMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import org.junit.BeforeClass
import org.junit.Test

class UploadEventKotlinTest : BaseStartedTest() {

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
        }
    }

    @Test
    fun testMPEventUploadBypass() {
        val event = MPEvent.Builder("Should Not Upload")
            .shouldUploadEvent(false)
            .build()
        val event2 = MPEvent.Builder("Should Upload 1")
            .shouldUploadEvent(true)
            .build()
        val event3 = MPEvent.Builder("Should Upload 2")
            .build()

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.any<MPEventMessage> { it.name == "Should Upload 1" }
            }
            .and
            .assertWillReceive {
                it.body.any<MPEventMessage> { it.name == "Should Upload 2" }
            }
            .and
            .assertWillNotReceive {
                it.body.any<MPEventMessage> { it.name == "Should Not Upload" }
            }
            .after {
                MParticle.getInstance()?.logEvent(event)
                MParticle.getInstance()?.logEvent(event2)
                MParticle.getInstance()?.logEvent(event3)
                MParticle.getInstance()?.upload()
            }
            .blockUntilFinished()
        Server
            .endpoint(EndpointType.Events)
            .assertHasNotReceived { it.body.any<MPEventMessage> { it.name == "Should Not Upload" } }
    }

    @Test
    fun testMPScreenEventUploadBypass() {
        Server
            .endpoint(
                EndpointType.Events
            )
            .assertWillReceive {
                it.body
                    .run { any<ScreenViewMessage> { it.name == "Should Upload 1" } }
            }
            .after {
                MParticle.getInstance()?.logScreen("Should Not Upload", null, false)
                MParticle.getInstance()?.logScreen("Should Upload 1")
                MParticle.getInstance()?.logScreen("Should Upload 2", null)
                MParticle.getInstance()?.logScreen("Should Upload 3", null, true)
                MParticle.getInstance()?.upload()
            }
            .blockUntilFinished()
    }

    @Test
    fun testCommerceEventUploadBypass() {
        val product = Product.Builder("Should Not Upload", "sku1", 100.00)
            .build()
        val event = CommerceEvent.Builder(Product.ADD_TO_CART, product)
            .shouldUploadEvent(false)
            .build()
        var product2 = Product.Builder("Should Upload 1", "sku2", 100.00)
            .build()
        val event2 = CommerceEvent.Builder(Product.ADD_TO_CART, product2)
            .shouldUploadEvent(true)
            .build()
        var product3 = Product.Builder("Should Upload 2", "sku3", 100.00)
            .build()
        val event3 = CommerceEvent.Builder(Product.ADD_TO_CART, product3)
            .build()

        Server
            .endpoint(EndpointType.Events)
            .assertWillNotReceive {
                it.body.any<CommerceEventMessage> {
                    it.productActionObject?.productList?.firstOrNull()?.name == "Should Not Upload"
                }
            }.and
            .assertWillReceive {
                it.body.any<CommerceEventMessage> {
                    it.productActionObject?.productList?.firstOrNull()?.name == "Should Upload 1"
                }
            }.and
            .assertWillReceive {
                it.body.any<CommerceEventMessage> {
                    it.productActionObject?.productList?.firstOrNull()?.name == "Should Upload 2"
                }
            }
            .after {
                MParticle.getInstance()?.logEvent(event)
                MParticle.getInstance()?.logEvent(event2)
                MParticle.getInstance()?.logEvent(event3)
                MParticle.getInstance()?.upload()
            }
            .blockUntilFinished()
    }
}

inline fun <reified EventType : BaseEvent> BatchMessage.any(isMatch: (EventType) -> Boolean) = this.messages
    .filterIsInstance<EventType>()
    .any(isMatch)
