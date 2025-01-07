package com.mparticle.internal

import android.content.Context
import android.os.Looper
import com.google.firebase.messaging.FirebaseMessaging
import com.mparticle.MParticle
import com.mparticle.internal.MPUtility.isEmpty
import com.mparticle.internal.MPUtility.isFirebaseAvailable
import com.mparticle.internal.MPUtility.isFirebaseAvailablePostV21
import com.mparticle.internal.MPUtility.isFirebaseAvailablePreV21

object PushRegistrationHelper {
    fun getLatestPushRegistration(context: Context): PushRegistration {
        return ConfigManager.getInstance(context).pushRegistration
    }

    @JvmStatic
    @JvmOverloads
    fun requestInstanceId(context: Context, senderId: String = ConfigManager.getInstance(context).pushSenderId) {
        if (!ConfigManager.getInstance(context).isPushRegistrationFetched && isFirebaseAvailable) {
            val instanceRunnable = Runnable {
                try {
                    if (isFirebaseAvailablePreV21) {
                        val clazz = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
                        val instance = clazz.getMethod("getInstance").invoke(null)
                        val instanceId = clazz.getMethod("getToken", String::class.java, String::class.java)
                            .invoke(instance, senderId, "FCM") as String
                        setPushRegistration(context, instanceId, senderId)
                    } else if (isFirebaseAvailablePostV21) {
                        FirebaseMessaging.getInstance().token
                            .addOnSuccessListener { instanceId ->
                                setPushRegistration(
                                    context,
                                    instanceId,
                                    senderId
                                )
                            }
                            .addOnFailureListener { e ->
                                Logger.error(
                                    "Error registering for FCM Instance ID: ",
                                    e.message
                                )
                            }
                    } else {
                        Logger.error("Error registering FCM Instance ID: no Firebase library")
                    }
                } catch (ex: Exception) {
                    Logger.error("Error registering for FCM Instance ID: ", ex.message)
                }
            }
            if (Looper.getMainLooper() == Looper.myLooper()) {
                Thread(instanceRunnable).start()
            } else {
                instanceRunnable.run()
            }
        }
    }

    @JvmStatic
    fun setPushRegistration(context: Context, instanceId: String, senderId: String) {
        if (!isEmpty(instanceId)) {
            val mParticle = MParticle.getInstance()
            val configManager = ConfigManager.getInstance(context)
            configManager.setPushRegistrationFetched()
            val newPushRegistration = PushRegistration(instanceId, senderId)
            val pushRegistration = configManager.pushRegistration

            // If this new push registration matches the existing persisted value we can will defer logging it until a new Session starts
            if (mParticle == null || (mParticle.currentSession == null && newPushRegistration == pushRegistration)) {
                // If the SDK isn't started, OR if a Session hasn't started and this is a duplicate push registration,
                // log the push notification as a background push in the ConfigManager and we will send a IdentityApi.modify() call when it starts up.
                ConfigManager.getInstance(context).setPushRegistrationInBackground(PushRegistration(instanceId, senderId))
            } else {
                mParticle.logPushRegistration(instanceId, senderId)
            }
        }
    }

    class PushRegistration(@JvmField var instanceId: String?, @JvmField var senderId: String?) {
        override fun toString(): String {
            return "[" + (if (senderId == null) "null" else senderId) + ", " + (if (instanceId == null) "null" else instanceId) + "]"
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o == null || o !is PushRegistration) {
                return false
            }
            val other = o
            return (senderId == other.senderId && instanceId == other.instanceId)
        }
    }
}
