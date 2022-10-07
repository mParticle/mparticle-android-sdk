package com.mparticle.commerce

import com.mparticle.MParticle
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Test

class CommerceApiTest : BaseCleanStartedEachTest() {
    // just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    @Throws(InterruptedException::class)
    fun testCommerceProductEvent() {
        val product = Product.Builder("name", "sku", 10.00)
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.DETAIL, product)
            .build()
        MParticle.getInstance()?.logEvent(commerceEvent)
        MParticle.getInstance()?.upload()
        verifyEventSent()
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
        MParticle.getInstance()?.logEvent(commerceEvent)
        MParticle.getInstance()?.upload()
        verifyEventSent()
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
        MParticle.getInstance()?.logEvent(commerceEvent)
        MParticle.getInstance()?.upload()
        verifyEventSent()
    }

    @Throws(InterruptedException::class)
    private fun verifyEventSent() {
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl)
                .bodyMatch { jsonObject ->
                    var found = false
                    val messagesJSON = jsonObject.optJSONArray("msgs")
                    if (messagesJSON != null) {
                        for (i in 0 until messagesJSON.length()) {
                            val messageJSON = messagesJSON.optJSONObject(i)
                            if (messageJSON != null) {
                                val type = messageJSON.optString("dt")
                                if ("cm" == type) {
                                    found = true
                                }
                            }
                        }
                    }
                    found
                }
        )
    }
}
