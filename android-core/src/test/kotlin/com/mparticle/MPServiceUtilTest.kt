package com.mparticle

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class MPServiceUtilTest {
    @Test
    fun testOnHandleIntentIgnoresIntentsWithoutActionOrCloudMessage() {
        Mockito.mockStatic(Looper::class.java).use {
            Mockito.`when`(Looper.getMainLooper()).thenReturn(Mockito.mock(Looper::class.java))
            Mockito.mockConstruction(Handler::class.java).use {
                val serviceUtil = MPServiceUtil(MockContext())
                serviceUtil.onHandleIntent(null)
                val actions =
                    listOf(
                        null,
                        "com.google.android.c2dm.intent.REGISTRATION",
                        MPServiceUtil.INTERNAL_NOTIFICATION_TAP,
                        MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED,
                        MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED,
                    )
                for (action in actions) {
                    val intent = Mockito.mock(Intent::class.java)
                    Mockito.`when`(intent.action).thenReturn(action)
                    serviceUtil.onHandleIntent(intent)
                }
            }
        }
    }

    @Test
    fun testConstantsAndRunIntentInServiceFallbackOnIllegalStateException() {
        Assert.assertEquals("com.mparticle.default", MPServiceUtil.NOTIFICATION_CHANNEL)
        Assert.assertEquals("com.mparticle.push.notification_tapped", MPServiceUtil.INTERNAL_NOTIFICATION_TAP)

        Mockito.mockStatic(Looper::class.java).use {
            Mockito.`when`(Looper.getMainLooper()).thenReturn(Mockito.mock(Looper::class.java))
            Mockito.mockConstruction(Handler::class.java).use {
                val wakeLock = Mockito.mock(PowerManager.WakeLock::class.java)
                Mockito.`when`(wakeLock.isHeld).thenReturn(true)
                val powerManager = Mockito.mock(PowerManager::class.java)
                Mockito
                    .`when`(powerManager.newWakeLock(Mockito.eq(PowerManager.PARTIAL_WAKE_LOCK), Mockito.anyString()))
                    .thenReturn(wakeLock)

                val context = Mockito.mock(Context::class.java)
                Mockito.`when`(context.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager)
                val intent = Mockito.mock(Intent::class.java)
                Mockito.`when`(intent.action).thenReturn("com.google.android.c2dm.intent.REGISTRATION")
                Mockito.`when`(context.startService(intent)).thenThrow(IllegalStateException("background start"))

                MPServiceUtil.runIntentInService(context, intent)

                Mockito.verify(wakeLock).acquire()
                Mockito.verify(intent).setClass(context, MPService::class.java)
                Mockito.verify(wakeLock).release()
            }
        }
    }
}
