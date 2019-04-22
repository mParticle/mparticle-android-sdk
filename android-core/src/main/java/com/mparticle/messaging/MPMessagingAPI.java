package com.mparticle.messaging;

import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.mparticle.MPService;
import com.mparticle.MParticle;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.PushRegistrationHelper;

/**
 * Utility class to enable and adjust push notification behavior. Do not directly instantiate this class.
 *
 * @see com.mparticle.MParticle#Messaging()
 *
 */
public class MPMessagingAPI {
    private final Context mContext;

    @NonNull public static final String CLOUD_MESSAGE_EXTRA = "mp-push-message";

    /**
     * Listen for this broadcast (as an Intent action) to detect when the device received a push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    @NonNull public static final String BROADCAST_NOTIFICATION_RECEIVED = "com.mparticle.push.RECEIVE";
    /**
     * Listen for this broadcast (as an Intent action) to detect when a user taps a push message in the
     * notification bar of their device. You may then navigate or otherwise adjust the user experience based on
     * the payload of the push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    @NonNull public static final String BROADCAST_NOTIFICATION_TAPPED = "com.mparticle.push.TAP";

    private MPMessagingAPI(){
        mContext = null;
    }

    /**
     *
     */
    public MPMessagingAPI(@NonNull Context context) {
        super();
        mContext = context;
    }

    /**
     * Register the application for FCM notifications
     *
     * @param senderId the SENDER_ID for the application
     */
    public void enablePushNotifications(@NonNull String senderId) {
        MParticle.getInstance().Internal().getConfigManager().setPushSenderId(senderId);
        if (!MPUtility.isFirebaseAvailable()) {
            Logger.error("Push is enabled but Firebase Cloud Messaging library not found - you must add com.google.firebase:firebase-messaging:7.5 or later to your application.");
        }else if (!MPUtility.isServiceAvailable(mContext, MPService.class)){
            Logger.error("Push is enabled but you have not added <service android:name=\"com.mparticle.MPService\" /> to the <application> section of your AndroidManifest.xml");
        }else if (!MPUtility.checkPermission(mContext, "com.google.android.c2dm.permission.RECEIVE")){
            Logger.error("Attempted to enable push notifications without required permission: ", "\"com.google.android.c2dm.permission.RECEIVE\"");
        }else {
            PushRegistrationHelper.requestInstanceId(mContext, senderId);
        }
    }

    /**
     * Unregister the application for FCM notifications
     */
    public void disablePushNotifications() {
        MParticle.getInstance().Internal().getConfigManager().clearPushRegistration();
    }

    public void displayPushNotificationByDefault(@Nullable Boolean enabled) {
        MParticle.getInstance().Internal().getConfigManager().setDisplayPushNotifications(enabled);
    }

    /**
     * Register a custom implementation of PushAnalyticsReceiver, which will provide callbacks when
     * push messages are received
     * @param receiver the implementation of PushAnalyticsReceiver to be registered
     *
     * @see PushAnalyticsReceiver
     */
    public void registerPushAnalyticsReceiver(@NonNull PushAnalyticsReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_NOTIFICATION_RECEIVED);
        intentFilter.addAction(BROADCAST_NOTIFICATION_TAPPED);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, intentFilter);
    }

    /**
     * Unregister you custom implementation of PushAnalyticsReceiver. If the receiver is not found
     * to have been registered, nothing will happen.
     *
     * @param receiver the previously registered implementation of PushAnalyticsReceiver to be unregistered
     *
     * @see PushAnalyticsReceiver
     */
    public void unregisterPushAnalyticsReceiver(@Nullable PushAnalyticsReceiver receiver) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
    }
}
