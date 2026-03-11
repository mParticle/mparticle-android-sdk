package com.mparticle.kits.pushsample;

import android.app.Application;

import com.mparticle.MParticle;

public class SamplePushApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MParticle.start(this, "{YOUR_APP_KEY}", "{YOUR_APP_SECRET}");
        MParticle
                .getInstance()
                .Messaging()
                .enablePushNotifications("{YOUR_SENDER_ID}");

    }
}
