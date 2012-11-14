package com.mparticle;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.PrefKeys;

/**
 * This class provides access to the mParticle API.
 */
public class MParticleAPI {

    private static final String TAG = Constants.LOG_TAG;
    private static Map<String, MParticleAPI> sInstanceMap = new HashMap<String, MParticleAPI>();

    private MessageManager mMessageManager;
    private Handler mTimeoutHandler;
    private MParticleLocationListener mLocationListener;
    private SharedPreferences mPreferences;
    private Context mAppContext;
    private String mApiKey;
    private boolean mOptedOut = false;
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

    /* package-private */ MParticleAPI(Context appContext, String apiKey, MessageManager messageManager) {
        mAppContext = appContext;
        mApiKey = apiKey;
        mMessageManager = messageManager;
        HandlerThread timeoutHandlerThread = new HandlerThread("SessionTimeoutHandler", Process.THREAD_PRIORITY_BACKGROUND);
        timeoutHandlerThread.start();
        mTimeoutHandler = new SessionTimeoutHandler(this, timeoutHandlerThread.getLooper());

        mPreferences = mAppContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mOptedOut = mPreferences.getBoolean(PrefKeys.OPTOUT+mApiKey, false);
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
    public static MParticleAPI getInstance(Context context, String apiKey, String secret, long uploadInterval) {

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
            Context appContext = context.getApplicationContext();
            apiInstance = new MParticleAPI(appContext, apiKey,
                    MessageManager.getInstance(appContext, apiKey, secret, Math.abs(uploadInterval*1000)));
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
        return MParticleAPI.getInstance(context, apiKey, secret, Constants.DEFAULT_UPLOAD_INTERVAL);
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
        if (mOptedOut) {
            return;
        }
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
        if (mSessionStartTime==0 || mOptedOut) {
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
        if (mOptedOut) {
            return;
        }
        endSession();
        beginSession();
    }

    /**
     * Explicitly terminates the user session.
     */
    public void endSession() {
        if (mSessionStartTime==0 || mOptedOut) {
            return;
        }
        long sessionEndTime=System.currentTimeMillis();
        endSession(sessionEndTime);
    }

    /**
     * Explicitly end the session at the given time and generate the end-session message
     * @param sessionEndTime
     */
    private void endSession(long sessionEndTime) {
        debugLog("Explicit End Session");
        if (0==sessionEndTime) {
            Log.w(TAG, "Session end time was unknown");
            sessionEndTime = System.currentTimeMillis();
        }
        stopActiveSession(sessionEndTime);
        mMessageManager.stopSession(mSessionID, sessionEndTime, mSessionLength);
        mMessageManager.endSession(mSessionID, sessionEndTime, mSessionLength);
        // reset agent to unstarted state
        mSessionStartTime = 0;
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
            endSession(mLastEventTime);
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
    public void logEvent(String eventName, Map<String, String> eventData) {
        if (mOptedOut) {
            return;
        }
        if (null==eventName) {
            Log.w(TAG,"eventName is required for logEvent");
            return;
        }
        if (eventName.length()>Constants.LIMIT_NAME) {
            Log.w(TAG,"The event name was too long. Discarding event.");
            return;
        }
        ensureActiveSession();
        if (checkEventLimit()) {
            JSONObject eventDataJSON = enforceAttributeConstraints(eventData);
            mMessageManager.logCustomEvent(mSessionID, mSessionStartTime, mLastEventTime, eventName, eventDataJSON);
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
    public void logScreenView(String screenName, Map<String, String> eventData) {
        if (mOptedOut) {
            return;
        }
        if (null==screenName) {
            Log.w(TAG,"screenName is required for logScreenView");
            return;
        }
        if (screenName.length()>Constants.LIMIT_NAME) {
            Log.w(TAG,"The screen name was too long. Discarding event.");
            return;
        }
        ensureActiveSession();
        if (checkEventLimit()) {
            JSONObject eventDataJSON = enforceAttributeConstraints(eventData);
            mMessageManager.logScreenView(mSessionID, mSessionStartTime, mLastEventTime, screenName, eventDataJSON);
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
        logErrorEvent(eventName, data, null);
    }

    /**
     * Log an error event with data attributes and an exception
     * @param eventName the name of the error event to be tracked
     * @param data a Map of data attributes
     * @param e an Exception
     */
    // TODO: this method may be dropped - will decide in a later iteration
    public void logErrorEvent(String eventName, JSONObject data, Exception e) {
        if (mOptedOut) {
            return;
        }
        if (null==eventName) {
            Log.w(TAG,"eventName is required for logErrorEvent");
            return;
        }
        ensureActiveSession();
        mMessageManager.logErrorEvent(mSessionID, mSessionStartTime, mLastEventTime, eventName, data, e);
        debugLog("Logged error: " + eventName);
    }

    /**
     * Enables location tracking given a provider and update frequency criteria. The provider must be available
     * and the correct permissions must have been requested during installation.
     * @param provider the provider key
     * @param minTime the minimum time (in milliseconds) to trigger an update
     * @param minDistance the minimum distance (in meters) to trigger an update
     */
    public void enableLocationTracking(String provider, long minTime, long minDistance) {
        if (mOptedOut) {
            return;
        }
        try {
            LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(provider)) {
                Log.w(TAG, "That requested location provider is not available");
                return;
            };
            if (mLocationListener==null) {
                mLocationListener = new MParticleLocationListener(this);
            } else {
                // clear the location listener, so it can be added again
                locationManager.removeUpdates(mLocationListener);
            }
            locationManager.requestLocationUpdates(provider, minTime, minDistance, mLocationListener);
        } catch (SecurityException e) {
            Log.w(TAG, "The app must require the appropriate permissions to track location using this provider");
        }
    }

    /**
     * Set the current location of the active session.
     * @param location
     */
    public void setLocation(Location location) {
        MessageManager.setLocation(location);
    }

    /**
     * Set a single session attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setSessionProperty(String key, Object value) {
        if (mOptedOut) {
            return;
        }
        ensureActiveSession();
        if (setCheckedAttribute(mSessionAttributes, key, value)) {
            mMessageManager.setSessionAttributes(mSessionID, mSessionAttributes);
        }
        debugLog("Set Session: " + key + "=" + value);
    }

    /**
     * Set a single user attribute. The property will combined with any existing attributes.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setUserProperty(String key, Object value) {
        if (mOptedOut) {
            return;
        }
        debugLog("Set User: " + key + "=" + value);
        if (setCheckedAttribute(mUserAttributes, key, value)) {
            mPreferences.edit().putString(PrefKeys.USER_ATTRS+mApiKey, mUserAttributes.toString()).commit();
        }
    }

    /* package-private */ void clearUserProperties() {
        mUserAttributes = new JSONObject();
        mPreferences.edit().putString(PrefKeys.USER_ATTRS+mApiKey, mUserAttributes.toString()).commit();
    }

    /**
     * Control the opt-in/opt-out status for the application.
     * @param optOutStatus set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(boolean optOutStatus) {
        if (optOutStatus==mOptedOut) {
            return;
        }
        if (!optOutStatus) {
            ensureActiveSession();
        }
        mMessageManager.optOut(mSessionID, mSessionStartTime, System.currentTimeMillis(), optOutStatus);
        if (optOutStatus && mSessionStartTime>0) {
            endSession();
        }

        mPreferences.edit().putBoolean(PrefKeys.OPTOUT+mApiKey, optOutStatus).commit();
        mOptedOut = optOutStatus;

        debugLog("Set Opt Out: " + mOptedOut);
    }

    /**
     * Get the current opt-out status for the application.
     * @return the opt-out status
     */
    public boolean getOptOut() {
        return mOptedOut;
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

    /**
     * This method makes sure the constraints on event attributes are enforced. A JSONObject version of the attributes
     * is return with data that exceeds the limits removed.
     * NOTE: Non-string attributes are not converted to strings, currently.
     * @param attributes the user-provided JSONObject
     * @return a cleansed copy of the JSONObject
     */
    /* package-private */ JSONObject enforceAttributeConstraints(Map<String, String> attributes) {
        if (null==attributes) {
            return null;
        }
        JSONObject checkedAttributes = new JSONObject();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setCheckedAttribute(checkedAttributes, key, value);
        }
        return checkedAttributes;
    }

    /* package-private */ boolean setCheckedAttribute(JSONObject attributes, String key, Object value) {
        if (null==attributes || null==key ) {
            return false;
        }
        try {
            if (Constants.LIMIT_ATTR_COUNT==attributes.length() && !attributes.has(key)) {
                Log.w(TAG, "Attribute count exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (null!=value && value.toString().length()>Constants.LIMIT_ATTR_VALUE) {
                Log.w(TAG, "Attribute value length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (key.length()>Constants.LIMIT_ATTR_NAME) {
                Log.w(TAG, "Attribute name length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            attributes.put(key, value);
        } catch (JSONException e) {
            Log.w(TAG, "JSON error processing attributes. Discarding attribute: " + key);
            return false;
        }
        return true;
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

    private static final class MParticleLocationListener implements LocationListener {
        private MParticleAPI mParticleAPI;

        public MParticleLocationListener(MParticleAPI mParticleAPI) {
            this.mParticleAPI = mParticleAPI;
        }

        @Override
        public void onLocationChanged(Location location) {
            mParticleAPI.setLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

    }

    /// Possibly for development only
    public void setConnectionProxy(String host, int port) {
        mMessageManager.setConnectionProxy(host, port);
    }

}
