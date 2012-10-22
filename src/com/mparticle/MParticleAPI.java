package com.mparticle;

import java.net.URL;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.mparticle.MessageManager.MessageKey;

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
    private Handler mTimeoutHandler;

    /* package-private */ String mSessionID;
    /* package-private */ int mSessionTimeout = 30 * 60 * 1000;
    /* package-private */ long mSessionStartTime = 0;
    /* package-private */ long mLastEventTime = 0;

    /* package-private */ MParticleAPI(Context context, String apiKey, String secret, MessageManager messageManager) {
        this.mContext = context.getApplicationContext();
        this.mApiKey = apiKey;
        this.mSecret = secret;
        this.mMessageManager = messageManager;
        HandlerThread timeoutHandlerThread = new HandlerThread("SessionTimeoutHandler", Process.THREAD_PRIORITY_BACKGROUND);
        timeoutHandlerThread.start();
        this.mTimeoutHandler = new SessionTimeoutHandler(this, timeoutHandlerThread.getLooper());
    }

    public static MParticleAPI getInstance(Context context, String apiKey, String secret,
            int uploadInterval) {
        MParticleAPI apiInstance;
        if (sInstanceMap.containsKey(apiKey)) {
            apiInstance = sInstanceMap.get(apiKey);
        } else {
            apiInstance = new MParticleAPI(context, apiKey, secret, MessageManager.getInstance(context));
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
        this.ensureActiveSession();
    }

    public void stop() {
        this.mLastEventTime = System.currentTimeMillis();
        this.debugLog("Stop Called");
    }

    public void newSession() {
        if (0!=this.mSessionStartTime) {
            closeSession(System.currentTimeMillis());
        }
        this.start();
    }

    public void endSession() {
        closeSession(System.currentTimeMillis());
        this.debugLog("Explicit End Session");
    }

    private void ensureActiveSession() {
        checkSessionTimeout();
        if (0==this.mSessionStartTime) {
            this.beginSession();
        }
        this.mLastEventTime = System.currentTimeMillis();
    }

    /* package-private */ void checkSessionTimeout() {
        long now = System.currentTimeMillis();
        if (0!=this.mSessionStartTime && (this.mSessionTimeout < now-this.mLastEventTime) ) {
            this.debugLog("Session Timed Out");
            this.closeSession(this.mLastEventTime);
        }
    }

    private void beginSession() {
        this.mSessionStartTime = System.currentTimeMillis();
        this.mLastEventTime = this.mSessionStartTime;
        this.mSessionID = UUID.randomUUID().toString();
        this.mMessageManager.beginSession(mSessionID, mSessionStartTime, null);
        this.mTimeoutHandler.sendEmptyMessageDelayed(0, this.mSessionTimeout);
        this.debugLog("Start New Session");
    }

    private void closeSession(long endTime) {
        long sessionEndTime = endTime;
        if (0==sessionEndTime) {
            Log.w(TAG, "Session end time was unknown");
            sessionEndTime = System.currentTimeMillis();
        }
        Map<String, String> sessionData=new HashMap<String, String>();
        sessionData.put("duration", ""+(sessionEndTime-mSessionStartTime));
        this.mMessageManager.closeSession(mSessionID, sessionEndTime, sessionData);

        // reset agent to unstarted state
        this.mSessionStartTime = 0;
    }

    public void upload() {
        this.debugLog("Upload");
    }

    public void logEvent(String eventName) {
        logEvent(eventName, new HashMap<String, String>());
    }

    public void logEvent(String eventName, Map<String, String> eventData) {
        this.ensureActiveSession();
        this.mMessageManager.logCustomEvent(this.mSessionID, this.mLastEventTime, eventName, eventData);
        this.debugLog("Logged event: " + eventName + " with data " + eventData);
    }

    public void logScreenView(String screenName) {
        logScreenView(screenName, new HashMap<String, String>());
    }

    public void logScreenView(String screenName, Map<String, String> eventData) {
        this.ensureActiveSession();
        this.mMessageManager.logScreenView(this.mSessionID, this.mLastEventTime, screenName, eventData);
        this.debugLog("Logged screen: " + screenName + " with data " + eventData);
    }

    public void logErrorEvent(String eventName) {
        logErrorEvent(eventName, new HashMap<String, String>());
    }

    public void logErrorEvent(String eventName, Map<String, String> data) {
        this.ensureActiveSession();
        this.debugLog("Logged error: " + eventName);
    }

    public void logErrorEvent(String eventName, Map<String, String> data, Exception e) {
    }

    public void identifyUser(String userId) {
        identifyUser("user_id", userId);
    }

    public void identifyUser(String key, String userId) {
        this.ensureActiveSession();
        this.debugLog("Identified user: " + userId);
    }

    public void setLocation(double longitude, double latitude) {
        this.ensureActiveSession();
        this.debugLog("Set Location: " + longitude + " " + latitude);
    }

    public void setSessionProperty(String key, String value) {
        this.ensureActiveSession();
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

        // TODO: verify this is the correct value for this key
        properties.put(MessageKey.APPLICATION_KEY, this.mApiKey);

        try {
            String packageName = mContext.getPackageName();
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
            properties.put(MessageKey.APPLICATION_VERSION, pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            properties.put(MessageKey.APPLICATION_VERSION, "Unknown");
        }

        properties.put(MessageKey.MPARTICLE_VERSION, MParticleAPI.VERSION);

        // device IDs
        properties.put(MessageKey.DEVICE_ID, Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        // TODO: get network MAC addresses?

        // device/OS properties
        properties.put(MessageKey.MANUFACTURER, android.os.Build.MANUFACTURER);
        properties.put(MessageKey.PLATFORM, "Android");
        properties.put(MessageKey.OS_VERSION, android.os.Build.VERSION.SDK_INT);
        properties.put(MessageKey.MODEL, android.os.Build.MODEL);

        // screen height/width
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        properties.put(MessageKey.SCREEN_HEIGHT, metrics.heightPixels);
        properties.put(MessageKey.SCREEN_WIDTH, metrics.widthPixels);

        // locales
        Locale locale = Locale.getDefault();
        properties.put(MessageKey.DEVICE_COUNTRY, locale.getDisplayCountry());
        properties.put(MessageKey.DEVICE_LOCALE_COUNTRY, locale.getCountry());
        properties.put(MessageKey.DEVICE_LOCALE_LANGUAGE, locale.getLanguage());

        // TODO: network
        TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        properties.put(MessageKey.NETWORK_CARRIER, telephonyManager.getNetworkOperatorName());
        properties.put(MessageKey.NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso());
        // TODO: android appears to combine MNC+MCC into network operator
        properties.put(MessageKey.MOBILE_NETWORK_CODE, telephonyManager.getNetworkOperator());
        properties.put(MessageKey.MOBILE_COUNTRY_CODE, telephonyManager.getNetworkOperator());

        // additional info available but possibly not needed
        ConnectivityManager connectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        properties.put("extra_mobile_network_type", telephonyManager.getNetworkType());

        // NOTE: this requires ACCESS_NETWORK_STATE permission - which should already be granted. possibly move check elsewhere.
        if (PackageManager.PERMISSION_GRANTED ==
                mContext.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            properties.put(MessageKey.DATA_CONNECTION, networkInfo.getTypeName());
        } else {
            properties.put(MessageKey.DATA_CONNECTION, "Forbidden");
        }

        properties.put("extra_screen_orientation", windowManager.getDefaultDisplay().getRotation());
        properties.put("extra_screen_metrics", metrics);

        properties.put("device", android.os.Build.DEVICE);
        properties.put("display", android.os.Build.DISPLAY);
        properties.put("hardware", android.os.Build.HARDWARE);
        properties.put("build_id", android.os.Build.ID);
        properties.put("product", android.os.Build.PRODUCT);

        // internal diagnostics for development, to be removed
        properties.put("secret", this.mSecret);
        properties.put("session_timeout", this.mSessionTimeout);
        properties.put("session_id", this.mSessionID);
        properties.put("session_start", this.mSessionStartTime);
        properties.put("last_event", this.mLastEventTime);

        return properties;
    }

    private void debugLog(String message) {
        if (MParticleAPI.debugMode) {
            Log.d(TAG, this.mSessionID + ": " + message);
        }
    }

    public static final class SessionTimeoutHandler extends Handler {
        private MParticleAPI mParticleAPI;
        public SessionTimeoutHandler(MParticleAPI mParticleAPI, Looper looper) {
            super(looper);
            this.mParticleAPI = mParticleAPI;
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mParticleAPI.checkSessionTimeout();
            if (0!=mParticleAPI.mSessionStartTime) {
                // just check once every session timeout period
                this.sendEmptyMessageDelayed(0, mParticleAPI.mSessionTimeout);
                // or... use lastEventTime to decide when to check next
            }
        }
    }
}
