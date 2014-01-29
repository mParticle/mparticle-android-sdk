package com.mparticle;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class GcmIntentService extends IntentService {

    private static final String TAG = Constants.LOG_TAG;
    private static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.notification_opened";

    private static PowerManager.WakeLock sWakeLock;
    private static final Object LOCK = GcmIntentService.class;

    public GcmIntentService() {
        super("com.mparticle.GcmIntentService");
    }

    static void runIntentInService(Context context, Intent intent) {
        synchronized (LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
        }
        sWakeLock.acquire();
        intent.setClass(context, GcmIntentService.class);
        Log.i("GcmIntentService", "Running intent");
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("GcmIntentService", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            Log.i("GcmIntentService", "Handling action: " + action);
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                handleMessage(intent);
            } else if (action.equals("com.google.android.c2dm.intent.UNREGISTER")) {
                intent.putExtra("unregistered", "true");
                handleRegistration(intent);
            }else if (action.equals(MPARTICLE_NOTIFICATION_OPENED)){
                handleNotificationClick(intent);
            }
        } finally {
            synchronized (LOCK) {
                if (sWakeLock != null) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void handleNotificationClick(Intent intent) {
        try {
            MParticle.lastNotificationBundle = intent.getExtras().getBundle(Constants.MessageKey.PAYLOAD);
            MParticle mMParticle = MParticle.getInstance(this);
            mMParticle.logNotification(intent);
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }

        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);


    }

    private void handleRegistration(Intent intent) {
        try {
            MParticle mMParticle = MParticle.getInstance(this);
            String registrationId = intent.getStringExtra("registration_id");
            String unregistered = intent.getStringExtra("unregistered");
            String error = intent.getStringExtra("error");

            if (registrationId != null) {
                // registration succeeded
                mMParticle.setPushRegistrationId(registrationId);
            } else if (unregistered != null) {
                // unregistration succeeded
                mMParticle.clearPushNotificationId();
            } else if (error != null) {
                // Unrecoverable error, log it
                Log.i(TAG, "GCM registration error: " + error);
            }
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }
    }

    private void handleMessage(Intent intent) {

        try {
            Bundle extras = intent.getExtras();
            Bundle newExtras = new Bundle();
            newExtras.putBundle(Constants.MessageKey.PAYLOAD, extras);
            if (!MParticle.appRunning){
                newExtras.putString(Constants.MessageKey.APP_STATE, AppStateManager.APP_STATE_NOTRUNNING);
            }

            MParticle mMParticle = MParticle.getInstance(this);

            if (!newExtras.containsKey(Constants.MessageKey.APP_STATE)){
                if (mMParticle.mAppStateManager.isBackgrounded()){
                    newExtras.putString(Constants.MessageKey.APP_STATE, AppStateManager.APP_STATE_BACKGROUND);
                }else{
                    newExtras.putString(Constants.MessageKey.APP_STATE, AppStateManager.APP_STATE_FOREGROUND);
                }
            }

            String title = "Unknown";
            try{
                int stringId = getApplicationInfo().labelRes;
                title = getResources().getString(stringId);
            }catch(Resources.NotFoundException ex){

            }

            //TODO: support collapse key to auto-update notifications that are still visible.

            String[] keys = mMParticle.mConfigManager.getPushKeys();
            if (keys != null){

                for (String key : keys){
                    String message = extras.getString(key);
                    if (message != null && message.length() > 0){
                        //redact message contents for privacy concerns
                        extras.remove(key);
                        extras.putString(key, "");
                        showPushWithMessage(title, message, extras);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }
    }

    private void showPushWithMessage(String title, String message, Bundle extras) {
        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();

        int applicationIcon = 0;
        try {
            applicationIcon = packageManager.getApplicationInfo(packageName, 0).icon;
        } catch (NameNotFoundException e) {
            // use the ic_dialog_alert icon if the app's can not be found
        }
        if (0 == applicationIcon) {
            applicationIcon = android.R.drawable.ic_dialog_alert;
        }


        Intent launchIntent = new Intent(getApplicationContext(), GcmIntentService.class);
        launchIntent.setAction(MPARTICLE_NOTIFICATION_OPENED);
        launchIntent.putExtras(extras);

        PendingIntent notifyIntent = PendingIntent.getService(getApplicationContext(), 0, new Intent(getApplicationContext(), GcmIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification(applicationIcon, message, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this, title, message, notifyIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

}