package com.mparticle.messaging;

import androidx.annotation.Nullable;

public interface MessagingConfigCallbacks {
    void onInstanceIdRegistered(@Nullable String instanceId);
}
