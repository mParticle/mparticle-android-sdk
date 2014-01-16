package com.mparticle.com.particle.activity;

import android.app.ListActivity;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/16/14.
 */
public class MPListActivity extends ListActivity  {
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
