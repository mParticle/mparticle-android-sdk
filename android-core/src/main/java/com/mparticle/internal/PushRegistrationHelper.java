package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;

import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mparticle.MParticle;

import static com.mparticle.internal.MPUtility.getAvailableInstanceId;

public class PushRegistrationHelper {

    public static PushRegistration getLatestPushRegistration(Context context) {
        return ConfigManager.getInstance(context).getPushRegistration();
    }

    public static void requestInstanceId(Context context) {
        requestInstanceId(context, ConfigManager.getInstance(context).getPushSenderId());
    }

    public static void requestInstanceId(final Context context, final String senderId) {
        if (getLatestPushRegistration(context) == null && MPUtility.isInstanceIdAvailable()) {
            final Runnable instanceRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        String instanceId = null;
                        switch (getAvailableInstanceId()) {
                            case GCM:
                                instanceId =  InstanceID.getInstance(context).getToken(senderId, "GCM");
                                break;
                            case FCM:
                                instanceId = FirebaseInstanceId.getInstance().getToken();
                                break;
                        }
                        setPushRegistration(context, instanceId, senderId);
                    } catch (Exception ex) {
                        Logger.error("Error registering for GCM Instance ID: ", ex.getMessage());
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
            if (mParticle != null) {
                MParticle.getInstance().logPushRegistration(instanceId, senderId);
            } else {
                //If the SDK isn't started, log the push notification in the ConfigManager
                //so we will know to send a IdentityApi.modify() call when it starts up
                ConfigManager.getInstance(context).setPushRegistrationInBackground(new PushRegistration(instanceId, senderId));
            }
        }
    }

    public static class PushRegistration {
        public String senderId;
        public String instanceId;

        public PushRegistration(String instanceId, String senderId) {
            this.instanceId = instanceId;
            this.senderId = senderId;
        }

        @Override
        public String toString() {
            return "[" + senderId + ", " + instanceId + "]";
        }
    }
}
