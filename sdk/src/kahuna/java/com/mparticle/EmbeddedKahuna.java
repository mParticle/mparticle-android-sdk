package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import com.kahuna.sdk.KahunaAnalytics;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.KahunaUserCredentialKeys;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p/>
 * Embedded implementation of the Kochava SDK
 * <p/>
 */
class EmbeddedKahuna extends EmbeddedProvider implements MPActivityCallbacks {

    boolean initialized = false;
    private static final String KEY_TRANSACTION_DATA = "sendTransactionData";
    private static final String KEY_SECRET_KEY = "secretKey";
    private boolean sendTransactionData = false;

    public EmbeddedKahuna(Context context) throws ClassNotFoundException {
        super(context);
    }

    @Override
    protected EmbeddedProvider update() {
        if (!initialized) {
            KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), MParticle.getInstance().mConfigManager.getPushSenderId());
            initialized = true;
        }
        sendTransactionData = properties.containsKey(KEY_TRANSACTION_DATA) && Boolean.parseBoolean(properties.get(KEY_TRANSACTION_DATA));
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
        if (TextUtils.isEmpty(name)) {
            if (sendTransactionData && eventAttributes != null && eventAttributes.has(Constants.MessageKey.RESERVED_KEY_LTV)){
                Double amount = Double.parseDouble(eventAttributes.getString(Constants.MessageKey.RESERVED_KEY_LTV)) * 100;
                KahunaAnalytics.trackEvent("purchase", 1, amount.intValue());
            }else if (shouldSend(type, name)) {
                KahunaAnalytics.trackEvent(name);
            }
        }
    }

    @Override
    protected boolean shouldSend(MParticle.EventType type, String name) {
        return name != null && includedEvents != null && includedEvents.contains(name.toLowerCase());
    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {
        if (sendTransactionData && transaction != null){
            double revenue = transaction.getTotalRevenue() * 100;
            KahunaAnalytics.trackEvent("purchase", transaction.getQuantity(), (int)revenue);
        }
    }

    @Override
    public void logScreen(String screenName, JSONObject eventAttributes) throws Exception {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setUserAttributes(JSONObject attributes) {
        Map<String, String> kahunaAttributes = null;
        if (attributes != null){
            Iterator<String> keys = attributes.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = attributes.optString(key, "");
                if (kahunaAttributes == null){
                    kahunaAttributes = new HashMap<String, String>(attributes.length());
                }
                kahunaAttributes.put(convertMpKeyToKahuna(key), value);
            }
        }
        KahunaAnalytics.setUserAttributes(kahunaAttributes);
    }

    private static final String convertMpKeyToKahuna(String key) {
        if (key.equals(MParticle.UserAttributes.FIRSTNAME)) {
            return KahunaUserAttributesKeys.FIRST_NAME_KEY;
        } else if (key.equals(MParticle.UserAttributes.LASTNAME)) {
            return KahunaUserAttributesKeys.LAST_NAME_KEY;
        } else if (key.equals(MParticle.UserAttributes.GENDER)) {
            return KahunaUserAttributesKeys.GENDER_KEY;
        } else {
            return key;
        }
    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        String kahunaKey;
        switch (identityType){
            case Facebook:
                kahunaKey = KahunaUserCredentialKeys.FACEBOOK_KEY;
                break;
            case Email:
                kahunaKey = KahunaUserCredentialKeys.EMAIL_KEY;
                break;
            case Other:
            case CustomerId:
                kahunaKey = KahunaUserCredentialKeys.USERNAME_KEY;
                break;
            case Twitter:
                kahunaKey = KahunaUserCredentialKeys.TWITTER_KEY;
                break;
            default:
                return;

        }
        KahunaAnalytics.setUserCredential(kahunaKey, id);
    }

    @Override
    public void removeUserIdentity(String id) {
        Map<String, String> credentials = KahunaAnalytics.getUserCredentials();
        if (credentials != null) {
            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(id)){
                    KahunaAnalytics.removeUserCredential(entry.getValue());
                }
            }
        }
    }

    @Override
    public void logout() {
        KahunaAnalytics.logout();
    }



    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        update();
    }

    @Override
    public void onActivityResumed(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.stop();
    }

    @Override
    public void onActivityStarted(Activity activity, int activityCount) {
        update();
        KahunaAnalytics.start();
    }
}