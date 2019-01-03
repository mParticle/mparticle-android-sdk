package com.mparticle.messaging;

import android.support.annotation.Nullable;

public interface MessagingConfigCallbacks {
    void onInstanceIdRegistered(@Nullable String instanceId);
}
