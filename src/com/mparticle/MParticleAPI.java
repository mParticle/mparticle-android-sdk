package com.mparticle;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
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

import com.mparticle.Constants.MessageKey;

/**
 * This class provides access to the mParticle API.
 */
public class MParticleAPI {

    private static final String VERSION = "0.1";

    private static final String TAG = "mParticleAPI";
    private static boolean optOutFlag = false;
    private static boolean debugMode = false;
    private static Map<String, MParticleAPI> sInstanceMap = new HashMap<String, MParticleAPI>();

    private MessageManager mMessageManager;
    private Handler mTimeoutHandler;

    /* package-private */ String mSessionID;
    /* package-private */ int mSessionTimeout = 30 * 60 * 1000;
    /* package-private */ long mSessionStartTime = 0;
    /* package-private */ long mLastEventTime = 0;

    /* package-private */ MParticleAPI(MessageManager messageManager) {
        this.mMessageManager = messageManager;
        HandlerThread timeoutHandlerThread = new HandlerThread("SessionTimeoutHandler", Process.THREAD_PRIORITY_BACKGROUND);
        timeoutHandlerThread.start();
        this.mTimeoutHandler = new SessionTimeoutHandler(this, timeoutHandlerThread.getLooper());
    }

    /**
     * Initialize or return an instance of the mParticle SDK with a specific upload interval
     * @param context the Activity that is creating the instance
     * @param apiKey the API key for your account
     * @param secret the API secret for your account
     * @param uploadInterval the upload interval (in seconds)
     * @return An instance of the mParticle SDK configured with your API key
     */
    public static MParticleAPI getInstance(Context context, String apiKey, String secret,
            int uploadInterval) {

        if (null==context) {
            throw new IllegalArgumentException("context is required");
        }
        if (null==apiKey || null==secret) {
            throw new IllegalArgumentException("apiKey and secret are required");
        }

        MParticleAPI apiInstance;
        if (sInstanceMap.containsKey(apiKey)) {
            apiInstance = sInstanceMap.get(apiKey);
        } else {
            apiInstance = new MParticleAPI(MessageManager.getInstance(context, apiKey, secret));
            sInstanceMap.put(apiKey, apiInstance);
        }
        return apiInstance;
    }

    /**
     * Initialize or return an instance of the mParticle SDK
     * @param context the Activity that is creating the instance
     * @param apiKey the API key for your account
     * @param secret the API secret for your account
     * @return an instance of the mParticle SDK configured with your API key
     */
    public static MParticleAPI getInstance(Context context, String apiKey, String secret) {
        return MParticleAPI.getInstance(context, apiKey, secret, 0);
    }

    /**
     * Set the user session timeout interval.
     *
     * A session is ended when no events (logged events or start/stop events) has occurred
     * within the session timeout interval.
     *
     * @param sessionTimeout
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.mSessionTimeout = sessionTimeout;
    }

    /**
     * Starts tracking a user session. If a session is already active, it will be resumed.
     *
     * This method should be called from an Activity's onStart() method.
     */
    public void start() {
        this.ensureActiveSession();
    }

    /**
     * Stop tracking a user session. If the session is restarted before the session timeout it will be resumed.
     *
     * This method should be called from an Activity's onStop() method.
     *
     * To explicitly end a session use the endSession() method.
     */
    public void stop() {
        this.mLastEventTime = System.currentTimeMillis();
        this.mMessageManager.stopSession(mSessionID, mSessionStartTime, mLastEventTime);
        this.debugLog("Stop Called");
    }

    /**
     * Begin tracking a new session. Ends the current session.
     */
    public void newSession() {
        if (0!=this.mSessionStartTime) {
            endSession();
        }
        this.beginSession();
    }

    /**
     * Explicitly terminates the user session.
     */
    public void endSession() {
        long sessionEndTime=System.currentTimeMillis();
        closeSession(sessionEndTime);
        this.mMessageManager.endSession(mSessionID, mSessionStartTime, sessionEndTime);
        this.debugLog("Explicit End Session");
    }

    /**
     * Ensures a session is active.
     */
    private void ensureActiveSession() {
        checkSessionTimeout();
        this.mLastEventTime = System.currentTimeMillis();
        if (0==this.mSessionStartTime) {
            this.beginSession();
        }
    }

    /**
     * Check current session timeout and end the session if needed. Will not start a new session.
     */
    /* package-private */ void checkSessionTimeout() {
        long now = System.currentTimeMillis();
        if (0!=this.mSessionStartTime && (this.mSessionTimeout < now-this.mLastEventTime) ) {
            this.debugLog("Session Timed Out");
            this.closeSession(this.mLastEventTime);
        }
    }

    /**
     * Creates a new session and generates the start-session message.
     */
    private void beginSession() {
        this.mSessionStartTime = System.currentTimeMillis();
        this.mLastEventTime = this.mSessionStartTime;
        this.mSessionID = UUID.randomUUID().toString();
        this.mMessageManager.startSession(mSessionID, mSessionStartTime);
        this.mTimeoutHandler.sendEmptyMessageDelayed(0, this.mSessionTimeout);
        this.debugLog("Start New Session");
    }

    /**
     * End the current session and generate the end-session message.
     * @param endTime the timestamp of the last event in the session (if known)
     */
    private void closeSession(long endTime) {
        long sessionEndTime = endTime;
        if (0==sessionEndTime) {
            Log.w(TAG, "Session end time was unknown");
            sessionEndTime = System.currentTimeMillis();
        }
        this.mMessageManager.stopSession(mSessionID, mSessionStartTime, sessionEndTime);

        // reset agent to unstarted state
        this.mSessionStartTime = 0;
    }

    /**
     * Upload queued messages to the mParticle server.
     */
    public void upload() {
        this.debugLog("Upload");
        this.mMessageManager.doUpload();
    }

    /**
     * Log an event
     * @param eventName the name of the event to be tracked
     */
    public void logEvent(String eventName) {
        logEvent(eventName, null);
    }

    /**
     * Log an event with data attributes
     * @param eventName the name of the event to be tracked
     * @param eventData a Map of data attributes
     */
    public void logEvent(String eventName, JSONObject eventData) {
        if (null==eventName) {
            Log.w(TAG,"eventName is required for logEvent");
            return;
        }
        this.ensureActiveSession();
        this.mMessageManager.logCustomEvent(mSessionID, mSessionStartTime, mLastEventTime, eventName, eventData);
        this.debugLog("Logged event: " + eventName + " with data " + eventData);
    }

    /**
     * Log a screen view event
     * @param screenName the name of the View to be tracked
     */
    public void logScreenView(String screenName) {
        logScreenView(screenName, null);
    }

    /**
     * Log a screen view event with data attributes
     * @param screenName the name of the View to be tracked
     * @param eventData a Map of data attributes
     */
    public void logScreenView(String screenName, JSONObject eventData) {
        if (null==screenName) {
            Log.w(TAG,"screenName is required for logScreenView");
            return;
        }
        this.ensureActiveSession();
        this.mMessageManager.logScreenView(mSessionID, mSessionStartTime, mLastEventTime, screenName, eventData);
        this.debugLog("Logged screen: " + screenName + " with data " + eventData);
    }

    /**
     * Log an error event
     * @param eventName the name of the error event to be tracked
     */
    public void logErrorEvent(String eventName) {
        logErrorEvent(eventName, null);
    }

    /**
     * Log an error event with data attributes
     * @param eventName the name of the error event to be tracked
     * @param data a Map of data attributes
     */
    public void logErrorEvent(String eventName, JSONObject data) {
        if (null==eventName) {
            Log.w(TAG,"eventName is required for logErrorEvent");
            return;
        }
        this.ensureActiveSession();
        this.debugLog("Logged error: " + eventName);
    }

    /**
     * Log an error event with data attributes and an exception
     * @param eventName the name of the error event to be tracked
     * @param data a Map of data attributes
     * @param e an Exception
     */
    // TODO: this method may be dropped - will decide in a later iteration
    public void logErrorEvent(String eventName, JSONObject data, Exception e) {
    }

    /**
     * Identify the current user
     * @param userId the primary id of the current application user
     */
    public void identifyUser(String userId) {
        identifyUser("user_id", userId);
    }

    /**
     * Identify user with an alternate identifier.
     * Can be used to track multiple aliases or accounts from more than one provider.
     * @param key the identify provider
     * @param userId the user identity
     */
    public void identifyUser(String key, String userId) {
        if (null==userId) {
            Log.w(TAG,"userId is required for identifyUser");
            return;
        }
        if (null==key) {
            Log.w(TAG,"key is required for identifyUser");
            return;
        }
        this.ensureActiveSession();
        this.debugLog("Identified user: " + userId);
    }

    /**
     * Set the current location of the active session.
     * @param longitude
     * @param latitude
     */
    public void setLocation(double longitude, double latitude) {
        this.ensureActiveSession();
        Location location = new Location("user");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        MessageManager.setLocation(location);
        this.debugLog("Set Location: " + longitude + " " + latitude);
    }

    /**
     * Set a single session attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setSessionProperty(String key, String value) {
        this.ensureActiveSession();
        this.debugLog("Set Session: " + key + "=" + value);
    }

    /**
     * Set a collection of session attributes
     * @param data key/value pairs of session attributes
     */
    public void setSessionProperties(JSONObject data) {
    }

    /**
     * Set a single user attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setUserProperty(String key, String value) {
        this.debugLog("Set User: " + key + "=" + value);
    }

    /**
     * Set a collection of user attributes
     * @param data key/value pairs of user attributes
     */
    public void setUserProperties(JSONObject data) {
    }

    /**
     * Get the current user segment as determined by mParticle.
     * This method makes a synchronous call to the mParticle server so you should manage threads accordingly.
     * @return the user segment
     */
    public String getUserSegment() {
        return "default";
    }

    /**
     * Control the opt-in/opt-out status for the application.
     * @param optOutFlag set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(boolean optOutFlag) {
        MParticleAPI.optOutFlag = optOutFlag;
        this.debugLog("Set Opt Out: " + MParticleAPI.optOutFlag);
    }

    /**
     * Get the current opt-out status for the application.
     * @return the opt-out status
     */
    public boolean getOptOut() {
        return MParticleAPI.optOutFlag;
    }

    /**
     * Turn on or off debug mode for mParticle.
     * In debug mode, the mParticle SDK will output informational messages to LogCat.
     * @param debugMode
     */
    public void setDebug(boolean debugMode) {
        MParticleAPI.debugMode = debugMode;
        this.debugLog("Set Debug Mode: " + MParticleAPI.debugMode);
    }

    /**
     * Enable mParticle exception handling to automatically log events on uncaught exceptions
     */
    public void handleExceptions() {
    }

    /**
     * Set the referral URL for the user session.
     * @param url the referral URL
     */
    public void setReferralURL(URL url) {
    }

    /**
     * Register the application to receive push notifications from mParticle
     * @param token TBD
     */
    public void setPushRegistrationId(String token) {
        this.debugLog("Set Push Token: " + token);
    }

    /**
     * Unregister the application from receiving push notifications
     */
    public void clearPushRegistrationId() {
        this.debugLog("Clear Push Token");
    }

    /**
     * Register an event collector to generate log events on a periodic basis.
     * @param collector an instance of the EventCollectorIterface which provides event information
     * @param timeInterval the time interval (in seconds)
     */
    public void registerEventCollector(EventCollectorInterface collector, int timeInterval) {
    }

    /**
     * Used to provide event data for recurring events
     */
    public interface EventCollectorInterface {
        /**
         * Called on a periodic interval to provide event data
         * @return a map of key/value pairs to be logged with the event
         */
        JSONObject provideEventData();
    }

    /**
     * Generates a collection of device properties
     * @param context the application context
     * @return a JSONObject of device-specific attributes
     */
    public static JSONObject collectDeviceProperties(Context context) {
        JSONObject properties = new JSONObject();

        try {
            try {
                String packageName = context.getPackageName();
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                properties.put(MessageKey.APPLICATION_VERSION, pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                properties.put(MessageKey.APPLICATION_VERSION, "Unknown");
            }

            properties.put(MessageKey.MPARTICLE_VERSION, MParticleAPI.VERSION);

            // device IDs
            properties.put(MessageKey.DEVICE_ID, Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            // TODO: get network MAC addresses?

            // device/OS properties
            properties.put(MessageKey.MANUFACTURER, android.os.Build.MANUFACTURER);
            properties.put(MessageKey.PLATFORM, "Android");
            properties.put(MessageKey.OS_VERSION, android.os.Build.VERSION.SDK_INT);
            properties.put(MessageKey.MODEL, android.os.Build.MODEL);

            // screen height/width
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
            TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            properties.put(MessageKey.NETWORK_CARRIER, telephonyManager.getNetworkOperatorName());
            properties.put(MessageKey.NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso());
            // TODO: android appears to combine MNC+MCC into network operator
            properties.put(MessageKey.MOBILE_NETWORK_CODE, telephonyManager.getNetworkOperator());
            // properties.put(MessageKey.MOBILE_COUNTRY_CODE, telephonyManager.getNetworkOperator());

        } catch (JSONException e) {
            // ignore JSON exceptions
        }

        return properties;
    }

    private void debugLog(String message) {
        if (MParticleAPI.debugMode) {
            Log.d(TAG, this.mSessionID + ": " + message);
        }
    }

    private static final class SessionTimeoutHandler extends Handler {
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
