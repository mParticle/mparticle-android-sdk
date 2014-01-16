package com.mparticle.com.particle.activity;

import android.support.v4.app.FragmentActivity;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/16/14.
 */
public class MPFragmentActivity extends FragmentActivity {
    @Override
    protected void onStart() {
        super.onStart();
        MParticle.getInstance(this).activityStarted(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MParticle.getInstance(this).activityStopped(this);
    }
}
