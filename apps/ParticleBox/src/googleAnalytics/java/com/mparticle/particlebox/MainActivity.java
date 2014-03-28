package com.mparticle.particlebox;

import com.google.android.gms.analytics.GoogleAnalytics;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {


    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);   // Add this method.
    }
}
