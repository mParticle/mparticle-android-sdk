package com.mparticle

import android.content.Context
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.PushRegistrationHelper
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.TestingUtils
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch

class PushRegistrationTest : BaseCleanStartedEachTest() {
    // So other classes can use the test fields
    fun setContext(context: Context) {
        mContext = context
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPushEnabledOnStartup() {
        MParticle.reset(mContext)
        val newToken = mRandomUtils.getAlphaNumericString(30)
        startMParticle()
        TestingUtils.setFirebasePresent(true, newToken)
        val latch: CountDownLatch = MPLatch(1)
        mServer.waitForVerify(Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid))) { request ->
            val identitChanges = request.asIdentityRequest().body.identity_changes
            Assert.assertEquals(1, identitChanges.size.toLong())
            try {
                Assert.assertEquals(newToken, identitChanges[0].getString("new_value"))
                latch.countDown()
            } catch (e: JSONException) {
                RuntimeException(e)
            }
        }
        MParticle.getInstance()?.Messaging()?.enablePushNotifications("12345")
        latch.await()
        TestingUtils.setFirebasePresent(false, null)
    }

    @Test
    fun testPushRegistrationSet() {
        Assert.assertEquals(
            mStartingMpid.toLong(),
            MParticle.getInstance()?.Identity()?.currentUser?.id
        )
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                mRandomUtils.getAlphaNumericString(10),
                mRandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (getPush in getPushes) {
                val fetchedPushValue = getPush.pushRegistration
                val fetchedSenderId = fetchedPushValue.senderId
                val fetchedInstanceId = fetchedPushValue.instanceId
                if (pushRegistration.senderId != fetchedSenderId) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration.senderId + "\" is set with: " + setPush.name + ". A different value \"" + fetchedSenderId + "\" is returned with:" + getPush.name)
                }
                if (pushRegistration.instanceId != fetchedInstanceId) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration.instanceId + "\" is set with: " + setPush.name + ". A different value \"" + fetchedInstanceId + "\" is returned with:" + getPush.name)
                }
            }
        }
    }

    @Test
    fun testPushRegistrationCleared() {
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                mRandomUtils.getAlphaNumericString(10),
                mRandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (clearPush in clearPushes) {
                clearPush.clearPush()
                for (getPush in getPushes) {
                    val fetchedPushRegistration = getPush.pushRegistration
                    if (fetchedPushRegistration.instanceId != null && fetchedPushRegistration.senderId != null) {
                        Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", and cleared with: " + clearPush.name + ", the value is not null when fetched with:" + getPush.name)
                    }
                }
            }
        }
    }

    @Test
    fun testPushRegistrationEnabledDisabled() {
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                mRandomUtils.getAlphaNumericString(10),
                mRandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (pushEnabled in pushEnableds) {
                if (!pushEnabled.isPushEnabled) {
                    Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", push IS NOT enabled with:" + pushEnabled.name)
                }
            }
            for (clearPush in clearPushes) {
                clearPush.clearPush()
                for (pushEnabled in pushEnableds) {
                    if (pushEnabled.isPushEnabled) {
                        Assert.fail("Mismatch! When push value of \"" + pushRegistration + "\" is set with: " + setPush.name + ", and cleared with: " + clearPush.name + ", push IS enabled with:" + pushEnabled.name)
                    }
                }
            }
        }
    }

    @JvmField
    var setPushes = arrayOf(
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance()
                    ?.logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId)
            }

            override val name: String
                get() = "MParticle.getInstance()?.logPushRegistration(senderId, instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance()?.mInternal?.configManager?.pushRegistration =
                    pushRegistration
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(pushRegistration())"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance()?.mInternal?.configManager?.pushSenderId =
                    pushRegistration.senderId
                MParticle.getInstance()?.mInternal?.configManager?.pushInstanceId =
                    pushRegistration.instanceId
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(senderId) + ConfigManager.setPushRegistration(instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                // For enablePushNotifications() to set the push registration, we need to mimic
                // the Firebase dependency, and clear the push-fetched flags
                TestingUtils.setFirebasePresent(true, pushRegistration.instanceId)
                pushRegistration.senderId?.let {
                    MParticle.getInstance()?.Messaging()?.enablePushNotifications(
                        it
                    )
                }
                // this method setting push is async, so wait for confirmation before continuing
                val configManager = ConfigManager.getInstance(mContext)
                while (!configManager.isPushEnabled) {
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                TestingUtils.setFirebasePresent(false, null)
            }

            override val name: String
                get() = "MessagingApi.enablePushNotification(senderId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.setInstance(null)
                try {
                    startMParticle(
                        pushRegistration.instanceId?.let { instanceId ->
                            pushRegistration.senderId?.let { senderId ->
                                MParticleOptions.builder(mContext).pushRegistration(
                                    instanceId,
                                    senderId
                                )
                            }
                        }
                    )
                } catch (e: InterruptedException) {
                    Assert.fail(e.message)
                }
            }

            override val name: String
                get() = "MParticleOptions.pushRegistration(instanceId, senderId)"
        }
    )
    var clearPushes = arrayOf(
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance()?.Messaging()?.disablePushNotifications()
            }

            override val name: String
                get() = "MessagingApi.disablePushNotifications"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance()?.mInternal?.configManager?.pushSenderId = null
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance()?.mInternal?.configManager?.pushRegistration = null
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance()?.mInternal?.configManager?.pushRegistration =
                    PushRegistration("instanceId", null)
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(PushRegistration(\"instanceId\", null))"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.setInstance(null)
                try {
                    startMParticle(MParticleOptions.builder(mContext).pushRegistration("", ""))
                } catch (e: InterruptedException) {
                    Assert.fail(e.message)
                }
            }

            override val name: String
                get() = "startMParticle(MParticleOptions.builder(mContext).pushRegistration(null, null))"
        }
    )
    var getPushes = arrayOf(
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() {
                    val senderId: String? =
                        MParticle.getInstance()?.mInternal?.configManager?.pushSenderId
                    val instanceId: String? =
                        MParticle.getInstance()?.mInternal?.configManager?.pushInstanceId
                    return PushRegistration(instanceId, senderId)
                }
            override val name: String
                get() = "ConfigManager.getPushSenderId() + ConfigManager.getPushInstanceId()"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = PushRegistrationHelper.getLatestPushRegistration(mContext)
            override val name: String
                get() = "PushRegistrationHelper.getLatestPushRegistration(context)"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = MParticle.getInstance()?.mInternal?.configManager?.pushRegistration!!
            override val name: String
                get() = "ConfigManager.getPushRegistration()"
        }
    )
    var pushEnableds = arrayOf<PushEnabled>(
        object : PushEnabled {
            override val isPushEnabled: Boolean
                get() = MParticle.getInstance()?.mInternal?.configManager?.isPushEnabled == true
            override val name: String
                get() = "ConfigManager.isPushEnabled()"
        }
    )

    interface SynonymousMethod {
        val name: String
    }

    interface SetPush : SynonymousMethod {
        fun setPushRegistration(pushRegistration: PushRegistration)
    }

    interface ClearPush : SynonymousMethod {
        fun clearPush()
    }

    interface GetPush : SynonymousMethod {
        val pushRegistration: PushRegistration
    }

    interface PushEnabled : SynonymousMethod {
        val isPushEnabled: Boolean
    }

    fun setServer(server: MockServer): PushRegistrationTest {
        mServer = server
        return this
    }
}
