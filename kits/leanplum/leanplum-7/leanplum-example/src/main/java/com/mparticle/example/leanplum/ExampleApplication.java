package com.mparticle.example.leanplum;

import android.app.Application;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

/**
 * Leanplum Kit Example Application
 */
public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //The Leanplum kit uses Firebase by default
        //invoke this API *before* starting the mParticle SDK
        //if you'd still like to use GCM
        //LeanplumKit.disableFirebase = true;

        MParticle.start(
                MParticleOptions.builder(this)
                        .credentials("REPLACE ME", "REPLACE ME")
                        .build()
        );
    }
}
