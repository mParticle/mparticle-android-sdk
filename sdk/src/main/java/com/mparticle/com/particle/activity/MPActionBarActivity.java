package com.mparticle.com.particle.activity;

import android.os.Build;
import android.support.v7.app.ActionBarActivity;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/16/14.
 */
public class MPActionBarActivity extends ActionBarActivity {
    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            MParticle.getInstance(this).activityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            MParticle.getInstance(this).activityStopped(this);
        }
    }
}
