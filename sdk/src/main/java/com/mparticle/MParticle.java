package com.mparticle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.PrefKeys;
import com.mparticle.segmentation.SegmentListener;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

/**
 * The primary access point to the mParticle SDK. In order to use this class, you must first call {@link #start(android.content.Context)}, which requires
 * configuration via {@link <a href="http://developer.android.com/guide/topics/resources/providing-resources.html">Android Resources</a>}. You can then retrieve a reference
 * to an instance of this class via {@link #getInstance()}
 * <p/>
 * It's recommended to keep configuration parameters in a single xml file located within your res/values folder. The full list of configuration options is as follows:
 * <p/>
 * <h4>Required parameters</h4>
 * <ul>
 * <li>mp_key - {@link <a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a>} - This is the key used to authenticate with the mParticle SDK server API</li>
 * <li>mp_secret - {@link <a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a>} - This is the secret used to authenticate with the mParticle SDK server API</li>
 * </ul>
 * <h4>Required for push notifications</h4>
 * <ul>
 * <li> mp_enablePush - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a>} - Enable push registration, notifications, and analytics. <i>Default: false</i></li>
 * <li> mp_pushSenderId - {@link <a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a>} - {@link <a href="http://developer.android.com/google/gcm/gcm.html#senderid">GCM Sender ID</a>}</li>
 * </ul>
 * <h4>Required for licensing</h4>
 * <ul>
 * <li> mp_enableLicenseCheck - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a>} - By enabling license check, MParticle will automatically validate that the app was downloaded and/or bought via Google Play, or if it was "pirated" or "side-loaded". <i>Default: false</i></li>
 * <li> mp_appLicenseKey - {@link <a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a>} - The {@link <a href="http://developer.android.com/google/play/licensing/adding-licensing.html#account-key">public key</a>} used by your app to verify the user's license with Google Play.</li>
 * </ul>
 * <h4>Optional</h4>
 * <ul>
 * <li>mp_enableAutoScreenTracking - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a>} - Enable automatic screen view events. Note that *prior to ICS/API level 14*, this functionality requires instrumentation via an mParticle Activity implementation or manually. </li>
 * <li>mp_productionUploadInterval - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a>} - The length of time in seconds to send batches of messages to mParticle. Setting this too low could have an adverse effect on the device battery. <i>Default: 600</i></li>
 * <li>mp_reportUncaughtExceptions - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a>} - By enabling this, the MParticle SDK will automatically log and report any uncaught exceptions, including stack traces. <i>Default: false</i></li>
 * <li>mp_sessionTimeout - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a>} - The length of time (in seconds) that a user session will remain valid while application has been paused and put into the background. <i>Default: 60</i></li>
 * <li>mp_enableNetworkPerformanceMeasurement - {@link <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a>} - Enabling this will allow the mParticle SDK to measure network requests made with Apache's HttpClient as well as UrlConnection. <i>Default: false</i></li>
 * </ul>
 */
public class MParticle {

    static Boolean appRunning;
    final ConfigManager mConfigManager;
    final AppStateManager mAppStateManager;
    final MeasuredRequestManager measuredRequestManager;
    final EmbeddedKitManager mEmbeddedKitManager;
    JSONArray mUserIdentities = new JSONArray();
    String mSessionID;

    private static Bundle lastNotificationBundle;

    JSONObject mUserAttributes = new JSONObject();
    private JSONObject mSessionAttributes;

    private long mSessionStartTime = 0;
    private long mLastEventTime = 0;
    private final MessageManager mMessageManager;
    private static volatile MParticle instance;
    private static SharedPreferences sPreferences;
    private final Handler mTimeoutHandler;
    private MParticleLocationListener mLocationListener;
    private ExceptionHandler mExHandler;
    private Context mAppContext;
    private String mApiKey;
    private int mEventCount = 0;
    private LicenseCheckerCallback clientLicensingCallback;
    private static final HandlerThread sTimeoutHandlerThread = new HandlerThread("mParticleSessionTimeoutHandler",
            Process.THREAD_PRIORITY_BACKGROUND);

    MParticle(Context context, MessageManager messageManager, ConfigManager configManager, EmbeddedKitManager embeddedKitManager) {
        appRunning = true;
        mConfigManager = configManager;
        mAppContext = context.getApplicationContext();
        mApiKey = mConfigManager.getApiKey();
        mMessageManager = messageManager;
        mAppStateManager = new AppStateManager(mAppContext, embeddedKitManager);
        measuredRequestManager = new MeasuredRequestManager();
        mTimeoutHandler = new SessionTimeoutHandler(this, sTimeoutHandlerThread.getLooper());
        mEmbeddedKitManager = embeddedKitManager;

        String userAttrs = sPreferences.getString(PrefKeys.USER_ATTRS + mApiKey, null);

        if (null != userAttrs) {
            try {
                mUserAttributes = new JSONObject(userAttrs);
            } catch (JSONException e) {
                // carry on without user attributes
            }
        }

        String userIds = sPreferences.getString(PrefKeys.USER_IDENTITIES + mApiKey, null);
        if (null != userIds) {
            try {
                mUserIdentities = new JSONArray(userIds);
            } catch (JSONException e) {
                // carry on without user identities
            }
            try {
                Boolean changeMade = false;
                for (int i = 0; i < mUserIdentities.length(); i++) {
                    JSONObject identity = mUserIdentities.getJSONObject(i);
                    if (!identity.has(MessageKey.IDENTITY_DATE_FIRST_SEEN)) {
                        identity.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, 0);
                        changeMade = true;
                    }
                    if (!identity.has(MessageKey.IDENTITY_FIRST_SEEN)) {
                        identity.put(MessageKey.IDENTITY_FIRST_SEEN, true);
                        changeMade = true;
                    }
                }
                if (changeMade) {
                    sPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, mUserIdentities.toString()).commit();
                }
            } catch (JSONException jse) {
                //swallow this
            }
        }
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     *
     * @param context Required reference to a Context object
     */

    public static void start(Context context) {
        start(context, InstallType.AutoDetect);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session.
     *
     * @param context Required reference to a Context object
     * @param apiKey  The API key to use for authentication with mParticle
     * @param secret  The API secret to use for authentication with mParticle
     */

    public static void start(Context context, String apiKey, String secret) {
        start(context, apiKey, secret, InstallType.AutoDetect);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session.
     * <p/>
     * The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     *
     * @param context     Required reference to a Context object
     * @param apiKey      The API key to use for authentication with mParticle
     * @param secret      The API secret to use for authentication with mParticle
     * @param installType Specify whether this is a new install or an upgrade, or let mParticle detect
     * @see com.mparticle.MParticle.InstallType
     */

    public static void start(final Context context, final String apiKey, final String secret, final InstallType installType) {
        if (context == null) {
            throw new IllegalArgumentException("mParticle failed to start: context is required.");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("mParticle failed to start: apiKey is required.");
        }
        if (secret == null) {
            throw new IllegalArgumentException("mParticle failed to start: secret is required.");
        }
        if (installType == null) {
            throw new IllegalArgumentException("mParticle failed to start: installType is required.");
        }
        MParticle.getInstance(context.getApplicationContext(), apiKey, secret, installType);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     * <p/>
     * The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     *
     * @param context     Required reference to a Context object
     * @param installType Specify whether this is a new install or an upgrade, or let mParticle detect
     * @see com.mparticle.MParticle.InstallType
     */

    public static void start(Context context, InstallType installType) {
        if (context == null) {
            throw new IllegalArgumentException("mParticle failed to start: context is required.");
        }
        MParticle.getInstance(context.getApplicationContext(), null, null, installType);
    }

    /**
     * Initialize or return a thread-safe instance of the mParticle SDK, specifying the API credentials to use. If this
     * or any other {@link #getInstance()} has already been called in the application's lifecycle, the
     * API credentials will be ignored and the current instance will be returned.
     *
     * @param context the Activity that is creating the instance
     * @param apiKey  the API key for your account
     * @param secret  the API secret for your account
     * @return An instance of the mParticle SDK configured with your API key
     */
    private static MParticle getInstance(Context context, String apiKey, String secret, InstallType installType) {
        if (instance == null) {
            synchronized (MParticle.class) {
                if (instance == null) {
                    if (PackageManager.PERMISSION_DENIED == context
                            .checkCallingOrSelfPermission(android.Manifest.permission.INTERNET)) {
                        throw new IllegalArgumentException("mParticle requires android.permission.INTERNET permission");
                    }

                    if (!sTimeoutHandlerThread.isAlive()) {
                        sTimeoutHandlerThread.start();
                    }

                    if (null == sPreferences) {
                        sPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
                    }

                    EmbeddedKitManager embeddedKitManager1 = new EmbeddedKitManager(context);
                    ConfigManager appConfigManager = new ConfigManager(context, apiKey, secret, embeddedKitManager1);
                    Context appContext = context.getApplicationContext();

                    Boolean firstRun = sPreferences.getBoolean(PrefKeys.FIRSTRUN + appConfigManager.getApiKey(), true);
                    if (firstRun) {
                        sPreferences.edit().putBoolean(PrefKeys.FIRSTRUN + appConfigManager.getApiKey(), false).commit();
                    }

                    MessageManager messageManager = new MessageManager(appContext, appConfigManager);


                    instance = new MParticle(appContext, messageManager, appConfigManager, embeddedKitManager1);
                    messageManager.start(appContext, firstRun, installType);

                    if (appConfigManager.getLogUnhandledExceptions()) {
                        instance.enableUncaughtExceptionLogging();
                    }

                    if (appConfigManager.isPushEnabled()) {
                        instance.enablePushNotifications(appConfigManager.getPushSenderId());
                    }
                    if (appConfigManager.isLicensingEnabled()) {
                        instance.performLicenseCheck();
                    }
                    if (appConfigManager.isNetworkPerformanceEnabled()) {
                        instance.beginMeasuringNetworkPerformance();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Retrieve an instance of the MParticle class. {@link #start(android.content.Context)} or {@link #start(android.content.Context, String, String)} must
     * be called prior to this or a {@code java.lang.IllegalStateException} will be thrown.
     *
     * @return An instance of the mParticle SDK configured with your API key
     */
    public static MParticle getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Failed to get MParticle instance, getInstance() called prior to start().");
        }
        return getInstance(null, null, null, null);
    }


    static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, boolean increment) {
        return setCheckedAttribute(attributes, key, value, false, increment);
    }


    static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, Boolean caseInsensitive, boolean increment) {
        if (null == attributes || null == key) {
            return false;
        }
        try {
            if (caseInsensitive) {
                key = findCaseInsensitiveKey(attributes, key);
            }

            if (Constants.LIMIT_ATTR_COUNT == attributes.length() && !attributes.has(key)) {
                ConfigManager.log(LogLevel.ERROR, "Attribute count exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (null != value && value.toString().length() > Constants.LIMIT_ATTR_VALUE) {
                ConfigManager.log(LogLevel.ERROR, "Attribute value length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (key.length() > Constants.LIMIT_ATTR_NAME) {
                ConfigManager.log(LogLevel.ERROR, "Attribute name length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (value == null) {
                value = JSONObject.NULL;
            }
            if (increment){
                String oldValue = attributes.optString(key, "0");
                int oldInt = Integer.parseInt(oldValue);
                value = Integer.toString((Integer)value + oldInt);
            }
            attributes.put(key, value);
        } catch (JSONException e) {
            ConfigManager.log(LogLevel.ERROR, "JSON error processing attributes. Discarding attribute: " + key);
            return false;
        } catch (NumberFormatException nfe){
            ConfigManager.log(LogLevel.ERROR, "Attempted to increment a key that could not be parsed as an integer: " + key);
            return false;
        } catch (Exception e){
            ConfigManager.log(LogLevel.ERROR, "Failed to add attribute: " + e.getMessage());
            return false;
        }
        return true;
    }

    static String findCaseInsensitiveKey(JSONObject jsonObject, String key) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String currentKey = keys.next();
            if (currentKey.equalsIgnoreCase(key)) {
                return currentKey;
            }
        }
        return key;
    }


    Boolean shouldProcessUrl(String url) {
        return mConfigManager.isNetworkPerformanceEnabled() &&
                measuredRequestManager.isUriAllowed(url) && !mEmbeddedKitManager.isEmbeddedKitUri(url);
    }

    void logStateTransition(String transitionType, String currentActivity) {
       logStateTransition(transitionType, currentActivity, 0, 0, null, null, null, 0);
    }

    void logStateTransition(String transitionType, String currentActivity, long previousForegroundTime, long suspendedTime, String dataString, String launchParameters, String launchPackage, int interruptions) {
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();

            mMessageManager.logStateTransition(transitionType,
                    mSessionID,
                    mSessionStartTime,
                    lastNotificationBundle,
                    currentActivity,
                    dataString,
                    launchParameters,
                    launchPackage,
                    previousForegroundTime,
                    suspendedTime,
                    interruptions
                    );
            if (Constants.StateTransitionType.STATE_TRANS_BG.equals(transitionType)) {
                lastNotificationBundle = null;
            }
        }
    }


    /**
     * Track that an Activity has started. Should only be called within the onStart method of your Activities,
     * and is only necessary for pre-API level 14 devices. Not necessary to use if your Activity extends an mParticle
     * Activity implementation.
     *
     * @see com.mparticle.activity.MPActivity
     * @see com.mparticle.activity.MPListActivity
     */
    public void activityStarted(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mConfigManager.getSendOoEvents()) {
                ensureActiveSession();
                mAppStateManager.onActivityStarted(activity, 0);
            }
        }
    }

    /**
     * Track that an Activity has stopped. Should only be called within the onStop method of your Activities,
     * and is only necessary for pre-API level 14 devices. Not necessary to use if your Activity extends an mParticle
     * Activity implementation.
     *
     * @see com.mparticle.activity.MPActivity
     * @see com.mparticle.activity.MPListActivity
     */
    public void activityStopped(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mConfigManager.getSendOoEvents()) {
                ensureActiveSession();
                mAppStateManager.onActivityStopped(activity, 0);
            }
        }
    }

    /**
     * Explicitly begin tracking a new session. Usually not necessary unless {@link #endSession()} is also used.
     */
    public void beginSession() {
        if (mConfigManager.getSendOoEvents()) {
            endSession();
            newSession();
        }
    }

    /**
     * Explicitly terminate the current user's session.
     */
    public void endSession() {
        if (mConfigManager.getSendOoEvents()) {
            long sessionEndTime = System.currentTimeMillis();
            endSession(sessionEndTime);
        }
    }

    private void endSession(long sessionEndTime) {
        ConfigManager.log(LogLevel.DEBUG, "Ended session");

        // mMessageManager.stopSession(mSessionID, sessionEndTime, sessionEndTime - mSessionStartTime);
        mMessageManager.endSession(mSessionID, sessionEndTime, sessionEndTime - mSessionStartTime);
        // reset agent to unstarted state
        mSessionStartTime = 0;
        mSessionID = "";
    }


    boolean isSessionActive() {
        return mSessionStartTime == 0;
    }

    private void ensureActiveSession() {
        //    checkSessionTimeout();
        mLastEventTime = System.currentTimeMillis();
        if (0 == mSessionStartTime) {
            newSession();
        }
    }

    /**
     * Check current session timeout and end the session if needed. Will not start a new session.
     */
    Boolean checkSessionTimeout() {
        long now = System.currentTimeMillis();
        if (0 != mSessionStartTime &&
                mAppStateManager.isBackgrounded() &&
                (mConfigManager.getSessionTimeout() < now - mLastEventTime)) {
            ConfigManager.log(LogLevel.DEBUG, "Session timed out");

            endSession(mLastEventTime);
            return true;
        }
        return false;
    }

    /**
     * Creates a new session and generates the start-session message.
     */
    private void newSession() {
        mLastEventTime = mSessionStartTime = System.currentTimeMillis();
        mSessionID = UUID.randomUUID().toString();
        mEventCount = 0;
        mSessionAttributes = new JSONObject();
        mMessageManager.startSession(mSessionID, mSessionStartTime);
        mTimeoutHandler.sendEmptyMessageDelayed(0, mConfigManager.getSessionTimeout());
        ConfigManager.log(LogLevel.DEBUG, "Started new session");
    }

    /**
     * Force upload all queued messages to the mParticle server.
     */
    public void upload() {
        mMessageManager.doUpload();
    }

    /**
     * Manually set the install referrer. This will replace any install referrer that was
     * automatically retrieved upon installation from Google Play.
     */
    public void setInstallReferrer(String referrer) {
        sPreferences.edit().putString(PrefKeys.INSTALL_REFERRER, referrer).commit();
        ConfigManager.log(LogLevel.DEBUG, "Set installReferrer: ", referrer);
    }

    /**
     * Logs an event
     *
     * @param eventName the name of the event to be tracked (required not null)
     * @param eventType the type of the event to be tracked
     */
    public void logEvent(String eventName, EventType eventType) {
        logEvent(eventName, eventType, null, 0, null);
    }

    /**
     * Logs an event
     *
     * @param eventName the name of the event to be tracked (required not null)
     * @param eventType the type of the event to be tracked
     * @param category  the Google Analytics category with which to associate this event
     */
    public void logEvent(String eventName, EventType eventType, String category) {
        logEvent(eventName, eventType, null, 0, category);
    }

    /**
     * Logs an event
     *
     * @param eventName   the name of the event to be tracked  (required not null)
     * @param eventType   the type of the event to be tracked
     * @param eventLength the duration of the event in milliseconds
     */
    public void logEvent(String eventName, EventType eventType, long eventLength) {
        logEvent(eventName, eventType, null, eventLength);
    }

    /**
     * Log an event with data attributes
     *
     * @param eventName the name of the event to be tracked  (required not null)
     * @param eventType the type of the event to be tracked
     * @param eventInfo a Map of data attributes
     */
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo) {
        logEvent(eventName, eventType, eventInfo, 0);
    }

    /**
     * Log an event with data attributes
     *
     * @param eventName the name of the event to be tracked  (required not null)
     * @param eventType the type of the event to be tracked
     * @param eventInfo a Map of data attributes
     * @param category  the Google Analytics category with which to associate this event
     */
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, String category) {
        logEvent(eventName, eventType, eventInfo, 0, category);
    }

    /**
     * Log an event with data attributes
     *
     * @param eventName   the name of the event to be tracked  (required not null)
     * @param eventType   the type of the event to be tracked
     * @param eventInfo   a Map of data attributes to associate with the event
     * @param eventLength the duration of the event in milliseconds
     */
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, long eventLength) {
        logEvent(eventName, eventType, eventInfo, eventLength, null);
    }

    /**
     * Log an event with data attributes
     *
     * @param eventName   the name of the event to be tracked  (required not null)
     * @param eventType   the type of the event to be tracked
     * @param eventInfo   a Map of data attributes to associate with the event
     * @param eventLength the duration of the event in milliseconds
     * @param category    the Google Analytics category with which to associate this event
     */
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, long eventLength, String category) {
        if (null == eventName) {
            ConfigManager.log(LogLevel.ERROR, "eventName is required for logEvent");
            return;
        }

        if (eventName.length() > Constants.LIMIT_NAME) {
            ConfigManager.log(LogLevel.ERROR, "The event name was too long. Discarding event.");
            return;
        }
        ensureActiveSession();
        if (checkEventLimit()) {
            if (category != null) {
                if (eventInfo == null) {
                    eventInfo = new HashMap<String, String>();
                }
                eventInfo.put(Constants.MessageKey.EVENT_CATEGORY, category);
            }
            JSONObject eventDataJSON = enforceAttributeConstraints(eventInfo);
            if (mConfigManager.getSendOoEvents()) {
                mMessageManager.logEvent(mSessionID, mSessionStartTime, mLastEventTime, eventName, eventType, eventDataJSON, eventLength, mAppStateManager.getCurrentActivity());

                if (null == eventDataJSON) {
                    ConfigManager.log(LogLevel.DEBUG, "Logged event: ", eventName);
                } else {
                    ConfigManager.log(LogLevel.DEBUG, "Logged event: ", eventName, " with data ", eventDataJSON.toString());
                }


            }
            mEmbeddedKitManager.logEvent(eventType, eventName, eventDataJSON);
        }
    }

    /**
     * Logs an increase in the lifetime value of a user. This will signify an increase
     * in the revenue assigned to this user for service providers that support revenue tracking.
     *
     * @param valueIncreased The currency value by which to increase the current user's LTV (required)
     * @param eventName      An event name to be associated with this increase in LTV (optional)
     * @param contextInfo    An MPProduct or any set of data to associate with this increase in LTV (optional)
     */
    public void logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        if (valueIncreased == null) {
            throw new IllegalArgumentException("ValueIncreased must not be null.");
        }
        if (contextInfo == null) {
            contextInfo = new HashMap<String, String>();
        }
        contextInfo.put(Constants.MessageKey.RESERVED_KEY_LTV, valueIncreased.toPlainString());
        contextInfo.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_LTV);
        logEvent(eventName == null ? "Increase LTV" : eventName, EventType.Transaction, contextInfo);
    }


    /**
     * Log an E-Commerce related event associated to a product
     *
     * @param event
     * @param product
     * @see com.mparticle.MPProduct
     * @see com.mparticle.MPProduct.Event
     */
    public void logProductEvent(final MPProduct.Event event, MPProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("MPProduct is required.");
        }
        if (product.isEmpty()) {
            throw new IllegalArgumentException("MPProduct data was null, please check that the MPProduct was built properly.");
        }
        if (event == null) {
            throw new IllegalArgumentException("MPProduct.EVENT is required.");
        }
        boolean purchaseEvent = false;
        switch (event) {
            case VIEW:
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_VIEW);
                break;
            case REMOVE_FROM_CART:
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_REMOVE_FROM_CART);
                break;
            case ADD_TO_CART:
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_ADD_TO_CART);
                break;
            case ADD_TO_WISHLIST:
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_ADD_TO_WISHLIST);
                break;
            case REMOVE_FROM_WISHLIST:
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_REMOVE_FROM_WISHLIST);
                break;
            case PURCHASE:
                purchaseEvent = true;
                product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE);
                break;
        }

        ensureActiveSession();
        if (checkEventLimit()) {
            JSONObject transactionJson = enforceAttributeConstraints(product);
            mMessageManager.logEvent(mSessionID, mSessionStartTime, mLastEventTime, event.toString(), EventType.Transaction, transactionJson, 0, mAppStateManager.getCurrentActivity());
            ConfigManager.log(LogLevel.DEBUG, "Logged product event with data: ", product.toString());
        }
        if (purchaseEvent) {
            mEmbeddedKitManager.logTransaction(product);
        }

    }

    /**
     * Logs an e-commerce transaction event
     *
     * @param product (required not null)
     * @see com.mparticle.MPProduct
     */
    public void logTransaction(MPProduct product) {
        logProductEvent(MPProduct.Event.PURCHASE, product);
    }

    void logScreen(String screenName, Map<String, String> eventData, Boolean started) {
        if (null == screenName) {
            ConfigManager.log(LogLevel.ERROR, "screenName is required for logScreen");
            return;
        }
        if (screenName.length() > Constants.LIMIT_NAME) {
            ConfigManager.log(LogLevel.ERROR, "The screen name was too long. Discarding event.");
            return;
        }
        ensureActiveSession();
        if (checkEventLimit()) {
            JSONObject eventDataJSON = enforceAttributeConstraints(eventData);
            if (mConfigManager.getSendOoEvents()) {
                mMessageManager.logScreen(mSessionID, mSessionStartTime, mLastEventTime, screenName, eventDataJSON, started);

                if (null == eventDataJSON) {
                    ConfigManager.log(LogLevel.DEBUG, "Logged screen: ", screenName);
                } else {
                    ConfigManager.log(LogLevel.DEBUG, "Logged screen: ", screenName, " with data ", eventDataJSON.toString());
                }

            }
            mEmbeddedKitManager.logScreen(screenName, eventDataJSON);
        }
    }

    /**
     * Logs a screen view event
     *
     * @param screenName the name of the screen to be tracked
     */
    public void logScreen(String screenName) {
        logScreen(screenName, null);
    }

    /**
     * Logs a screen view event
     *
     * @param screenName the name of the screen to be tracked
     * @param eventData  a Map of data attributes to associate with this screen view
     */
    public void logScreen(String screenName, Map<String, String> eventData) {
        logScreen(screenName, eventData, true);
    }

    /**
     * Leave a breadcrumb to be included with error and exception logging, as well as
     * with regular session events.
     *
     * @param breadcrumb
     */
    public void leaveBreadcrumb(String breadcrumb) {
        if (mConfigManager.getSendOoEvents()) {
            if (null == breadcrumb) {
                ConfigManager.log(LogLevel.ERROR, "breadcrumb is required for leaveBreadcrumb");
                return;
            }
            if (breadcrumb.length() > Constants.LIMIT_NAME) {
                ConfigManager.log(LogLevel.ERROR, "The breadcrumb name was too long. Discarding event.");
                return;
            }
            ensureActiveSession();
            mMessageManager.logBreadcrumb(mSessionID, mSessionStartTime, mLastEventTime, breadcrumb);
            ConfigManager.log(LogLevel.DEBUG, "Logged breadcrumb: " + breadcrumb);

        }
    }

    /**
     * Logs an error event
     *
     * @param message the name of the error event to be tracked
     */
    public void logError(String message) {
        logError(message, null);
    }

    /**
     * Logs an error event
     *
     * @param message   the name of the error event to be tracked
     * @param eventData a Map of data attributes to associate with this error
     */
    public void logError(String message, Map<String, String> eventData) {
        if (mConfigManager.getSendOoEvents()) {
            if (null == message) {
                ConfigManager.log(LogLevel.ERROR, "message is required for logErrorEvent");
                return;
            }
            ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(mSessionID, mSessionStartTime, mLastEventTime, message, null, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged error with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString())
                );
            }
        }
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived) {
        logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, null);
    }

    void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString) {
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            if (checkEventLimit()) {
                mMessageManager.logNetworkPerformanceEvent(mSessionID, mSessionStartTime, startTime, method, url, length, bytesSent, bytesReceived, requestString);
            }
        }
    }

    private void initNetworkMonitoring() {

        try {
            SocketImpl socket = (SocketImpl) MPUtility.getAccessibleObject(MPUtility.getAccessibleField(Socket.class, SocketImpl.class), new Socket());
            SocketImplFactory factory = new MPSocketImplFactory(socket.getClass());
            Socket.setSocketImplFactory(factory);
        } catch (Error e) {
            ConfigManager.log(LogLevel.WARNING, "Error initiating network performance monitoring: " + e.getMessage());

        } catch (Exception e) {
            ConfigManager.log(LogLevel.WARNING, "Exception initiating network performance monitoring: " + e.getMessage());
        }
        try {
            SSLSocketFactory currentSocketFactory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
            javax.net.ssl.SSLSocketFactory innerFactory = (javax.net.ssl.SSLSocketFactory) MPUtility.getAccessibleField(org.apache.http.conn.ssl.SSLSocketFactory.class, javax.net.ssl.SSLSocketFactory.class).get(currentSocketFactory);
            MPSSLSocketFactory wrapperFactory = new MPSSLSocketFactory(innerFactory);
            MPUtility.getAccessibleField(org.apache.http.conn.ssl.SSLSocketFactory.class, javax.net.ssl.SSLSocketFactory.class).set(currentSocketFactory, wrapperFactory);
        } catch (Error e) {
            ConfigManager.log(LogLevel.WARNING, "Error initiating network performance monitoring: " + e.getMessage());

        } catch (Exception e) {
            ConfigManager.log(LogLevel.WARNING, "Exception initiating network performance monitoring: " + e.getMessage());

        }

        try {
            MPUrlStreamHandlerFactory factory = new MPUrlStreamHandlerFactory();

            try {
                factory.createURLStreamHandler("https");
                factory.createURLStreamHandler("http");
            } catch (IllegalArgumentException iae) {
                throw iae;
            } catch (Exception e) {

            }


            URL.setURLStreamHandlerFactory(factory);
        } catch (Error e) {
            ConfigManager.log(LogLevel.WARNING, "Error initiating network performance monitoring: " + e.getMessage());
        } catch (Exception e) {
            ConfigManager.log(LogLevel.WARNING, "Exception initiating network performance monitoring: " + e.getMessage());
        }

        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(new MPSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory()));
        } catch (Error e) {
            ConfigManager.log(LogLevel.WARNING, "Error initiating network performance monitoring: ", e.getMessage());

        } catch (Exception e) {
            ConfigManager.log(LogLevel.WARNING, "Exception initiating network performance monitoring: " + e.getMessage());
        }
        measuredRequestManager.setEnabled(true);

    }

    /**
     * Begin measuring network performance. This method only needs to be called one time during the runtime of an application.
     */
    public void beginMeasuringNetworkPerformance() {
        if (!measuredRequestManager.getEnabled()) {
            mConfigManager.setNetworkingEnabled(true);
            initNetworkMonitoring();
        }
    }


    /**
     * Stop measuring network performance.
     */
    public void endMeasuringNetworkPerformance() {
        if (measuredRequestManager.getEnabled()) {
            measuredRequestManager.setEnabled(false);
            mConfigManager.setNetworkingEnabled(false);
            try {
                javax.net.ssl.SSLSocketFactory current = HttpsURLConnection.getDefaultSSLSocketFactory();
                if (current instanceof MPSSLSocketFactory) {
                    HttpsURLConnection.setDefaultSSLSocketFactory(((MPSSLSocketFactory) current).delegateFactory);
                }
            } catch (Exception e) {
                ConfigManager.log(LogLevel.WARNING, "Error stopping network performance monitoring: ", e.getMessage());
            }
        }
    }

    /**
     * Exclude the given URL substring from network measurement tracking. This method may be called repeatedly to add
     * multiple excluded URLs.
     *
     * @param url
     * @see #resetNetworkPerformanceExclusionsAndFilters()
     */
    public void excludeUrlFromNetworkPerformanceMeasurement(String url) {
        measuredRequestManager.addExcludedUrl(url);
    }

    /**
     * Specify a filter for query strings that should be logged. Call this method repeatedly to specify
     * multiple query string filters. By default, query strings will be removed from all measured URLs.
     *
     * @param filter
     * @see #resetNetworkPerformanceExclusionsAndFilters()
     */
    public void addNetworkPerformanceQueryOnlyFilter(String filter) {
        measuredRequestManager.addQueryStringFilter(filter);
    }

    /**
     * Remove all previously excluded URLs and allowed query filters. After this, all URLs will be
     * measured, and all query strings will be redacted when logging measurements.
     *
     * @see #excludeUrlFromNetworkPerformanceMeasurement(String)
     * @see #addNetworkPerformanceQueryOnlyFilter(String)
     */
    public void resetNetworkPerformanceExclusionsAndFilters() {
        measuredRequestManager.resetFilters();
    }

    /**
     * Logs an Exception
     *
     * @param exception an Exception
     */
    public void logException(Exception exception) {
        logException(exception, null, null);
    }

    /**
     * Logs an Exception
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     */
    public void logException(Exception exception, Map<String, String> eventData) {
        logException(exception, eventData, null);
    }

    /**
     * Logs an Exception
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     * @param message   the name of the error event to be tracked
     */
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(mSessionID, mSessionStartTime, mLastEventTime, message, exception, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged exception with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString()) +
                                " with exception: " + (exception == null ? "<none>" : exception.getMessage())
                );
            }
        }
    }

    void logUnhandledError(Throwable t) {
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            mMessageManager.logErrorEvent(mSessionID, mSessionStartTime, mLastEventTime, t != null ? t.getMessage() : null, t, null, false);
            //we know that the app is about to crash and therefore exit
            logStateTransition(Constants.StateTransitionType.STATE_TRANS_EXIT, mAppStateManager.getCurrentActivity());
            endSession(System.currentTimeMillis());
        }
    }

    /**
     * Enables location tracking given a provider and update frequency criteria. The provider must
     * be available and the correct permissions must have been requested within your application's manifest XML file.
     *
     * @param provider    the provider key
     * @param minTime     the minimum time (in milliseconds) to trigger an update
     * @param minDistance the minimum distance (in meters) to trigger an update
     */
    public void enableLocationTracking(String provider, long minTime, long minDistance) {
        if (mConfigManager.getSendOoEvents()) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(provider)) {
                    ConfigManager.log(LogLevel.ERROR, "That requested location provider is not available");
                    return;
                }

                if (null == mLocationListener) {
                    mLocationListener = new MParticleLocationListener(this);
                } else {
                    // clear the location listener, so it can be added again
                    locationManager.removeUpdates(mLocationListener);
                }
                locationManager.requestLocationUpdates(provider, minTime, minDistance, mLocationListener);
            } catch (SecurityException e) {
                ConfigManager.log(LogLevel.ERROR, "The app must require the appropriate permissions to track location using this provider");
            }
        }
    }

    /**
     * Disables any mParticle location tracking that had been started
     */
    public void disableLocationTracking() {
        if (null != mLocationListener) {
            LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
    }

    /**
     * Set the current location of the active session.
     *
     * @param location
     */
    public void setLocation(Location location) {
        mMessageManager.setLocation(location);
        mEmbeddedKitManager.setLocation(location);
    }

    /**
     * Set a single <i>session</i> attribute. The attribute will combined with any existing session attributes.
     *
     * @param key   the attribute key
     * @param value the attribute value. This value will be converted to its String representation as dictated by its <code>toString()</code> method.
     */
    public void setSessionAttribute(String key, Object value) {
        if (key == null){
            ConfigManager.log(LogLevel.WARNING, "setSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (value != null){
            value = value.toString();
        }
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Set session attribute: " + key + "=" + value);

            if (setCheckedAttribute(mSessionAttributes, key, value, true, false)) {
                mMessageManager.setSessionAttributes(mSessionID, mSessionAttributes);
            }
        }
    }

    /**
     * Increment a single <i>session</i> attribute. If the attribute does not exist, it will be added as a new attribute.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public void incrementSessionAttribute(String key, int value) {
        if (key == null){
            ConfigManager.log(LogLevel.WARNING, "incrementSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Incrementing session attribute: " + key + "=" + value);


            if (setCheckedAttribute(mSessionAttributes, key, value, true, true)) {
                mMessageManager.setSessionAttributes(mSessionID, mSessionAttributes);
            }
        }
    }

    /**
     * Signal to the mParticle platform that the current user has logged out. As of 1.6.x of
     * the SDK, this function is only used as a signaling mechanism to server providers
     * that support an explicit logout. As of 1.6.x, after calling this method, all user attributes
     * and identities will stay the same.
     */
    public void logout() {
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Logging out.");


            mMessageManager.logProfileAction(Constants.ProfileActions.LOGOUT, mSessionID, mSessionStartTime);
        }
        mEmbeddedKitManager.logout();
    }

    /**
     * Set a single <i>user</i> attribute. The attribute will combined with any existing user attributes.
     *
     * @param key   the attribute key
     * @param value the attribute value. This value will be converted to its String representation as dictated by its <code>toString()</code> method.
     */
    public void setUserAttribute(String key, Object value) {
        if (key == null){
            ConfigManager.log(LogLevel.WARNING, "setUserAttribute called with null key. Ignoring...");
            return;
        }
        if (value != null) {
            value = value.toString();
            ConfigManager.log(LogLevel.DEBUG, "Set user attribute: " + key + " with value " + value);
        } else {
            ConfigManager.log(LogLevel.DEBUG, "Set user attribute: " + key);
        }

        if (setCheckedAttribute(mUserAttributes, key, value, false)) {
            sPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).commit();
            mEmbeddedKitManager.setUserAttributes(mUserAttributes);
        }

    }

    /**
     * Increment a single <i>user</i> attribute. If the attribute does not already exist, a new one will be created.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public void incrementUserAttribute(String key, int value) {
        if (key == null){
            ConfigManager.log(LogLevel.WARNING, "incrementUserAttribute called with null key. Ignoring...");
            return;
        }
        ConfigManager.log(LogLevel.DEBUG, "Incrementing user attribute: " + key + " with value " + value);

        if (setCheckedAttribute(mUserAttributes, key, value, true)) {
            sPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).commit();
            mEmbeddedKitManager.setUserAttributes(mUserAttributes);
        }

    }

    /**
     * Remove a <i>user</i> attribute
     *
     * @param key the key of the attribute
     */
    public void removeUserAttribute(String key) {
        if (key != null) {
            ConfigManager.log(LogLevel.DEBUG, "Removing user attribute: " + key);
        }
        if (mUserAttributes.has(key) || mUserAttributes.has(findCaseInsensitiveKey(mUserAttributes, key))) {
            mUserAttributes.remove(key);
            sPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).commit();
            attributeRemoved(key);
            mEmbeddedKitManager.removeUserAttribute(key);
        }
    }

    private void attributeRemoved(String key) {
        String serializedJsonArray = sPreferences.getString(PrefKeys.USER_ATTRS + mApiKey, null);
        JSONArray deletedAtributes;
        try {
            deletedAtributes = new JSONArray(serializedJsonArray);
        } catch (Exception jse) {
            deletedAtributes = new JSONArray();
        }
        deletedAtributes.put(key);

        sPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, deletedAtributes.toString()).commit();
    }

    /**
     * Set a single user tag, it will be combined with any existing tags.
     *
     * @param tag a tag assigned to a user
     */
    public void setUserTag(String tag) {
        setUserAttribute(tag, null);
    }

    /**
     * Remove a user tag.
     *
     * @param tag a tag that was previously added
     */
    public void removeUserTag(String tag) {
        removeUserAttribute(tag);
    }


    /**
     * Set the current user's identity
     *
     * @param id
     * @param identityType
     */

    public void setUserIdentity(String id, IdentityType identityType) {
        if (id != null && id.length() > 0) {
            ConfigManager.log(LogLevel.DEBUG, "Setting user identity: " + id);

            if (null != id && id.length() > Constants.LIMIT_ATTR_VALUE) {
                ConfigManager.log(LogLevel.WARNING, "Id value length exceeds limit. Discarding id: " + id);
                return;
            }

            mEmbeddedKitManager.setUserIdentity(id, identityType);

            try {
                int index = -1;
                for (int i = 0; i < mUserIdentities.length(); i++) {
                    if (mUserIdentities.getJSONObject(i).get(MessageKey.IDENTITY_NAME).equals(identityType.value)) {
                        index = i;
                        break;
                    }
                }

                JSONObject newObject = new JSONObject();
                newObject.put(MessageKey.IDENTITY_NAME, identityType.value);
                newObject.put(MessageKey.IDENTITY_VALUE, id);

                if (index >= 0) {
                    newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, mUserIdentities.getJSONObject(index).optLong(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis()));
                    newObject.put(MessageKey.IDENTITY_FIRST_SEEN, false);
                    mUserIdentities.put(index, newObject);
                } else {
                    newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis());
                    newObject.put(MessageKey.IDENTITY_FIRST_SEEN, true);
                    mUserIdentities.put(newObject);
                }

            } catch (JSONException e) {
                ConfigManager.log(LogLevel.ERROR, "Error setting identity: " + id);
                return;
            }

            sPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, mUserIdentities.toString()).commit();
        }
    }

    /**
     * Remove an identity matching this id
     * <p/>
     * Note: this will only remove the *first* matching id
     *
     * @param id the id to remove
     */
    public void removeUserIdentity(String id) {
        if (id != null && id.length() > 0 && mUserIdentities != null) {
            try {
                int indexToRemove = -1;
                for (int i = 0; i < mUserIdentities.length(); i++) {
                    if (mUserIdentities.getJSONObject(i).getString(MessageKey.IDENTITY_VALUE).equals(id)) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        KitKatHelper.remove(mUserIdentities, indexToRemove);
                    } else {
                        JSONArray newIdentities = new JSONArray();
                        for (int i = 0; i < mUserIdentities.length(); i++) {
                            if (i != indexToRemove) {
                                newIdentities.put(mUserIdentities.get(i));
                            }
                        }
                    }
                    sPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, mUserIdentities.toString()).commit();

                }
            } catch (JSONException jse) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Error removing identity: " + id);
            }
            mEmbeddedKitManager.removeUserIdentity(id);
        }
    }

    /**
     * Get the current opt-out status for the application.
     *
     * @return the opt-out status
     */
    public Boolean getOptOut() {
        return mConfigManager.getOptedOut();
    }

    /**
     * Control the opt-in/opt-out status for the application.
     *
     * @param optOutStatus set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(Boolean optOutStatus) {
        if (optOutStatus != mConfigManager.getOptedOut()) {
            if (!optOutStatus) {
                ensureActiveSession();
            }
            mMessageManager.optOut(mSessionID, mSessionStartTime, System.currentTimeMillis(), optOutStatus);
            if (optOutStatus && mSessionStartTime > 0) {
                endSession();
            }

            mConfigManager.setOptOut(optOutStatus);

            ConfigManager.log(LogLevel.DEBUG, "Set opt-out: " + optOutStatus);
        }
    }

    /**
     * Force the SDK into either Production or Development mode. See {@link com.mparticle.MParticle.Environment}
     * for implications of each mode. The SDK automatically determines which mode it should be in depending
     * on the signing and the DEBUGGABLE flag of your application's AndroidManifest.xml, so this method should
     * typically not be used.
     * <p/>
     * This method can however be useful while you're testing a release-signed version of your application, and you have *not* set the
     * debuggable flag in your AndroidManifest.xml. In this case, you can force the SDK into development mode to prevent sending
     * your test usage/data as production data. It's crucial, however, that prior to submission to Google Play that you ensure
     * you are no longer forcing development mode.
     *
     * @param environment
     */
    public void setEnvironment(Environment environment) {
        if (environment != null) {
            if (environment.equals(Environment.Development)) {
                if (mConfigManager.isDebugEnvironment()) {
                    Log.w(Constants.LOG_TAG, "Forcing environment to DEVELOPMENT, but your app is already debuggable and hence in DEVELOPMENT mode - did you mean to call forceEnvironment(Environment.Production) instead?");
                } else {
                    Log.w(Constants.LOG_TAG, "Forcing environment to DEVELOPMENT on a production app! Be careful, be sure not to do this in an application that you submit to Google Play.");
                }
            } else if (environment.equals(Environment.Production)) {
                if (mConfigManager.isDebugEnvironment()) {
                    Log.w(Constants.LOG_TAG, "Forcing environment to PRODUCTION on a debuggable app. Be careful, you are now in PRODUCTION and any test event data will be mixed with live event data!");
                } else {
                    Log.w(Constants.LOG_TAG, "Forcing environment to PRODUCTION, but your app is already in PRODUCTION mode - did you mean to call forceEnvironment(Environment.Development) instead?");
                }
            }
        }
        mConfigManager.setForceEnvironment(environment);
    }

    /**
     * Get the current Environment that the SDK has interpreted. Will never return AutoDetect.
     *
     * @return
     */
    public Environment getEnvironment() {
        return mConfigManager.getEnvironment();
    }

    /**
     * Set the upload interval period to control how frequently uploads occur.
     *
     * @param uploadInterval the number of seconds between uploads
     */
    public void setUploadInterval(int uploadInterval) {
        mConfigManager.setUploadInterval(uploadInterval);
    }

    /**
     * Enable mParticle exception handling to automatically log events on uncaught exceptions
     */
    public void enableUncaughtExceptionLogging() {
        if (null == mExHandler) {
            UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (!(currentUncaughtExceptionHandler instanceof ExceptionHandler)) {
                mExHandler = new ExceptionHandler(mMessageManager, currentUncaughtExceptionHandler);
                Thread.setDefaultUncaughtExceptionHandler(mExHandler);
            }
        }
    }

    /**
     * Disables mParticle exception handling and restores the original UncaughtExceptionHandler
     */
    public void disableUncaughtExceptionLogging() {
        if (null != mExHandler) {
            UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (currentUncaughtExceptionHandler instanceof ExceptionHandler) {
                Thread.setDefaultUncaughtExceptionHandler(mExHandler.getOriginalExceptionHandler());
                mExHandler = null;
            }
        }
    }

    /**
     * Register the application for GCM notifications
     *
     * @param senderId the SENDER_ID for the application
     */
    public void enablePushNotifications(String senderId) {
        if (!MPUtility.isServiceAvailable(mAppContext, MPService.class)){
            ConfigManager.log(LogLevel.ERROR, "Push is enabled but you have not added <service android:name=\"com.mparticle.MPService\" /> to the <application> section of your AndroidManifest.xml");
        }else if (!MPUtility.checkPermission(mAppContext, "com.google.android.c2dm.permission.RECEIVE")){
            ConfigManager.log(LogLevel.ERROR, "Attempted to enable push notifications without required permission: ", "\"com.google.android.c2dm.permission.RECEIVE\"");
        }else {
            mConfigManager.setPushSenderId(senderId);
            PushRegistrationHelper.enablePushNotifications(mAppContext, senderId);
        }
    }

    /**
     * Unregister the application for GCM notifications
     */
    public void disablePushNotifications() {
        PushRegistrationHelper.disablePushNotifications(mAppContext);
    }

    /**
     * Enable the default notification sound for push notifications. This is a user preference that will be persisted across
     * application sessions.
     *
     * @param enabled
     */
    public void setNotificationSoundEnabled(Boolean enabled) {
        mConfigManager.setPushSoundEnabled(enabled);
    }

    /**
     * Enable the default notification vibration for push notifications. This is a user preference that will be persisted across
     * application sessions.
     *
     * @param enabled
     */
    public void setNotificationVibrationEnabled(Boolean enabled) {
        mConfigManager.setPushVibrationEnabled(enabled);
    }

    void clearPushNotificationId() {
        PushRegistrationHelper.clearPushRegistrationId(mAppContext);
        mMessageManager.setPushRegistrationId(mSessionID, mSessionStartTime, System.currentTimeMillis(), null, true);
    }

    void setPushRegistrationId(String registrationId) {
        PushRegistrationHelper.storeRegistrationId(mAppContext, registrationId);
        mMessageManager.setPushRegistrationId(mSessionID, mSessionStartTime, System.currentTimeMillis(), registrationId, true);
    }

    /**
     * This method checks the event count is below the limit and increments the event count. A
     * warning is logged if the limit has been reached.
     *
     * @return true if event count is below limit
     */
    private Boolean checkEventLimit() {
        if (mEventCount < Constants.EVENT_LIMIT) {
            mEventCount++;
            return true;
        } else {
            ConfigManager.log(MParticle.LogLevel.WARNING, "The event limit has been exceeded for this session.");
            return false;
        }
    }

    /**
     * This method makes sure the constraints on event attributes are enforced. A JSONObject version
     * of the attributes is return with data that exceeds the limits removed. NOTE: Non-string
     * attributes are not converted to strings, currently.
     *
     * @param attributes the user-provided JSONObject
     * @return a cleansed copy of the JSONObject
     */
    JSONObject enforceAttributeConstraints(Map<String, String> attributes) {
        if (null == attributes) {
            return null;
        }
        JSONObject checkedAttributes = new JSONObject();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setCheckedAttribute(checkedAttributes, key, value, false);
        }
        return checkedAttributes;
    }

    void performLicenseCheck() {
        String deviceId = Settings.Secure.getString(mAppContext.getContentResolver(), Settings.Secure.ANDROID_ID);

        MPLicenseCheckerCallback licenseCheckerCallback = new MPLicenseCheckerCallback();

        LicenseChecker checker = new LicenseChecker(
                mAppContext, new ServerManagedPolicy(mAppContext,
                new AESObfuscator(Constants.LICENSE_CHECK_SALT, mAppContext.getPackageName(), deviceId)),
                mConfigManager.getLicenseKey()
        );
        checker.checkAccess(licenseCheckerCallback);
    }

    /**
     * Performs a license check to ensure that the application
     * was downloaded and/or purchased from Google Play and not "pirated" or "side-loaded".
     * <p/>
     * Optionally use the licensingCallback to allow or disallow access to features of your application.
     *
     * @param encodedPublicKey  GBase64-encoded RSA public key of your application
     * @param policy            <b>Optional</b> {@link Policy}, will default to {@link ServerManagedPolicy}
     * @param licensingCallback <b>Optional</b> {@link LicenseCheckerCallback} callback for licensing checking
     */
    private void performLicenseCheck(String encodedPublicKey, Policy policy, LicenseCheckerCallback licensingCallback) {
        if (encodedPublicKey == null || encodedPublicKey.length() == 0) {
            throw new IllegalArgumentException("LicenseKey null or invalid.");
        }

        if (licensingCallback == null) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "No licensing callback specified, using MParticle default.");
        }

        clientLicensingCallback = licensingCallback;

        if (policy == null) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "No policy specified, using default ServerManagedPolicy");
            String deviceId = Settings.Secure.getString(mAppContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            policy = new ServerManagedPolicy(mAppContext,
                    new AESObfuscator(Constants.LICENSE_CHECK_SALT, mAppContext.getPackageName(), deviceId));
        }

        MPLicenseCheckerCallback licenseCheckerCallback = new MPLicenseCheckerCallback();
        LicenseChecker checker = new LicenseChecker(
                mAppContext, policy, encodedPublicKey);
        checker.checkAccess(licenseCheckerCallback);
    }

    /**
     * Retrieves the current setting of automatic screen tracking.
     *
     * @return The current setting of automatic screen tracking.
     */

    public Boolean isAutoTrackingEnabled() {
        return mConfigManager.isAutoTrackingEnabled();
    }

    /**
     * Retrieves the current session timeout setting in seconds
     *
     * @return The current session timeout setting in seconds
     */
    public int getSessionTimeout() {
        return mConfigManager.getSessionTimeout() / 1000;
    }

    /**
     * Set the user session timeout interval.
     * <p/>
     * A session has ended once the application has been in the background for more than this timeout
     *
     * @param sessionTimeout Session timeout in seconds
     */
    public void setSessionTimeout(int sessionTimeout) {
        mConfigManager.setSessionTimeout(sessionTimeout);
    }

    /* package private */ void logNotification(Bundle notificationBundle, String appState) {
        lastNotificationBundle = notificationBundle;
        if (mConfigManager.getSendOoEvents()) {
            ensureActiveSession();
            mMessageManager.logNotification(mSessionID, mSessionStartTime, lastNotificationBundle, appState);
        }
    }

    /**
     * Set the resource ID of the icon to be shown in the notification bar when a notification is received.
     * <p/>
     * By default, the app launcher icon will be shown.
     *
     * @param resId the resource id of a drawable
     */
    public void setPushNotificationIcon(int resId) {
        mConfigManager.setPushNotificationIcon(resId);
    }

    /**
     * Set the resource ID of the title to be shown in the notification bar when a notification is received
     * <p/>
     * By default, the title of the application will be shown.
     *
     * @param resId the resource id of a string
     */
    public void setPushNotificationTitle(int resId) {
        mConfigManager.setPushNotificationTitle(resId);
    }

    public void getUserSegments(long timeout, String endpointId, SegmentListener listener) {
        if (mMessageManager != null && mMessageManager.mUploadHandler != null) {
            mMessageManager.mUploadHandler.fetchSegments(timeout, endpointId, listener);
        }
    }

    /**
     * Instrument a WebView so that the mParticle Javascript SDK may be used within the given website or web app
     *
     * @param webView
     */
    public void registerWebView(WebView webView) {
        if (webView != null) {
            webView.addJavascriptInterface(
                    new MParticleJSInterface(),
                    MParticleJSInterface.INTERFACE_NAME
            );
        }
    }

    /**
     * The log level is used to moderate the amount of messages that are printed
     * by the SDK to the console. Note that while the SDK is in the Production,
     * <i>no log messages will be printed</i>.
     *
     * @see com.mparticle.MParticle.LogLevel
     *
     * @param level
     */
    public void setLogLevel(LogLevel level) {
        if (level != null) {
            mConfigManager.setLogLevel(level);
        }
    }


    /**
     * Event type to use when logging events.
     *
     * @see #logEvent(String, com.mparticle.MParticle.EventType)
     */
    public enum EventType {
        Unknown, Navigation, Location, Search, Transaction, UserContent, UserPreference, Social, Other;

        public String toString() {
            return name();
        }
    }

    /**
     * To be used when initializing MParticle
     *
     * @see #start(android.content.Context, com.mparticle.MParticle.InstallType)
     */
    public enum InstallType {
        /**
         * This is the default value. Using this value will rely on the mParticle SDK to differentiate a new install vs. an upgrade
         */
        AutoDetect,
        /**
         * In the case where your app has never seen this user before.
         */
        KnownInstall,
        /**
         * In the case where you app has seen this user before
         */
        KnownUpgrade;

        public String toString() {
            return name();
        }
    }

    /**
     * Identity type to use when setting the user identity.
     *
     * @see #setUserIdentity(String, com.mparticle.MParticle.IdentityType)
     */

    public enum IdentityType {
        Other(0),
        CustomerId(1),
        Facebook(2),
        Twitter(3),
        Google(4),
        Microsoft(5),
        Yahoo(6),
        Email(7),
        Alias(8);

        private final int value;

        private IdentityType(int value) {
            this.value = value;
        }

        public static IdentityType parseInt(int val) {
            switch (val) {
                case 1:
                    return CustomerId;
                case 2:
                    return Facebook;
                case 3:
                    return Twitter;
                case 4:
                    return Google;
                case 5:
                    return Microsoft;
                case 6:
                    return Yahoo;
                case 7:
                    return Email;
                default:
                    return Other;

            }
        }

        public int getValue() {
            return value;
        }

    }

    /**
     * The Environment in which the SDK and hosting app are running. The method should not usually be necessary - the SDK
     * automatically detects the Environment based on the <code>DEBUGGABLE</code> flag of your application. The <code>DEBUGGABLE</code>  flag of your
     * application will be <code>TRUE</code> when signing with a debug certificate during development, or if you have explicitly set your
     * application to debug within your AndroidManifest.xml.
     *
     * @see {@link #setEnvironment(com.mparticle.MParticle.Environment)} to override this behavior.
     * to override this behavior.
     *
     */
    public enum Environment {
        /**
         * AutoDetect mode (default). In this mode, the SDK will automatically configure itself based on the signing configuration
         * and the <code>DEBUGGABLE</code> flag of your application.
         */
        AutoDetect(0),
        /**
         * Development mode. In this mode, all data from the SDK will be treated as development data, and will be siloed from your
         * production data. Additionally, the SDK will more aggressively upload data to the mParticle platform, to aide in a faster implementation.
         */
        Development(1),
        /**
         * Production mode. In this mode, all data from the SDK will be treated as production data, and will be forwarded to all configured
         * integrations for your application. The SDK will honor the configured upload interval.
         *
         * @see #setUploadInterval(int)
         */
        Production(2);
        private final int value;

        int getValue() {
            return value;
        }

        private Environment(int value) {
            this.value = value;
        }
    }

    /**
     * Enumeration used to moderate the amount of messages that are printed
     * by the SDK to the console. Note that while the SDK is in the Production,
     * <i>no log messages will be printed</i>.
     * <p/>
     * The default is WARNING, which means only ERROR and WARNING level messages will appear in the console, viewable by logcat or another utility.
     *
     * @see #setLogLevel(com.mparticle.MParticle.LogLevel)
     */
    public enum LogLevel {
        /**
         * Disable logging completely.
         */
        NONE,
        /**
         * Used for critical issues with the SDK or it's configuration.
         */
        ERROR,
        /**
         * (default) Used to warn developers of potentially unintended consequences of their use of the SDK.
         */
        WARNING,
        /**
         * Used to communicate the internal state and processes of the SDK.
         */
        DEBUG;
    }

    /**
     * This interface defines a series of constants that can be used to specify certain characteristics of a user. There are many 3rd party services
     * that support, for example, specifying a gender of a user. The mParticle platform will look for these constants within the user attributes that
     * you have set for a given user, and forward any attributes to the services that support them.
     *
     * @see #setUserAttribute(String, Object)
     */
    public interface UserAttributes {
        /**
         * A special attribute string to specify the mobile number of the consumer's device
         */
        public static final String MOBILE_NUMBER = "$Mobile";
        /**
         * A special attribute string to specify the user's gender.
         */
        public static final String GENDER = "$Gender";
        /**
         * A special attribute string to specify the user's age.
         */
        public static final String AGE = "$Age";
        /**
         * A special attribute string to specify the user's country.
         */
        public static final String COUNTRY = "$Country";
        /**
         * A special attribute string to specify the user's zip code.
         */
        public static final String ZIPCODE = "$Zip";
        /**
         * A special attribute string to specify the user's city.
         */
        public static final String CITY = "$City";
        /**
         * A special attribute string to specify the user's state or region.
         */
        public static final String STATE = "$State";
        /**
         * A special attribute string to specify the user's street address and apartment number.
         */
        public static final String ADDRESS = "$Address";
        /**
         * A special attribute string to specify the user's first name.
         */
        public static final String FIRSTNAME = "$FirstName";
        /**
         * A special attribute string to specify the user's last name.
         */
        public static final String LASTNAME = "$LastName";
    }

    private static final class SessionTimeoutHandler extends Handler {
        private final MParticle mParticle;

        public SessionTimeoutHandler(MParticle mParticle, Looper looper) {
            super(looper);
            this.mParticle = mParticle;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!mParticle.checkSessionTimeout()) {
                sendEmptyMessageDelayed(0, mParticle.getSessionTimeout() * 1000);
            }
        }
    }

    private static final class MParticleLocationListener implements LocationListener {
        private final MParticle mParticle;

        public MParticleLocationListener(MParticle mParticle) {
            this.mParticle = mParticle;
        }

        @Override
        public void onLocationChanged(Location location) {
            mParticle.setLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    private class MPLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int policyReason) {
            if (policyReason == Policy.LICENSED) {
                sPreferences.edit().putBoolean(PrefKeys.PIRATED, false).commit();
            }
            if (clientLicensingCallback != null) {
                clientLicensingCallback.allow(policyReason);
            }
        }

        public void dontAllow(int policyReason) {
            if (policyReason == ServerManagedPolicy.NOT_LICENSED) {
                sPreferences.edit().putBoolean(PrefKeys.PIRATED, true).commit();
            }
            if (clientLicensingCallback != null) {
                clientLicensingCallback.dontAllow(policyReason);
            }
        }

        public void applicationError(int errorCode) {
            if (errorCode == LicenseCheckerCallback.ERROR_MISSING_PERMISSION) {
                ConfigManager.log(LogLevel.ERROR, "License checking enabled but app is missing permission: \"com.android.vending.CHECK_LICENSE\"");
            }
            if (clientLicensingCallback != null) {
                clientLicensingCallback.applicationError(errorCode);
            }
        }
    }

}
