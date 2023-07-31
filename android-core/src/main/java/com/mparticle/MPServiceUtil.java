package com.mparticle;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.KitsLoadedListener;
import com.mparticle.internal.KitsLoadedListenerConfiguration;
import com.mparticle.internal.Logger;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;

public class MPServiceUtil {
    public static final String NOTIFICATION_CHANNEL = "com.mparticle.default";
    public static final String INTERNAL_NOTIFICATION_TAP = "com.mparticle.push.notification_tapped";
    private static PowerManager.WakeLock sWakeLock;
    private static final String TAG = Constants.LOG_TAG + ":wakeLock";
    private static final Object LOCK = MPService.class;

    private Context mContext;


    {   // This a required workaround for a bug in AsyncTask in the Android framework.
        // AsyncTask.java has code that needs to run on the main thread,
        // but that is not guaranteed since it will be initialized on whichever
        // thread happens to cause the class to run its static initializers. See, 
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
        try {
            context.startService(intent);
        } catch (IllegalStateException ignore) {
            new MPServiceUtil(context).onHandleIntent(intent);
        }
    }

    public MPServiceUtil(Context context) {
        mContext = context;
    }


    public void onHandleIntent(Intent intent) {
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
            } else if (action.equals(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED)) {
                final ProviderCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
                showNotification(message);
            }
        } finally {
            synchronized (LOCK) {
                if (sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void showNotification(final ProviderCloudMessage message) {
        if (!message.getDisplayed() && ConfigManager.isDisplayPushNotifications(mContext)) {
            (new AsyncTask<ProviderCloudMessage, Void, Notification>() {
                @Override
                protected Notification doInBackground(ProviderCloudMessage... params) {
                    return message.buildNotification(mContext, System.currentTimeMillis());
                }

                @Override
                protected void onPostExecute(Notification notification) {
                    super.onPostExecute(notification);
                    if (notification != null) {
                        NotificationManager mNotifyMgr =
                                (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            createNotificationChannel(mNotifyMgr);
                        }
                        mNotifyMgr.cancel(message.getId());
                        mNotifyMgr.notify(message.getId(), notification);
                    }
                }
            }).execute(message);
        }
        String appState = MParticle.getAppState();
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            instance.logNotification(message, false, appState);
        }
    }

    private void handleNotificationTap(Intent intent) {
        ProviderCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
        Intent pe = message.getDefaultOpenIntent(mContext, message);
        pe.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pe.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity
                    (mContext, 0, pe, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(mContext, 0, pe, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {

            }
        }
    }

    private void generateCloudMessage(final Intent intent) {
        try {
            KitsLoadedListener kitsLoadedListener = () -> {
                try {
                    MParticle instance = MParticle.getInstance();
                    if (instance != null) {
                        ProviderCloudMessage cloudMessage = ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mContext));
                        boolean handled = instance.Internal().getKitManager().onMessageReceived(mContext.getApplicationContext(), intent);
                        cloudMessage.setDisplayed(handled);
                        broadcastNotificationReceived(cloudMessage);
                    }
                } catch (Exception e) {
                    Logger.warning("FCM parsing error: " + e);
                }
            };
            MParticle instance = MParticle.getInstance();
            if (instance != null) {
                instance.Internal().getKitManager().addKitsLoadedListener(kitsLoadedListener);
            } else {
                MParticle.start(MParticleOptions
                        .builder(mContext)
                        .configuration(new KitsLoadedListenerConfiguration(kitsLoadedListener))
                        .buildForInternalRestart());
            }

        } catch (Exception e) {
            Logger.warning("FCM parsing error: " + e);
        }
    }

    private void broadcastNotificationReceived(ProviderCloudMessage message) {
        Intent intent = new Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        onHandleIntent(intent);
    }

    private void handleNotificationTapInternal(Intent intent) {
        ProviderCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);

        NotificationManager manager =
                (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(message.getId());

        MParticle.start(MParticleOptions.builder(mContext.getApplicationContext()).buildForInternalRestart());
        Intent broadcast = new Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
        broadcast.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadcast);
        onHandleIntent(broadcast);
    }

    @RequiresApi(api = 26)
    private void createNotificationChannel(NotificationManager mNotifyMgr) {
        android.app.NotificationChannel channel = new android.app.NotificationChannel(NOTIFICATION_CHANNEL, "Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        mNotifyMgr.createNotificationChannel(channel);
    }
}
