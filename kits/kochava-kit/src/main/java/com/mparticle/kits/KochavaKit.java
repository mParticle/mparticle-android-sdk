package com.mparticle.kits;

import android.content.Context;
import android.location.Location;

import com.kochava.android.tracker.Feature;
import com.mparticle.MParticle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KochavaKit extends KitIntegration implements KitIntegration.AttributeListener {

    private static final String APP_ID = "appId";
    private static final String USE_CUSTOMER_ID = "useCustomerId";
    private static final String INCLUDE_ALL_IDS = "passAllOtherIdentities";
    private static final String LIMIT_ADD_TRACKING = "limitAdTracking";
    private static final String RETRIEVE_ATT_DATA = "retrieveAttributionData";
    private static final String ENABLE_LOGGING = "enableLogging";
    private static final String CURRENCY = "currency";
    private Feature feature;

    @Override
    public Object getInstance() {
        return feature;
    }

    @Override
    public String getName() {
        return "Kochava";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        createKochava();
        if (feature != null) {
            feature.setAppLimitTracking(Boolean.parseBoolean(getSettings().get(LIMIT_ADD_TRACKING)));
            Feature.setErrorDebug(Boolean.parseBoolean(getSettings().get(ENABLE_LOGGING)));
        }
        return null;
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
    public void setUserAttribute(String attributeKey, String attributeValue) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
        if (feature != null) {
            if (identityType == MParticle.IdentityType.CustomerId) {
                if (!getSettings().containsKey(USE_CUSTOMER_ID) ||
                        Boolean.parseBoolean(getSettings().get(USE_CUSTOMER_ID))) {
                    Map<String, String> map = new HashMap<String, String>(1);
                    map.put(identityType.name(), id);
                    feature.linkIdentity(map);
                }
            } else {
                if (Boolean.parseBoolean(getSettings().get(INCLUDE_ALL_IDS))) {
                    Map<String, String> map = new HashMap<String, String>(1);
                    map.put(identityType.name(), id);
                    feature.linkIdentity(map);
                }
            }
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {

    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    private void createKochava() {
        if (feature == null) {
            HashMap<String, Object> datamap = new HashMap<String, Object>();
            datamap.put(Feature.INPUTITEMS.KOCHAVA_APP_ID, getSettings().get(APP_ID));
            if (getSettings().containsKey(CURRENCY)) {
                datamap.put(Feature.INPUTITEMS.CURRENCY, getSettings().get(CURRENCY));
            } else {
                datamap.put(Feature.INPUTITEMS.CURRENCY, "USD");
            }
            datamap.put(Feature.INPUTITEMS.APP_LIMIT_TRACKING, getSettings().get(LIMIT_ADD_TRACKING));
            datamap.put(Feature.INPUTITEMS.DEBUG_ON, Boolean.parseBoolean(getSettings().get(ENABLE_LOGGING)));
            datamap.put(Feature.INPUTITEMS.REQUEST_ATTRIBUTION, Boolean.parseBoolean(getSettings().get(RETRIEVE_ATT_DATA)));
            feature = new Feature(getContext(), datamap);

        }
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        if (feature != null) {
            feature.setAppLimitTracking(optOutStatus);
            List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
            messageList.add(
                    new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                            .setOptOut(optOutStatus)
            );
            return messageList;
        }else {
            return null;
        }
    }
}
