package com.mparticle.activity;

import android.app.ListActivity;
import android.os.Build;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 1/16/14.
 */
public class MPListActivity extends ListActivity {
    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            MParticle.getInstance().activityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            MParticle.getInstance().activityStopped(this);
        }
    }
}
