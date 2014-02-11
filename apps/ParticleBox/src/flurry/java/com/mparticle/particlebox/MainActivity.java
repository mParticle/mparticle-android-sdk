package com.mparticle.particlebox;

import com.flurry.android.FlurryAgent;
import com.mparticle.MParticle;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onStart() {
        super.onStart();
        MParticle.getInstance().disableUncaughtExceptionLogging();
        FlurryAgent.setUseHttps(false);
        FlurryAgent.onStartSession(this, "P6CPJY6QRSSNKMM5TPWR");

        FlurryAgent.onPageView();

    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }
}
