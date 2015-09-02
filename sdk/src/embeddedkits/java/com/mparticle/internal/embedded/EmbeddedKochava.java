package com.mparticle.internal.embedded;

import android.app.Activity;
import android.location.Location;

import com.kochava.android.tracker.Feature;
import com.mparticle.MParticle;
import com.mparticle.internal.MPActivityCallbacks;

import java.util.HashMap;
import java.util.Map;

class EmbeddedKochava extends EmbeddedProvider implements MPActivityCallbacks {
    private static final String APP_ID = "appId";
    private static final String USE_CUSTOMER_ID = "useCustomerId";
    private static final String INCLUDE_ALL_IDS = "passAllOtherIdentities";
    private static final String LIMIT_ADD_TRACKING = "limitAdTracking";
    private static final String RETRIEVE_ATT_DATA = "retrieveAttributionData";
    private static final String ENABLE_LOGGING = "enableLogging";
    private static final String CURRENCY = "currency";
    private static final String HOST = "kochava.com";
    private Feature feature;

    EmbeddedKochava(int moduleId, EmbeddedKitManager context) {
        super(moduleId, context);
    }

    @Override
    protected EmbeddedProvider update() {
        if (feature != null) {
            feature.setAppLimitTracking(Boolean.parseBoolean(properties.get(LIMIT_ADD_TRACKING)));
            Feature.setErrorDebug(Boolean.parseBoolean(properties.get(ENABLE_LOGGING)));
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
    public void setLocation(Location location) {
        if (feature != null && location != null) {
            feature.setLatlong(
                    Double.toString(location.getLatitude()),
                    Double.toString(location.getLongitude()));
        }
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
            } else {
                if (Boolean.parseBoolean(properties.get(INCLUDE_ALL_IDS))) {
                    Map<String, String> map = new HashMap<String, String>(1);
                    map.put(identityType.name(), id);
                    feature.linkIdentity(map);
                }
            }
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

    private void createKochava(Activity activity) {
        if (feature == null) {
            HashMap<String, Object> datamap = new HashMap<String, Object>();
            datamap.put(Feature.INPUTITEMS.KOCHAVA_APP_ID, properties.get(APP_ID));
            if (properties.containsKey(CURRENCY)) {
                datamap.put(Feature.INPUTITEMS.CURRENCY, properties.get(CURRENCY));
            } else {
                datamap.put(Feature.INPUTITEMS.CURRENCY, Feature.CURRENCIES.USD);
            }
            datamap.put(Feature.INPUTITEMS.APP_LIMIT_TRACKING, properties.get(LIMIT_ADD_TRACKING));
            datamap.put(Feature.INPUTITEMS.DEBUG_ON, Boolean.parseBoolean(properties.get(ENABLE_LOGGING)));
            datamap.put(Feature.INPUTITEMS.REQUEST_ATTRIBUTION, Boolean.parseBoolean(properties.get(RETRIEVE_ATT_DATA)));
            feature = new Feature(activity, datamap);
        }
    }
}
