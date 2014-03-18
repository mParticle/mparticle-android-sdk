package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created by sdozor on 3/13/14.
 *
 * Embedded implementation of the MAT SDK, tested against MAT v. 3.1.
 *
 * Detailed behavior defined here:
 * https://mparticle.atlassian.net/wiki/pages/viewpage.action?spaceKey=DMP&title=MobileAppTracking
 */
public class EmbeddedMAT extends EmbeddedProvider implements MPActivityCallbacks{

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

    public EmbeddedMAT(Context context) throws ClassNotFoundException {
        super(context);
        try {
            Class.forName("com.mobileapptracker.MobileAppTracker");
        }catch (ClassNotFoundException cnfe){
            if (MParticle.getInstance().getDebugMode()){
                Log.w(Constants.LOG_TAG, "Failed in initiate MAT - library not found. Have you added it to your application's classpath?");
            }
            throw cnfe;
        }
    }

    @Override
    public void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) {
        if (shouldSend(type, name)) {
            com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
            if (eventAttributes != null) {
                eventAttributes = filterAttributes(type, name, eventAttributes);
                if (eventAttributes.length() > 0) {
                    JSONObject lowercaseAttributes = new JSONObject();
                    Iterator<String> keys = eventAttributes.keys();
                    while (keys.hasNext()){
                        String key = keys.next();
                        try {
                            lowercaseAttributes.put(key.toLowerCase(), eventAttributes.optString(key));
                        }catch (JSONException jse){

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
            }else{
                instance.measureAction(name);
            }
        }
    }

    @Override
    public void logTransaction(MPTransaction transaction) {
        JSONObject transData = transaction.getData();
        com.mobileapptracker.MobileAppTracker instance = com.mobileapptracker.MobileAppTracker.getInstance();
        com.mobileapptracker.MATEventItem item = new com.mobileapptracker.MATEventItem(
                transData.optString(MPTransaction.NAME),
                transData.optInt(MPTransaction.QUANTITY, 1),
                transData.optDouble(MPTransaction.UNITPRICE,0),
                transData.optDouble(MPTransaction.REVENUE,0),
                transData.optString(MPTransaction.TAX),
                transData.optString(MPTransaction.SHIPPING),
                transData.optString(MPTransaction.SKU),
                transData.optString(MPTransaction.AFFILIATION),
                transData.optString(MPTransaction.CATEGORY)
                );
        instance.measureAction("purchase",
                item,
                transData.optDouble(MPTransaction.REVENUE, 0),
                transData.optString(MPTransaction.CURRENCY,"USD"),
                transData.optString(MPTransaction.TRANSACTION_ID, UUID.randomUUID().toString())
        );
    }

    @Override
    public void logScreen(String screenName, JSONObject eventAttributes) throws Exception {
        logEvent(MParticle.EventType.Navigation, screenName, eventAttributes);
    }

    @Override
    public void setLocation(Location location) {
        if (location != null){
            com.mobileapptracker.MobileAppTracker.getInstance().setLatitude(location.getLatitude());
            com.mobileapptracker.MobileAppTracker.getInstance().setLongitude(location.getLongitude());
            com.mobileapptracker.MobileAppTracker.getInstance().setAltitude(location.getAltitude());
        }
    }

    @Override
    public void setUserAttributes(JSONObject mUserAttributes) {
        if (mUserAttributes != null &&
                (!properties.containsKey(INCLUDE_USER_DATA) || Boolean.parseBoolean(properties.get(INCLUDE_USER_DATA)))){
            com.mobileapptracker.MobileAppTracker instance  = com.mobileapptracker.MobileAppTracker.getInstance();
            Iterator<String> keys = mUserAttributes.keys();
            String firstName = "";
            String lastName = "";
            while (keys.hasNext()){
                String key = keys.next();
                try {
                    if (MParticle.UserAttributes.AGE.equalsIgnoreCase(key)) {
                        instance.setAge(Integer.parseInt(mUserAttributes.getString(key)));
                    }else if (MParticle.UserAttributes.GENDER.equalsIgnoreCase(key)) {
                        instance.setGender(Integer.parseInt(mUserAttributes.getString(key)));
                    }else if (MParticle.UserAttributes.FIRSTNAME.equalsIgnoreCase(key)) {
                        firstName = mUserAttributes.optString(key);
                    }else if (MParticle.UserAttributes.LASTNAME.equalsIgnoreCase(key)) {
                        lastName = mUserAttributes.optString(key);
                    }
                }catch (JSONException jse){

                }catch (NumberFormatException nfe){
                    if (MParticle.getInstance().getDebugMode()){
                        Log.e(Constants.LOG_TAG, getName() + " requires user attribute: " + key + " to be parsable as an integer");
                    }
                }
            }
            if (firstName.length() > 0 || lastName.length() > 0){
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
        if (identityType == MParticle.IdentityType.CustomerId){
            if (!properties.containsKey(USE_CUSTOMER_ID) ||
                    Boolean.parseBoolean(properties.get(USE_CUSTOMER_ID))) {
                instance.setUserId(id);
            }
        }else if (Boolean.parseBoolean(properties.get(INCLUDE_ALL_IDS))) {
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
        }
        if (Boolean.parseBoolean(properties.get(VIEW_SERVER_RESPONSE))){
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

        com.mobileapptracker.MobileAppTracker.getInstance().setDebugMode(MParticle.getInstance().getDebugMode());
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

    private boolean needsRestart() {
        return !properties.get(ADVERTISER_ID).equals(advertiserId) || !properties.get(CONVERSION_ID).equals(conversionId);
    }

    @Override
    public void onActivityCreated(Activity activity) {
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
                            Log.w(Constants.LOG_TAG, "Failed to retrieve Google Advertising id: " + e.getMessage());
                        }
                    }
                }).start();
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (Intent.ACTION_MAIN.equals(activity.getIntent().getAction())) {
            com.mobileapptracker.MobileAppTracker.getInstance().measureSession();
        }
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
