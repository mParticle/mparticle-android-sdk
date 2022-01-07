package com.mparticle.kits

import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.kits.testkits.*
import com.mparticle.testutils.MPLatch
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class KitManagerImplTests: BaseKitOptionsTest() {

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
                    startMParticle(it, mServer)
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
                    ConfigManager.getInstance(mContext).updateConfig(it)
                }
        MParticle.reset(mContext)
        latch.countDown()
    }

}