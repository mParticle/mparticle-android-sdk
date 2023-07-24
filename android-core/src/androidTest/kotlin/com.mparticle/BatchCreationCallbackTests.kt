package com.mparticle

import com.mparticle.internal.Constants
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.TestingUtils.assertJsonEqual
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BatchCreationCallbackTests : BaseCleanInstallEachTest() {

    @Test
    fun testListenerNoChange() {
        var receivedBatch: JSONObject? = null
        val options = MParticleOptions.builder(mContext)
            .batchCreationListener {
                receivedBatch = JSONObject(it.toString())
                it
            }
        startMParticle(options)

        MParticle.getInstance()?.apply {
            logEvent(
                MPEvent.Builder("test event")
                    .build()
            )
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == "test event" }
                    ?.also { isMatch ->
                        val modified = it.remove(Constants.MessageKey.MODIFIED_BATCH)
                        if (modified != null) {
                            assertTrue(modified.toString().toBooleanStrict())
                        }
                        if (isMatch) {
                            // check new key
                            assertJsonEqual(it, receivedBatch)
                        }
                    } ?: false
            }
        )
    }

    @Test
    fun testNullBatchCreationSENDwithoutModify() {
        val targetEventName = "should send without modified"

        val options = MParticleOptions.builder(mContext)
            .batchCreationListener(null)
        startMParticle(options)

        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder(targetEventName).build())
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any {
                        it.optString("n") == targetEventName && it.optString("mb").isNullOrEmpty()
                    } ?: false
            }
        )

        mServer.Requests().events.any {
            it.bodyJson.optJSONArray("msgs")
                ?.toList()
                ?.filterIsInstance<JSONObject>()
                ?.any { it.optString("n") == targetEventName && it.optString("mb").isNullOrEmpty() }
                ?: false
        }.let {
            assertTrue { it }
        }
    }

    @Test
    fun testNullOnBatchCreatedShouldNOTsend() {
        val targetEventName = "should not send"

        val options = MParticleOptions.builder(mContext)
            .batchCreationListener { null }
        startMParticle(options)

        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder(targetEventName).build())
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == targetEventName } ?: false
            }
        )

        mServer.Requests().events.any {
            it.bodyJson.optJSONArray("msgs")
                ?.toList()
                ?.filterIsInstance<JSONObject>()
                ?.any { it.optString("n") == targetEventName } ?: false
        }.let {
            assertFalse { it }
        }
    }

    @Test
    fun testListenerModified() {
        var newBatch = JSONObject().put("the whole", "batch")
        val targetEventName = "should not send"

        val options = MParticleOptions.builder(mContext)
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

        MParticle.getInstance()?.apply {
            logEvent(
                MPEvent.Builder(targetEventName)
                    .build()
            )
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                val modified = it.remove(Constants.MessageKey.MODIFIED_BATCH)
                if (modified != null) {
                    assertTrue(modified.toString().toBooleanStrict())
                }
                it.toString() == newBatch.toString()
            }
        )

        // make sure the upload queue is cleared
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("test").build())
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == "test" } ?: false
            }
        )

        mServer.Requests().events.any {
            it.bodyJson.optJSONArray("msgs")
                ?.toList()
                ?.filterIsInstance<JSONObject>()
                ?.any { it.optString("n") == targetEventName } ?: false
        }.let {
            assertFalse { it }
        }
    }

    @Test
    fun testListenerCrashes() {
        val targetEventName = "should send"
        val options = MParticleOptions.builder(mContext)
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

        // make sure the upload queue is cleared
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("test").build())
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == "test" } ?: false
            }
        )

        mServer.Requests().events.any {
            it.bodyJson.optJSONArray("msgs")
                ?.toList()
                ?.filterIsInstance<JSONObject>()
                ?.any { it.optString("n") == targetEventName } ?: false
        }.let {
            assertTrue { it }
        }
    }

    @Test
    fun testNoMPWhenNoListener() {
        startMParticle()
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("test").build())
            upload()
        }

        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch {
                it.optJSONArray("msgs")
                    ?.toList()
                    ?.filterIsInstance<JSONObject>()
                    ?.any { it.optString("n") == "test" }
                    ?.also { result ->
                        if (result) {
                            assertNull(it.opt(Constants.MessageKey.MODIFIED_BATCH))
                        }
                    } ?: false
            }
        )
    }
}

fun JSONArray.toList(): List<Any> = (0 until length()).map { this.get(it) }
