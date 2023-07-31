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
import com.mparticle.networking.Matcher
import com.mparticle.testutils.MPLatch
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KitManagerImplTests : BaseKitOptionsTest() {

    @Test
    fun testKitIntializationViaKitOptions() {
        KitOptions()
            .addKit(1001, AttributeListenerTestKit::class.java)
            .addKit(1002, IdentityListenerTestKit::class.java)
            .addKit(1003, UserAttributeListenerTestKit::class.java)
            .let {
                MParticleOptions.builder(mContext)
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
        val latch = MPLatch(1)
        // somewhat contrived, but this is simulating the main thread is blocked/unresponsive
        Handler(Looper.getMainLooper()).post { latch.await() }

        // Force the SDK to make a config request (using the ugly internals)
        JSONObject().put("eks", ConfigManager.getInstance(mContext).latestKitConfiguration)
            .let {
                ConfigManager.getInstance(mContext).updateConfig(it)
            }
        MParticle.reset(mContext)
        latch.countDown()
    }

    @Test
    fun testActiveKitReporting() {
        MParticleOptions.builder(mContext)
            .configuration(
                ConfiguredKitOptions {
                    addKit(-1, AttributeListenerTestKit::class.java, JSONObject().put("eau", true))
                    addKit(-2, IdentityListenerTestKit::class.java)
                    addKit(-3, UserAttributeListenerTestKit::class.java)
                    addKit(-4, AttributeListenerTestKit::class.java, JSONObject().put("eau", true))
                    addKit(-5, IdentityListenerTestKit::class.java)
                    addKit(-6, UserAttributeListenerTestKit::class.java)
                }.apply {
                    // do not make a config for kits with id -2 or -5..This will mimic having the
                    // kit dependency in the classpath, but not in the /config response
                    testingConfiguration[-2] = null
                    testingConfiguration[-5] = null
                }
            ).let {
                startMParticle(it)
                waitForKitReload() {
                    MParticle.getInstance()?.Internal()?.configManager?.setMpid(123, false)
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
        val expectedBundledKits = "-6,-5,-4,-3,-2.-1"
        assertEquals(
            expectedActiveKits,
            MParticle.getInstance()?.Internal()?.configManager?.activeModuleIds
        )

        // check that the active kits value is sent to the server
        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("some event").build())
            upload()
        }
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl)) {
            assertEquals(it.headers["x-mp-kits"], expectedActiveKits)
            assertEquals(it.headers["x-mp-bundled-kits"], expectedBundledKits)
        }
    }
}
