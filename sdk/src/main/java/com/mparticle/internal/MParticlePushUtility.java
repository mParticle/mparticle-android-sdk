package com.mparticle.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/**
 * Helper class to listen for different events related to receiving and interacting with push notifications
 * received from the Google Cloud Messaging service.
 *
 * Note that listening for MParticle push events and broadcasts requires that you've properly registered the correct
 * permission in your AndroidManifest.xml. This is done so that other apps can not intercept potentially sensitive data sent
 * via a GCM message:
 *
 * <pre>
 * {@code
 *  <permission
 *      android:name="YOURPACKAGENAME.mparticle.permission.NOTIFICATIONS"
 *      android:protectionLevel="signature" />
 * <uses-permission android:name="YOURPACKAGENAME.mparticle.permission.NOTIFICATIONS" />
 * }</pre>
 */
public final class MParticlePushUtility {
    private MParticlePushUtility() {
        super();
    }

    /**
     * Listen for this broadcast (as an Intent action) to detect when the device received a push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    public static final String BROADCAST_NOTIFICATION_RECEIVED = "com.mparticle.push.NOTIFICATION_RECEIVED";
    /**
     * Listen for this broadcast (as an Intent action) to detect when a user taps a push message in the
     * notification bar of their device. You may then navigate or otherwise adjust the user experience based on
     * the payload of the push.
     *
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    public static final String BROADCAST_NOTIFICATION_TAPPED = "com.mparticle.push.NOTIFICATION_TAPPED";
    /**
     * If leveraging either <code>BROADCAST_NOTIFICATION_RECEIVED</code> or <code>BROADCAST_NOTIFICATION_TAPPED</code>, you
     * have access to the entire payload of a push notification. Depending on the service that has sent the push, for example
     * Urban Airship or Mixpanel, the primary alert text will have a different key associated with it. The mParticle SDK
     * take care of this for you, and will add the primary alert of the push an an extra to the received intent with this key.
     * This allows you to switch push notification providers without having to adjust the code of your application.
     */
    public static final String PUSH_ALERT_EXTRA = "com.mparticle.push.alert";

    /**
     * Register to receive callbacks for when a Push notification is received and when it is tapped.
     *
     * It's important, as with all BroadcastReceivers, to call {@link android.content.Context#unregisterReceiver(android.content.BroadcastReceiver)}
     * once you no longer need it or when your {@link android.app.Activity} is being destroyed, for example.
     *
     * Note: If your app is no longer running on a users devices (because they either restarted their device, or they forceably closed your application), this
     * receiver will no longer be registered. In order to work around this you can either override the Android {@link android.app.Application } class, or you can
     * declare a {@link MParticlePushUtility.MParticlePushReceiver in your AndroidManifest.xml}.
     *
     * @param context Required: Used to derive the package name and register the receiver.
     * @param receiver Required: Your listener to be called.
     */
    public final static void registerReceiver(Context context, MParticlePushReceiver receiver) {
        if (context == null){
            throw new IllegalArgumentException("Context must not be null.");
        }
        if (receiver == null){
            throw new IllegalArgumentException("Receiver must not be null.");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NOTIFICATION_RECEIVED);
        filter.addAction(BROADCAST_NOTIFICATION_TAPPED);
        context.registerReceiver(receiver, filter, context.getApplicationContext().getPackageName() + ".mparticle.permission.NOTIFICATIONS", null);
    }

    /**
     * Use this BroadcastReceiver as a convenience, which will automatically call the appropriate method
     * depending on if a user has just received or has clicked a notification. This receiver additionally parses the
     * message that was included in the push.
     *
     */
    public static abstract class MParticlePushReceiver extends BroadcastReceiver {

        /**
         * Called when a notification created by MParticle is tapped.
         * Will correlate with the {@link #BROADCAST_NOTIFICATION_TAPPED}
         * Intent broadcast.
         *
         * @param message The primary alert message of the notification, if any.
         * @param allExtras All of the extras that were included. You can use this to support deep linking or other directives to your app
         *                  that this push notification should trigger.
         */
        public abstract void onNotificationTapped(String message, Bundle allExtras);
        /**
         * Called when a notification created by MParticle is tapped.
         * Will correlate with the {@link #BROADCAST_NOTIFICATION_RECEIVED}
         * Intent broadcast.
         *
         * @param message The primary alert message of the notification, if any.
         * @param allExtras All of the extras that were included. You can use this to support deep linking or other directives to your app
         */
        public abstract void onNotificationReceived(String message, Bundle allExtras);

        @Override
        public final void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_NOTIFICATION_RECEIVED)){
                onNotificationReceived(intent.getExtras().getString(PUSH_ALERT_EXTRA), intent.getExtras());
            }else if (intent.getAction().equals(BROADCAST_NOTIFICATION_TAPPED)){
                onNotificationTapped(intent.getExtras().getString(PUSH_ALERT_EXTRA), intent.getExtras());
            }
        }
    }
}
