package com.mparticle;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class MParticleAPI {

    private static final String TAG = "mParticleAPI";
    private static boolean optOutFlag = false;
    public static boolean debugMode = true; // TODO: this will default to false

    private Context mContext;

    private MParticleAPI(Context context, String api_key, String secret) {
        this.mContext = context;
    }

    public static MParticleAPI getInstance(Context context) {
        return new MParticleAPI(context, null, null);
    }

    public static MParticleAPI getInstance(Context context, String api_key, String secret) {
        return new MParticleAPI(context, api_key, secret);
    }

    public static MParticleAPI getInstance(Context context, String api_key, String secret,
            int uploadInterval) {
        return new MParticleAPI(context, api_key, secret);
    }

    public void startSession() {
        this.debugLog("Start Session");
    }

    public void endSession() {
        this.debugLog("End Session");
    }

    public void upload() {
        this.debugLog("Upload");
    }

    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    public void logEvent(String eventName, Map<String, String> eventData) {
        this.debugLog("Logged event: " + eventName + " with data " + eventData);
    }

    public void logScreenView(String screenName) {
        logScreenView(screenName, null);
    }

    public void logScreenView(String screenName, Map<String, String> eventData) {
        this.debugLog("Logged screen: " + screenName + " with data " + eventData);
    }

    public void logErrorEvent(String eventName) {
        logErrorEvent(eventName, null);
    }

    public void logErrorEvent(String eventName, Map<String, String> data) {
        this.debugLog("Logged error: " + eventName);
    }

    public void logErrorEvent(String eventName, Map<String, String> data, Exception e) {
    }

    public void identifyUser(String userId) {
        identifyUser("user_id", userId);
    }

    public void identifyUser(String key, String userId) {
        this.debugLog("Identified user: " + userId);
    }

    public void setLocation(double longitude, double latitude) {
        this.debugLog("Set Location: " + longitude + " " + latitude);
    }

    public void setSessionProperty(String key, String value) {
        this.debugLog("Set Session: " + key + "=" + value);
    }

    public void setSessionProperties(Map<String, String> data) {
    }

    public void setUserProperty(String key, String value) {
        this.debugLog("Set User: " + key + "=" + value);
    }

    public void setUserProperties(Map<String, String> data) {
    }

    public String getUserSegment() {
        return "default";
    }

    public void setUserAge(int age) {
    }

    public void setUserGender(String gender) {
    }

    public void setOptOut(boolean optOutFlag) {
        MParticleAPI.optOutFlag = optOutFlag;
        this.debugLog("Set Opt Out: " + MParticleAPI.optOutFlag);
    }

    public boolean getOptOut() {
        return MParticleAPI.optOutFlag;
    }

    public void setDebug(boolean debugMode) {
        MParticleAPI.debugMode = debugMode;
        this.debugLog("Set Debug Mode: " + MParticleAPI.debugMode);
    }

    public void handleExceptions() {
    }

    public void setReferralURL(URL url) {
    }

    public void setPushRegistrationId(String token) {
        this.debugLog("Set Push Token: " + token);
    }

    public void clearPushRegistrationId() {
        this.debugLog("Clear Push Token");
    }

    public void registerEventCollector(EventCollectorInterface collector, int timeInterval) {
    }

    public interface EventCollectorInterface {
        Map<String, String> provideEventData();
    }

    public Map<String, Object> collectDeviceProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("ANDROID_ID", Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID));

        properties.put("manufacturer", android.os.Build.MANUFACTURER);
        properties.put("device", android.os.Build.DEVICE);
        properties.put("display", android.os.Build.DISPLAY);
        properties.put("hardware", android.os.Build.HARDWARE);
        properties.put("id", android.os.Build.ID);
        properties.put("model", android.os.Build.MODEL);
        properties.put("product", android.os.Build.PRODUCT);

        return properties;
    }

    private void debugLog(String message) {
        if (MParticleAPI.debugMode) {
            Log.d(TAG, message);
            // temporarily show Toast messages in debug mode.
            // this will be removed.
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }

}
