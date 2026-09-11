package com.mparticle.sizereport;

import android.app.Application;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

/**
 * Holds a live reference to the SDK's public entry point so R8 keeps it reachable.
 * Never launched -- the fixture exists only to be measured.
 */
public class SizeHostApplication extends Application {
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
    }
}
