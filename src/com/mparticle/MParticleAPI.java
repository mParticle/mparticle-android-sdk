package com.mparticle;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
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
import com.mparticle.Constants.PrefKeys;

/**
 * This class provides access to the mParticle API.
 */
public class MParticleAPI {

    /* package-private */ static final String VERSION = "0.1";

    private static final String TAG = "mParticleAPI";
    private static Map<String, MParticleAPI> sInstanceMap = new HashMap<String, MParticleAPI>();

    private MessageManager mMessageManager;
    private Handler mTimeoutHandler;
    private SharedPreferences mPreferences;
    private Context mContext;
    private String mApiKey;
    private boolean mOptOutStatus = false;
    private boolean mDebugMode = false;

    /* package-private */ String mSessionID;
    /* package-private */ int mSessionTimeout = 30 * 60 * 1000;
    /* package-private */ long mSessionStartTime = 0;
    /* package-private */ long mLastEventTime = 0;
    /* package-private */ long mSessionActiveStart = 0;
    /* package-private */ long mSessionLength = 0;
    /* package-private */ long mEventCount = 0;
    /* package-private */ JSONObject mUserAttributes = new JSONObject();
    /* package-private */ JSONObject mSessionAttributes;

    /* package-private */ MParticleAPI(Context context, String apiKey, MessageManager messageManager) {
        mContext = context.getApplicationContext();
        mApiKey = apiKey;
        mMessageManager = messageManager;
        HandlerThread timeoutHandlerThread = new HandlerThread("SessionTimeoutHandler", Process.THREAD_PRIORITY_BACKGROUND);
        timeoutHandlerThread.start();
        mTimeoutHandler = new SessionTimeoutHandler(this, timeoutHandlerThread.getLooper());

        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mOptOutStatus = mPreferences.getBoolean(PrefKeys.OPTOUT+mApiKey, false);
        String userAttrs = mPreferences.getString(PrefKeys.USER_ATTRS+mApiKey, null);
        if (null!=userAttrs) {
            try {
                mUserAttributes = new JSONObject(userAttrs);
            } catch (JSONException e) {
                // carry on without user attributes
            }
        }

        if (!mPreferences.contains(PrefKeys.INSTALL_TIME)) {
            mPreferences.edit().putLong(PrefKeys.INSTALL_TIME, System.currentTimeMillis()).commit();
        }

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
            apiInstance = new MParticleAPI(context, apiKey, MessageManager.getInstance(context, apiKey, secret));
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
        mSessionTimeout = sessionTimeout;
    }

    /**
     * Starts tracking a user session. If a session is already active, it will be resumed.
     *
     * This method should be called from an Activity's onStart() method.
     */
    public void start() {
        ensureActiveSession();
    }

    /**
     * Stop tracking a user session. If the session is restarted before the session timeout it will be resumed.
     *
     * This method should be called from an Activity's onStop() method.
     *
     * To explicitly end a session use the endSession() method.
     */
    public void stop() {
        if (mSessionStartTime==0) {
            return;
        }
        long stopTime = System.currentTimeMillis();
        mLastEventTime = stopTime;
        stopActiveSession(mLastEventTime);
        mMessageManager.stopSession(mSessionID, stopTime, mSessionLength);
        debugLog("Stop Called");
    }

    /**
     * Begin tracking a new session. Ends the current session.
     */
    public void newSession() {
        endSession();
        beginSession();
    }

    /**
     * Explicitly terminates the user session.
     */
    public void endSession() {
        if (mSessionStartTime==0) {
            return;
        }
        long sessionEndTime=System.currentTimeMillis();
        closeSession(sessionEndTime);
        mMessageManager.endSession(mSessionID, sessionEndTime, mSessionLength);
        debugLog("Explicit End Session");
    }

    /**
     * Ensures a session is active.
     */
    private void ensureActiveSession() {
        checkSessionTimeout();
        mLastEventTime = System.currentTimeMillis();
        if (0==mSessionStartTime) {
            beginSession();
        }
        if (0==mSessionActiveStart) {
            mSessionActiveStart=mLastEventTime;
        }
    }

    private void stopActiveSession(long stopTime) {
        if (0!=mSessionActiveStart) {
            Log.d(TAG, String.format("Updating active session: %s %s %s %s",
                    mSessionLength,
                    mSessionActiveStart,
                    stopTime,
                    stopTime-mSessionActiveStart)
            );
            mSessionLength += stopTime-mSessionActiveStart;
            mSessionActiveStart = 0;
        }
    }

    /**
     * Check current session timeout and end the session if needed. Will not start a new session.
     */
    /* package-private */ void checkSessionTimeout() {
        long now = System.currentTimeMillis();
        if (0!=mSessionStartTime && (mSessionTimeout < now-mLastEventTime) ) {
            debugLog("Session Timed Out");
            closeSession(mLastEventTime);
        }
    }

    /**
     * Creates a new session and generates the start-session message.
     */
    private void beginSession() {
        mSessionStartTime = System.currentTimeMillis();
        mLastEventTime = mSessionStartTime;
        mSessionID = UUID.randomUUID().toString();
        mSessionLength = 0;
        mEventCount = 0;
        mSessionAttributes = new JSONObject();
        mMessageManager.startSession(mSessionID, mSessionStartTime);
        mTimeoutHandler.sendEmptyMessageDelayed(0, mSessionTimeout);
        debugLog("Start New Session");
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
        stopActiveSession(sessionEndTime);
        mMessageManager.stopSession(mSessionID, sessionEndTime, mSessionLength);

        // reset agent to unstarted state
        mSessionStartTime = 0;
    }

    /**
     * Upload queued messages to the mParticle server.
     */
    public void upload() {
        debugLog("Upload");
        mMessageManager.doUpload();
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
        ensureActiveSession();
        if (checkEventLimit()) {
            mMessageManager.logCustomEvent(mSessionID, mSessionStartTime, mLastEventTime, eventName, eventData);
            debugLog("Logged event: " + eventName + " with data " + eventData);
        }
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
        ensureActiveSession();
        if (checkEventLimit()) {
            mMessageManager.logScreenView(mSessionID, mSessionStartTime, mLastEventTime, screenName, eventData);
            debugLog("Logged screen: " + screenName + " with data " + eventData);
        }
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
        ensureActiveSession();
        debugLog("Logged error: " + eventName);
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
        identifyUser("app", userId);
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
        ensureActiveSession();
        setUserProperty("mp::id::"+key, userId);
        debugLog("Identified user: " + userId);
    }

    /**
     * Set the current location of the active session.
     * @param longitude
     * @param latitude
     */
    public void setLocation(double longitude, double latitude) {
        ensureActiveSession();
        Location location = new Location("user");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        MessageManager.setLocation(location);
        debugLog("Set Location: " + longitude + " " + latitude);
    }

    /**
     * Set a single session attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setSessionProperty(String key, String value) {
        ensureActiveSession();
        // TODO: needs constraints checks
        try {
            mSessionAttributes.put(key, value);
            mMessageManager.setSessionAttributes(mSessionID, mSessionAttributes);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to add session attribute");
        }
        debugLog("Set Session: " + key + "=" + value);
    }

    /**
     * Set a collection of session attributes
     * @param data key/value pairs of session attributes
     */
    public void setSessionProperties(JSONObject data) {
        ensureActiveSession();
        try {
            for (Iterator<?> iter=  data.keys(); iter.hasNext(); ) {
                String key = (String) iter.next();
                mSessionAttributes.put(key, data.getString(key));
            }
            mMessageManager.setSessionAttributes(mSessionID, mSessionAttributes);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to add session attribute");
        }
    }

    /**
     * Set a single user attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setUserProperty(String key, String value) {
        debugLog("Set User: " + key + "=" + value);
        try {
            mUserAttributes.put(key, value);
            mPreferences.edit().putString(PrefKeys.USER_ATTRS+mApiKey, mUserAttributes.toString()).commit();
        } catch (JSONException e) {
            Log.w(TAG, "Failed to save user attribute: "+key);
        }
    }

    /**
     * Set a collection of user attributes
     * @param data key/value pairs of user attributes
     */
    public void setUserProperties(JSONObject data) {
        try {
            for (Iterator<?> iter=  data.keys(); iter.hasNext(); ) {
                String key = (String) iter.next();
                setUserProperty(key, data.getString(key));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to add session attribute");
        }


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
     * @param optOutStatus set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(boolean optOutStatus) {
        // TODO: for now, require an active session so the message has a session id. may want to remove this requirement.
        ensureActiveSession();
        mOptOutStatus = optOutStatus;
        mPreferences.edit().putBoolean(PrefKeys.OPTOUT+mApiKey, optOutStatus).commit();
        mMessageManager.optOut(mSessionID, mSessionStartTime, System.currentTimeMillis(), optOutStatus);
        debugLog("Set Opt Out: " + mOptOutStatus);
    }

    /**
     * Get the current opt-out status for the application.
     * @return the opt-out status
     */
    public boolean getOptOut() {
        return mOptOutStatus;
    }

    /**
     * Turn on or off debug mode for mParticle.
     * In debug mode, the mParticle SDK will output informational messages to LogCat.
     * @param debugMode
     */
    public void setDebug(boolean debugMode) {
        mDebugMode = debugMode;
        debugLog("Set Debug Mode: " + mDebugMode);
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
        debugLog("Set Push Token: " + token);
    }

    /**
     * Unregister the application from receiving push notifications
     */
    public void clearPushRegistrationId() {
        debugLog("Clear Push Token");
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
     * Generates a collection of application properties
     * @param context the application context
     * @return a JSONObject of application-specific attributes
     */
    public static JSONObject collectAppInfo(Context context) {
        JSONObject properties = new JSONObject();

        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            properties.put(MessageKey.APP_PACKAGE_NAME, packageName);
            String installerPackageName = packageManager.getInstallerPackageName(packageName);
            if (null!=installerPackageName) {
                properties.put(MessageKey.APP_INSTALLER_NAME, installerPackageName);
            }
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName,0);
                properties.put(MessageKey.APP_NAME, packageManager.getApplicationLabel(appInfo));
            } catch (PackageManager.NameNotFoundException e) {
                properties.put(MessageKey.APP_NAME, "Unknown");
            }
            try {
                PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                properties.put(MessageKey.APP_VERSION, pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                properties.put(MessageKey.APP_VERSION, "Unknown");
            }
        } catch (JSONException e) {
            // ignore JSON exceptions
        }
        return properties;
    }

    /**
     * Generates a collection of device properties
     * @param context the application context
     * @return a JSONObject of device-specific attributes
     */
    public static JSONObject collectDeviceInfo(Context context) {
        JSONObject properties = new JSONObject();

        try {
            // device IDs
            properties.put(MessageKey.DEVICE_ID, Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            // TODO: get network MAC addresses?

            // device/OS properties
            properties.put(MessageKey.BRAND, android.os.Build.BRAND);
            properties.put(MessageKey.PRODUCT, android.os.Build.PRODUCT);
            properties.put(MessageKey.DEVICE, android.os.Build.DEVICE);
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

            // timezone
            properties.put(MessageKey.TIMEZONE, TimeZone.getDefault().getRawOffset()/(1000*60*60));
        } catch (JSONException e) {
            // ignore JSON exceptions
        }

        return properties;
    }

    /**
     * This method checks the event count is below the limit and increments the event count.
     * A warning is logged if the limit has been reached.
     * @return true if event count is below limit
     */
    private boolean checkEventLimit() {
        if ( mEventCount < Constants.EVENT_LIMIT) {
            mEventCount++;
            return true;
        } else {
            Log.w(TAG,"The event limit has been exceeded for this session.");
            return false;
        }
    }

    private void debugLog(String message) {
        if (mDebugMode) {
            Log.d(TAG, mSessionID + ": " + message);
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
                sendEmptyMessageDelayed(0, mParticleAPI.mSessionTimeout);
                // or... use lastEventTime to decide when to check next
            }
        }
    }

    /// Possibly for development only
    public void setConnectionProxy(String host, int port) {
        mMessageManager.setConnectionProxy(host, port);
    }

}
