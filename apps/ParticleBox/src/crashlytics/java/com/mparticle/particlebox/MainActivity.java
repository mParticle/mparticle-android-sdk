package com.mparticle.particlebox;

import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.mparticle.MParticle;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onStart() {
        super.onStart();
        MParticle.getInstance().disableUncaughtExceptionLogging();
 }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
    }
}
