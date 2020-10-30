package com.mparticle.internal

import com.mparticle.MParticleOptions
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigRequestTests: BaseCleanInstallEachTest() {

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

        //make sure it works on subsiquent requests
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

        //make sure it works on subsiquent requests
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

        //make sure it works on subsiquent requests
        (0..2).forEach {
            AccessUtils.getUploadHandler().mApiClient.fetchConfig(true)
            mServer.waitForVerify(Matcher(mServer.Endpoints().configUrl)) {
                assertFalse { it.url.contains("plan_id") }
                assertFalse { it.url.contains("plan_version") }
            }
        }
    }
}