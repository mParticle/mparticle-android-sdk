package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;

import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mparticle.MParticle;

import static com.mparticle.internal.MPUtility.getAvailableInstanceId;

public class PushRegistrationHelper {

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static PushRegistration getLatestPushRegistration(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String registrationId = preferences.getString(Constants.PrefKeys.PUSH_REGISTRATION_INSTANCE_ID, "");
        if (MPUtility.isEmpty(registrationId)) {
            return null;
        }
        String senderId = preferences.getString(Constants.PrefKeys.PUSH_REGISTRATION_SENDER_ID, "");
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = preferences.getInt(Constants.PrefKeys.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        int osVersion = preferences.getInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Integer.MIN_VALUE);
        if (registeredVersion != currentVersion || osVersion != Build.VERSION.SDK_INT) {
            Logger.debug("App or OS version changed, clearing instance ID.");
            return null;
        }
        return new PushRegistration(registrationId, senderId);
    }

    public static void requestInstanceId(Context context) {
        requestInstanceId(context, MParticle.getInstance().getConfigManager().getPushSenderId());
    }

    public static class PushRegistration {
        public String senderId;
        public String instanceId;

        public PushRegistration(String instanceId, String senderId) {
            this.instanceId = instanceId;
            this.senderId = senderId;
        }
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
                        if (!MPUtility.isEmpty(instanceId)) {
                            MParticle.getInstance().logPushRegistration(instanceId, senderId);
                        }
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


    public static void setInstanceId(Context context, PushRegistration registration) {
        int appVersion = getAppVersion(context);
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.PrefKeys.PUSH_REGISTRATION_INSTANCE_ID, registration.instanceId);
        editor.putString(Constants.PrefKeys.PUSH_REGISTRATION_SENDER_ID, registration.senderId);
        editor.putInt(Constants.PrefKeys.PROPERTY_APP_VERSION, appVersion);
        editor.putInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Build.VERSION.SDK_INT);
        editor.apply();
    }

    /**
     * Unregister the device from GCM notifications
     */
    public static void disablePushNotifications(final Context context) {
        if (MPUtility.isInstanceIdAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PushRegistrationHelper.clearInstanceId(context);
                }
            }).start();
        }
    }

    /**
     * Manually un-register the device token for receiving push notifications from mParticle
     */
    public static void clearInstanceId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(Constants.PrefKeys.PUSH_REGISTRATION_INSTANCE_ID)
                .remove(Constants.PrefKeys.PUSH_REGISTRATION_SENDER_ID)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, false)
                .apply();
    }
}
