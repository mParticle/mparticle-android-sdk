package com.mparticle;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudBackgroundMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.ProviderCloudMessage;

import java.security.Provider;
import java.util.List;

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

    { // http://stackoverflow.com/questions/4280330/onpostexecute-not-being-called-in-asynctask-handler-runtime-exception
        Looper looper = Looper.getMainLooper();
        Handler handler = new Handler(looper);
        handler.post(new Runnable() {
            public void run() {
                try {
                    Class.forName("android.os.AsyncTask");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static final String TAG = Constants.LOG_TAG;
    public static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.push.notification_opened";
    public static final String INTERNAL_NOTIFICATION_TAP = "com.mparticle.push.notification_tapped";
    private static final Object LOCK = MPService.class;
    private static final String INTERNAL_DELAYED_RECEIVE = "com.mparticle.delayeddelivery";

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
            } else if (action.startsWith(INTERNAL_NOTIFICATION_TAP)) {
                handleNotificationTapInternal(intent);
            } else if (action.equals(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED)) {
                handleNotificationTap(intent);
            } else if (action.equals(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED)){
                final AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
                showNotification(message);
                release = false;
            } else if (action.equals(INTERNAL_DELAYED_RECEIVE)){
                final MPCloudNotificationMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
                broadcastNotificationReceived(message);
            }
        } finally {
            synchronized (LOCK) {
                if (release && sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void showNotification(final AbstractCloudMessage message) {
        final boolean isNetworkingEnabled = ConfigManager.isNetworkPerformanceEnabled();
        if (isNetworkingEnabled){
            ConfigManager.setNetworkingEnabled(false);
        }
        (new AsyncTask<AbstractCloudMessage, Void, Notification>() {
            @Override
            protected Notification doInBackground(AbstractCloudMessage... params) {
                Notification notification =  params[0].buildNotification(MPService.this, System.currentTimeMillis());

                MParticle.getInstance().internal().logNotification(params[0], null, false, getAppState(), AbstractCloudMessage.FLAG_RECEIVED | AbstractCloudMessage.FLAG_DISPLAYED);
                return notification;
            }

            @Override
            protected void onPostExecute(Notification notification) {
                super.onPostExecute(notification);
                if (notification != null) {
                    NotificationManager mNotifyMgr =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.cancel(message.getId().hashCode());
                    mNotifyMgr.notify(message.getId().hashCode(), notification);
                }

                if (isNetworkingEnabled){
                    ConfigManager.setNetworkingEnabled(true);
                }
                synchronized (LOCK) {
                    if (sWakeLock != null && sWakeLock.isHeld()) {
                        sWakeLock.release();
                    }
                }
            }
        }).execute(message);
        MParticle.getInstance().endMeasuringNetworkPerformance();
    }

    private void handleNotificationTap(Intent intent) {
        CloudAction action = intent.getParcelableExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA);
        AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
        PendingIntent actionIntent = action.getIntent(getApplicationContext(), message, action);
        if (actionIntent != null) {
            try {
                actionIntent.send();
            } catch (PendingIntent.CanceledException e) {

            }
        }
    }

    private String getAppState(){
        String appState = AppStateManager.APP_STATE_NOTRUNNING;
        if (AppStateManager.appRunning) {
            if (MParticle.getInstance().mAppStateManager.isBackgrounded()) {
                appState = AppStateManager.APP_STATE_BACKGROUND;
            } else {
                appState = AppStateManager.APP_STATE_FOREGROUND;
            }
        }
        return appState;
    }



    private void handleRegistration(Intent intent) {
        try {
            MParticle mMParticle = MParticle.getInstance();
            String registrationId = intent.getStringExtra("registration_id");
            String unregistered = intent.getStringExtra("unregistered");
            String error = intent.getStringExtra("error");

            if (registrationId != null) {
                mMParticle.internal().setPushRegistrationId(registrationId);
            } else if (unregistered != null) {
                mMParticle.internal().clearPushNotificationId();
            } else if (error != null) {
                Log.i(TAG, "GCM registration error: " + error);
            }
        } catch (Throwable t) {

        }
    }

    private void generateCloudMessage(Intent intent) {
        if (!MPCloudBackgroundMessage.processSilentPush(this, intent.getExtras())){
            try {
                AbstractCloudMessage cloudMessage = AbstractCloudMessage.createMessage(intent, ConfigManager.getPushKeys(this));
                String appState = getAppState();
                if (cloudMessage instanceof MPCloudNotificationMessage){

                    MParticle.start(this);
                    MParticle.getInstance().saveGcmMessage(((MPCloudNotificationMessage)cloudMessage), appState);
                    if (((MPCloudNotificationMessage)cloudMessage).isDelayed()){
                        MParticle.getInstance().internal().logNotification(cloudMessage, null, false, appState, AbstractCloudMessage.FLAG_RECEIVED);
                        scheduleFutureNotification((MPCloudNotificationMessage) cloudMessage);
                        return;
                    }
                }else if (cloudMessage instanceof ProviderCloudMessage){
                    MParticle.getInstance().saveGcmMessage(((ProviderCloudMessage)cloudMessage), appState);
                }
                broadcastNotificationReceived(cloudMessage);
            }catch (Exception e){
                Log.w(TAG, "GCM parsing error: " + e.toString());
            }
        }
    }

    private void scheduleFutureNotification(MPCloudNotificationMessage message){
        AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MPService.INTERNAL_DELAYED_RECEIVE);
        intent.setClass(this, MPService.class);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);

        PendingIntent pIntent = PendingIntent.getService(this, message.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmService.setExact(AlarmManager.RTC, message.getDeliveryTime(), pIntent);
        }else{
            alarmService.set(AlarmManager.RTC, message.getDeliveryTime(), pIntent);
        }
    }

    private void broadcastNotificationReceived(AbstractCloudMessage message) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        intent.addCategory(getPackageName());

        List<ResolveInfo> result = getPackageManager().queryBroadcastReceivers(intent, 0);
        if (result != null && result.size() > 0){
            sendBroadcast(intent, null);
        } else {
            onHandleIntent(intent);
        }
    }

    private void handleNotificationTapInternal(Intent intent) {
        AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
        CloudAction action = intent.getParcelableExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA);

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(message.getId().hashCode());

        MParticle.start(getApplicationContext());
        MParticle.getInstance().internal().logNotification(message,
                action, true, getAppState(), AbstractCloudMessage.FLAG_READ | AbstractCloudMessage.FLAG_DIRECT_OPEN);

        Intent broadcast = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED);
        broadcast.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        broadcast.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);

        List<ResolveInfo> result = getPackageManager().queryBroadcastReceivers(broadcast, 0);
        if (result != null && result.size() > 0){
            sendBroadcast(broadcast, "com.mparticle.permission.push");
        } else {
            onHandleIntent(broadcast);
        }
    }

}