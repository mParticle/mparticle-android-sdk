package com.mparticle.particlebox;

import android.os.Bundle;

import com.bugsense.trace.BugSenseHandler;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        BugSenseHandler.initAndStartSession(this, "f5f6e24c");

    }

}
