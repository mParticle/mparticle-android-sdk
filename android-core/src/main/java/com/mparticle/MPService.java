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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.KitsLoadedListener;
import com.mparticle.internal.Logger;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;

import java.util.List;

/**
 * {@code IntentService } used internally by the SDK to process incoming broadcast messages in the background. Required for push notification functionality.
 * <p></p>
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 * <p></p>
 * <pre>
 * {@code
 * <service android:name="com.mparticle.MPService" />}
 * </pre>
 */
@SuppressLint("Registered")
public class MPService extends IntentService {

    {   // This a required workaround for a bug in AsyncTask in the Android framework.
        // AsyncTask.java has code that needs to run on the main thread,
        // but that is not guaranteed since it will be initialized on whichever
        // thread happens to cause the class to run its static initializers.
        // https://code.google.com/p/android/issues/detail?id=20915
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
    public static final String INTERNAL_NOTIFICATION_TAP = "com.mparticle.push.notification_tapped";
    private static final Object LOCK = MPService.class;
    private static final String INTERNAL_DELAYED_RECEIVE = "com.mparticle.delayeddelivery";

    private static PowerManager.WakeLock sWakeLock;

    public MPService() {
        super("com.mparticle.MPService");
    }

    /**
     *
     */
    public static void runIntentInService(Context context, Intent intent) {
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

    /**
     *
     */
    @Override
    public final void onHandleIntent(final Intent intent) {
        boolean release = true;
        try {
            String action = intent.getAction();
            Logger.info("MPService", "Handling action: " + action);
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                //Android will an Intent with this action, but we don't care - we capture the ID synchronously.
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                generateCloudMessage(intent);
            } else if (action.startsWith(INTERNAL_NOTIFICATION_TAP)) {
                handleNotificationTapInternal(intent);
            } else if (action.equals(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED)) {
                handleNotificationTap(intent);
            } else if (action.equals(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED)){
                final AbstractCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
                showNotification(message);
                release = false;
            } else if (action.equals(INTERNAL_DELAYED_RECEIVE)){
                final MPCloudNotificationMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
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
        if (!message.getDisplayed() && ConfigManager.isDisplayPushNotifications(this)) {
            (new AsyncTask<AbstractCloudMessage, Void, Notification>() {
                @Override
                protected Notification doInBackground(AbstractCloudMessage... params) {
                    return message.buildNotification(MPService.this, System.currentTimeMillis());
                }

                @Override
                protected void onPostExecute(Notification notification) {
                    super.onPostExecute(notification);
                    if (notification != null) {
                        NotificationManager mNotifyMgr =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNotifyMgr.cancel(message.getId());
                        mNotifyMgr.notify(message.getId(), notification);
                    }
                    synchronized (LOCK) {
                        if (sWakeLock != null && sWakeLock.isHeld()) {
                            sWakeLock.release();
                        }
                    }
                }
            }).execute(message);
        }
        String appState = MParticle.getAppState();
        if (message instanceof ProviderCloudMessage) {
            MParticle.getInstance().logNotification((ProviderCloudMessage) message, false, appState);
        } else if (message instanceof MPCloudNotificationMessage) {
            MParticle.getInstance().logNotification((MPCloudNotificationMessage) message, null, false, appState, AbstractCloudMessage.FLAG_RECEIVED | AbstractCloudMessage.FLAG_DISPLAYED);
        }
    }

    private void handleNotificationTap(Intent intent) {
        CloudAction action = intent.getParcelableExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA);
        AbstractCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
        PendingIntent actionIntent = action.getIntent(getApplicationContext(), message, action);
        if (actionIntent != null) {
            try {
                actionIntent.send();
            } catch (PendingIntent.CanceledException e) {

            }
        }
    }

    private void generateCloudMessage(final Intent intent) {
        if (!processSilentPush(getApplicationContext(), intent.getExtras())){
            try {

                KitsLoadedListener kitsLoadedListener = new KitsLoadedListener() {
                    @Override
                    public void onKitsLoaded() {
                        try{
                            MParticle.getInstance().getKitManager().loadKitLibrary();
                            AbstractCloudMessage cloudMessage = AbstractCloudMessage.createMessage(intent, ConfigManager.getPushKeys(MPService.this));
                            boolean handled = MParticle.getInstance().getKitManager().onMessageReceived(getApplicationContext(), intent);
                            cloudMessage.setDisplayed(handled);
                            String appState = MParticle.getAppState();
                            if (cloudMessage instanceof MPCloudNotificationMessage){
                                MParticle.getInstance().saveGcmMessage(((MPCloudNotificationMessage)cloudMessage), appState);
                                if (((MPCloudNotificationMessage)cloudMessage).isDelayed()){
                                    MParticle.getInstance().logNotification((MPCloudNotificationMessage)cloudMessage, null, false, appState, AbstractCloudMessage.FLAG_RECEIVED);
                                    scheduleFutureNotification((MPCloudNotificationMessage) cloudMessage);
                                    return;
                                }
                            } else if (cloudMessage instanceof ProviderCloudMessage){
                                MParticle.getInstance().saveGcmMessage(((ProviderCloudMessage)cloudMessage), appState);
                            }
                            broadcastNotificationReceived(cloudMessage);
                        }catch (Exception e){
                            Logger.warning(TAG, "GCM parsing error: " + e.toString());
                        }
                    }
                };
                KitFrameworkWrapper.setKitsLoadedListener(kitsLoadedListener);
                MParticle.start(this);

            }catch (Exception e){
               Logger.warning(TAG, "GCM parsing error: " + e.toString());
            }
        }
    }

    public boolean processSilentPush(Context context, Bundle extras) {
        if (extras != null &&
                extras.containsKey(MPCloudNotificationMessage.COMMAND)){
            int command = Integer.parseInt(extras.getString(MPCloudNotificationMessage.COMMAND));
            switch (command){
                case MPCloudNotificationMessage.COMMAND_ALERT_CONFIG_REFRESH:
                    MParticle.start(context);
                    MParticle.getInstance().refreshConfiguration();
                case MPCloudNotificationMessage.COMMAND_DONOTHING:
                    return true;
                default:
                    return false;
            }


        }
        return false;
    }

    private void scheduleFutureNotification(MPCloudNotificationMessage message){
        AlarmManager alarmService = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MPService.INTERNAL_DELAYED_RECEIVE);
        intent.setClass(this, MPService.class);
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);

        PendingIntent pIntent = PendingIntent.getService(this, message.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmService.setExact(AlarmManager.RTC, message.getDeliveryTime(), pIntent);
        }else{
            alarmService.set(AlarmManager.RTC, message.getDeliveryTime(), pIntent);
        }
    }

    private void broadcastNotificationReceived(AbstractCloudMessage message) {
        Intent intent = new Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
        intent.addCategory(getPackageName());

        List<ResolveInfo> result = getPackageManager().queryBroadcastReceivers(intent, 0);
        if (result != null && result.size() > 0){
            sendBroadcast(intent, null);
        } else {
            onHandleIntent(intent);
        }
    }

    private void handleNotificationTapInternal(Intent intent) {
        AbstractCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
        CloudAction action = intent.getParcelableExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA);

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(message.getId());

        MParticle.start(getApplicationContext());
        if (message instanceof MPCloudNotificationMessage) {
            MParticle.getInstance().logNotification((MPCloudNotificationMessage) message,
                    action, true, MParticle.getAppState(), AbstractCloudMessage.FLAG_READ | AbstractCloudMessage.FLAG_DIRECT_OPEN);
        }

        Intent broadcast = new Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
        broadcast.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
        broadcast.putExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA, action);

        List<ResolveInfo> result = getPackageManager().queryBroadcastReceivers(broadcast, 0);
        if (result != null && result.size() > 0){
            sendBroadcast(broadcast, null);
        } else {
            onHandleIntent(broadcast);
        }
    }

}