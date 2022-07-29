package com.mparticle.kits

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.mparticle.MPServiceUtil
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.testkits.PushListenerTestKit
import com.mparticle.testing.context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GCMPushMessageForwardingTest : BaseKitOptionsTest() {

    @Test
    fun testPushForwardedAfterSDKStarted() {
        var receivedIntent: Intent? = null

        MParticleOptions.builder(context)
            .credentials("key", "secret")
            .configuration(
                ConfiguredKitOptions {
                    addKit(PushListenerTestKit::class.java, 1)
                }
            )
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
        MPServiceUtil(context).onHandleIntent(intent)

        assertNotNull(receivedIntent)
        assertEquals(intent, receivedIntent)
    }
}
