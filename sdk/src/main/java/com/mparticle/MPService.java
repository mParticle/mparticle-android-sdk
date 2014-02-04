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

/**
 * {@code IntentService } used internally by the SDK to process incoming broadcast messages in the background. Required for push notification functionality.
 *
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 *
 * <pre>
 * {@code
 * <service android:name="com.mparticle.MPService" />}
 *</pre>
 */
public class MPService extends IntentService {

    private static final String TAG = Constants.LOG_TAG;
    private static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.notification_opened";

    private static PowerManager.WakeLock sWakeLock;
    private static final Object LOCK = MPService.class;

    public MPService() {
        super("com.mparticle.MPService");
    }

    static void runIntentInService(Context context, Intent intent) {
        synchronized (LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
        }
        sWakeLock.acquire();
        intent.setClass(context, MPService.class);
        Log.i("MPService", "Running intent");
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MPService", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            Log.i("MPService", "Handling action: " + action);
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
                if (sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void handleNotificationClick(Intent intent) {
        try {
            MParticle.lastNotificationBundle = intent.getExtras().getBundle(Constants.MessageKey.PAYLOAD);
            MParticle.start(getApplicationContext());
            MParticle mMParticle = MParticle.getInstance();
            mMParticle.logNotification(intent);
        } catch (Throwable t) {

        }

        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launchIntent);


    }

    private void handleRegistration(Intent intent) {
        try {
            MParticle mMParticle = MParticle.getInstance();
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

            MParticle mMParticle = MParticle.getInstance();

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
                        newExtras.getBundle(Constants.MessageKey.PAYLOAD).remove(key);
                        newExtras.getBundle(Constants.MessageKey.PAYLOAD).putString(key, "");
                        showPushWithMessage(title, message, newExtras);
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
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        MParticle mMParticle = MParticle.getInstance();
        Intent launchIntent = new Intent(getApplicationContext(), MPService.class);
        launchIntent.setAction(MPARTICLE_NOTIFICATION_OPENED);
        launchIntent.putExtras(extras);
        PendingIntent notifyIntent = PendingIntent.getService(getApplicationContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (mMParticle.customNotification != null){
            mMParticle.customNotification.setContentIntent(notifyIntent);
            notificationManager.notify(extras.hashCode(), mMParticle.customNotification.setContentText(message).build());
        }else{
            Notification notification = new Notification(applicationIcon, message, System.currentTimeMillis());
            if (mMParticle.mConfigManager.isPushSoundEnabled()){
                notification.flags |= Notification.DEFAULT_SOUND;
            }
            if (mMParticle.mConfigManager.isPushVibrationEnabled()){
                notification.flags |= Notification.DEFAULT_VIBRATE;
            }
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.setLatestEventInfo(this, title, message, notifyIntent);
            notificationManager.notify(extras.hashCode(), notification);
        }
    }

}