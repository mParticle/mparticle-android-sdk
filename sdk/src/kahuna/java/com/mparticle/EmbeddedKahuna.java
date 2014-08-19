package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import com.kahuna.sdk.KahunaAnalytics;

import org.json.JSONObject;

/**
 * <p/>
 * Embedded implementation of the Kochava SDK
 * <p/>
 */
class EmbeddedKahuna extends EmbeddedProvider implements MPActivityCallbacks {

    boolean initialized = false;
    private static final String KEY_TRANSACTION_DATA = "sendTransactionData";
    private static final String KEY_SECRET_KEY = "secretKey";
    private static final String KEY_INCLUDED_EVENTS = "eventList";
    private String eventFilter = "";
    public EmbeddedKahuna(Context context) throws ClassNotFoundException {
        super(context);
    }

    @Override
    protected EmbeddedProvider update() {
        if (!initialized) {
            KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), MParticle.getInstance().mConfigManager.getPushSenderId());
            initialized = true;
        }
        return this;
    }

    @Override
    public String getName() {
        return "Kahuna";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.contains("tap-nexus.appspot.com");
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) throws Exception {
        if (TextUtils.isEmpty(name) && !eventFilter.contains(name)) {
            KahunaAnalytics.trackEvent(name);
        }
    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {

    }

    @Override
    public void logScreen(String screenName, JSONObject eventAttributes) throws Exception {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {

    }
}