package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.kochava.android.tracker.Feature;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.MPProduct;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
    private static final String HOST = "kochava.com";
    private Feature feature;

    EmbeddedKochava(Context context) throws ClassNotFoundException {
        super(context);
        try {
            Class.forName("com.kochava.android.tracker.Feature");
        } catch (ClassNotFoundException cnfe) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Failed in initiate Kochava - library not found. Have you added it to your application's classpath?");
            throw cnfe;
        }
    }

    @Override
    protected EmbeddedProvider update() {
        if (feature != null){
            feature.setAppLimitTracking(Boolean.parseBoolean(properties.get(LIMIT_ADD_TRACKING)));
            Feature.setErrorDebug(Boolean.parseBoolean(properties.get(ENABLE_LOGGING)));
            Feature.setRequestAttributionData(Boolean.parseBoolean(properties.get(RETRIEVE_ATT_DATA)));
        }

        return this;
    }

    @Override
    public String getName() {
        return "Kochava";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(HOST);
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) throws Exception {
        if (feature != null){
            if (eventAttributes != null &&
                    (eventAttributes.containsKey(SPACIAL_X) || eventAttributes.containsKey(SPACIAL_Y) || eventAttributes.containsKey(SPACIAL_Z))) {

                double spacialX = 0;
                double spacialY = 0;
                double spacialZ = 0;
                if (eventAttributes.containsKey(SPACIAL_X)){
                    try{
                        spacialX = Double.parseDouble(eventAttributes.get(SPACIAL_X));
                    }catch (NumberFormatException nfe){

                    }
                }
                if (eventAttributes.containsKey(SPACIAL_Y)){
                    try{
                        spacialY = Double.parseDouble(eventAttributes.get(SPACIAL_Y));
                    }catch (NumberFormatException nfe){

                    }
                }
                if (eventAttributes.containsKey(SPACIAL_Z)){
                    try{
                        spacialZ = Double.parseDouble(eventAttributes.get(SPACIAL_Z));
                    }catch (NumberFormatException nfe){

                    }
                }
                feature.eventSpatial(name,
                        spacialX,
                        spacialY,
                        spacialZ,
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
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
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
        if (feature != null) {
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
    }

    @Override
    public void logout() {
        //no matching feature in Kochava
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

    private void createKochava(Activity activity){
        if (feature == null){
            HashMap<String, Object> datamap = new HashMap<String, Object>();
            datamap.put(Feature.INPUTITEMS.KOCHAVA_APP_ID , properties.get(APP_ID));
            datamap.put(Feature.INPUTITEMS.CURRENCY , Feature.CURRENCIES.EUR);
            if (properties.containsKey(CURRENCY)) {
                datamap.put(Feature.INPUTITEMS.CURRENCY , properties.get(CURRENCY));
            }else{
                datamap.put(Feature.INPUTITEMS.CURRENCY , Feature.CURRENCIES.USD);
            }
            datamap.put(Feature.INPUTITEMS.APP_LIMIT_TRACKING , properties.get(LIMIT_ADD_TRACKING));
            datamap.put(Feature.INPUTITEMS.DEBUG_ON , Boolean.parseBoolean(properties.get(ENABLE_LOGGING)));
            datamap.put(Feature.INPUTITEMS.REQUEST_ATTRIBUTION , Boolean.parseBoolean(properties.get(RETRIEVE_ATT_DATA)));
            feature = new Feature(activity, datamap);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        createKochava(activity);
    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {
        createKochava(activity);
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {
        feature = null;
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        createKochava(activity);
    }
}
