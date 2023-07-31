package com.mparticle

import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Test
import kotlin.test.assertNotEquals

class UploadEventKotlinTest : BaseCleanStartedEachTest() {
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
        MParticle.getInstance()?.logEvent(event)
        MParticle.getInstance()?.logEvent(event2)
        MParticle.getInstance()?.logEvent(event3)
        MParticle.getInstance()?.upload()

        // Wait for an event that matched Matcher"
        // This matcher contains
        //     1) a url (mServer.Endpoints().eventsUrl
        //     2) a "body match" (bodyMatch {} )
        //
        // These 3 events are logged within the same upload loop, with the same mpid and sessionid, so they
        // will be logged in the same upload message. This logic will basically wait until the "Should Upload"
        // messages are received in an upload message and fail if that, or any previous message, contains the
        // "Should Not Upload" message
        var numUploadedEvents = 0
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")?.let { messagesArray ->
                    (0 until messagesArray.length())
                        .any {
                            val eventMessageName = messagesArray.getJSONObject(it).optString("n")
                            assertNotEquals("Should Not Upload", eventMessageName)
                            if (eventMessageName == "Should Upload 1" || eventMessageName == "Should Upload 2") {
                                numUploadedEvents++
                            }
                            numUploadedEvents == 2
                        }
                } ?: false
            }
        )
    }

    @Test
    fun testMPScreenEventUploadBypass() {
        MParticle.getInstance()?.logScreen("Should Upload 1")
        MParticle.getInstance()?.logScreen("Should Upload 2", null)
        MParticle.getInstance()?.logScreen("Should Upload 3", null, true)
        MParticle.getInstance()?.logScreen("Should Not Upload ", null, false)
        MParticle.getInstance()?.upload()

        // Wait for an event that matched Matcher"
        // This matcher contains
        //     1) a url (mServer.Endpoints().eventsUrl
        //     2) a "body match" (bodyMatch {} )
        //
        // These 3 events are logged within the same upload loop, with the same mpid and sessionid, so they
        // will be logged in the same upload message. This logic will basically wait until the "Should Upload"
        // messages are received in an upload message and fail if that, or any previous message, contains the
        // "Should Not Upload" message
        var numUploadedEvents = 0
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")?.let { messagesArray ->
                    (0 until messagesArray.length())
                        .any {
                            val eventMessageName = messagesArray.getJSONObject(it).optString("n")
                            assertNotEquals("Should Not Upload", eventMessageName)
                            if (eventMessageName == "Should Upload 1" || eventMessageName == "Should Upload 2" || eventMessageName == "Should Upload 3") {
                                numUploadedEvents++
                            }
                            numUploadedEvents == 3
                        }
                } ?: false
            }
        )
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
        MParticle.getInstance()?.logEvent(event)
        MParticle.getInstance()?.logEvent(event2)
        MParticle.getInstance()?.logEvent(event3)
        MParticle.getInstance()?.upload()

        // Wait for an event that matched Matcher"
        // This matcher contains
        //     1) a url (mServer.Endpoints().eventsUrl
        //     2) a "body match" (bodyMatch {} )
        //
        // These 3 events are logged within the same upload loop, with the same mpid and sessionid, so they
        // will be logged in the same upload message. This logic will basically wait until the "Should Upload"
        // messages are received in an upload message and fail if that, or any previous message, contains the
        // "Should Not Upload" message
        var numUploadedEvents = 0
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")?.let { messagesArray ->
                    (0 until messagesArray.length())
                        .any {
                            val eventProductName =
                                messagesArray.getJSONObject(it).optJSONObject("pd")
                                    ?.optJSONArray("pl")?.optJSONObject(0)?.optString("nm")
                            assertNotEquals("Should Not Upload", eventProductName)
                            if (eventProductName == "Should Upload 1" || eventProductName == "Should Upload 2") {
                                numUploadedEvents++
                            }
                            numUploadedEvents == 2
                        }
                } ?: false
            }
        )
    }
}
