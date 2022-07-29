package com.mparticle.kits

import android.os.Handler
import android.os.Looper
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.kits.testkits.AttributeListenerTestKit
import com.mparticle.kits.testkits.IdentityListenerTestKit
import com.mparticle.kits.testkits.UserAttributeListenerTestKit
import com.mparticle.messages.KitConfigMessage
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KitManagerImplTests : BaseKitOptionsTest() {

    @Before
    fun before() {
        MParticle.setInstance(null)
    }

    @Test
    fun testKitIntializationViaKitOptions() {
        ConfiguredKitOptions {
            addKit(AttributeListenerTestKit::class.java, 1001)
            addKit(IdentityListenerTestKit::class.java, 1002)
            addKit(UserAttributeListenerTestKit::class.java, 1003)
        }
            .let {
                MParticleOptions.builder(context)
                    .configuration(it)
            }
            .let {
                startMParticle(it)
            }
        fun getKit(kitId: Int) = MParticle.getInstance()?.getKitInstance(kitId)

        assertTrue(getKit(1001) is AttributeListenerTestKit)
        assertTrue(getKit(1002) is IdentityListenerTestKit)
        assertTrue(getKit(1003) is UserAttributeListenerTestKit)
    }

    @Test
    fun testKitConfigParsingFailsGracefullyOnReset() {
        val latch = FailureLatch()
        // somewhat contrived, but this is simulating the main thread is blocked/unresponsive
        Handler(Looper.getMainLooper()).post { latch.await() }

        // Force the SDK to make a config request (using the ugly internals)
        JSONObject().put("eks", ConfigManager.getInstance(context).latestKitConfiguration)
            .let {
                ConfigManager.getInstance(context).updateConfig(it)
            }
        MParticle.reset(context)
        latch.countDown()
    }

    @Test
    fun testActiveKitReporting() {
        MParticleOptions.builder(context)
            .configuration(
                ConfiguredKitOptions {
                    addKit(AttributeListenerTestKit::class.java, KitConfigMessage(-1).apply { excludeAnnonymousUsers = true })
                    addKit(IdentityListenerTestKit::class.java, -2, false)
                    addKit(UserAttributeListenerTestKit::class.java, -3)
                    addKit(AttributeListenerTestKit::class.java, KitConfigMessage(-4).apply { excludeAnnonymousUsers = true })
                    addKit(IdentityListenerTestKit::class.java, -5, false)
                    addKit(UserAttributeListenerTestKit::class.java, -6)
                }
            ).let {
                startMParticle(it)
                waitForKitReload() {
                    MParticle.getInstance()?.Internal()?.configManager?.setMpid(1234, false)
                }
            }
        val kitStatus = MParticle.getInstance()?.Internal()?.kitManager?.kitStatus
        assertNotNull(kitStatus)
        if (kitStatus == null) {
            return
        }
        assertEquals(6, kitStatus.size)
        assertEquals(kitStatus[-1], KitManager.KitStatus.STOPPED)
        assertEquals(kitStatus[-2], KitManager.KitStatus.NOT_CONFIGURED)
        assertEquals(kitStatus[-3], KitManager.KitStatus.ACTIVE)
        assertEquals(kitStatus[-4], KitManager.KitStatus.STOPPED)
        assertEquals(kitStatus[-5], KitManager.KitStatus.NOT_CONFIGURED)
        assertEquals(kitStatus[-6], KitManager.KitStatus.ACTIVE)

        // double check that ConfigManager is generating the right string
        val expectedActiveKits = "-6,-4,-3,-1"
        val expectedBundledKits = "-1,-2,-3,-4,-5,-6"
        assertEquals(expectedActiveKits, MParticle.getInstance()?.Internal()?.configManager?.activeModuleIds)

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.headers?.let {
                    assertEquals(expectedActiveKits, it["x-mp-kits"])
                    assertEquals(expectedBundledKits, it["x-mp-bundled-kits"])
                    true
                } ?: false
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("some event").build())
                    upload()
                }
            }
            .blockUntilFinished()
    }
}
