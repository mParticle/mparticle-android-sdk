package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtil
import com.mparticle.internal.AccessUtils
import com.mparticle.kits.testkits.BaseTestKit
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.KitConfigMessage
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateConfigTest : BaseKitOptionsTest() {

    @Test
    fun testKitsLoadFromRemoteConfig() {
        mockingPlatforms.setCachedConfig(null)
        MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions {
                    addKit(BaseTestKit::class.java, 1)
                    addKit(BaseTestKit::class.java, 2, false)
                    addKit(BaseTestKit::class.java, 3)
                    addKit(BaseTestKit::class.java, 4, false)
                    addKit(BaseTestKit::class.java, 5)
                }
            ).let {
                startMParticle(it)
            }

        waitForKitToStart(5)

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
        mockingPlatforms.setCachedConfig(null)
        MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions {
                    addKit(BaseTestKit::class.java, 1)
                    addKit(BaseTestKit::class.java, 2)
                }
            ).let {
                startMParticle(it)
            }

        waitForKitToStart(1)

        Server
            .endpoint(EndpointType.Config)
            .addResponseLogic {
                SuccessResponse {
                    responseObject = ConfigResponseMessage().apply {
                        kits = listOf(
                            KitConfigMessage(1),
                            KitConfigMessage(2)
                        )
                    }
                }
            }

        AccessUtil.kitManager().addKitsLoadedListener { kits, previousKits, kitConfigs ->
            assertEquals(1, previousKits.size)
            assertEquals(2, kits.size)
            assertTrue(previousKits.containsKey(1))
            assertTrue(kits.containsKey(1))
            assertTrue(kits.containsKey(2))
        }
    }

    @Test
    fun testShutdownKitWithNewRemoteConfig() {
        mockingPlatforms.setCachedConfig(null)
        MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions {
                    addKit(BaseTestKit::class.java, 1)
                    addKit(BaseTestKit::class.java, 2)
                }
            ).let {
                startMParticle(it)
            }

        Server
            .endpoint(EndpointType.Config)
            .addResponseLogic {
                SuccessResponse {
                    responseObject = ConfigResponseMessage().apply {
                        kits = listOf(KitConfigMessage(1))
                    }
                }
            }

        val latch = FailureLatch()
        AccessUtil.kitManager().addKitsLoadedListener { kits, previousKits, kitConfigs ->
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
