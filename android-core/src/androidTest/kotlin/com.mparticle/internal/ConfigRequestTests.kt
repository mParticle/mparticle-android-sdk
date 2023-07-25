package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigRequestTests : BaseCleanInstallEachTest() {

    @Test
    fun testConfigRequestWithDataplanIdAndVersion() {
        MParticleOptions
            .builder(mContext)
            .credentials("key", "secret")
            .dataplan("dpId", 101)
            .let {
                startMParticle(it)
            }
        mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
            assertTrue { it.url.contains("plan_id=dpId") }
            assertTrue { it.url.contains("plan_version=101") }
        }

        // make sure it works on subsiquent requests
        AccessUtils.getUploadHandler().mApiClient.fetchConfig(true)
        (0..2).forEach {
            mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
                assertTrue { it.url.contains("plan_id=dpId") }
                assertTrue { it.url.contains("plan_version=101") }
            }
        }
    }

    @Test
    fun testConfigRequestWithDataplanIdNoVersion() {
        MParticleOptions
            .builder(mContext)
            .credentials("key", "secret")
            .dataplan("dpId", null)
            .let {
                startMParticle(it)
            }
        mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
            assertTrue { it.url.contains("plan_id") }
            assertFalse { it.url.contains("plan_version") }
        }

        // make sure it works on subsiquent requests
        (0..2).forEach {
            AccessUtils.getUploadHandler().mApiClient.fetchConfig(true)
            mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
                assertTrue { it.url.contains("plan_id") }
                assertFalse { it.url.contains("plan_version") }
            }
        }
    }

    @Test
    fun testConfigRequestWithDataplanVersionButNoId() {
        MParticleOptions
            .builder(mContext)
            .credentials("key", "secret")
            .dataplan(null, 2)
            .let {
                startMParticle(it)
            }
        mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
            assertFalse { it.url.contains("plan_id") }
            assertFalse { it.url.contains("plan_version") }
        }

        // make sure it works on subsiquent requests
        (0..2).forEach {
            AccessUtils.getUploadHandler().mApiClient.fetchConfig(true)
            mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
                assertFalse { it.url.contains("plan_id") }
                assertFalse { it.url.contains("plan_version") }
            }
        }
    }

    @Test
    fun testRemoteConfigApplied() {
        val latch = MPLatch(1)

        mServer.setupConfigResponse(simpleConfigWithKits.toString(), 100)
        setCachedConfig(JSONObject())
        mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) { request ->
            assertTrue {
                MPUtility.isEmpty(
                    MParticle.getInstance()!!.Internal().configManager.latestKitConfiguration
                )
            }
            latch.countDown()
        }
        startMParticle()
        latch.await()
        assertEquals(
            simpleConfigWithKits[ConfigManager.KEY_EMBEDDED_KITS].toString(),
            MParticle.getInstance()?.Internal()?.configManager?.latestKitConfiguration.toString()
        )
    }
}
