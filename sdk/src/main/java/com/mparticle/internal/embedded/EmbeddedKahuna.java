package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.text.TextUtils;

import com.kahuna.sdk.KahunaAnalytics;
import com.kahuna.sdk.KahunaPushReceiver;
import com.kahuna.sdk.KahunaPushService;
import com.kahuna.sdk.KahunaUserAttributesKeys;
import com.kahuna.sdk.KahunaUserCredentialKeys;
import com.mparticle.MPProduct;
import com.mparticle.MPReceiver;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.MPUtility;

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

    static KahunaPushReceiver sReceiver;
    boolean initialized = false;
    private static final String KEY_TRANSACTION_DATA = "sendTransactionData";
    private static final String KEY_SECRET_KEY = "secretKey";
    private boolean sendTransactionData = false;

    public EmbeddedKahuna(Context context) {
        super(context);
    }

    @Override
    protected EmbeddedProvider update() {
        if (!initialized) {
            KahunaAnalytics.setDebugMode(MParticle.getInstance().internal().getConfigurationManager().isDebugEnvironment());
            if (MParticle.getInstance().internal().getConfigurationManager().isPushEnabled()) {
                // registerForPush(context);
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), MParticle.getInstance().internal().getConfigurationManager().getPushSenderId());
                KahunaAnalytics.disableKahunaGenerateNotifications();
            } else {
                KahunaAnalytics.onAppCreate(context, properties.get(KEY_SECRET_KEY), null);
            }
            KahunaAnalytics.start();

            initialized = true;
            if (!MPUtility.isServiceAvailable(context, KahunaPushService.class)) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "The Kahuna SDK is enabled for this application, but you have not added <service android:name=\"com.kahuna.sdk.KahunaPushService\" /> to the <application> section in your application's AndroidManifest.xml");
            }
        }
        sendTransactionData = properties.containsKey(KEY_TRANSACTION_DATA) && Boolean.parseBoolean(properties.get(KEY_TRANSACTION_DATA));
        return this;
    }

    public void registerForPush() {
        sReceiver = new KahunaPushReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.google.android.c2dm.intent.REGISTRATION");
        filter.addAction("com.google.android.c2dm.intent.RECEIVE");
        filter.addCategory(context.getPackageName());
        context.registerReceiver(sReceiver, filter, "com.google.android.c2dm.permission.SEND", null);
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
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) throws Exception {
        if (!TextUtils.isEmpty(name)) {
            if (sendTransactionData && eventAttributes != null && eventAttributes.containsKey(Constants.MessageKey.RESERVED_KEY_LTV)) {
                Double amount = Double.parseDouble(eventAttributes.get(Constants.MessageKey.RESERVED_KEY_LTV)) * 100;
                KahunaAnalytics.trackEvent("purchase", 1, amount.intValue());
                if (eventAttributes != null)
                    this.setUserAttributes(eventAttributes);
            } else {
                KahunaAnalytics.trackEvent(name);
                if (eventAttributes != null) {
                    this.setUserAttributes(eventAttributes);
                }
            }
        }
    }

    @Override
    public void logTransaction(MPProduct transaction) throws Exception {
        if (sendTransactionData && transaction != null){
            Double revenue = transaction.getTotalRevenue() * 100;
            KahunaAnalytics.trackEvent("purchase", (int) transaction.getQuantity(), revenue.intValue());
        }
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {

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
        if (kahunaAttributes != null) {
            KahunaAnalytics.setUserAttributes(kahunaAttributes);
        }
    }

    private void setUserAttributes(Map<String, String> attributes){
        if (attributes != null){
            HashMap<String, String> kahunaAttributes = new HashMap<String, String>(attributes.size());
            for (Map.Entry<String, String> entry : attributes.entrySet())
            {
                kahunaAttributes.put(convertMpKeyToKahuna(entry.getKey()), entry.getValue());
            }
        }
    }

    private static String convertMpKeyToKahuna(String key) {
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
    public void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
            if (EmbeddedKahuna.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
            }
        } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
            if (EmbeddedKahuna.sReceiver == null) {
                registerForPush();
                intent.putExtra(MPReceiver.MPARTICLE_IGNORE, true);
                context.sendBroadcast(intent);
            }
        }
    }

    @Override
    public void startSession() {

    }

    @Override
    public void endSession() {

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