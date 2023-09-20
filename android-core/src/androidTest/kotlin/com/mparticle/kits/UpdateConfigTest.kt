package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtils
import com.mparticle.kits.testkits.BaseTestKit
import com.mparticle.testutils.MPLatch
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateConfigTest : BaseKitOptionsTest() {

    @Test
    fun testKitsLoadFromRemoteConfig() {
        setCachedConfig(null)
        MParticleOptions.builder(mContext)
            .configuration(
                ConfiguredKitOptions {
                    addKit(1, BaseTestKit::class.java)
                    addKit(2, BaseTestKit::class.java, null)
                    addKit(3, BaseTestKit::class.java)
                    addKit(4, BaseTestKit::class.java, null)
                    addKit(5, BaseTestKit::class.java)
                }
            ).let {
                startMParticle(it)
            }

        waitForKitToStart(1)

        MParticle.getInstance()!!.let {
            assertNotNull(it.getKitInstance(1))
            assertNull(it.getKitInstance(2))
            assertNotNull(it.getKitInstance(3))
            assertNull(it.getKitInstance(4))
            assertNotNull(it.getKitInstance(5))
        }
    }

    @Test
    fun testStartKitWithNewRemoteConfig() {
        setCachedConfig(null)
        MParticleOptions.builder(mContext)
            .configuration(
                ConfiguredKitOptions {
                    addKit(1, BaseTestKit::class.java)
                    addKit(2, BaseTestKit::class.java, null)
                }
            ).let {
                startMParticle(it)
            }

        waitForKitToStart(1)

        mServer.setupConfigResponse(
            JSONObject()
                .put(
                    "eks",
                    JSONArray()
                        .put(JSONObject().put("id", 1))
                        .put(JSONObject().put("id", 2))
                ).toString()
        )

        AccessUtils.getKitManager().addKitsLoadedListener { kits, previousKits, kitConfigs ->
            assertEquals(1, previousKits.size)
            assertEquals(2, kits.size)
            assertTrue(previousKits.containsKey(1))
            assertTrue(kits.containsKey(1))
            assertTrue(kits.containsKey(2))
        }
    }

    @Test
    fun testShutdownKitWithNewRemoteConfig() {
        setCachedConfig(null)
        MParticleOptions.builder(mContext)
            .configuration(
                ConfiguredKitOptions {
                    addKit(1, BaseTestKit::class.java)
                    addKit(2, BaseTestKit::class.java)
                }
            ).let {
                startMParticle(it)
            }

        mServer.setupConfigResponse(
            JSONObject()
                .put(
                    "eks",
                    JSONArray()
                        .put(JSONObject().put("id", 1))
                ).toString()
        )

        val latch = MPLatch(1)
        AccessUtils.getKitManager().addKitsLoadedListener { kits, previousKits, kitConfigs ->
            assertEquals(2, previousKits.size)
            assertEquals(1, kits.size)
            assertTrue(previousKits.containsKey(1))
            assertTrue(previousKits.containsKey(2))
            assertTrue(kits.containsKey(1))
            latch.countDown()
        }
        AccessUtils.forceFetchConfig()
        latch.await()
    }
}
