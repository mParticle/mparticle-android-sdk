package com.mparticle.internal.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.MPProduct;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by sdozor on 3/13/14.
 * <p/>
 * Embedded implementation of the MAT SDK, tested against MAT v. 3.1.
 * <p/>
 * Detailed behavior defined here:
 * https://mparticle.atlassian.net/wiki/pages/viewpage.action?spaceKey=DMP&title=MobileAppTracking
 */
class EmbeddedMAT extends EmbeddedProvider implements MPActivityCallbacks {
    //Server config constants defined for this provider
    //keys to provide access to the MAT account.
    private static final String ADVERTISER_ID = "advertiserId";
    private static final String CONVERSION_ID = "conversionKey";

    //If set to true, we will pass customer id from user identities (if it exists) to MAT as user_id
    private static final String USE_CUSTOMER_ID = "useCustomerId";

    //if true, we'll forward facebook/google/twitter Ids (if available) to MAT
    private static final String INCLUDE_ALL_IDS = "includeAllOtherUserIdentities";

    //if true, we forward available user data (e.g., name, email, age, etc.) to MAT
    private static final String INCLUDE_USER_DATA = "includeUserData";

    //If true, we'll turn on MAT's "viewing server response" feature. Useful for debugging
    private static final String VIEW_SERVER_RESPONSE = "viewServerResponse";
    private String conversionId;
    private String advertiserId;
    private static final String HOST = "engine.mobileapptracking.com";

    EmbeddedMAT(Context context) throws ClassNotFoundException {
        super(context);
        try {
            Class.forName("com.mobileapptracker.MobileAppTracker");
        } catch (ClassNotFoundException cnfe) {
            ConfigManager.log(MParticle.LogLevel.ERROR, "Failed in initiate MAT - library not found. Have you added it to your application's classpath?");
            throw cnfe;
        }
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, Map<String, String> eventAttributes) {
        com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
        if (eventAttributes != null) {
            if (eventAttributes.size() > 0) {
                JSONObject lowercaseAttributes = new JSONObject();
                Iterator<String> keys = eventAttributes.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        lowercaseAttributes.put(key.toLowerCase(), eventAttributes.get(key));
                    } catch (JSONException jse) {

                    }
                }
                com.mobileapptracker.MATEventItem item = new com.mobileapptracker.MATEventItem(name,
                        lowercaseAttributes.optString("mat_eventattribute1"),
                        lowercaseAttributes.optString("mat_eventattribute2"),
                        lowercaseAttributes.optString("checkintime"),
                        lowercaseAttributes.optString("checkouttime"),
                        lowercaseAttributes.optString("guestcount")
                );
                instance.measureAction(name, item);
            }
        } else {
            instance.measureAction(name);
        }
    }

    @Override
    public void logTransaction(MPProduct transaction) {
        com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
        com.mobileapptracker.MATEventItem item = new com.mobileapptracker.MATEventItem(
                transaction.get(MPProduct.NAME,""),
                (int)transaction.getQuantity(),
                Double.parseDouble(transaction.get(MPProduct.UNITPRICE, "0")),
                Double.parseDouble(transaction.get(MPProduct.REVENUE, "0")),
                transaction.get(MPProduct.TAX, "0"),
                transaction.get(MPProduct.SHIPPING, "0"),
                transaction.get(MPProduct.SKU, ""),
                transaction.get(MPProduct.AFFILIATION, ""),
                transaction.get(MPProduct.CATEGORY, "")
        );
        instance.measureAction("purchase",
                item,
                Double.parseDouble(transaction.get(MPProduct.REVENUE, "0")),
                transaction.get(MPProduct.CURRENCY, "USD"),
                transaction.get(MPProduct.TRANSACTION_ID, UUID.randomUUID().toString())
        );
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception {
        if (screenName == null){
            screenName = "Viewed";
        }else{
            screenName = "Viewed " + screenName;
        }
        logEvent(MParticle.EventType.Navigation, screenName, eventAttributes);
    }

    @Override
    public void setLocation(Location location) {
        if (location != null) {
            com.mobileapptracker.MobileAppTracker.getInstance().setLatitude(location.getLatitude());
            com.mobileapptracker.MobileAppTracker.getInstance().setLongitude(location.getLongitude());
            com.mobileapptracker.MobileAppTracker.getInstance().setAltitude(location.getAltitude());
        }
    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {
        if (mUserAttributes != null &&
                (!properties.containsKey(INCLUDE_USER_DATA) || Boolean.parseBoolean(properties.get(INCLUDE_USER_DATA)))) {
            com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
            Iterator<String> keys = mUserAttributes.keys();
            String firstName = "";
            String lastName = "";
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (MParticle.UserAttributes.AGE.equalsIgnoreCase(key)) {
                        instance.setAge(Integer.parseInt(mUserAttributes.getString(key)));
                    } else if (MParticle.UserAttributes.GENDER.equalsIgnoreCase(key)) {
                        instance.setGender(Integer.parseInt(mUserAttributes.getString(key)));
                    } else if (MParticle.UserAttributes.FIRSTNAME.equalsIgnoreCase(key)) {
                        firstName = mUserAttributes.optString(key);
                    } else if (MParticle.UserAttributes.LASTNAME.equalsIgnoreCase(key)) {
                        lastName = mUserAttributes.optString(key);
                    }
                } catch (JSONException jse) {

                } catch (NumberFormatException nfe) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, getName() + " requires user attribute: " + key + " to be parsable as an integer");
                }
            }
            if (firstName.length() > 0 || lastName.length() > 0) {
                String fullName = firstName + " " + lastName;
                instance.setUserName(fullName);
            }
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        //MAT doesn't really support this...all of their attributes are primitives/non-nulls.
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
        if (identityType == MParticle.IdentityType.CustomerId) {
            if (!properties.containsKey(USE_CUSTOMER_ID) ||
                    Boolean.parseBoolean(properties.get(USE_CUSTOMER_ID))) {
                instance.setUserId(id);
            }
        } else if (Boolean.parseBoolean(properties.get(INCLUDE_ALL_IDS))) {
            switch (identityType) {
                case Facebook:
                    instance.setFacebookUserId(id);
                    break;
                case Google:
                    instance.setGoogleUserId(id);
                    break;
                case Twitter:
                    instance.setTwitterUserId(id);
                    break;
            }
        }
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

    @Override
    protected EmbeddedProvider update() {
        if (needsRestart()) {
            advertiserId = properties.get(ADVERTISER_ID);
            conversionId = properties.get(CONVERSION_ID);
            com.mobileapptracker.MobileAppTracker.init(
                    context,
                    advertiserId,
                    conversionId
            );
            boolean existingUser = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
                    .getBoolean(Constants.PrefKeys.MAT_EXISTING_USER, false);
            com.mobileapptracker.MobileAppTracker.getInstance().setExistingUser(existingUser);
            context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(Constants.PrefKeys.MAT_EXISTING_USER, true)
                    .commit();
            com.mobileapptracker.MobileAppTracker.getInstance().measureSession();
        }
        if (Boolean.parseBoolean(properties.get(VIEW_SERVER_RESPONSE))) {
            com.mobileapptracker.MobileAppTracker.getInstance().setMATResponse(new com.mobileapptracker.MATResponse() {
                @Override
                public void enqueuedActionWithRefId(String id) {
                    Log.d(Constants.LOG_TAG, getName() + " enqueued action with ref id: " + id);
                }

                @Override
                public void didSucceedWithData(JSONObject data) {
                    Log.d(Constants.LOG_TAG, getName() + " succeeded with data:\n" + data.toString());
                }

                @Override
                public void didFailWithError(JSONObject data) {
                    Log.d(Constants.LOG_TAG, getName() + " failed with data:\n" + data.toString());
                }
            });
        }

        com.mobileapptracker.MobileAppTracker.getInstance().setDebugMode(MParticle.getInstance().mConfigManager.isDebugEnvironment());
        SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String referrer = preferences.getString(Constants.PrefKeys.INSTALL_REFERRER, "");
        if (referrer.length() > 0) {
            com.mobileapptracker.MobileAppTracker.getInstance().setInstallReferrer(referrer);
        }

        return this;
    }

    @Override
    public String getName() {
        return "Mobile App Tracking";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(HOST);
    }

    private boolean needsRestart() {
        return !properties.get(ADVERTISER_ID).equals(advertiserId) || !properties.get(CONVERSION_ID).equals(conversionId);
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        if (Intent.ACTION_MAIN.equals(activity.getIntent().getAction())) {
            com.mobileapptracker.MobileAppTracker.getInstance().setReferralSources(activity);
            if (MPUtility.isGooglePlayServicesAvailable()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            com.google.android.gms.ads.identifier.AdvertisingIdClient.Info adInfo = com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context.getApplicationContext());
                            com.mobileapptracker.MobileAppTracker.getInstance().setGoogleAdvertisingId(adInfo.getId(), adInfo.isLimitAdTrackingEnabled());
                        } catch (Exception e) {
                            // Unrecoverable error connecting to Google Play services (e.g.,
                            // the old version of the service doesn't support getting AdvertisingId).
                            ConfigManager.log(MParticle.LogLevel.DEBUG, getName(),  " failed to retrieve Google Advertising id: ", e.getMessage());
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {
        if (Intent.ACTION_MAIN.equals(activity.getIntent().getAction())) {
            com.mobileapptracker.MobileAppTracker.getInstance().measureSession();
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {

    }

    @Override
    public void onActivityStopped(Activity activity, int currentCount) {

    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {

    }
}
