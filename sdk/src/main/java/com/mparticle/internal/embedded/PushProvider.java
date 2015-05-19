package com.mparticle.internal.embedded;

import android.content.Intent;

import com.mparticle.messaging.AbstractCloudMessage;

public interface PushProvider {
    boolean handleGcmMessage(Intent intent);
}
