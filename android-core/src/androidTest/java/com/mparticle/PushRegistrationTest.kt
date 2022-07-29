package com.mparticle

import com.mparticle.internal.ConfigManager
import com.mparticle.internal.PushRegistrationHelper
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.Utils.setFirebasePresent
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.orThrow
import com.mparticle.utils.startMParticle
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class PushRegistrationTest : BaseStartedTest() {
    var localContext = context
    // So other classes can use the test fields

    @Test
    @Throws(InterruptedException::class)
    fun testPushEnabledOnStartup() {
        MParticle.reset(localContext)
        val newToken: String = RandomUtils.getAlphaNumericString(30)
        startMParticle()
        setFirebasePresent(true, newToken)
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertNextRequest {
                it.body.identityChanges?.let {
                    it.size == 1 &&
                        it.first().newValue == newToken
                } ?: false
            }
            .after {
                MParticle.getInstance()!!.Messaging().enablePushNotifications("12345")
            }
            .blockUntilFinished()
        setFirebasePresent(false, null)
    }

    @Test
    fun testPushRegistrationSet() {
        assertEquals(
            mStartingMpid,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        for (setPush in setPushes) {
            val pushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (getPush in getPushes) {
                val fetchedPushValue: PushRegistration = getPush.pushRegistration
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
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(pushRegistration)
            for (clearPush in clearPushes) {
                clearPush.clearPush()
                for (getPush in getPushes) {
                    val fetchedPushRegistration: PushRegistration = getPush.pushRegistration
                    if (fetchedPushRegistration != null && fetchedPushRegistration.instanceId != null && fetchedPushRegistration.senderId != null) {
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
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
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

    var setPushes = arrayOf(
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance()!!
                    .logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId)
            }

            override val name: String
                get() = "MParticle.getInstance().logPushRegistration(senderId, instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance().orThrow().Internal().configManager.pushRegistration =
                    pushRegistration
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(pushRegistration())"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.getInstance().orThrow().Internal().configManager.pushSenderId =
                    pushRegistration.senderId
                MParticle.getInstance().orThrow().Internal().configManager.pushInstanceId =
                    pushRegistration.instanceId
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(senderId) + ConfigManager.setPushRegistration(instanceId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                // For enablePushNotifications() to set the push registration, we need to mimic
                // the Firebase dependency, and clear the push-fetched flags
                setFirebasePresent(true, pushRegistration.instanceId)
                MParticle.getInstance()!!.Messaging()
                    .enablePushNotifications(pushRegistration.senderId!!)
                // this method setting push is async, so wait for confirmation before continuing
                val configManager: ConfigManager = ConfigManager.getInstance(localContext)
                while (!configManager.isPushEnabled) {
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                setFirebasePresent(false, null)
            }

            override val name: String
                get() = "MessagingApi.enablePushNotification(senderId)"
        },
        object : SetPush {
            override fun setPushRegistration(pushRegistration: PushRegistration) {
                MParticle.setInstance(null)
                try {
                    startMParticle(
                        MParticleOptions.builder(localContext).pushRegistration(
                            pushRegistration.instanceId!!,
                            pushRegistration.senderId!!
                        )
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
                MParticle.getInstance()!!.Messaging().disablePushNotifications()
            }

            override val name: String
                get() = "MessagingApi.disablePushNotifications"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().orThrow().Internal().configManager.pushSenderId = null
            }

            override val name: String
                get() = "ConfigManager.setPushSenderId(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().orThrow().Internal().configManager.pushRegistration = null
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(null)"
        },
        object : ClearPush {
            override fun clearPush() {
                MParticle.getInstance().orThrow().Internal().configManager.pushRegistration =
                    PushRegistration("instanceId", null)
            }

            override val name: String
                get() = "ConfigManager.setPushRegistration(PushRegistration(\"instanceId\", null))"
        }
    )
    var getPushes = arrayOf(
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() {
                    val senderId: String? =
                        MParticle.getInstance().orThrow().Internal().configManager.pushSenderId
                    val instanceId: String? =
                        MParticle.getInstance().orThrow().Internal().configManager.pushInstanceId
                    return PushRegistration(instanceId, senderId)
                }
            override val name: String
                get() = "ConfigManager.getPushSenderId() + ConfigManager.getPushInstanceId()"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = PushRegistrationHelper.getLatestPushRegistration(localContext)
            override val name: String
                get() = "PushRegistrationHelper.getLatestPushRegistration(localContext)"
        },
        object : GetPush {
            override val pushRegistration: PushRegistration
                get() = MParticle.getInstance().orThrow().Internal().configManager.pushRegistration
            override val name: String
                get() = "ConfigManager.getPushRegistration()"
        }
    )
    var pushEnableds = arrayOf<PushEnabled>(
        object : PushEnabled {
            override val isPushEnabled: Boolean
                get() = MParticle.getInstance().orThrow().Internal().configManager.isPushEnabled
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
}
