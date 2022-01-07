package com.mparticle.kits

import com.mparticle.*
import com.mparticle.internal.ConfigManager
import com.mparticle.kits.testkits.BaseTestKit
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class KitBatchingTest: BaseKitOptionsTest() {

    @Before
    fun before() {
        val options = MParticleOptions.builder(mContext)
                .configuration(
                        KitOptions()
                                .addKit(123, BatchKit::class.java)
                )
        startMParticle(options, mServer)
    }

    @Test
    fun testBatchArrives() {
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("some event").build())
            logEvent(MPEvent.Builder("some other event").build())
            upload()
        }
        (MParticle.getInstance()?.getKitInstance(123) as BatchKit).let { batchKit ->
            batchKit.await();
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
        //send another event, since batches aren't forwarded to the kits until after the upload,
        //this is the only way we can gaurantee an batch forwarding *could* have happened. Since it's
        //not supposed to happen when the upload fails, we can't await() in the kit like the previous test
        instance.apply {
            logEvent(MPEvent.Builder("some other event").build())
            upload()
        }
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl))


        val batchKit = (MParticle.getInstance()?.getKitInstance(123) as BatchKit)
        assertEquals(0, batchKit.batches.size)

        //see if it recovers...
        mServer.setupHappyEvents()
        val uploadsCount = getDatabaseContents(listOf("uploads")).getJSONArray("uploads").length()
        instance.upload()
        for (i in 1..uploadsCount) {
            batchKit.await()
        }

        assertEquals(2, batchKit.batches.size)
    }



    class BatchKit: BaseTestKit(), KitIntegration.BatchListener {
        var batches = mutableListOf<JSONObject>()
        override fun getName() = "Batch Kit"
        private var latch = MPLatch(1);

        fun await() {
            latch = MPLatch(1)
            latch.await()
        }

        override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
            //do nothing
            return listOf()
        }

        override fun logBatch(jsonObject: JSONObject): List<ReportingMessage> {
            batches.add(jsonObject)
            latch.countDown();
            return listOf()
        }
    }

    fun <T> JSONArray.toList(): List<Any> =  (0..length() -1).map { get(it) }
}