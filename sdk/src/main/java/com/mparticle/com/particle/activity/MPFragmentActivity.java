package com.mparticle.com.particle.activity;

import android.os.Build;
import android.support.v4.app.FragmentActivity;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/16/14.
 */
public class MPFragmentActivity extends FragmentActivity {
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
