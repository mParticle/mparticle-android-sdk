package com.mparticle.kits.adjust.example.java;

import android.app.Application;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MParticleOptions options =
                MParticleOptions.builder(this)
                        .credentials(
                                "REPLACE WITH YOUR MPARTICLE API KEY",
                                "REPLACE WITH YOUR MPARTICLE API SECRET")
                        .logLevel(MParticle.LogLevel.VERBOSE)
                        .build();
        MParticle.start(options);
        MParticle.getInstance()
                .logEvent(new MPEvent.Builder("foo", MParticle.EventType.Other).build());
    }
}
