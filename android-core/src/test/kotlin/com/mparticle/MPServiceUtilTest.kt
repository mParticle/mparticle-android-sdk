package com.mparticle

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.messaging.MPMessagingRouter
import com.mparticle.messaging.ProviderCloudMessage
import com.mparticle.mock.MockContext
import org.junit.Test
import org.mockito.Mockito

class MPServiceUtilTest {
    private fun stubStringExtras(intent: Intent) {
        val extras = mutableMapOf<String, String?>()
        Mockito
            .`when`(intent.putExtra(Mockito.anyString(), Mockito.anyString()))
            .thenAnswer { invocation ->
                extras[invocation.getArgument(0)] = invocation.getArgument(1)
                intent
            }
        Mockito
            .`when`(intent.getStringExtra(Mockito.anyString()))
            .thenAnswer { invocation ->
                extras[invocation.getArgument(0)]
            }
    }

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
                    stubStringExtras(intent)
                    MPMessagingRouter.markInternalBroadcast(intent)
                    serviceUtil.onHandleIntent(intent)
                }
            }
        }
    }

    @Test
    fun testOnHandleIntentValidatesBroadcastAuthenticity() {
        Mockito.mockStatic(Looper::class.java).use {
            Mockito.`when`(Looper.getMainLooper()).thenReturn(Mockito.mock(Looper::class.java))
            Mockito.mockConstruction(Handler::class.java).use {
                Mockito.mockStatic(PendingIntent::class.java).use { pendingIntentStatic ->
                    Mockito.mockStatic(MParticle::class.java).use { mparticleStatic ->
                        val context = MockContext()
                        val serviceUtil = MPServiceUtil(context)
                        val mparticle = Mockito.mock(MParticle::class.java)
                        mparticleStatic.`when`<String> { MParticle.getAppState() }.thenReturn("not_running")
                        mparticleStatic.`when`<MParticle> { MParticle.getInstance() }.thenReturn(mparticle)

                        val receiveMessage = Mockito.mock(ProviderCloudMessage::class.java)
                        Mockito.`when`(receiveMessage.displayed).thenReturn(true)

                        val unauthReceiveIntent = Mockito.mock(Intent::class.java)
                        Mockito.`when`(unauthReceiveIntent.action).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED)
                        Mockito
                            .`when`(unauthReceiveIntent.getParcelableExtra<ProviderCloudMessage>(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                            .thenReturn(receiveMessage)
                        serviceUtil.onHandleIntent(unauthReceiveIntent)
                        Mockito.verifyNoInteractions(receiveMessage)
                        Mockito.verifyNoInteractions(mparticle)

                        val authReceiveIntent = Mockito.mock(Intent::class.java)
                        Mockito.`when`(authReceiveIntent.action).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED)
                        Mockito
                            .`when`(authReceiveIntent.getParcelableExtra<ProviderCloudMessage>(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                            .thenReturn(receiveMessage)
                        stubStringExtras(authReceiveIntent)
                        MPMessagingRouter.markInternalBroadcast(authReceiveIntent)
                        serviceUtil.onHandleIntent(authReceiveIntent)
                        Mockito.verify(receiveMessage).displayed
                        Mockito.verify(mparticle).logNotification(receiveMessage, false, "not_running")

                        val tapMessage = Mockito.mock(ProviderCloudMessage::class.java)
                        val openIntent = Mockito.mock(Intent::class.java)
                        val pendingIntent = Mockito.mock(PendingIntent::class.java)
                        Mockito.`when`(tapMessage.getDefaultOpenIntent(context, tapMessage)).thenReturn(openIntent)
                        pendingIntentStatic
                            .`when`<PendingIntent> {
                                PendingIntent.getActivity(
                                    Mockito.eq(context),
                                    Mockito.eq(0),
                                    Mockito.eq(openIntent),
                                    Mockito.anyInt(),
                                )
                            }.thenReturn(pendingIntent)

                        val unauthTapIntent = Mockito.mock(Intent::class.java)
                        Mockito.`when`(unauthTapIntent.action).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED)
                        Mockito
                            .`when`(unauthTapIntent.getParcelableExtra<ProviderCloudMessage>(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                            .thenReturn(tapMessage)
                        serviceUtil.onHandleIntent(unauthTapIntent)
                        Mockito.verifyNoInteractions(tapMessage)
                        Mockito.verify(pendingIntent, Mockito.never()).send()

                        val authTapIntent = Mockito.mock(Intent::class.java)
                        Mockito.`when`(authTapIntent.action).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED)
                        Mockito
                            .`when`(authTapIntent.getParcelableExtra<ProviderCloudMessage>(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                            .thenReturn(tapMessage)
                        stubStringExtras(authTapIntent)
                        MPMessagingRouter.markInternalBroadcast(authTapIntent)
                        serviceUtil.onHandleIntent(authTapIntent)
                        Mockito.verify(tapMessage).getDefaultOpenIntent(context, tapMessage)
                        Mockito.verify(pendingIntent).send()
                    }
                }
            }
        }
    }
}
