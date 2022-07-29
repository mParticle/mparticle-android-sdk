package com.mparticle.commerce

import com.mparticle.MParticle
import com.mparticle.messages.events.CommerceEventMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import org.junit.Test

class CommerceApiTest : BaseStartedTest() {
    // just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    @Throws(InterruptedException::class)
    fun testCommerceProductEvent() {
        val product = Product.Builder("name", "sku", 10.00)
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.DETAIL, product)
            .build()
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<CommerceEventMessage>()
                    .any { it.productActionObject?.action == Product.DETAIL }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(commerceEvent)
                    upload()
                }
            }
            .blockUntilFinished()
    }

    // just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    @Throws(InterruptedException::class)
    fun testCommercePromotionEvent() {
        val promotion = Promotion()
            .setName("name")
            .setId("123")
        val commerceEvent = CommerceEvent.Builder(Promotion.CLICK, promotion)
            .build()
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<CommerceEventMessage>()
                    .any { it.promotionActionObject?.action == Promotion.CLICK }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(commerceEvent)
                    upload()
                }
            }
            .blockUntilFinished()
    }

    // just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    @Throws(InterruptedException::class)
    fun testCommerceImpressionEvent() {
        val product = Product.Builder("name", "sku", 10.00)
            .build()
        val impression = Impression("my impression", product)
        val commerceEvent = CommerceEvent.Builder(impression)
            .build()

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<CommerceEventMessage>()
                    .any { it.impressionObject?.firstOrNull()?.name == "my impression" }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(commerceEvent)
                    upload()
                }
            }
            .blockUntilFinished()
    }
}
