package com.mparticle.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * <code>BroadcastReceiver</code> to be used to listen for, manipulate, and react to FCM notifications.
 *
 * In order to use this class:
 *
 * 1. Create your own class which extends this one, for example MyPushReceiver.java
 * 2. Register an instance via {@link MPMessagingAPI#registerPushAnalyticsReceiver(PushAnalyticsReceiver)}:
 * <pre>
 *  {@code MParticle.getInstance().Messaging().registerPushAnalyticsReceiver(new MyPushReceiver());}</pre>
 *
 * Note: {@code com.mparticle.push.RECEIVE} and {@code com.mparticle.push.TAP} are dispatched internally via
 * {@link androidx.localbroadcastmanager.content.LocalBroadcastManager} to receivers registered through
 * {@link MPMessagingAPI#registerPushAnalyticsReceiver(PushAnalyticsReceiver)} rather than manifest-declared receivers.
 *
 * @see MPMessagingAPI#registerPushAnalyticsReceiver(PushAnalyticsReceiver)
 * @see MPMessagingAPI#unregisterPushAnalyticsReceiver(PushAnalyticsReceiver)
 * @see #onNotificationReceived(ProviderCloudMessage)
 * @see #onNotificationTapped(ProviderCloudMessage)
 */
public class PushAnalyticsReceiver extends BroadcastReceiver implements PushAnalyticsReceiverCallback {

    @Override
    public final void onReceive(Context context, Intent intent) {
        MPMessagingRouter.onReceive(context, intent, this);
    }

    @Override
    public boolean onNotificationReceived(ProviderCloudMessage message) {
        return false;
    }

    @Override
    public boolean onNotificationTapped(ProviderCloudMessage message) {
        return false;
    }
}