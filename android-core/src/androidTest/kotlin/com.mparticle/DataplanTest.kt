package com.mparticle

import com.mparticle.internal.Constants
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer
import com.mparticle.networking.MockServer.JSONMatch
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class DataplanTest : BaseCleanInstallEachTest() {
    var testingUtils: TestingUtils = TestingUtils.getInstance()
    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanTest() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan(null, null)
        )
        val messageCount = AndroidUtils.Mutable(0)
        val latch = MPLatch(1)
        MockServer.getInstance().waitForVerify(
            Matcher().bodyMatch(
                JSONMatch { bodyJson ->
                    try {
                        TestCase.assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value == 3) {
                            latch.countDown()
                            return@JSONMatch true
                        }
                    } catch (_: JSONException) {
                    }
                    false
                }), latch
        )
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertEquals(3, messageCount.value.toInt().toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanPartialTest() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan("plan1", null)
        )
        val messageCount = AndroidUtils.Mutable(0)
        val latch = MPLatch(1)
        MockServer.getInstance().waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch(
                JSONMatch { bodyJson ->
                    try {
                        Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        val dataplanContext =
                            bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                        val dataplanJSON =
                            dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                        Assert.assertEquals(
                            "plan1",
                            dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                        )
                        TestCase.assertNull(
                            dataplanJSON.optString(
                                Constants.MessageKey.DATA_PLAN_VERSION,
                                null
                            )
                        )
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value == 3) {
                            latch.countDown()
                            return@JSONMatch true
                        }
                    } catch (_: JSONException) {
                    }
                    false
                }), latch
        )
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertEquals(3, messageCount.value.toInt().toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun noDataPlanIdTest() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan(null, 1)
        )
        val messageCount = AndroidUtils.Mutable(0)
        val latch = MPLatch(1)
        MockServer.getInstance().waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch(
                JSONMatch { bodyJson ->
                    try {
                        TestCase.assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value == 3) {
                            latch.countDown()
                            return@JSONMatch true
                        }
                    } catch (_: JSONException) {
                    }
                    false
                }), latch
        )
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertEquals(3, messageCount.value.toInt().toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataPlanSetTest() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 1)
        )
        val messageCount = AndroidUtils.Mutable(0)
        val latch = MPLatch(1)
        MockServer.getInstance().waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch(
                JSONMatch { bodyJson ->
                    try {
                        Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                        val dataplanContext =
                            bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                        val dataplanJSON =
                            dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                        Assert.assertEquals(
                            "dataplan1",
                            dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                        )
                        Assert.assertEquals(
                            "1",
                            dataplanJSON.optString(Constants.MessageKey.DATA_PLAN_VERSION, "")
                        )
                        messageCount.value += getMessageCount(bodyJson)
                        if (messageCount.value == 3) {
                            latch.countDown()
                            return@JSONMatch true
                        }
                    } catch (ex: Exception) {
                        Assert.fail(ex.toString())
                    }
                    false
                }), latch
        )
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertEquals(3, messageCount.value.toInt().toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun dataplanChanged() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 1)
        )
        val totalMessageCount = AndroidUtils.Mutable(0)
        val dataplan1MessageCount = AndroidUtils.Mutable(0)
        val dataplan2MessageCount = AndroidUtils.Mutable(0)
        val latch = MPLatch(1)
        MockServer.getInstance()
            .waitForVerify(Matcher(mServer.Endpoints().eventsUrl).bodyMatch { bodyJson ->
                try {
                    Assert.assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT))
                    val dataplanContext =
                        bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT)
                    val dataplanJSON =
                        dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY)
                    val dataplanId = dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID)
                    val dataplanVersion =
                        dataplanJSON.optInt(Constants.MessageKey.DATA_PLAN_VERSION, -1)
                    val messageCount = getMessageCount(bodyJson)
                    if (1 == dataplanVersion) {
                        Assert.assertEquals("dataplan1", dataplanId)
                        dataplan1MessageCount.value += messageCount
                    }
                    if (2 == dataplanVersion) {
                        Assert.assertEquals("dataplan1", dataplanId)
                        dataplan2MessageCount.value += messageCount
                    }
                    totalMessageCount.value += messageCount
                    if (totalMessageCount.value == 5) {
                        latch.countDown()
                    }
                } catch (ex: Exception) {
                    Assert.fail(ex.toString())
                }
                false
            }, latch)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 2)
        )
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.logEvent(testingUtils.randomMPEventRich)
        MParticle.getInstance()?.upload()

        //not sure why it needs upload() twice, but this cuts the runtime down from 10s to .7s
        MParticle.getInstance()?.upload()
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertEquals(3, dataplan1MessageCount.value.toInt().toLong())
        Assert.assertEquals(2, dataplan2MessageCount.value.toInt().toLong())
        Assert.assertEquals(5, totalMessageCount.value.toInt().toLong())
    }

    @Throws(JSONException::class)
    private fun getMessageCount(bodyJson: JSONObject): Int {
        var count = 0
        val messages = bodyJson.optJSONArray("msgs")
        if (messages != null) {
            for (i in 0 until messages.length()) {
                val messageJSON = messages.getJSONObject(i)
                if (messageJSON.getString("dt") == "e") {
                    count++
                }
            }
        }
        return count
    }
}