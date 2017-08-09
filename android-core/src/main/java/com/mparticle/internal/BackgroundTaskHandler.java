package com.mparticle.internal;

public interface BackgroundTaskHandler {
    void executeNetworkRequest(Runnable runnable);
}
