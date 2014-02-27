package com.mparticle.particlebox;

import com.google.analytics.tracking.android.EasyTracker;
/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {


    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }
}
