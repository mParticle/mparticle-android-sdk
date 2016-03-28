package com.mparticle.messaging;

import android.content.Context;

import com.mparticle.MPService;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
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

    public static final String CLOUD_MESSAGE_EXTRA = "mp-push-message";
    public static final String CLOUD_ACTION_EXTRA = "mp-push-action";

    /**
     * Listen for this broadcast (as an Intent action) to detect when the device received a push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    public static final String BROADCAST_NOTIFICATION_RECEIVED = "com.mparticle.push.RECEIVE";
    /**
     * Listen for this broadcast (as an Intent action) to detect when a user taps a push message in the
     * notification bar of their device. You may then navigate or otherwise adjust the user experience based on
     * the payload of the push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    public static final String BROADCAST_NOTIFICATION_TAPPED = "com.mparticle.push.TAP";

    private MPMessagingAPI(){
        mContext = null;
    }

    /**
     *
     */
    public MPMessagingAPI(Context context) {
        super();
        mContext = context;
    }

    /**
     * Set the resource ID of the icon to be shown in the notification bar when a notification is received.
     *
     * By default, the app launcher icon will be shown.
     *
     * @param resId the resource id of a drawable
     */
    public void setPushNotificationIcon(int resId) {

    }

    /**
     * Set the resource ID of the title to be shown in the notification bar when a notification is received
     *
     * By default, the title of the application will be shown.
     *
     * @param resId the resource id of a string
     */
    public void setPushNotificationTitle(int resId) {
    }

    /**
     * Register the application for GCM notifications
     *
     * @param senderId the SENDER_ID for the application
     */
    public void enablePushNotifications(String senderId) {
        if (!MPUtility.isInstanceIdAvailable()) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Push is enabled but Google Cloud Messaging library not found - you must add com.google.android.gms:play-services-gcm:7.5 or later to your application.");
        }else if (!MPUtility.isServiceAvailable(mContext, MPService.class)){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Push is enabled but you have not added <service android:name=\"com.mparticle.MPService\" /> to the <application> section of your AndroidManifest.xml");
        }else if (!MPUtility.checkPermission(mContext, "com.google.android.c2dm.permission.RECEIVE")){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Attempted to enable push notifications without required permission: ", "\"com.google.android.c2dm.permission.RECEIVE\"");
        }else {
            PushRegistrationHelper.requestInstanceId(mContext, senderId);
        }
    }

    /**
     * Unregister the application for GCM notifications
     */
    public void disablePushNotifications() {
        PushRegistrationHelper.disablePushNotifications(mContext);
    }

    /**
     * Enable the default notification sound for push notifications. This is a user preference that will be persisted across
     * application sessions.
     *
     * @param enabled
     */
    public void setNotificationSoundEnabled(Boolean enabled) {
    }

    /**
     * Enable the default notification vibration for push notifications. This is a user preference that will be persisted across
     * application sessions.
     *
     * @param enabled
     */
    public void setNotificationVibrationEnabled(Boolean enabled) {
    }
}
