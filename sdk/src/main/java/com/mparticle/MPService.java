package com.mparticle;

import android.annotation.SuppressLint;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.PowerManager;
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
    private static final Object LOCK = MPService.class;

    private static PowerManager.WakeLock sWakeLock;
    private static final String APP_STATE = "com.mparticle.push.appstate";
    //private static final String PUSH_ORIGINAL_PAYLOAD = "com.mparticle.push.originalpayload";
    //private static final String PUSH_REDACTED_PAYLOAD = "com.mparticle.push.redactedpayload";
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
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MPService", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onHandleIntent(final Intent intent) {
        boolean release = true;
        try {

            MParticle.start(getApplicationContext());
            MParticle.getInstance().mEmbeddedKitManager.handleIntent(intent);

            String action = intent.getAction();
            Log.i("MPService", "Handling action: " + action);
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                release = false;
                (new AsyncTask<Intent, Void, Notification>() {
                    @Override
                    protected Notification doInBackground(Intent... params) {
                        return generateGcmNotification(params[0]);
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
                }).execute(intent);

            } else if (action.equals("com.google.android.c2dm.intent.UNREGISTER")) {
                intent.putExtra("unregistered", "true");
                handleRegistration(intent);
            } else if (action.equals(MPARTICLE_NOTIFICATION_OPENED)) {
                handleNotificationTap(intent);
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

    private void handeReRegistration() {
        MParticle.start(getApplicationContext());
        MParticle.getInstance();
    }

    private void handleNotificationTap(Intent intent) {
       AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
       broadcastNotificationTapped((AbstractCloudMessage) intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA));

        try {
            MParticle.start(getApplicationContext());
            MParticle.getInstance().logNotification(message);
        } catch (Throwable t) {

        }
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getPackageName());
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

        }
    }

    private Notification generateGcmNotification(Intent intent) {
        String appState = AppStateManager.APP_STATE_NOTRUNNING;
        if (MParticle.appRunning) {
            if (MParticle.getInstance().mAppStateManager.isBackgrounded()) {
                appState = AppStateManager.APP_STATE_BACKGROUND;
            } else {
                appState = AppStateManager.APP_STATE_FOREGROUND;
            }
        }

        AbstractCloudMessage cloudMessage;
        try {
            cloudMessage = new MPCloudMessage(intent.getExtras());
        }catch (JSONException jse){
            cloudMessage = new ProviderCloudMessage(intent.getExtras());
        }
        cloudMessage.setAppState(appState);
        broadcastNotificationReceived(cloudMessage);
        return cloudMessage.buildNotification(getApplicationContext());

    }

    private void broadcastNotificationReceived(AbstractCloudMessage message) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }

    private void broadcastNotificationTapped(AbstractCloudMessage message) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }





}