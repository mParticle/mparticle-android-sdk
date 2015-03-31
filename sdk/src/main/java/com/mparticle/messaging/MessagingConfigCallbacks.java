package com.mparticle.messaging;

/**
 * @hide
 */
public interface MessagingConfigCallbacks {
    void setPushNotificationIcon(int resId);

    void setPushNotificationTitle(int resId);

    void setPushSenderId(String senderId);

    void setPushSoundEnabled(boolean enabled);

    void setPushVibrationEnabled(boolean enabled);

    void setPushRegistrationId(String registrationId);
}
