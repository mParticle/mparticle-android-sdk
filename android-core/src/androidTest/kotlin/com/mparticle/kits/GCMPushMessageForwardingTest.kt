package com.mparticle.kits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.mparticle.MPServiceUtil
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.testkits.PushListenerTestKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GCMPushMessageForwardingTest : BaseKitOptionsTest() {

    @Test
    fun testPushForwardedAfterSDKStarted() {
        var receivedIntent: Intent? = null

        MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configuration(KitOptions().addKit(1, PushListenerTestKit::class.java))
            .let {
                startMParticle(it)
            }

        val intent = Intent()
            .apply {
                action = "com.google.android.c2dm.intent.RECEIVE"
                data = Uri.EMPTY
                putExtras(Bundle())
            }
        (MParticle.getInstance()?.getKitInstance(1) as PushListenerTestKit).onPushMessageReceived =
            { context, intent ->
                receivedIntent = intent
            }
        MPServiceUtil(mContext).onHandleIntent(intent)

        assertNotNull(receivedIntent)
        assertEquals(intent, receivedIntent)
    }
}
