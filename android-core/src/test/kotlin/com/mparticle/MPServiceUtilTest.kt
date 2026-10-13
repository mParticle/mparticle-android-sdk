package com.mparticle

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.mock.MockContext
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
}
