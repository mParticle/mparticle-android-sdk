package com.mparticle.internal.embedded;

import android.content.Intent;

import com.mparticle.messaging.AbstractCloudMessage;

public interface PushProvider {
    void onRegistered(String registrationId);
    boolean handleGcmMessage(Intent intent);
}
