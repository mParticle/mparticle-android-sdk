package com.mparticle

import android.os.Handler
import android.os.Looper
import com.mparticle.internal.Constants
import com.mparticle.internal.EmptyApiClient
import com.mparticle.internal.MPUtility
import com.mparticle.messages.events.MPEventMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.orThrow
import com.mparticle.utils.randomMPEventRich
import com.mparticle.utils.setMParticleApiClient
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class UploadMessageTest : BaseStartedTest() {
    /**
     * set MPID, log between 0 and 20 random MPEvents, and check to make sure each one is properly
     * attributed to the correct MPID, and there are no duplicates
     */
    @Test
    @Throws(Exception::class)
    fun testCorrectlyAttributeEventsToMpid() {
        val numberOfEvents = 3
        val handler: Handler = Handler(Looper.getMainLooper())
        val mpid: Long = Random.Default.nextLong()
        MParticle.getInstance().orThrow().Internal().configManager.setMpid(mpid, Random.Default.nextBoolean())
        val events: MutableMap<String, MPEvent> = HashMap()
        val latch: CountDownLatch = FailureLatch(count = numberOfEvents)
        val matchingJSONEvents: MutableMap<Long, MutableMap<String, JSONObject>> = HashMap()
        setMParticleApiClient(object : EmptyApiClient() {
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
                            for (i in 0 until jsonArray.length()) {
                                val eventObject = jsonArray.getJSONObject(i)
                                if (eventObject.getString("dt") == Constants.MessageType.EVENT) {
                                    val eventName = eventObject.getString("n")
                                    val matchingEvent = events[eventName]
                                    if (matchingEvent != null) {
                                        val eventType = eventObject.getString("et")
                                        if (matchingEvent.eventType.toString() == eventType) {
                                            if (matchingMpidJSONEvents!!.containsKey(eventName)) {
                                                Assert.fail("Duplicate Event Message Sent")
                                            } else {
                                                matchingMpidJSONEvents!![eventName] = eventObject
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
            val event: MPEvent = randomMPEventRich()
            if (events.containsKey(event.eventName)) {
                j--
            } else {
                events[event.eventName] = event
                MParticle.getInstance()!!.logEvent(event)
            }
            j++
        }
        MParticle.getInstance()!!.upload()
        latch.await()
        val jsonMap: Map<String, JSONObject> = matchingJSONEvents[mpid]!!
        if (events.size > 0) {
            Assert.assertNotNull(jsonMap)
        }
        if (events != null && events.size != 0 && events.size != jsonMap.size) {
            assertEquals(events.size.toLong(), jsonMap.size.toLong())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEventAccuracy() {
        val receivedEvents = mutableMapOf<String, MPEventMessage>()
        val sentEvents: MutableMap<String, MPEvent> = HashMap()
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .let {
                        it.forEach { event ->
                            assertNull(receivedEvents[event.name])
                            receivedEvents[event.name!!] = event
                        }
                    }
                receivedEvents.size == sentEvents.size
            }
            .after {
                (0..3).forEach {
                    val event: MPEvent = randomMPEventRich()
                    if (!sentEvents.containsKey(event.eventName)) {
                        sentEvents[event.eventName] = event
                        MParticle.getInstance()!!.logEvent(event)
                    }
                }
                MParticle.getInstance()?.upload()
            }
            .blockUntilFinished()

        sentEvents.values.forEach {
            assertEventEquals(it, receivedEvents[it.eventName])
        }
    }

    @Throws(JSONException::class)
    fun assertEventEquals(mpEvent: MPEvent, eventMessage: MPEventMessage?) {
        if (eventMessage == null) {
            assertNotNull(eventMessage)
            return
        }
        if (eventMessage.name !== mpEvent.eventName) {
            Assert.assertTrue(mpEvent.eventName == eventMessage.name)
        }
        if (mpEvent.length != null || eventMessage.eventDuration != null) {
            assertEquals(mpEvent.length!!, eventMessage.eventDuration!!, .1)
        }
        if (mpEvent.eventType.name != eventMessage.eventType?.name) {
            assertEquals(mpEvent.eventType.name, eventMessage.eventType?.name)
        }
        val customAttributesTarget =
            if (mpEvent.customAttributes == null) mapOf() else mpEvent.customAttributes!!
        val customAttributes = mpEvent.customAttributes ?: mapOf()
        assertEquals(customAttributesTarget.size, customAttributes.size)
        customAttributes
            .filter { it.value != customAttributesTarget[it.key] }
            .filter { it.value == "null" && customAttributesTarget[it.key] == null }
            .filter { it.key != "EventLength" }
            .let {
                assertEquals("extra keys in message: ${it.entries.joinToString { it.toString() }}", 0, it.size)
            }
        customAttributesTarget
            .filter { !customAttributes.containsKey(it.key) }.let {
                assertEquals("missing keys in message: ${it.entries.joinToString { it.toString() }}", 0, it.size)
            }
        val customFlagTarget = mpEvent.customFlags ?: mapOf()
        val customFlags = eventMessage.eventFlags
        if (customFlags != null) {
            customFlags.keys
                .forEach {
                    with(customFlags[it]) {
                        when (this) {
                            is List<*> -> assertArraysEqual(this, customFlagTarget[it] ?: listOf())
                            else -> assertEquals(this.toString(), customFlagTarget[it].toString())
                        }
                    }
                }
        }
    }

    @Throws(JSONException::class)
    fun assertArraysEqual(messageValue: List<*>, testValue: List<String>) {
        val jsonArrayList: MutableList<String> = ArrayList()
        for (i in 0 until messageValue.size) {
            jsonArrayList.add(messageValue.get(i).toString())
        }
        assertEquals(testValue.size.toLong(), jsonArrayList.size.toLong())
        Collections.sort(testValue)
        Collections.sort(jsonArrayList)
        for (i in testValue.indices) {
            val a = testValue[i]
            val b = messageValue[i]
            if (a == null) {
                assertEquals(b, "null")
            } else {
                assertEquals(a, b)
            }
        }
    }
}
