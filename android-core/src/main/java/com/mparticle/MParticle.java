package com.mparticle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.ProductBagApi;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.KitKatHelper;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MPLocationListener;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.media.MediaCallbacks;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The primary access point to the mParticle SDK. In order to use this class, you must first call {@link #start(Context)}, which requires
 * configuration via <code><a href="http://developer.android.com/guide/topics/resources/providing-resources.html">Android Resources</a></code>. You can then retrieve a reference
 * to an instance of this class via {@link #getInstance()}
 * <p></p>
 * It's recommended to keep configuration parameters in a single xml file located within your res/values folder. The full list of configuration options is as follows:
 * <p></p>
 * Required parameters
 * <ul>
 * <li>mp_key - <code><a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a></code> - This is the key used to authenticate with the mParticle SDK server API</li>
 * <li>mp_secret - <code> <a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a></code> - This is the secret used to authenticate with the mParticle SDK server API</li>
 * </ul>
 * Required for push notifications
 * <ul>
 * <li> mp_enablePush - <code><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a></code> - Enable push registration, notifications, and analytics. <i>Default: false</i></li>
 * <li> mp_pushSenderId - <code><a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a></code> - <code><a href="http://developer.android.com/google/gcm/gcm.html#senderid">GCM Sender ID</a></code></li>
 * </ul>
 * Required for licensing
 * <ul>
 * <li> mp_enableLicenseCheck - <code><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a></code> - By enabling license check, MParticle will automatically validate that the app was downloaded and/or bought via Google Play, or if it was "pirated" or "side-loaded". <i>Default: false</i></li>
 * <li> mp_appLicenseKey - <code><a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a></code> - The <code><a href="http://developer.android.com/google/play/licensing/adding-licensing.html#account-key">public key</a></code> used by your app to verify the user's license with Google Play.</li>
 * </ul>
 * Optional
 * <ul>
 * <li>mp_enableAutoScreenTracking - <code> <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a></code> - Enable automatic screen view events. Note that *prior to ICS/API level 14*, this functionality requires instrumentation via an mParticle Activity implementation or manually. </li>
 * <li>mp_productionUploadInterval - <code> <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a></code> - The length of time in seconds to send batches of messages to mParticle. Setting this too low could have an adverse effect on the device battery. <i>Default: 600</i></li>
 * <li>mp_reportUncaughtExceptions - <code> <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a></code> - By enabling this, the MParticle SDK will automatically log and report any uncaught exceptions, including stack traces. <i>Default: false</i></li>
 * <li>mp_sessionTimeout - <code> <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a></code> - The length of time (in seconds) that a user session will remain valid while application has been paused and put into the background. <i>Default: 60</i></li>
 * </ul>
 */
public class MParticle {
    /**
     * The ConfigManager is tasked with incorporating server-based, run-time, and XML configuration,
     * and surfacing the result/winner.
     */
    protected ConfigManager mConfigManager;

    /**
     * Used to delegate messages, events, user actions, etc on to embedded kits.
     */

    protected KitFrameworkWrapper mKitManager;
    /**
     * The state manager is primarily concerned with Activity lifecycle and app visibility in order to manage sessions,
     * automatically log screen views, and pass lifecycle information on top embedded kits.
     */
    AppStateManager mAppStateManager;

    private JSONArray mUserIdentities = new JSONArray();

    protected MessageManager mMessageManager;
    private static volatile MParticle instance;
    protected SharedPreferences mPreferences;
    protected MPLocationListener mLocationListener;
    private Context mAppContext;

    protected MPMessagingAPI mMessaging;
    protected MPMediaAPI mMedia;
    protected CommerceApi mCommerce;
    protected ProductBagApi mProductBags;
    protected volatile DeepLinkListener mDeepLinkListener;
    private static volatile boolean androidIdDisabled;

    MParticle() { }


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
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     *
     * @param context Required reference to a Context object
     * @param apiKey Your application's mParticle key retrieved from app.mparticle.com/apps
     * @param apiSecret Your application's mParticle secret retrieved from app.mparticle.com/apps
     *
     */

    public static void start(Context context, String apiKey, String apiSecret) {
        start(context, InstallType.AutoDetect, apiKey, apiSecret);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     * <p></p>
     * The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     *
     * @param context     Required reference to a Context object
     * @param installType Specify whether this is a new install or an upgrade, or let mParticle detect
     * @see MParticle.InstallType
     */

    public static void start(Context context, InstallType installType) {
        start(context, installType, Environment.AutoDetect);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     * <p></p>
     * The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     *
     * @param context       Required reference to a Context object
     * @param installType   Specify whether this is a new install or an upgrade, or let mParticle detect
     * @param apiKey        Your application's mParticle key retrieved from app.mparticle.com/apps
     * @param apiSecret     Your application's mParticle secret retrieved from app.mparticle.com/apps
     *
     * @see MParticle.InstallType
     */

    public static void start(Context context, InstallType installType, String apiKey, String apiSecret) {
        start(context, installType, Environment.AutoDetect, apiKey, apiSecret);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     * <p></p>
     *
     *
     * @param context     Required reference to a Context object
     * @param installType The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     * @param environment Force the SDK into either Production or Development mode. See {@link MParticle.Environment}
     * for implications of each mode. The SDK automatically determines which mode it should be in depending
     * on the signing and the DEBUGGABLE flag of your application's AndroidManifest.xml, so this initializer is not typically needed.
     * <p></p>
     *
     * This initializer can however be useful while you're testing a release-signed version of your application, and you have *not* set the
     * debuggable flag in your AndroidManifest.xml. In this case, you can force the SDK into development mode to prevent sending
     * your test usage/data as production data. It's crucial, however, that prior to submission to Google Play that you ensure
     * you are no longer forcing development mode.
     */
    public static void start(Context context, InstallType installType, Environment environment) {
        if (context == null) {
            throw new IllegalArgumentException("mParticle failed to start: context is required.");
        }
        MParticle.getInstance(context.getApplicationContext(), installType, environment, null, null);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     * This method requires that your API key and secret are contained in your XML configuration.
     * <p></p>
     *
     * @param context       Required reference to a Context object
     * @param installType   The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     * @param environment   Force the SDK into either Production or Development mode. See {@link MParticle.Environment}
     * for implications of each mode. The SDK automatically determines which mode it should be in depending
     * on the signing and the DEBUGGABLE flag of your application's AndroidManifest.xml, so this initializer is not typically needed.
     * @param apiKey Your application's mParticle key retrieved from app.mparticle.com/apps
     * @param apiSecret Your application's mParticle secret retrieved from app.mparticle.com/apps
     */
    public static void start(Context context, InstallType installType, Environment environment, String apiKey, String apiSecret) {
        if (context == null) {
            throw new IllegalArgumentException("mParticle failed to start: context is required.");
        }
        MParticle.getInstance(context.getApplicationContext(), installType, environment, apiKey, apiSecret);
    }



    /**
     * Initialize or return a thread-safe instance of the mParticle SDK, specifying the API credentials to use. If this
     * or any other {@link #getInstance()} has already been called in the application's lifecycle, the
     * API credentials will be ignored and the current instance will be returned.
     *
     * @param context the Activity that is creating the instance
     * @return An instance of the mParticle SDK configured with your API key
     * @param apiKey Your application's mParticle key retrieved from app.mparticle.com/apps
     * @param apiSecret Your application's mParticle secret retrieved from app.mparticle.com/apps
     *
     */
    private static MParticle getInstance(Context context, InstallType installType, Environment environment, String apiKey, String apiSecret) {
        if (instance == null) {
            synchronized (MParticle.class) {
                if (instance == null) {
                    if (!MPUtility.checkPermission(context, Manifest.permission.INTERNET)) {
                        Log.e(Constants.LOG_TAG, "mParticle requires android.permission.INTERNET permission");
                    }

                    ConfigManager configManager = new ConfigManager(context, environment, apiKey, apiSecret);
                    AppStateManager appStateManager = new AppStateManager(context);
                    appStateManager.setConfigManager(configManager);

                    instance = new MParticle();
                    instance.mAppContext = context;
                    instance.mConfigManager = configManager;
                    instance.mAppStateManager = appStateManager;
                    instance.mCommerce = new CommerceApi(context);
                    instance.mProductBags = new ProductBagApi(context);
                    instance.mMessageManager = new MessageManager(context, configManager, installType, appStateManager);
                    instance.mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
                    instance.mKitManager = new KitFrameworkWrapper(context, instance.mMessageManager, configManager, appStateManager);
                    instance.mMessageManager.refreshConfiguration();

                    if (configManager.getLogUnhandledExceptions()) {
                        instance.enableUncaughtExceptionLogging();
                    }

                    //there are a number of settings that don't need to be enabled right away
                    //queue up a delayed init and let the start() call return ASAP.
                    instance.mMessageManager.initConfigDelayed();
                    appStateManager.init(Build.VERSION.SDK_INT);
                }
            }
        }
        return instance;
    }

    public KitFrameworkWrapper getKitManager() {
        return mKitManager;
    }

    public ConfigManager getConfigManager() {
        return mConfigManager;
    }

    public AppStateManager getAppStateManager() {
        return mAppStateManager;
    }

    /**
     * Retrieve an instance of the MParticle class. {@link #start(Context)} must
     * be called prior to this.
     *
     * @return An instance of the mParticle SDK configured with your API key
     */
    public static MParticle getInstance() {
        if (instance == null) {
            Log.e(Constants.LOG_TAG, "Failed to get MParticle instance, getInstance() called prior to start().");
            return null;
        }
        return getInstance(null, null, null, null, null);
    }

    /**
     *
     * Use this method for your own unit testing. Using a framework such as Mockito, or
     * by extending MParticle, use this method to set a mock of mParticle.
     *
     * @param instance
     */
    public static void setInstance(MParticle instance) {
        MParticle.instance = instance;
    }

    /**
     * Query the status of Android ID collection.
     *
     * By default, the SDK will collect <a href="http://developer.android.com/reference/android/provider/Settings.Secure.html#ANDROID_ID">Android Id</a> for the purpose
     * of anonymous analytics. If you're not using an mParticle integration that consumes Android ID, the value will be sent to the mParticle
     * servers and then immediately discarded. Use this API if you would like to additionally disable it from being collected entirely.
     *
     *
     * @return true if Android ID collection is disabled. (false by default)

     * @see MParticle#setAndroidIdDisabled(boolean)
     */
    public static boolean isAndroidIdDisabled() {
        return androidIdDisabled;
    }

    /**
     * Disable Android ID collection. This *must* be called before {@link MParticle#start(Context)}.
     *
     * By default, the SDK will collect <a href="http://developer.android.com/reference/android/provider/Settings.Secure.html#ANDROID_ID">Android Id</a> for the purpose
     * of anonymous analytics. If you're not using an mParticle integration that consumes Android ID, the value will be sent to the mParticle
     * servers and then immediately discarded. Use this API if you would like to additionally disable it from being collected entirely.
     *
     * @param disable true to disable collection (false by default)
     */
    public static void setAndroidIdDisabled(boolean disable) {
        androidIdDisabled = disable;
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
            if (mConfigManager.isEnabled()) {
                mAppStateManager.ensureActiveSession();
                mAppStateManager.onActivityStarted(activity);
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
            if (mConfigManager.isEnabled()) {
                mAppStateManager.ensureActiveSession();
                mAppStateManager.onActivityStopped(activity);
            }
        }
    }

    /**
     * Explicitly terminate the current user's session.
     */
    private void endSession() {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    boolean isSessionActive() {
        return mAppStateManager.getSession().isActive();
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
        ReferrerReceiver.setInstallReferrer(mAppContext, referrer);
    }

    public String getInstallReferrer() {
        return mPreferences.getString(PrefKeys.INSTALL_REFERRER, null);
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
        logEvent(
                new MPEvent.Builder(eventName, eventType)
                        .info(eventInfo)
                        .duration(eventLength)
                        .category(category)
                        .build()
        );
    }

    /**
     * Log an event with an {@link MPEvent} object
     *
     * @param event the event object to log
     */
    public void logEvent(MPEvent event) {
        if (mConfigManager.isEnabled() && checkEventLimit()) {
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event, mAppStateManager.getCurrentActivityName());
            ConfigManager.log(LogLevel.DEBUG, "Logged event - \n", event.toString());
            mKitManager.logEvent(event);

        }
    }

    /**
     * Log an e-Commerce related event with a {@link CommerceEvent} object
     *
     * @param event the event to log
     *
     * @see CommerceEvent
     */
    public void logEvent(CommerceEvent event) {
        if (mConfigManager.isEnabled() && checkEventLimit()) {
            Cart cart = Cart.getInstance(mAppContext);
            if (event.getProductAction() != null) {
                List<Product> productList = event.getProducts();
                if (event.getProductAction().equalsIgnoreCase(Product.ADD_TO_CART)) {
                    if (productList != null) {
                        for (Product product : productList) {
                            cart.add(product, false);
                        }
                    }
                } else if (event.getProductAction().equalsIgnoreCase(Product.REMOVE_FROM_CART)) {
                    if (productList != null) {
                        for (Product product : productList) {
                            cart.remove(product, false);
                        }
                    }
                }
            }
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event);
            ConfigManager.log(LogLevel.DEBUG, "Logged commerce event - \n", event.toString());
            mKitManager.logCommerceEvent(event);
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
            ConfigManager.log(LogLevel.ERROR, "ValueIncreased must not be null.");
            return;
        }
        if (contextInfo == null) {
            contextInfo = new HashMap<String, String>();
        }
        contextInfo.put(MessageKey.RESERVED_KEY_LTV, valueIncreased.toPlainString());
        contextInfo.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_LTV);
        logEvent(eventName == null ? "Increase LTV" : eventName, EventType.Transaction, contextInfo);
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
        logScreen(new MPEvent.Builder(screenName).info(eventData).build().setScreenEvent(true));
    }


    /**
     * Logs a screen view event
     *
     * @param screenEvent an event object, the name of the event will be used as the screen name
     */
    public void logScreen(MPEvent screenEvent) {
        screenEvent.setScreenEvent(true);
        if (MPUtility.isEmpty(screenEvent.getEventName())) {
            ConfigManager.log(LogLevel.ERROR, "screenName is required for logScreen");
            return;
        }
        if (screenEvent.getEventName().length() > Constants.LIMIT_EVENT_NAME) {
            ConfigManager.log(LogLevel.ERROR, "The screen name was too long. Discarding event.");
            return;
        }
        if (checkEventLimit()) {
            mAppStateManager.ensureActiveSession();
            if (mConfigManager.isEnabled()) {
                mMessageManager.logScreen(screenEvent, screenEvent.getNavigationDirection());

                if (null == screenEvent.getInfo()) {
                    ConfigManager.log(LogLevel.DEBUG, "Logged screen: ", screenEvent.toString());
                }

            }
            if (screenEvent.getNavigationDirection()) {
                mKitManager.logScreen(screenEvent);
            }
        }
    }

    /**
     * Leave a breadcrumb to be included with error and exception logging, as well as
     * with regular session events.
     *
     * @param breadcrumb
     */
    public void leaveBreadcrumb(String breadcrumb) {
        if (mConfigManager.isEnabled()) {
            if (MPUtility.isEmpty(breadcrumb)) {
                ConfigManager.log(LogLevel.ERROR, "breadcrumb is required for leaveBreadcrumb");
                return;
            }
            if (breadcrumb.length() > Constants.LIMIT_EVENT_NAME) {
                ConfigManager.log(LogLevel.ERROR, "The breadcrumb name was too long. Discarding event.");
                return;
            }
            mAppStateManager.ensureActiveSession();
            mMessageManager.logBreadcrumb(breadcrumb);
            ConfigManager.log(LogLevel.DEBUG, "Logged breadcrumb: " + breadcrumb);
            mKitManager.leaveBreadcrumb(breadcrumb);
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
     * @param errorAttributes a Map of data attributes to associate with this error
     */
    public void logError(String message, Map<String, String> errorAttributes) {
        if (mConfigManager.isEnabled()) {
            if (MPUtility.isEmpty(message)) {
                ConfigManager.log(LogLevel.ERROR, "message is required for logErrorEvent");
                return;
            }
            mAppStateManager.ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(errorAttributes);
                mMessageManager.logErrorEvent(message, null, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged error with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString())
                );
            }
            mKitManager.logError(message, errorAttributes);
        }
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            if (checkEventLimit()) {
                mMessageManager.logNetworkPerformanceEvent(startTime, method, url, length, bytesSent, bytesReceived, requestString);
            }
            mKitManager.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);

        }
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
     * Query Kits to determine if this user installed and/or opened the app by way
     * of a deeplink.
     *
     * @param deepLinkListener Your deep link listener implementation. Use this to react to the result of the deep link query.
     */
    public void checkForDeepLink(DeepLinkListener deepLinkListener) {
        setDeepLinkListener(deepLinkListener);
        checkForDeepLink();
    }

    private void checkForDeepLink() {
        if (mDeepLinkListener != null) {
            mKitManager.checkForDeepLink();
        }
    }

    /**
     * Set the deep link listener. Call this to set the listener to null once you
     * have finished querying for deep links.
     *
     * @param deepLinkListener
     */
    public void setDeepLinkListener(DeepLinkListener deepLinkListener) {
        mDeepLinkListener = deepLinkListener;
    }

    /**
     * Retrieve the current deeplink listener
     *
     * @return
     */
    public DeepLinkListener getDeepLinkListener() {
        return mDeepLinkListener;
    }

    /**
     * Logs an Exception
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     * @param message   the name of the error event to be tracked
     */
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(message, exception, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged exception with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString()) +
                                " with exception: " + (exception == null ? "<none>" : exception.getMessage())
                );
            }
            mKitManager.logException(exception, eventData, message);
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
        if (mConfigManager.isEnabled()) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(provider)) {
                    ConfigManager.log(LogLevel.ERROR, "That requested location provider is not available");
                    return;
                }

                if (null == mLocationListener) {
                    mLocationListener = new MPLocationListener(this);
                } else {
                    // clear the location listener, so it can be added again
                    locationManager.removeUpdates(mLocationListener);
                }
                locationManager.requestLocationUpdates(provider, minTime, minDistance, mLocationListener);
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(PrefKeys.LOCATION_PROVIDER, provider)
                        .putLong(PrefKeys.LOCATION_MINTIME, minTime)
                        .putLong(PrefKeys.LOCATION_MINDISTANCE, minDistance)
                        .apply();

            } catch (SecurityException e) {
                ConfigManager.log(LogLevel.ERROR, "The app must require the appropriate permissions to track location using this provider");
            }
        }
    }


    /**
     * Disables any mParticle location tracking that had been started
     */
    public void disableLocationTracking() {
        disableLocationTracking(true);
    }

    /**
     * Disables any mParticle location tracking that had been started
     */
    private void disableLocationTracking(boolean userTriggered) {
        if (mLocationListener != null) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);

                if (MPUtility.checkPermission(mAppContext, Manifest.permission.ACCESS_FINE_LOCATION) ||
                        MPUtility.checkPermission(mAppContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    locationManager.removeUpdates(mLocationListener);
                }
                mLocationListener = null;
                if (userTriggered){
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.remove(PrefKeys.LOCATION_PROVIDER)
                            .remove(PrefKeys.LOCATION_MINTIME)
                            .remove(PrefKeys.LOCATION_MINDISTANCE)
                            .apply();
                }
            }catch (Exception e){

            }
        }
    }

    /**
     * Set the current location of the active session.
     *
     * @param location
     */
    public void setLocation(Location location) {
        mMessageManager.setLocation(location);
        mKitManager.setLocation(location);

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
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Set session attribute: " + key + "=" + value);

            if (MPUtility.setCheckedAttribute(mAppStateManager.getSession().mSessionAttributes, key, value, true, false)) {
                mMessageManager.setSessionAttributes();
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
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Incrementing session attribute: " + key + "=" + value);

            if (MPUtility.setCheckedAttribute(mAppStateManager.getSession().mSessionAttributes, key, value, true, true)) {
                mMessageManager.setSessionAttributes();
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
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Logging out.");
            mMessageManager.logProfileAction(Constants.ProfileActions.LOGOUT);
        }
        mKitManager.logout();

    }

    /**
     * Set a single <i>user</i> attribute. The attribute will be combined with any existing user attributes.
     *
     * @param key   the attribute key
     * @param value the attribute value. This value will be converted to its String representation as dictated by its <code>toString()</code> method.
     */
    public boolean setUserAttribute(String key, Object value) {
        if (MPUtility.isEmpty(key)){
            ConfigManager.log(LogLevel.WARNING, "setUserAttribute called with null key. This is a no-op.");
            return false;
        }
        if (key.length() > Constants.LIMIT_ATTR_NAME) {
            ConfigManager.log(LogLevel.WARNING, "User attribute keys cannot be longer than " + Constants.LIMIT_ATTR_NAME + " characters, attribute not set: " + key);
            return false;
        }

        if (value != null && value instanceof List) {
            List<Object> values = (List<Object>)value;
            if (values.size() > Constants.LIMIT_USER_ATTR_LIST_LENGTH) {
                ConfigManager.log(LogLevel.WARNING, "setUserAttribute called with list longer than "+Constants.LIMIT_USER_ATTR_LIST_LENGTH+" elements, list not set.");
                return false;
            }
            List<String> clonedList = new ArrayList<String>();
            try {
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i).toString().length() > Constants.LIMIT_USER_ATTR_LIST_ITEM_LENGTH) {
                        ConfigManager.log(LogLevel.WARNING, "setUserAttribute called with list containing element longer than " + Constants.LIMIT_USER_ATTR_LIST_ITEM_LENGTH + " characters, dropping entire list.");
                        return false;
                    } else {
                        clonedList.add(values.get(i).toString());
                    }
                }
                ConfigManager.log(LogLevel.DEBUG, "Set user attribute list: " + key + " with values: " + values.toString());
                mMessageManager.setUserAttribute(key, clonedList);
                mKitManager.setUserAttributeList(key, clonedList);
            }catch (Exception e) {
                ConfigManager.log(LogLevel.DEBUG, "Error while setting attribute list: " + e.toString());
                return false;
            }
        }else {
            String stringValue = null;
            if (value != null) {
                stringValue = value.toString();
                if (stringValue.length() > Constants.LIMIT_USER_ATTR_VALUE) {
                    ConfigManager.log(LogLevel.WARNING, "setUserAttribute called with string-value longer than " + Constants.LIMIT_USER_ATTR_VALUE + " characters. Attribute not set.");
                    return false;
                }
                ConfigManager.log(LogLevel.DEBUG, "Set user attribute: " + key + " with value: " + stringValue);
            } else {
                ConfigManager.log(LogLevel.DEBUG, "Set user tag: " + key);
            }
            mMessageManager.setUserAttribute(key, stringValue);
            mKitManager.setUserAttribute(key, stringValue);
        }
        return true;
    }

    /**
     *
     * @param key
     * @param attributeList
     * @return
     */
    public boolean setUserAttributeList(String key, List<String> attributeList) {
        if (attributeList == null) {
            ConfigManager.log(LogLevel.WARNING, "setUserAttributeList called with null list, this is a no-op.");
            return false;
        }
        return setUserAttribute(key, attributeList);
    }

    /**
     * Increment a single <i>user</i> attribute. If the attribute does not already exist, a new one will be created.
     *
     * If the value of the attribute cannot be parsed as an integer, this method is a no-op.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public boolean incrementUserAttribute(String key, int value) {
        if (key == null){
            ConfigManager.log(LogLevel.WARNING, "incrementUserAttribute called with null key. Ignoring...");
            return false;
        }
        ConfigManager.log(LogLevel.DEBUG, "Incrementing user attribute: " + key + " with value " + value);
        mMessageManager.incrementUserAttribute(key, value);
        return true;
    }

    /**
     * Remove a <i>user</i> attribute - this applies both to lists and single-value attributes
     *
     * @param key the key of the attribute
     */
    public boolean removeUserAttribute(String key) {
        if (MPUtility.isEmpty(key)) {
            ConfigManager.log(LogLevel.DEBUG, "removeUserAttribute called with empty key.");
            return false;
        }

        ConfigManager.log(LogLevel.DEBUG, "Removing user attribute: " + key);
        mMessageManager.removeUserAttribute(key);
        mKitManager.removeUserAttribute(key);
        return true;
    }

    /**
     * Set a single user tag, it will be combined with any existing tags.
     *
     * @param tag a tag assigned to a user
     */
    public boolean setUserTag(String tag) {
        return setUserAttribute(tag, null);
    }

    /**
     * Remove a user tag. This is the same as calling {@link MParticle#removeUserAttribute(String)}.
     *
     * @param tag a tag that was previously added
     */
    public boolean removeUserTag(String tag) {
        return removeUserAttribute(tag);
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

            mKitManager.setUserIdentity(id, identityType);

            JSONArray userIdentities = getUserIdentityJson();

            try {
                int index = -1;
                for (int i = 0; i < userIdentities.length(); i++) {
                    if (userIdentities.getJSONObject(i).get(MessageKey.IDENTITY_NAME).equals(identityType.value)) {
                        index = i;
                        break;
                    }
                }

                JSONObject newObject = new JSONObject();
                newObject.put(MessageKey.IDENTITY_NAME, identityType.value);
                newObject.put(MessageKey.IDENTITY_VALUE, id);

                if (index >= 0) {
                    newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, userIdentities.getJSONObject(index).optLong(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis()));
                    newObject.put(MessageKey.IDENTITY_FIRST_SEEN, false);
                    userIdentities.put(index, newObject);
                } else {
                    newObject.put(MessageKey.IDENTITY_DATE_FIRST_SEEN, System.currentTimeMillis());
                    newObject.put(MessageKey.IDENTITY_FIRST_SEEN, true);
                    userIdentities.put(newObject);
                }

            } catch (JSONException e) {
                ConfigManager.log(LogLevel.ERROR, "Error setting identity: " + id);
                return;
            }

            mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + getConfigManager().getApiKey(), userIdentities.toString()).apply();
        }
    }

    private JSONArray getUserIdentityJson(){
        if (mUserIdentities == null){
            String userIds = mPreferences.getString(PrefKeys.USER_IDENTITIES + getConfigManager().getApiKey(), null);

            Boolean changeMade = false;
            try {
                mUserIdentities = new JSONArray(userIds);
            } catch (JSONException e) {
                mUserIdentities = new JSONArray();
            }
            try {

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
                    mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + getConfigManager().getApiKey(), mUserIdentities.toString()).apply();
                }
            } catch (JSONException jse) {
                //swallow this
            }
        }

        return mUserIdentities;

    }

    public Map<IdentityType, String> getUserIdentities(){
        JSONArray identities = getUserIdentityJson();
        Map<IdentityType, String> identityTypeStringMap = new HashMap<IdentityType, String>(identities.length());

        for (int i = 0; i < identities.length(); i++) {
            try {
                JSONObject identity = identities.getJSONObject(i);
                identityTypeStringMap.put(
                        IdentityType.parseInt(identity.getInt(MessageKey.IDENTITY_NAME)),
                        identity.getString(MessageKey.IDENTITY_VALUE)
                );
            }catch (JSONException jse) {

            }
        }

        return identityTypeStringMap;

    }

    /**
     * Remove an identity matching this id
     * <p></p>
     * Note: this will only remove the *first* matching id
     *
     * @param id the id to remove
     */
    public void removeUserIdentity(String id) {
        JSONArray userIdentities = getUserIdentityJson();
        if (id != null && id.length() > 0) {
            try {
                int indexToRemove = -1;
                IdentityType identityType = null;
                for (int i = 0; i < userIdentities.length(); i++) {
                    if (userIdentities.getJSONObject(i).getString(MessageKey.IDENTITY_VALUE).equals(id)) {
                        indexToRemove = i;
                        try {
                            identityType = IdentityType.valueOf(userIdentities.getJSONObject(i).getString(MessageKey.IDENTITY_NAME));
                        }catch (Exception e) {

                        }
                        break;
                    }
                }
                if (indexToRemove >= 0) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        KitKatHelper.remove(userIdentities, indexToRemove);
                    } else {
                        JSONArray newIdentities = new JSONArray();
                        for (int i = 0; i < userIdentities.length(); i++) {
                            if (i != indexToRemove) {
                                newIdentities.put(userIdentities.get(i));
                            }
                        }
                    }
                    mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + getConfigManager().getApiKey(), userIdentities.toString()).apply();
                    if (identityType != null) {
                        getKitManager().removeUserIdentity(identityType);
                    }

                }
            } catch (JSONException jse) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Error removing identity: " + id);
            }

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
        if (optOutStatus != null) {
            if (optOutStatus != mConfigManager.getOptedOut()) {
                if (!optOutStatus) {
                    mAppStateManager.ensureActiveSession();
                }
                mMessageManager.optOut(System.currentTimeMillis(), optOutStatus);
                if (optOutStatus && isSessionActive()) {
                    endSession();
                }

                mConfigManager.setOptOut(optOutStatus);

                ConfigManager.log(LogLevel.DEBUG, "Set opt-out: " + optOutStatus);
            }
            mKitManager.setOptOut(optOutStatus);
        }
    }

    /**
     * Retrieve a URL to be loaded within a {@link WebView} to show the user a survey
     * or feedback form.
     *
     * @param kitId The ID of the desired survey/feedback service.
     * @return a fully-formed URI, or null if no URL exists for the given ID.
     *
     * @see MParticle.ServiceProviders
     */
    public Uri getSurveyUrl(final int kitId) {
        return mKitManager.getSurveyUrl(kitId, getUserAttributes(), getUserAttributeLists());
    }

    /**
     *
     * This method is deprecated. Use <code>start()</code> or XML configuration if you need to customize the environment.
     *
     * @see #start(Context, MParticle.InstallType, MParticle.Environment)
     *
     * @param environment
     */
    @Deprecated
    public void setEnvironment(Environment environment) {
        Log.w(Constants.LOG_TAG, "setEnvironment is deprecated and is a no-op. Use start() or XML configuration if you must customize environment.");
    }

    /**
     * Get the current Environment that the SDK has interpreted. Will never return AutoDetect.
     *
     * @return the current environment, either production or development
     */
    public Environment getEnvironment() {
        return ConfigManager.getEnvironment();
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
        mConfigManager.enableUncaughtExceptionLogging(true);
    }

    /**
     * Disables mParticle exception handling and restores the original UncaughtExceptionHandler
     */
    public void disableUncaughtExceptionLogging() {
        mConfigManager.disableUncaughtExceptionLogging(true);
    }

    /**
     * This method checks the event count is below the limit and increments the event count. A
     * warning is logged if the limit has been reached.
     *
     * @return true if event count is below limit
     */
    private Boolean checkEventLimit() {
        return mAppStateManager.getSession().checkEventLimit();
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
     * <p></p>
     * A session has ended once the application has been in the background for more than this timeout
     *
     * @param sessionTimeout Session timeout in seconds
     */
    public void setSessionTimeout(int sessionTimeout) {
        mConfigManager.setSessionTimeout(sessionTimeout);
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
    @SuppressLint("AddJavascriptInterface")
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
     * @see MParticle.LogLevel
     *
     * @param level
     */
    public void setLogLevel(LogLevel level) {
        if (level != null) {
            mConfigManager.setLogLevel(level);
        }
    }

    /**
     * Entry point to the Messaging APIs
     *
     * @return a helper object that allows for interaction with the Messaging APIs
     */
    public MPMessagingAPI Messaging() {
        if (mMessaging == null){
            mMessaging = new MPMessagingAPI(mAppContext);
        }
        return mMessaging;
    }

    /**
     * Retrieve an instance of the {@link CommerceApi} helper class, used to access the {@link Cart} and as a helper class to log {@link CommerceEvent} events
     * with the {@link Product} objects currently in the Cart.
     *
     * @return returns a global CommerceApi instance.
     */
    public CommerceApi Commerce() {
        return mCommerce;
    }

    /**
     * Retrieve the global {@link ProductBagApi} instance. Use this API to associate {@link com.mparticle.commerce.ProductBag} objects the user.
     *
     * @return a global ProductBagApi instance
     *
     * @see ProductBagApi
     */
    public ProductBagApi ProductBags() {
        return mProductBags;
    }

    /**
     * Entry point to the Media APIs
     *
     * @return a helper object that allows for interaction with the Media APIs
     */
    public MPMediaAPI Media() {
        if (mMedia == null){
            mMedia = new MPMediaAPI(mAppContext, new MediaCallbacks() {
                @Override
                public void onAudioPlaying() {
                    mAppStateManager.ensureActiveSession();
                }

                @Override
                public void onAudioStopped() {
                    try {
                        mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
                    }catch (Exception e){

                    }
                }
            });
        }
        return mMedia;
    }

    /**
     * Detect whether the given service provider is active. Use this method
     * only when you need to make direct calls to an embedded SDK.
     *
     * You can also register a {@link android.content.BroadcastReceiver} with an {@link android.content.IntentFilter}, using an action of
     * {@link MParticle.ServiceProviders#BROADCAST_ACTIVE} or {@link MParticle.ServiceProviders#BROADCAST_DISABLED}
     * concatenated with the service provider ID:
     *
     * <pre>
     * {@code
     * Context.registerReceiver(yourReceiver, new IntentFilter(MParticle.ServiceProviders.BROADCAST_ACTIVE + MParticle.ServiceProviders.APPBOY));}
     * </pre>
     *
     * @deprecated
     *
     * @param serviceProviderId
     * @return True if you can safely make direct calls to the given service provider.
     *
     * @see MParticle.ServiceProviders
     */
    public boolean isProviderActive(int serviceProviderId){
        return isKitActive(serviceProviderId);
    }

    /**
     * Detect whether the given service provider kit is active. Use this method
     * only when you need to make direct calls to an embedded SDK.
     *
     * You can also register a {@link android.content.BroadcastReceiver} with an {@link android.content.IntentFilter}, using an action of
     * {@link MParticle.ServiceProviders#BROADCAST_ACTIVE} or {@link MParticle.ServiceProviders#BROADCAST_DISABLED}
     * concatenated with the service provider ID:
     *
     * <pre>
     * {@code
     * Context.registerReceiver(yourReceiver, new IntentFilter(MParticle.ServiceProviders.BROADCAST_ACTIVE + MParticle.ServiceProviders.APPBOY));}
     * </pre>
     *
     * @param serviceProviderId
     * @return True if you can safely make direct calls to the given service provider.
     *
     * @see MParticle.ServiceProviders
     */
    public boolean isKitActive(int serviceProviderId){
        return mKitManager.isKitActive(serviceProviderId);
    }

    /**
     * Retrieve the underlying object for the given Kit Id for direct calls.
     *
     * @param kitId
     * @return
     */
    public Object getKitInstance(int kitId) {
        return mKitManager.getKitInstance(kitId);
    }

    void saveGcmMessage(MPCloudNotificationMessage cloudMessage, String appState) {
        mMessageManager.saveGcmMessage(cloudMessage, appState);
    }

    void saveGcmMessage(ProviderCloudMessage cloudMessage, String appState) {
        mMessageManager.saveGcmMessage(cloudMessage, appState);
    }

    public void logPushRegistration(String instanceId, String senderId) {
        mAppStateManager.ensureActiveSession();
        PushRegistrationHelper.PushRegistration registration = new PushRegistrationHelper.PushRegistration();
        registration.instanceId = instanceId;
        registration.senderId = senderId;
        PushRegistrationHelper.setInstanceId(mAppContext, registration);
        mMessageManager.setPushRegistrationId(instanceId, true);
        mKitManager.onPushRegistration(instanceId, senderId);
    }

    void logNotification(MPCloudNotificationMessage cloudMessage, CloudAction action, boolean startSession, String appState, int behavior) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage.getId(), cloudMessage.getRedactedJsonPayload().toString(), action, appState, behavior);
        }
    }

    void logNotification(ProviderCloudMessage cloudMessage, boolean startSession, String appState) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage, appState);
        }
    }

    void refreshConfiguration() {
        ConfigManager.log(LogLevel.DEBUG, "Refreshing configuration...");
        mMessageManager.refreshConfiguration();
    }

    /**
     * Event type to use when logging events.
     *
     * @see #logEvent(String, MParticle.EventType)
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
     * @see #start(Context, MParticle.InstallType)
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
     * @see #setUserIdentity(String, MParticle.IdentityType)
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
        Alias(8),
        FacebookCustomAudienceId(9);

        private final int value;

        IdentityType(int value) {
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
                case 8:
                    return Alias;
                case 9:
                    return FacebookCustomAudienceId;
                default:
                    return Other;

            }
        }

        public int getValue() {
            return value;
        }

    }

    public Map<String, String> getUserAttributes() {
        return mMessageManager.getUserAttributes(null);
    }

    public Map<String, List<String>> getUserAttributeLists() {
        return mMessageManager.getUserAttributeLists();
    }

    public Map<String, Object> getAllUserAttributes() {
        return mMessageManager.getAllUserAttributes(null);
    }

    public void getAllUserAttributes(UserAttributeListener listener) {
        mMessageManager.getAllUserAttributes(listener);
    }
    /**
     * The Environment in which the SDK and hosting app are running. The SDK
     * automatically detects the Environment based on the <code>DEBUGGABLE</code> flag of your application. The <code>DEBUGGABLE</code>  flag of your
     * application will be <code>TRUE</code> when signing with a debug certificate during development, or if you have explicitly set your
     * application to debug within your AndroidManifest.xml.
     *
     * @see MParticle#start(Context, InstallType, Environment)
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

        public int getValue() {
            return value;
        }

        Environment(int value) {
            this.value = value;
        }
    }

    /**
     * Enumeration used to moderate the amount of messages that are printed
     * by the SDK to the console. Note that while the SDK is in the Production,
     * <i>no log messages will be printed</i>.
     * <p></p>
     * The default is WARNING, which means only ERROR and WARNING level messages will appear in the console, viewable by logcat or another utility.
     *
     * @see #setLogLevel(MParticle.LogLevel)
     */
    public enum LogLevel {
        /**
         * Disable logging completely.
         */
        NONE,
        /**
         * Used for critical issues with the SDK or its configuration.
         */
        ERROR,
        /**
         * (default) Used to warn developers of potentially unintended consequences of their use of the SDK.
         */
        WARNING,
        /**
         * Used to communicate the internal state and processes of the SDK.
         */
        DEBUG
    }

    void logUnhandledError(Throwable t) {
        if (mConfigManager.isEnabled()) {
            mMessageManager.logErrorEvent(t != null ? t.getMessage() : null, t, null, false);
            //we know that the app is about to crash and therefore exit
            mAppStateManager.logStateTransition(Constants.StateTransitionType.STATE_TRANS_EXIT, mAppStateManager.getCurrentActivityName());
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    /**
     * This interface defines constants that can be used to interact with specific 3rd-party services.
     *
     * @see #getSurveyUrl(int)
     */
    public interface ServiceProviders {
        int APPBOY = 28;
        int TUNE = 32;
        int KOCHAVA = 37;
        int COMSCORE = 39;
        int KAHUNA = 56;
        int FORESEE_ID = 64;
        int ADJUST = 68;
        int BRANCH_METRICS = 80;
        int FLURRY = 83;
        int LOCALYTICS = 84;
        int CRITTERCISM = 86;
        int WOOTRIC = 90;
        int APPSFLYER = 92;
        int APPTENTIVE = 97;
        int BUTTON = 1022;
        String BROADCAST_ACTIVE = "MPARTICLE_SERVICE_PROVIDER_ACTIVE_";
        String BROADCAST_DISABLED = "MPARTICLE_SERVICE_PROVIDER_DISABLED_";
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
        String MOBILE_NUMBER = "$Mobile";
        /**
         * A special attribute string to specify the user's gender.
         */
        String GENDER = "$Gender";
        /**
         * A special attribute string to specify the user's age.
         */
        String AGE = "$Age";
        /**
         * A special attribute string to specify the user's country.
         */
        String COUNTRY = "$Country";
        /**
         * A special attribute string to specify the user's zip code.
         */
        String ZIPCODE = "$Zip";
        /**
         * A special attribute string to specify the user's city.
         */
        String CITY = "$City";
        /**
         * A special attribute string to specify the user's state or region.
         */
        String STATE = "$State";
        /**
         * A special attribute string to specify the user's street address and apartment number.
         */
        String ADDRESS = "$Address";
        /**
         * A special attribute string to specify the user's first name.
         */
        String FIRSTNAME = "$FirstName";
        /**
         * A special attribute string to specify the user's last name.
         */
        String LASTNAME = "$LastName";
    }
}
