package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.mobileapptracker.MobileAppTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * <p/>
 * Embedded implementation of the Adjust SDK 4.x
 * <p/>
 */
class EmbeddedAdjust extends EmbeddedProvider implements MPActivityCallbacks {

    private static final String APP_TOKEN = "appToken";
    private static final String HOST = "app.adjust.com";

    boolean initialized = false;

    EmbeddedAdjust(Context context) throws ClassNotFoundException {
        super(context);
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) {

    }

    @Override
    public void logTransaction(MPProduct transaction) {

    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {

    }

    @Override
    public void removeUserAttribute(String key) {
        //MAT doesn't really support this...all of their attributes are primitives/non-nulls.
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }

    @Override
    public void logout() {
        //no matching MAT feature
    }

    @Override
    public void removeUserIdentity(String id) {

    }

    @Override
    public void handleIntent(Intent intent) {

    }

    @Override
    public void startSession() {

    }

    @Override
    public void endSession() {

    }

    private void initAdjust(){
        if (!initialized) {
            AdjustConfig config = AdjustConfig.getInstance(context,
                    properties.get(APP_TOKEN),
                    MParticle.getInstance().getEnvironment() == MParticle.Environment.Production ? AdjustConfig.PRODUCTION_ENVIRONMENT : AdjustConfig.SANDBOX_ENVIRONMENT);

            Adjust.getInstance().onCreate(config);
            initialized = true;
        }
    }

    @Override
    protected EmbeddedProvider update() {
        if (initialized) {
            String installReferrer = MParticle.getInstance().getInstallReferrer();
            if (installReferrer != null) {
                MParticle.getInstance().setInstallReferrer(installReferrer);
            }
            boolean optOut = MParticle.getInstance().getOptOut();
            if (optOut != Adjust.getInstance().isEnabled()){
                Adjust.getInstance().setEnabled(!optOut);
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
        initAdjust();
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {
        initAdjust();
        Adjust.getInstance().onResume();
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        initAdjust();
        Adjust.getInstance().onPause();
    }

    @Override
    public void onActivityStopped(Activity activity, int currentCount) {

    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {

    }
}
