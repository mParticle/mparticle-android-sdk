package com.mparticle.testutils;

import android.os.Handler;

import androidx.annotation.NonNull;

public class TestingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    Handler testHandler = new Handler();

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull final Throwable e) {
        if (t.getName().equals("mParticleMessageHandler") || t.getName().equals("mParticleUploadHandler")) {
            testHandler.post(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}