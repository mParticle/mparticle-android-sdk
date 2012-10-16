package com.mparticle;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class MParticleAPI {

    private static final String VERSION = "0.1";

    private static final String TAG = "mParticleAPI";
    private static boolean optOutFlag = false;
    public static boolean debugMode = true; // TODO: this will default to false
    private static Map<String, MParticleAPI> sInstanceMap = new HashMap<String, MParticleAPI>();

    private Context mContext;
    private String mApiKey;
    private String mSecret;
    private MessageManager mMessageManager;
    private UUID mSessionID;
    private int mSessionTimeout = 30*60*1000;
    private long mSessionStartTime = 0;
    private long mSessionEndTime = 0;
    private long mLastEventTime = 0;

    private MParticleAPI(Context context, String apiKey, String secret) {
        this.mContext = context.getApplicationContext();
        this.mApiKey = apiKey;
        this.mSecret = secret;
        this.mMessageManager = MessageManager.getInstance(mContext);
    }

    public static MParticleAPI getInstance(Context context, String apiKey, String secret,
            int uploadInterval) {
        MParticleAPI apiInstance;
        if (sInstanceMap.containsKey(apiKey)) {
            apiInstance = sInstanceMap.get(apiKey);
        } else {
            apiInstance = new MParticleAPI(context, apiKey, secret);
            sInstanceMap.put(apiKey, apiInstance);
        }
        return apiInstance;
    }

    public static MParticleAPI getInstance(Context context, String apiKey, String secret) {
        return MParticleAPI.getInstance(context, apiKey, secret, 0);
    }

    public static MParticleAPI getInstance(Context context) {
        return MParticleAPI.getInstance(context, null, null, 0);
    }

    // possible new method - for testing only right now
    public void setSessionTimeout(int sessionTimeout) {
        this.mSessionTimeout = sessionTimeout;
    }

    public void start() {
        this.checkSessionTimeout();
    }

    public void stop() {
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Stop Called");
    }

    public void newSession() {
        if (0!=this.mSessionStartTime) {
            this.endSession();
        }
        this.start();
    }

    public void endSession() {
        // generate session-end message
        this.debugLog("Explicit End Session");
        Map<String, String> sessionData=new HashMap<String, String>();
        sessionData.put("duration", ""+(System.currentTimeMillis()-mSessionStartTime));
        this.mMessageManager.handleEvent("session-ended", sessionData);
        // reset agent to unstarted state
        this.mSessionStartTime = 0;
    }

    private void checkSessionTimeout() {
        long now = System.currentTimeMillis();
        if (0==this.mSessionStartTime) {
            this.beginSession();
        } else if (this.mSessionTimeout < now-this.mLastEventTime) {
            this.debugLog("Session Timed Out");
            this.endSession();
            this.beginSession();
        }
    }

    private void beginSession() {
        this.mSessionStartTime = System.currentTimeMillis();
        this.mLastEventTime = this.mSessionStartTime;
        this.mSessionID = UUID.randomUUID();
        this.debugLog("Start New Session");
        this.mMessageManager.handleEvent("session-begin", null);
    }

    public void upload() {
        this.debugLog("Upload");
    }

    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    public void logEvent(String eventName, Map<String, String> eventData) {
        this.checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Logged event: " + eventName + " with data " + eventData);
        this.mMessageManager.handleEvent(eventName, eventData);
    }

    public void logScreenView(String screenName) {
        logScreenView(screenName, null);
    }

    public void logScreenView(String screenName, Map<String, String> eventData) {
        this.checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Logged screen: " + screenName + " with data " + eventData);
        this.mMessageManager.handleEvent(screenName, eventData);
    }

    public void logErrorEvent(String eventName) {
        logErrorEvent(eventName, null);
    }

    public void logErrorEvent(String eventName, Map<String, String> data) {
        this.checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Logged error: " + eventName);
    }

    public void logErrorEvent(String eventName, Map<String, String> data, Exception e) {
    }

    public void identifyUser(String userId) {
        identifyUser("user_id", userId);
    }

    public void identifyUser(String key, String userId) {
        this.checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Identified user: " + userId);
    }

    public void setLocation(double longitude, double latitude) {
        this.checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Set Location: " + longitude + " " + latitude);
    }

    public void setSessionProperty(String key, String value) {
        this.checkSessionTimeout();
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
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        properties.put("mparticle_sdk_version", MParticleAPI.VERSION);
        properties.put("api_key", this.mApiKey);
        properties.put("secret", this.mSecret);
        properties.put("session_timeout", this.mSessionTimeout);
        properties.put("session_id", this.mSessionID);
        properties.put("session_start", this.mSessionStartTime);
        properties.put("last_event", this.mLastEventTime);
        properties.put("session_end", this.mSessionEndTime);

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
