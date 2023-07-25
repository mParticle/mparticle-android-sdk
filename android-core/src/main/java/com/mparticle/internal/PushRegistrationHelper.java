package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mparticle.MParticle;

public class PushRegistrationHelper {

    public static PushRegistration getLatestPushRegistration(Context context) {
        return ConfigManager.getInstance(context).getPushRegistration();
    }

    public static void requestInstanceId(Context context) {
        requestInstanceId(context, ConfigManager.getInstance(context).getPushSenderId());
    }

    public static void requestInstanceId(final Context context, final String senderId) {
        if (!ConfigManager.getInstance(context).isPushRegistrationFetched() && MPUtility.isFirebaseAvailable()) {
            final Runnable instanceRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (MPUtility.isFirebaseAvailablePreV21()) {
                            Class<?> clazz = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
                            Object instance = clazz.getMethod("getInstance").invoke(null);
                            String instanceId = (String) clazz.getMethod("getToken", String.class, String.class).invoke(instance, senderId, "FCM");
                            setPushRegistration(context, instanceId, senderId);
                        } else if (MPUtility.isFirebaseAvailablePostV21()) {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String instanceId) {
                                            setPushRegistration(context, instanceId, senderId);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Logger.error("Error registering for FCM Instance ID: ", e.getMessage());
                                        }
                                    });
                        } else {
                            Logger.error("Error registering FCM Instance ID: no Firebase library");
                        }
                    } catch (Exception ex) {
                        Logger.error("Error registering for FCM Instance ID: ", ex.getMessage());
                    }
                }
            };
            if (Looper.getMainLooper() == Looper.myLooper()) {
                new Thread(instanceRunnable).start();
            } else {
                instanceRunnable.run();
            }
        }
    }


    static void setPushRegistration(Context context, String instanceId, String senderId) {
        if (!MPUtility.isEmpty(instanceId)) {
            MParticle mParticle = MParticle.getInstance();
            ConfigManager configManager = ConfigManager.getInstance(context);
            configManager.setPushRegistrationFetched();
            PushRegistration newPushRegistration = new PushRegistration(instanceId, senderId);
            PushRegistration pushRegistration = configManager.getPushRegistration();
            //If this new push registration matches the existing persisted value we can will defer logging it until a new Session starts

            if (mParticle == null || (mParticle.getCurrentSession() == null && newPushRegistration.equals(pushRegistration))) {
                //If the SDK isn't started, OR if a Session hasn't started and this is a duplicate push registration,
                // log the push notification as a background push in the ConfigManager and we will send a IdentityApi.modify() call when it starts up.
                ConfigManager.getInstance(context).setPushRegistrationInBackground(new PushRegistration(instanceId, senderId));
            } else {
                mParticle.logPushRegistration(instanceId, senderId);
            }
        }
    }

    public static class PushRegistration {
        @Nullable
        public String senderId;
        @Nullable
        public String instanceId;

        public PushRegistration(@Nullable String instanceId, @Nullable String senderId) {
            this.instanceId = instanceId;
            this.senderId = senderId;
        }

        @Override
        @NonNull
        public String toString() {
            return "[" + (senderId == null ? "null" : senderId) + ", " + (instanceId == null ? "null" : instanceId) + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !(o instanceof PushRegistration)) {
                return false;
            }
            PushRegistration other = (PushRegistration) o;
            return (senderId == other.senderId || (senderId != null && senderId.equals(other.senderId))) &&
                    (instanceId == other.instanceId || (instanceId != null && instanceId.equals(other.instanceId)));
        }
    }
}
