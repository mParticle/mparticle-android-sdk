package com.mparticle.particlebox;

import android.os.Bundle;


/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
       // GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
      // GoogleAnalytics.getInstance(this).reportActivityStop(this);   // Add this method.
    }
}
