package com.mparticle.kits

import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.kits.testkits.*
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test
import java.util.concurrent.CountDownLatch

class KitManagerImplTests: BaseCleanInstallEachTest() {

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
        //somewhat contrived, but this is simulating the main thread is blocked/unresponsive
        Handler(Looper.getMainLooper()).post { latch.await() }

        //Force the SDK to make a config request (using the ugly internals)
        JSONObject().put("eks", ConfigManager.getInstance(mContext).latestKitConfiguration)
                .let {
                    ConfigManager.getInstance(mContext).updateConfig(it, true)
                }
        MParticle.reset(mContext)
        latch.countDown()
    }

}