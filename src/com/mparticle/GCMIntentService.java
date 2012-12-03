package com.mparticle;

import com.mparticle.Constants.GCMNotificationKeys;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class GCMIntentService extends IntentService {

    public GCMIntentService() {
        super("com.mparticle.GCMIntentService");
    }

    private static final String TAG = Constants.LOG_TAG;

    private static PowerManager.WakeLock sWakeLock;
    private static final Object LOCK = GCMIntentService.class;

    static void runIntentInService(Context context, Intent intent) {
        synchronized(LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
        }
        sWakeLock.acquire();
        intent.setClassName(context, GCMIntentService.class.getName());
        context.startService(intent);
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                handleMessage(intent);
            }
        } finally {
            synchronized(LOCK) {
                sWakeLock.release();
            }
        }
    }

    private void handleRegistration(Intent intent) {

        MParticleAPI mParticleAPI;
        try {
            mParticleAPI = MParticleAPI.getInstance(this);
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }

        String registrationId = intent.getStringExtra("registration_id");
        String unregistered = intent.getStringExtra("unregistered");
        String error = intent.getStringExtra("error");

        if (registrationId != null) {
            // registration succeeded
            mParticleAPI.setPushRegistrationId(registrationId);
        } else  if (unregistered != null) {
            // unregistration succeeded
            mParticleAPI.clearPushRegistrationId();
        } else if (error != null) {
            // Unrecoverable error, log it
            Log.i(TAG, "GCM registration error: " + error);
        }

    }

    private void handleMessage(Intent intent) {
        Bundle extras = intent.getExtras();
        String title = extras.getString(GCMNotificationKeys.TITLE);
        String text = extras.getString(GCMNotificationKeys.TEXT);

        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();

        int applicationIcon = 0;
        try {
            applicationIcon = packageManager.getApplicationInfo(packageName, 0).icon;
        } catch (NameNotFoundException e) {
            // use the ic_dialog_alert icon if the app's can not be found
        }
        if (0==applicationIcon) {
            applicationIcon = android.R.drawable.ic_dialog_alert;
        }

        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        PendingIntent notifyIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(applicationIcon, text, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this, title, text, notifyIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

}
