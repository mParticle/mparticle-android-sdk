package com.mparticle.messaging;

import android.content.Context;
import android.content.Intent;

import com.mparticle.MPServiceUtil;

public class MPMessagingRouter {
    /**
     * Parses the incoming intent and delegates functionality to the given {@code callback} if appropriate. This implementation checks for
     * MParticle-specific actions and will not handle messages outside of that scope. MParticle actions can be found in
     * {@link MPMessagingAPI} {@code BROADCAST_*} constants.
     *
     * @param context
     * @param intent
     * @param callback
     * @return {@code true} if the {@link Intent} was handled by MParticle
     */
    public static boolean onReceive(Context context, Intent intent, PushAnalyticsReceiverCallback callback) {
        if (MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(intent.getAction())) {
            ProviderCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            if (!callback.onNotificationTapped(message)) {
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                MPServiceUtil.runIntentInService(context, intent);
            }
            return true;
        } else if (MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(intent.getAction())) {
            ProviderCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            if (!callback.onNotificationReceived(message)) {
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                MPServiceUtil.runIntentInService(context, intent);
            }
            return true;
        }

        return false;
    }
}
