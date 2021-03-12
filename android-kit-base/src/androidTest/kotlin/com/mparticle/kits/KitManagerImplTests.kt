package com.mparticle.kits

import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.ConfigManager
import com.mparticle.kits.testkits.AttributeListenerTestKit
import com.mparticle.kits.testkits.IdentityListenerTestKit
import com.mparticle.kits.testkits.UserAttributeListenerTestKit
import com.mparticle.testutils.MPLatch
import junit.framework.Assert.assertFalse
import org.json.JSONObject
import org.junit.Test

class KitManagerImplTests: BaseKitManagerStarted() {
    override fun registerCustomKits(): Map<String, JSONObject> {
        return mapOf(
                AttributeListenerTestKit::class.java.name to JSONObject(),
                IdentityListenerTestKit::class.java.name to JSONObject(),
                UserAttributeListenerTestKit::class.java.name to JSONObject(),
        )
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