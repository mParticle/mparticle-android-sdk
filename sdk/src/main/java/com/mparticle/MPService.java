package com.mparticle;

import android.annotation.SuppressLint;
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
 * <p/>
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 * <p/>
 * <pre>
 * {@code
 * <service android:name="com.mparticle.MPService" />}
 * </pre>
 */
@SuppressLint("Registered")
public class MPService extends IntentService {

    private static final String TAG = Constants.LOG_TAG;
    private static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.push.notification_opened";
    private static final Object LOCK = MPService.class;
    private static PowerManager.WakeLock sWakeLock;
    private static final String APP_STATE = "com.mparticle.push.appstate";
    private static final String PUSH_ORIGINAL_PAYLOAD = "com.mparticle.push.originalpayload";
    private static final String PUSH_REDACTED_PAYLOAD = "com.mparticle.push.redactedpayload";
    private static final String BROADCAST_PERMISSION = ".mparticle.permission.NOTIFICATIONS";

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
            } else if (action.equals(MPARTICLE_NOTIFICATION_OPENED)) {
                handleNotificationClick(intent);
            } else {
                handeReRegistration();
            }
        } finally {
            synchronized (LOCK) {
                if (sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void handeReRegistration() {
        MParticle.start(getApplicationContext());
        MParticle.getInstance();
    }

    private void handleNotificationClick(Intent intent) {

        broadcastNotificationClicked(intent.getBundleExtra(PUSH_ORIGINAL_PAYLOAD));

        try {
            MParticle.start(getApplicationContext());
            MParticle mMParticle = MParticle.getInstance();
            Bundle extras = intent.getExtras();
            String appState = extras.getString(APP_STATE);
            mMParticle.logNotification(intent.getExtras().getBundle(PUSH_REDACTED_PAYLOAD),
                                        appState);
        } catch (Throwable t) {

        }

        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);

        PackageManager packageManager = getPackageManager();
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
            newExtras.putBundle(PUSH_ORIGINAL_PAYLOAD, extras);
            newExtras.putBundle(PUSH_REDACTED_PAYLOAD, extras);

            if (!MParticle.appRunning) {
                extras.putString(APP_STATE, AppStateManager.APP_STATE_NOTRUNNING);
            }

            MParticle mMParticle = MParticle.getInstance();
            if (!extras.containsKey(Constants.MessageKey.APP_STATE)) {
                if (mMParticle.mAppStateManager.isBackgrounded()) {
                    extras.putString(APP_STATE, AppStateManager.APP_STATE_BACKGROUND);
                } else {
                    extras.putString(APP_STATE, AppStateManager.APP_STATE_FOREGROUND);
                }
            }

            String title = "Unknown";
            try {
                int stringId = getApplicationInfo().labelRes;
                title = getResources().getString(stringId);
            } catch (Resources.NotFoundException ex) {

            }

            String[] possibleKeys = mMParticle.mConfigManager.getPushKeys();
            String key = findMessageKey(possibleKeys, extras);
            String message = extras.getString(key);
            newExtras.getBundle(PUSH_REDACTED_PAYLOAD).putString(key, "");
            newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD).putString(MParticle.Push.PUSH_ALERT_EXTRA, message);

            showPushWithMessage(title, message, newExtras);

            broadcastNotificationReceived(newExtras.getBundle("originalPayload"));
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }
    }

    private void broadcastNotificationReceived(Bundle originalPayload) {
        Intent intent = new Intent(MParticle.Push.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtras(originalPayload);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }

    private void broadcastNotificationClicked(Bundle originalPayload) {
        Intent intent = new Intent(MParticle.Push.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtras(originalPayload);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }

    private String findMessageKey(String[] possibleKeys, Bundle extras){
        if (possibleKeys != null) {
            for (String key : possibleKeys) {
                String message = extras.getString(key);
                if (message != null && message.length() > 0) {
                    return key;
                }
            }
        }
        Log.w(Constants.LOG_TAG, "Failed to extract push alert message using configuration.");
        return null;
    }

    private void showPushWithMessage(String title, String message, Bundle newExtras) {
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
        launchIntent.putExtras(newExtras);
        PendingIntent notifyIntent = PendingIntent.getService(getApplicationContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification(applicationIcon, message, System.currentTimeMillis());
        if (mMParticle.mConfigManager.isPushSoundEnabled()) {
            notification.flags |= Notification.DEFAULT_SOUND;
        }
        if (mMParticle.mConfigManager.isPushVibrationEnabled()) {
            notification.flags |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this, title, message, notifyIntent);
        notificationManager.notify(message.hashCode(), notification);
    }

}