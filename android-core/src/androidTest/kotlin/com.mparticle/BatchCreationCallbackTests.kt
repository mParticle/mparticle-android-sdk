package com.mparticle

import com.mparticle.messages.DTO
import com.mparticle.messages.events.BatchMessage
import com.mparticle.messages.events.MPEventMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.Utils.assertJsonEqual
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.utils.startMParticle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatchCreationCallbackTests : BaseTest() {

    @Test
    fun testListenerNoChange() {
        var receivedBatch: JSONObject? = null
        val options = MParticleOptions.builder(context)
            .batchCreationListener {
                receivedBatch = JSONObject(it.toString())
                it
            }
        startMParticle(options)

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { it.name == "test event" }
                    .also { isMatch ->
                        assertTrue(it.body.modifiedBatch ?: false)
                        if (isMatch) {
                            val batchJson = JSONObject(
                                DTO.Companion.batchJsonBuilder.encodeToString(
                                    BatchMessage.serializer(),
                                    it.body
                                )
                            )
                            // remove modifyBatch key since it wouldn't be present in the batch forwarding
                            batchJson.remove("mb")
                            assertJsonEqual(batchJson, receivedBatch ?: JSONObject())
                        }
                    }
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(
                        MPEvent.Builder("test event")
                            .build()
                    )
                    upload()
                }
            }
            .blockUntilFinished()
    }

    @Test
    fun testListenerModified() {
        var newBatch = JSONObject().put("id", "12345")
        val targetEventName = "should not send"

        val options = MParticleOptions.builder(context)
            .batchCreationListener {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == targetEventName }
                    ?.let { result ->
                        if (result) {
                            JSONObject(newBatch.toString())
                        } else {
                            it
                        }
                    } ?: it
            }
        startMParticle(options)

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                assertTrue(it.body.modifiedBatch ?: false)
                val batchJson = JSONObject(
                    DTO.Companion.jsonBuilder.encodeToString(
                        BatchMessage.serializer(),
                        it.body
                    )
                )
                // remove "mb"
                batchJson.remove("mb")
                batchJson.toString() == newBatch.toString()
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(
                        MPEvent.Builder(targetEventName)
                            .build()
                    )
                    upload()
                }
            }
            .blockUntilFinished()

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { it.name == "test" }
            }
            .after {
                // make sure the upload queue is cleared
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("test").build())
                    upload()
                }
            }
            .blockUntilFinished()

        // make sure the original event was not sent
        Server
            .endpoint(EndpointType.Events)
            .requests
            .map { it.request }
            .any {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { message -> message.name == targetEventName }
            }
            .let {
                assertFalse() { it }
            }
    }

    @Test
    fun testListenerCrashes() {
        val targetEventName = "should send"
        val options = MParticleOptions.builder(context)
            .batchCreationListener {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == targetEventName }
                    ?.let { result ->
                        if (result) {
                            throw RuntimeException()
                        } else {
                            it
                        }
                    } ?: it
            }
        startMParticle(options)

        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder(targetEventName).build())
            upload()
        }

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { it.name == "test" }
            }
            .after {
                // make sure the upload queue is cleared
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("test").build())
                    upload()
                }
            }

        // make sure the original event was not sent
        Server
            .endpoint(EndpointType.Events)
            .requests
            .map {
                it.request
            }
            .any {
                it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { message -> message.name == targetEventName }
            }.let {
                assertFalse(it)
            }
    }

    @Test
    fun testNoMPWhenNoListener() {
        startMParticle()

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                val containsTargetMessage = it.body.messages
                    .filterIsInstance<MPEventMessage>()
                    .any { it.name == "test" }
                if (containsTargetMessage) {
                    assertNull(it.body.modifiedBatch)
                }
                containsTargetMessage
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("test").build())
                    upload()
                }
            }
            .blockUntilFinished()
    }
}

fun JSONArray.toList(): List<Any> = (0 until length()).map { this.get(it) }
