package com.mparticle.internal

import android.content.Context
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.mparticle.MParticle

open class PushRegistrationHelper {
    open class PushRegistration(@Nullable instanceId: String?, @Nullable senderId: String?) {
        @JvmField
        @Nullable
        var senderId: String? = senderId

        @JvmField
        @Nullable
        var instanceId: String? = instanceId

        @NonNull
        override fun toString(): String = "[${senderId ?: "null"}, ${instanceId ?: "null"}]"

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || other !is PushRegistration) {
                return false
            }
            return (senderId === other.senderId || (senderId != null && senderId == other.senderId)) &&
                (instanceId === other.instanceId || (instanceId != null && instanceId == other.instanceId))
        }
    }

    companion object {
        @JvmStatic
        fun getLatestPushRegistration(context: Context?): PushRegistration = ConfigManager.getInstance(context).pushRegistration

        @JvmStatic
        fun requestInstanceId(context: Context?) {
            requestInstanceId(context, ConfigManager.getInstance(context).pushSenderId)
        }

        @JvmStatic
        fun requestInstanceId(context: Context?, senderId: String?) {
            if (!ConfigManager.getInstance(context).isPushRegistrationFetched && MPUtility.isFirebaseAvailable()) {
                val instanceRunnable =
                    Runnable {
                        try {
                            if (MPUtility.isFirebaseAvailablePreV21()) {
                                val clazz = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
                                val instance = clazz.getMethod("getInstance").invoke(null)
                                val instanceId =
                                    clazz
                                        .getMethod("getToken", String::class.java, String::class.java)
                                        .invoke(instance, senderId, "FCM") as String?
                                setPushRegistration(context, instanceId, senderId)
                            } else if (MPUtility.isFirebaseAvailablePostV21()) {
                                com.google.firebase.messaging.FirebaseMessaging
                                    .getInstance()
                                    .token
                                    .addOnSuccessListener(
                                        OnSuccessListener<String> { instanceId ->
                                            setPushRegistration(context, instanceId, senderId)
                                        },
                                    ).addOnFailureListener(
                                        OnFailureListener { e ->
                                            Logger.error("Error registering for FCM Instance ID: ", e.message)
                                        },
                                    )
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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun setPushRegistration(context: Context?, instanceId: String?, senderId: String?) {
            if (!MPUtility.isEmpty(instanceId)) {
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
    }
}
