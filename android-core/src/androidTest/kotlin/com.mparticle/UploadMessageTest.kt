package com.mparticle

import android.os.Handler
import android.os.Looper
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.AccessUtils.EmptyMParticleApiClient
import com.mparticle.internal.Constants
import com.mparticle.internal.MPUtility
import com.mparticle.internal.MParticleApiClientImpl.MPRampException
import com.mparticle.internal.MParticleApiClientImpl.MPThrottleException
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.TestingUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch

class UploadMessageTest : BaseCleanStartedEachTest() {
    /**
     * set MPID, log between 0 and 20 random MPEvents, and check to make sure each one is properly
     * attributed to the correct MPID, and there are no duplicates
     */
    @Test
    @Throws(Exception::class)
    fun testCorrectlyAttributeEventsToMpid() {
        val numberOfEvents = 3
        val handler = Handler(Looper.getMainLooper())
        val mpid = ran.nextLong()
        MParticle.getInstance()?.mInternal?.configManager?.setMpid(mpid, ran.nextBoolean())
        val events: MutableMap<String, MPEvent> = HashMap()
        val latch: CountDownLatch = MPLatch(numberOfEvents)
        val matchingJSONEvents: MutableMap<Long, MutableMap<String, JSONObject>> = HashMap()
        AccessUtils.setMParticleApiClient(object : EmptyMParticleApiClient() {
            @Throws(IOException::class, MPThrottleException::class, MPRampException::class)
            override fun sendMessageBatch(message: String): Int {
                handler.post {
                    try {
                        val jsonObject = JSONObject(message)
                        val jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES)
                        val mpid = java.lang.Long.valueOf(jsonObject.getString("mpid"))
                        var matchingMpidJSONEvents = matchingJSONEvents[mpid]
                        if (matchingMpidJSONEvents == null) {
                            matchingJSONEvents[mpid] =
                                HashMap<String, JSONObject>().also { matchingMpidJSONEvents = it }
                        }
                        if (!MPUtility.isEmpty(jsonArray)) {
                            if (jsonArray != null) {
                                for (i in 0 until jsonArray.length()) {
                                    val eventObject = jsonArray.getJSONObject(i)
                                    if (eventObject.getString("dt") == Constants.MessageType.EVENT) {
                                        val eventName = eventObject.getString("n")
                                        val matchingEvent = events[eventName]
                                        if (matchingEvent != null) {
                                            val eventType = eventObject.getString("et")
                                            if (matchingEvent.eventType.toString() == eventType) {
                                                if (matchingMpidJSONEvents?.containsKey(eventName) == true) {
                                                    Assert.fail("Duplicate Event Message Sent")
                                                } else {
                                                    matchingMpidJSONEvents?.set(eventName,
                                                        eventObject
                                                    )
                                                }
                                            } else {
                                                Assert.fail("Unknown Event")
                                            }
                                        } else {
                                            Assert.fail("Unknown Event")
                                        }
                                        latch.countDown()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Assert.fail(e.toString())
                    }
                }
                return 202
            }
        })
        var j = 0
        while (j < 3) {
            val event = TestingUtils.getInstance().randomMPEventRich
            if (events.containsKey(event.eventName)) {
                j--
            } else {
                events[event.eventName] = event
                MParticle.getInstance()?.logEvent(event)
            }
            j++
        }
        MParticle.getInstance()?.upload()
        latch.await()
        val jsonMap: MutableMap<String, JSONObject>? = matchingJSONEvents[mpid]
        if (events.isNotEmpty()) {
            Assert.assertNotNull(jsonMap)
        }
        if (jsonMap != null) {
            if (events.isNotEmpty() && events.size != jsonMap.size) {
                Assert.assertEquals(events.size.toLong(), jsonMap.size.toLong())
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEventAccuracy() {
        val receivedEvents: MutableMap<String, MPEvent> = HashMap()
        val sentEvents: MutableMap<String, JSONObject> = HashMap()
        val latch: CountDownLatch = MPLatch(1)
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl)) { request ->
            try {
                val jsonObject = request.bodyJson
                val jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES)
                if (!MPUtility.isEmpty(jsonArray)) {
                    if (jsonArray != null) {
                        for (i in 0 until jsonArray.length()) {
                            val eventObject = jsonArray.getJSONObject(i)
                            if (eventObject.getString("dt") == Constants.MessageType.EVENT) {
                                val eventName = eventObject.getString("n")
                                if (sentEvents.containsKey(eventName)) {
                                    Assert.fail("Duplicate Event")
                                } else {
                                    sentEvents[eventName] = eventObject
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Assert.fail(e.toString())
            }
            if (sentEvents.size == receivedEvents.size) latch.countDown()
        }
        var j = 0
        while (j < 3) {
            val event = TestingUtils.getInstance().randomMPEventRich
            if (receivedEvents.containsKey(event.eventName)) {
                j--
            } else {
                receivedEvents[event.eventName] = event
                MParticle.getInstance()?.logEvent(event)
            }
            j++
        }
        MParticle.getInstance()?.upload()
        latch.await()
        for ((key, value) in receivedEvents) {
            if (!sentEvents.containsKey(key)) {
                Assert.assertNull(value)
            } else {
                Assert.assertTrue(sentEvents.containsKey(key))
                val jsonObject = sentEvents[key]
                if (jsonObject != null) {
                    assertEventEquals(value, jsonObject)
                }
            }
        }
    }

    @Throws(JSONException::class)
    fun assertEventEquals(mpEvent: MPEvent, jsonObject: JSONObject) {
        if (jsonObject.optString("n") !== mpEvent.eventName) {
            Assert.assertTrue(mpEvent.eventName == jsonObject.getString("n"))
        }
        if (mpEvent.length != null || jsonObject.has("el")) {
            mpEvent.length?.let { Assert.assertEquals(it, jsonObject.getDouble("el"), .1) }
        }
        if (mpEvent.eventType.toString() != jsonObject.optString("et")) {
            Assert.assertTrue(mpEvent.eventType.toString() == jsonObject.getString("et"))
        }
        val customAttributesTarget =
            if (mpEvent.customAttributeStrings == null) HashMap() else mpEvent.customAttributeStrings
        val customAttributes = jsonObject.optJSONObject("attrs")
        if (customAttributes != null) {
            val keysIterator = customAttributes.keys()
            while (keysIterator.hasNext()) {
                val key = customAttributes.keys().next()
                val jsonVal = keysIterator.next()
                val objVal = customAttributesTarget?.get(key)
                if (jsonVal !== objVal) {
                    val `val` = customAttributes.getString(key)
                    if (`val` != customAttributesTarget!![key] && key != "EventLength" && !(`val` == "null" && objVal == null)) {
                        Assert.assertTrue(customAttributes.getString(key) == customAttributesTarget[key])
                    }
                }
            }
        }
        val customFlagTarget = mpEvent.customFlags
        val customFlags = jsonObject.optJSONObject("flags")
        if (customFlags != null) {
            val flagsIterator = customFlags.keys()
            while (flagsIterator.hasNext()) {
                val key = flagsIterator.next()
                val values = customFlags.getJSONArray(key)
                val flags = customFlagTarget?.get(key)
                if (flags != null) {
                    assertArraysEqual(values, flags)
                }
            }
        }
    }

    @Throws(JSONException::class)
    fun assertArraysEqual(jsonArray: JSONArray, list: MutableList<String>) {
        val jsonArrayList: MutableList<String> = ArrayList()
        for (i in 0 until jsonArray.length()) {
            jsonArrayList.add(jsonArray.getString(i))
        }
        Assert.assertEquals(list.size.toLong(), jsonArrayList.size.toLong())
        list.sort()
        jsonArrayList.sort()
        for (i in list.indices) {
            val a = list[i]
            val b = jsonArrayList[i]
            Assert.assertTrue(a == b)
        }
    }
}