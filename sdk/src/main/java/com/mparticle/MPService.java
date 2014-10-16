package com.mparticle;

import android.annotation.SuppressLint;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;

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
    static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.push.notification_opened";
    private static final String INTERNAL_NOTIFICATION_TAP = "com.mparticle.push.notification_tapped";
    private static final Object LOCK = MPService.class;

    private static PowerManager.WakeLock sWakeLock;

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
        context.startService(intent);
    }

    @Override
    public final void onHandleIntent(final Intent intent) {
        boolean release = true;
        try {
            String action = intent.getAction();
            Log.i("MPService", "Handling action: " + action);
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                MParticle.start(getApplicationContext());
                MParticle.getInstance().mEmbeddedKitManager.handleIntent(intent);
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                generateCloudMessage(intent);
            } else if (action.equals("com.google.android.c2dm.intent.UNREGISTER")) {
                intent.putExtra("unregistered", "true");
                handleRegistration(intent);
            } else if (action.equals(INTERNAL_NOTIFICATION_TAP)) {
                handleNotificationTapInternal(intent);
            } else if (action.equals(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED)) {
                handleNotificationTap(intent);
            } else if (action.equals(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED)){
                release = false;
                (new AsyncTask<AbstractCloudMessage, Void, Notification>() {
                    @Override
                    protected Notification doInBackground(AbstractCloudMessage... params) {
                        return params[0].buildNotification(MPService.this);
                    }

                    @Override
                    protected void onPostExecute(Notification notification) {
                        super.onPostExecute(notification);
                        if (notification != null) {
                            NotificationManager mNotifyMgr =
                                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            mNotifyMgr.notify(notification.hashCode(), notification);
                        }
                        synchronized (LOCK) {
                            if (sWakeLock != null && sWakeLock.isHeld()) {
                                sWakeLock.release();
                            }
                        }
                    }
                }).execute((AbstractCloudMessage)intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA));
            } else {
                handeReRegistration();
            }
        } finally {
            synchronized (LOCK) {
                if (release && sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void handleNotificationTapInternal(Intent intent) {
        intent.setAction(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void handeReRegistration() {
        MParticle.start(getApplicationContext());
        MParticle.getInstance();
    }

    private void handleNotificationTap(Intent intent) {
        CloudAction action = intent.getParcelableExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA);
        PendingIntent actionIntent = action.getIntent(getApplicationContext(), null);
        if (actionIntent != null) {
            try {
                actionIntent.send();
            } catch (PendingIntent.CanceledException e) {

            }
        }
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

        }
    }

    private void generateCloudMessage(Intent intent) {
        String appState = AppStateManager.APP_STATE_NOTRUNNING;
        if (MParticle.appRunning) {
            if (MParticle.getInstance().mAppStateManager.isBackgrounded()) {
                appState = AppStateManager.APP_STATE_BACKGROUND;
            } else {
                appState = AppStateManager.APP_STATE_FOREGROUND;
            }
        }

        AbstractCloudMessage cloudMessage = AbstractCloudMessage.createMessage(intent);
        cloudMessage.setAppState(appState);
        broadcastNotificationReceived(cloudMessage);
    }

    private void broadcastNotificationReceived(AbstractCloudMessage message) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}