package com.mparticle.messaging;

public interface PushAnalyticsReceiverCallback {
    /**
     * Override this method to listen for when a notification has been received.
     *
     * @param message The message that was received. Depending on the push provider
     * @return True if you would like to handle this notification, False if you would like the mParticle to generate and show a {@link android.app.Notification}.
     */
    boolean onNotificationReceived(ProviderCloudMessage message);

    /**
     * Override this method to listen for when a notification has been tapped or acted on.
     *
     * @param message The message that was tapped. Depending on the push provider
     * @return True if you would like to consume this tap/action, False if the mParticle SDK should attempt to handle it.
     */
    boolean onNotificationTapped(ProviderCloudMessage message);
}
