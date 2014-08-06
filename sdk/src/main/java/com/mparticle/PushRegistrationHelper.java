package com.mparticle;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by sdozor on 1/23/14.
 */
class PushRegistrationHelper {

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
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

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    static String getRegistrationId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String registrationId = preferences.getString(Constants.PrefKeys.PUSH_REGISTRATION_ID, "");
        if (registrationId == null || registrationId.length() == 0) {
            MParticle.getInstance().mConfigManager.debugLog("GCM Registration ID not found.");
            return null;
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = preferences.getInt(Constants.PrefKeys.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        int osVersion = preferences.getInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Integer.MIN_VALUE);
        if (registeredVersion != currentVersion || osVersion != Build.VERSION.SDK_INT) {
            Log.i(Constants.LOG_TAG, "App or OS version changed.");
            return null;
        }
        return registrationId;
    }

    /**
     * Register the application for GCM notifications
     *
     * @param senderId the SENDER_ID for the application
     */
    public static void enablePushNotifications(Context context, String senderId) {
        if (getRegistrationId(context) == null) {
            Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
            registrationIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
            registrationIntent.putExtra("sender", senderId);
            context.startService(registrationIntent);
        }
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param regId   registration ID
     */
    public static void storeRegistrationId(Context context, String regId) {
        int appVersion = getAppVersion(context);
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.PrefKeys.PUSH_REGISTRATION_ID, regId);
        editor.putInt(Constants.PrefKeys.PROPERTY_APP_VERSION, appVersion);
        editor.putInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Build.VERSION.SDK_INT);
        editor.commit();
    }

    /**
     * Unregister the device from GCM notifications
     */
    public static void disablePushNotifications(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.PrefKeys.PUSH_ENABLED, false).commit();
        Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        unregIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(unregIntent);
    }

    /**
     * Manually un-register the device token for receiving push notifications from mParticle
     * <p/>
     */
    public static void clearPushRegistrationId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        preferences.edit().remove(Constants.PrefKeys.PUSH_REGISTRATION_ID).commit();
    }
}
