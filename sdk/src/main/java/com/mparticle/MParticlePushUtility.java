package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/**
 * Created by sdozor on 8/22/14.
 */
public final class MParticlePushUtility {
    /**
     * Listen for this broadcast (as an Intent action) to detect when the device received a push.
     * <p/>
     * In the received Intent, you will have access to the entire payload of the push notification.
     */
    public static final String BROADCAST_NOTIFICATION_RECEIVED = "com.mparticle.push.NOTIFICATION_RECEIVED";
    /**
     * Listen for this broadcast (as an Intent action) to detect when a user taps a push message in the
     * notification bar of their device. You may then navigate or otherwise adjust the user experience based on
     * the payload of the push.
     * <p/>
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

    public final static void registerReceiver(Context context, MPPushReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NOTIFICATION_RECEIVED);
        filter.addAction(BROADCAST_NOTIFICATION_TAPPED);
        context.registerReceiver(receiver, filter, BuildConfig.PACKAGE_NAME + ".mparticle.permission.NOTIFICATIONS", null);
    }

    public static abstract class MPPushReceiver extends BroadcastReceiver {

        public abstract void onNotificationTapped(String message, Bundle allExtras);
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
