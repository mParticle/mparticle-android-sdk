package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.kochava.android.tracker.Feature;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdozor on 7/24/14.
 */
class EmbeddedKochava extends EmbeddedProvider implements MPActivityCallbacks {
    private static final String APP_ID = "appId";

    //If set to true, we will pass customer id from user identities (if it exists) to KOCHAVA as user_id
    private static final String USE_CUSTOMER_ID = "useCustomerId";

    private static final String INCLUDE_ALL_IDS = "passAllOtherIdentities";

    private static final String LIMIT_ADD_TRACKING = "limitAdTracking";

    private static final String RETRIEVE_ATT_DATA = "retrieveAttributionData";
    private static final String ENABLE_LOGGING = "enableLogging";
    private static final String CURRENCY = "currency";
    private static final String SPACIAL_X = "SpacialX";
    private static final String SPACIAL_Y = "SpacialY";
    private static final String SPACIAL_Z = "SpacialZ";
    private String appId;
    private String currency;
    private Feature feature;

    EmbeddedKochava(Context context) throws ClassNotFoundException {
        super(context);
        try {
            Class.forName("com.kochava.android.tracker.Feature");
        } catch (ClassNotFoundException cnfe) {
            MParticle.getInstance().mConfigManager.debugLog("Failed in initiate Kochava - library not found. Have you added it to your application's classpath?");
            throw cnfe;
        }
    }

    @Override
    protected EmbeddedProvider update() {
        if (needsRestart()){
            appId = properties.get(APP_ID);
            currency = properties.get(CURRENCY);
            if (currency != null) {
                feature = new Feature(context, appId, currency);
            }else{
                feature = new Feature(context, appId, "USD");
            }
        }
        feature.setAppLimitTracking(Boolean.parseBoolean(properties.get(LIMIT_ADD_TRACKING)));
        Feature.setErrorDebug(Boolean.parseBoolean(properties.get(ENABLE_LOGGING)));
        Feature.setRequestAttributionData(Boolean.parseBoolean(properties.get(RETRIEVE_ATT_DATA)));

        return this;
    }

    private boolean needsRestart() {
        return feature == null ||
                appId != properties.get(APP_ID) ||
                currency != properties.get(CURRENCY) ;
    }

    @Override
    public String getName() {
        return "Kochava";
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) throws Exception {
        if (feature != null){
            if (eventAttributes != null &&
                    (eventAttributes.has(SPACIAL_X) || eventAttributes.has(SPACIAL_Y) || eventAttributes.has(SPACIAL_Z))) {
                feature.eventSpatial(name,
                        eventAttributes.optDouble(SPACIAL_X, 0),
                        eventAttributes.optDouble(SPACIAL_Y, 0),
                        eventAttributes.optDouble(SPACIAL_Z, 0),
                        null);
            }else{
                feature.event(name, null);
            }
        }
    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {
        if (feature != null) {
            String kochavaName = "eCommerce";
            feature.event(kochavaName, Double.toString(transaction.getTotalRevenue()));
        }
    }

    @Override
    public void logScreen(String screenName, JSONObject eventAttributes) throws Exception {
        String kochavaName = "Viewed " + (screenName == null ? "" : screenName);
        logEvent(null, kochavaName, eventAttributes);
    }

    @Override
    public void setLocation(Location location) {
        if (feature != null && location != null){
            feature.setLatlong(
                    Double.toString(location.getLatitude()),
                    Double.toString(location.getLongitude()));
        }
    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (identityType == MParticle.IdentityType.CustomerId) {
            if (!properties.containsKey(USE_CUSTOMER_ID) ||
                    Boolean.parseBoolean(properties.get(USE_CUSTOMER_ID))) {
                Map<String, String> map = new HashMap<String, String>(1);
                map.put(identityType.name(), id);
                feature.linkIdentity(map);
            }
        } else if (Boolean.parseBoolean(properties.get(INCLUDE_ALL_IDS))) {
            Map<String, String> map = new HashMap<String, String>(1);
            map.put(identityType.name(), id);
            feature.linkIdentity(map);
        }
    }

    @Override
    public void onActivityCreated(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }
}
