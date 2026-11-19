package com.mparticle.messaging;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.RestrictTo;

import com.mparticle.MPServiceUtil;
import com.mparticle.internal.Logger;

import java.util.UUID;

public class MPMessagingRouter {
    static final String INTERNAL_BROADCAST_AUTH_EXTRA = "com.mparticle.push.internal_auth_token";
    private static final String PROCESS_AUTH_TOKEN = UUID.randomUUID().toString();

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Intent markInternalBroadcast(Intent intent) {
        if (intent != null) {
            intent.putExtra(INTERNAL_BROADCAST_AUTH_EXTRA, PROCESS_AUTH_TOKEN);
        }
        return intent;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static boolean isInternalBroadcast(Intent intent) {
        if (intent == null) {
            return false;
        }
        try {
            return PROCESS_AUTH_TOKEN.equals(intent.getStringExtra(INTERNAL_BROADCAST_AUTH_EXTRA));
        } catch (RuntimeException e) {
            Logger.warning("MPMessagingRouter", "Unable to verify push broadcast authenticity: " + e);
            return false;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static ProviderCloudMessage extractCloudMessage(Intent intent) {
        if (intent == null) {
            return null;
        }
        try {
            Object extra = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            if (extra instanceof ProviderCloudMessage) {
                return (ProviderCloudMessage) extra;
            }
            if (extra != null) {
                Logger.warning("MPMessagingRouter", "Ignoring cloud message extra of unexpected type: " + extra.getClass().getName());
            }
        } catch (RuntimeException e) {
            Logger.warning("MPMessagingRouter", "Unable to read the cloud message extra: " + e);
        }
        return null;
    }

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
        if (intent == null || callback == null) {
            return false;
        }
        String action = intent.getAction();
        if (!MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(action)
                && !MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(action)) {
            return false;
        }
        if (!isInternalBroadcast(intent)) {
            Logger.warning("MPMessagingRouter", "Ignoring unauthenticated push broadcast for action: " + action);
            return false;
        }
        ProviderCloudMessage message = extractCloudMessage(intent);
        if (message == null) {
            Logger.warning("MPMessagingRouter", "Ignoring push broadcast with missing or invalid cloud message extra for action: " + action);
            return false;
        }
        if (MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(action)) {
            if (!callback.onNotificationTapped(message)) {
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                MPServiceUtil.runIntentInService(context, intent);
            }
            return true;
        } else if (MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(action)) {
            if (!callback.onNotificationReceived(message)) {
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                MPServiceUtil.runIntentInService(context, intent);
            }
            return true;
        }

        return false;
    }
}
