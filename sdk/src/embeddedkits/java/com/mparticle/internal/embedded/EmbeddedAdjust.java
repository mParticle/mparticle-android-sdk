package com.mparticle.internal.embedded;

import android.app.Activity;

import com.mparticle.MParticle;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.embedded.adjust.sdk.Adjust;
import com.mparticle.internal.embedded.adjust.sdk.AdjustConfig;
import com.mparticle.internal.embedded.adjust.sdk.LogLevel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p/>
 * Embedded implementation of the Adjust SDK 4.0.6
 * <p/>
 */
class EmbeddedAdjust extends EmbeddedProvider implements MPActivityCallbacks {

    private static final String APP_TOKEN = "appToken";
    private static final String HOST = "app.adjust.io";
    boolean initialized = false;
    private AtomicBoolean hasResumed = new AtomicBoolean(false);
    //check once per run to make sure we've set the referrer.
    private boolean referrerSet = false;

    EmbeddedAdjust(int id, EmbeddedKitManager ekManager) {
        super(id, ekManager);
        setRunning(false);
    }

    private void initAdjust(){
        if (!initialized) {
            boolean production = MParticle.Environment.Production.equals(MParticle.getInstance().getEnvironment());

            AdjustConfig config = new AdjustConfig(context,
                    properties.get(APP_TOKEN),
                    production ? AdjustConfig.ENVIRONMENT_PRODUCTION : AdjustConfig.ENVIRONMENT_SANDBOX);
            if (!production){
                config.setLogLevel(LogLevel.VERBOSE);
            }
            config.setEventBufferingEnabled(false);
            Adjust.onCreate(config);
            setRunning(true);
            if (!mEkManager.getAppStateManager().isBackgrounded()) {
                if (!hasResumed.get()) {
                    Adjust.onResume();
                    hasResumed.set(true);
                }
            }
            initialized = true;
        }
    }

    @Override
    protected EmbeddedProvider update() {
        initAdjust();

        if (!referrerSet) {
            String installReferrer = MParticle.getInstance().getInstallReferrer();
            if (installReferrer != null) {
                referrerSet = true;
                MParticle.getInstance().setInstallReferrer(installReferrer);
            }
        }
        return this;
    }

    @Override
    public String getName() {
        return "Adjust";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(HOST);
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {
        if (!hasResumed.get()) {
            Adjust.onResume();
            hasResumed.set(true);
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        Adjust.onPause();
        hasResumed.set(false);
    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {

    }



    @Override
    public void onActivityStarted(Activity activity, int activityCount) {

    }
}
