package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.iid.FirebaseInstanceId;
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
                        String instanceId = FirebaseInstanceId.getInstance().getToken(senderId, "FCM");
                        setPushRegistration(context, instanceId, senderId);
                    } catch (Exception ex) {
                        Logger.error("Error registering for FCM Instance ID: ", ex.getMessage());
                    }
                }
            };
            if (Looper.getMainLooper() == Looper.myLooper()) {
                new Thread(instanceRunnable).start();
            }else{
                instanceRunnable.run();
            }
        }
    }


    static void setPushRegistration(Context context, String instanceId, String senderId) {
        if (!MPUtility.isEmpty(instanceId)) {
            MParticle mParticle = MParticle.getInstance();
            ConfigManager.getInstance(context).setPushRegistrationFetched();
            if (mParticle != null) {
                MParticle.getInstance().logPushRegistration(instanceId, senderId);
            } else {
                //If the SDK isn't started, log the push notification in the ConfigManager
                //so we will know to send a IdentityApi.modify() call when it starts up.
                ConfigManager.getInstance(context).setPushRegistrationInBackground(new PushRegistration(instanceId, senderId));
            }
        }
    }

    public static class PushRegistration {
        @Nullable public String senderId;
        @Nullable public String instanceId;

        public PushRegistration(@Nullable String instanceId, @Nullable String senderId) {
            this.instanceId = instanceId;
            this.senderId = senderId;
        }

        @Override
        @NonNull
        public String toString() {
            return "[" + (senderId == null ? "null" : senderId) + ", " + (instanceId == null ? "null" : instanceId) + "]";
        }
    }
}
