package com.mparticle.kits

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.testkits.BaseTestKit
import com.mparticle.kits.testkits.EventTestKit
import com.mparticle.networking.Matcher
import com.mparticle.testutils.MPLatch
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KitBatchingTest : BaseKitOptionsTest() {

    @Before
    fun before() {
        val options = MParticleOptions.builder(mContext)
            .configuration(
                KitOptions()
                    .addKit(123, BatchKit::class.java)
                    .addKit(456, EventTestKit::class.java)
            )
        startMParticle(options)
    }

    @Test
    fun testEventAttributesRetainsOriginalTypes() {
        val latch = MPLatch(1)
        var receivedEvent: MPEvent? = null
        (MParticle.getInstance()?.getKitInstance(456) as EventTestKit).let { kit ->
            kit.onLogEvent = { event ->
                receivedEvent = event
                latch.countDown()
                null
            }
        }
        val event = MPEvent.Builder("some event")
            .customAttributes(
                mapOf(
                    "String" to "String",
                    "Long" to 100L,
                    "Double" to 1.1,
                    "Map" to mapOf("foo" to "bar", "buzz" to false)
                )
            )
            .build()
        MParticle.getInstance()?.logEvent(event)
        latch.await()

        assertNotNull(receivedEvent)
        receivedEvent!!.customAttributes!!.let {
            assertTrue(it["String"] is String)
            assertTrue(it["Long"] is Long)
            assertTrue(it["Double"] is Double)
            assertTrue(it["Map"] is Map<*, *>)
            val originalAttributes = event.customAttributes!!
            it.forEach { key, value -> assertEquals(value, originalAttributes[key]) }
        }
    }

    @Test
    fun testBatchArrives() {
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("some event").build())
            logEvent(MPEvent.Builder("some other event").build())
            upload()
        }
        (MParticle.getInstance()?.getKitInstance(123) as BatchKit).let { batchKit ->
            batchKit.await()
            assertEquals(1, batchKit.batches.size)
            with(batchKit.batches[0].getJSONArray("msgs").toList<Any>()) {
                assertTrue(size >= 4)
                assertTrue(any { (it as? JSONObject)?.getString("dt") == "fr" })
                assertTrue(any { (it as? JSONObject)?.getString("dt") == "ss" })
                assertEquals(2, filter { (it as? JSONObject)?.getString("dt") == "e" }.size)
            }
        }
    }

    @Test
    fun testBatchNotForwardedOnNetworkFailure() {
        val instance = MParticle.getInstance()!!
        mServer.setEventsResponseLogic(500)
        instance.apply {
            logEvent(MPEvent.Builder("some event").build())
            upload()
        }
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl))
        // send another event, since batches aren't forwarded to the kits until after the upload,
        // this is the only way we can gaurantee an batch forwarding *could* have happened. Since it's
        // not supposed to happen when the upload fails, we can't await() in the kit like the previous test
        instance.apply {
            logEvent(MPEvent.Builder("some other event").build())
            upload()
        }
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl))

        val batchKit = (MParticle.getInstance()?.getKitInstance(123) as BatchKit)
        assertEquals(0, batchKit.batches.size)

        // see if it recovers...
        mServer.setupHappyEvents()
        val uploadsCount = getDatabaseContents(listOf("uploads")).getJSONArray("uploads").length()
        instance.upload()
        for (i in 1..uploadsCount) {
            batchKit.await()
        }

        assertEquals(2, batchKit.batches.size)
    }

    class BatchKit : BaseTestKit(), KitIntegration.BatchListener {
        var batches = mutableListOf<JSONObject>()
        override fun getName() = "Batch Kit"
        private var latch = MPLatch(1)

        fun await() {
            latch = MPLatch(1)
            latch.await()
        }

        override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
            // do nothing
            return listOf()
        }

        override fun logBatch(jsonObject: JSONObject): List<ReportingMessage> {
            batches.add(jsonObject)
            latch.countDown()
            return listOf()
        }
    }

    fun <T> JSONArray.toList(): List<Any> = (0..length() - 1).map { get(it) }
}
