package com.mparticle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.mparticle.internal.MPLocationListener;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.embedded.EmbeddedKitManager;
import com.mparticle.internal.embedded.ReportingManager;
import com.mparticle.internal.np.MPSSLSocketFactory;
import com.mparticle.internal.np.MPSocketImplFactory;
import com.mparticle.internal.np.MPUrlStreamHandlerFactory;
import com.mparticle.internal.np.MeasuredRequestManager;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.media.MediaCallbacks;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;
import com.mparticle.segmentation.SegmentListener;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * The primary access point to the mParticle SDK. In order to use this class, you must first call {@link #start(android.content.Context)}, which requires
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
 * <li>mp_enableNetworkPerformanceMeasurement - <code> <a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a></code> - Enabling this will allow the mParticle SDK to measure network requests made with Apache's HttpClient as well as UrlConnection. <i>Default: false</i></li>
 * </ul>
 */
public class MParticle {

    /**
     * The ConfigManager is tasked with incorporating server-based, run-time, and XML configuration,
     * and surfacing the result/winner.
     */
    ConfigManager mConfigManager;

    /**
     * Used to filter, log, and drain a queue of measured HTTP requests.
     */
    MeasuredRequestManager measuredRequestManager;

    /**
     * Used to delegate messages, events, user actions, etc on to embedded kits.
     */
    EmbeddedKitManager mEmbeddedKitManager;
    /**
     * The state manager is primarily concerned with Activity lifecycle and app visibility in order to manage sessions,
     * automatically log screen views, and pass lifecycle information on top embedded kits.
     */
    AppStateManager mAppStateManager;

    private JSONArray mUserIdentities = new JSONArray();


    private JSONObject mUserAttributes = new JSONObject();

    private MessageManager mMessageManager;
    private static volatile MParticle instance;
    private SharedPreferences mPreferences;
    private MPLocationListener mLocationListener;
    private Context mAppContext;
    private String mApiKey;

    private MPMessagingAPI mMessaging;
    private MPMediaAPI mMedia;
    private CommerceApi mCommerce;
    private ProductBagApi mProductBags;

    MParticle() {}


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
     * <p></p>
     * The InstallType parameter is used to determine if this is a new install or an upgrade. In
     * the case where the mParticle SDK is being added to an existing app with existing users, this
     * parameter prevents mParticle from categorizing all users as new users.
     *
     * @param context     Required reference to a Context object
     * @param installType Specify whether this is a new install or an upgrade, or let mParticle detect
     * @see com.mparticle.MParticle.InstallType
     */

    public static void start(Context context, InstallType installType) {
        start(context, installType, Environment.AutoDetect);
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
     * @param environment Force the SDK into either Production or Development mode. See {@link com.mparticle.MParticle.Environment}
     * for implications of each mode. The SDK automatically determines which mode it should be in depending
     * on the signing and the DEBUGGABLE flag of your application's AndroidManifest.xml, so this initializer is not typically needed.
     * <p></p>
     *
     * This initializer can however be useful while you're testing a release-signed version of your application, and you have *not* set the
     * debuggable flag in your AndroidManifest.xml. In this case, you can force the SDK into development mode to prevent sending
     * your test usage/data as production data. It's crucial, however, that prior to submission to Google Play that you ensure
     * you are no longer forcing development mode.
     */
    public static void start(Context context, InstallType installType, Environment environment){
        if (context == null) {
            throw new IllegalArgumentException("mParticle failed to start: context is required.");
        }
        MParticle.getInstance(context.getApplicationContext(), installType, environment);
    }

    /**
     * Initialize or return a thread-safe instance of the mParticle SDK, specifying the API credentials to use. If this
     * or any other {@link #getInstance()} has already been called in the application's lifecycle, the
     * API credentials will be ignored and the current instance will be returned.
     *
     * @param context the Activity that is creating the instance
     * @return An instance of the mParticle SDK configured with your API key
     */
    private static MParticle getInstance(Context context, InstallType installType, Environment environment) {
        if (instance == null) {
            synchronized (MParticle.class) {
                if (instance == null) {
                    if (PackageManager.PERMISSION_DENIED == context
                            .checkCallingOrSelfPermission(android.Manifest.permission.INTERNET)) {
                        Log.e(Constants.LOG_TAG, "mParticle requires android.permission.INTERNET permission");
                    }

                    ConfigManager configManager = new ConfigManager(context, environment);
                    EmbeddedKitManager embeddedKitManager = new EmbeddedKitManager(context);
                    AppStateManager appStateManager = new AppStateManager(context);

                    embeddedKitManager.setConfigManager(configManager);
                    embeddedKitManager.setAppStateManager(appStateManager);
                    configManager.setEmbeddedKitManager(embeddedKitManager);
                    appStateManager.setEmbeddedKitManager(embeddedKitManager);
                    appStateManager.setConfigManager(configManager);

                    instance = new MParticle();
                    instance.mAppContext = context;
                    instance.mConfigManager = configManager;
                    instance.mApiKey = configManager.getApiKey();
                    instance.mAppStateManager = appStateManager;
                    instance.mCommerce = new CommerceApi(context);
                    instance.mProductBags = new ProductBagApi(context);
                    instance.mMessageManager = new MessageManager(context, configManager, installType, appStateManager);
                    instance.measuredRequestManager = MeasuredRequestManager.INSTANCE;
                    instance.measuredRequestManager.start(embeddedKitManager);
                    embeddedKitManager.setReportingManager((ReportingManager) instance.mMessageManager);
                    instance.mEmbeddedKitManager = embeddedKitManager;
                    instance.mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);


                    String userAttrs = instance.mPreferences.getString(Constants.PrefKeys.USER_ATTRS + instance.mApiKey, null);
                    try {
                        instance.mUserAttributes = new JSONObject(userAttrs);
                    } catch (Exception e) {
                        instance.mUserAttributes = new JSONObject();
                    }

                    configManager.restore();

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

    /**
     * Retrieve an instance of the MParticle class. {@link #start(android.content.Context)} must
     * be called prior to this.
     *
     * @return An instance of the mParticle SDK configured with your API key
     */
    public static MParticle getInstance() {
        if (instance == null) {
            Log.e(Constants.LOG_TAG, "Failed to get MParticle instance, getInstance() called prior to start().");
            return null;
        }
        return getInstance(null, null, null);
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
            if (mConfigManager.isEnabled()) {
                ensureActiveSession();
                mAppStateManager.onActivityStopped(activity, 0);
            }
        }
    }

    /**
     * Explicitly begin tracking a new session. Usually not necessary unless {@link #endSession()} is also explicitly used.
     */
    private void beginSession() {
        if (mConfigManager.isEnabled()) {
            endSession();
            newSession();
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

    private void ensureActiveSession() {
        mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
        if (!isSessionActive()) {
            newSession();
        }else{
            mMessageManager.updateSessionEnd(mAppStateManager.getSession());
        }
    }

    /**
     * Creates a new session and generates the start-session message.
     */
    private void newSession() {
        mAppStateManager.startSession();
        mMessageManager.startSession();
        ConfigManager.log(LogLevel.DEBUG, "Started new session");
        mEmbeddedKitManager.startSession();
        mMessageManager.startUploadLoop();
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
        Intent fakeReferralIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        fakeReferralIntent.putExtra(Constants.REFERRER, referrer);
        ReferrerReceiver.setInstallReferrer(mAppContext, fakeReferralIntent);
    }

    public String getInstallReferrer(){
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
    public void logEvent(MPEvent event){
        if (mConfigManager.isEnabled() && checkEventLimit()) {
            ensureActiveSession();
            mMessageManager.logEvent(event, mAppStateManager.getCurrentActivity());
            ConfigManager.log(LogLevel.DEBUG, "Logged event - \n", event.toString());
            mEmbeddedKitManager.logEvent(event);
        }
    }

    /**
     * Log an e-Commerce related event with a {@link com.mparticle.commerce.CommerceEvent} object
     *
     * @param event the event to log
     *
     * @see CommerceEvent
     */
    public void logEvent(CommerceEvent event) {
        if (mConfigManager.isEnabled() && checkEventLimit()) {
            Cart cart = Cart.getInstance(mAppContext);
            if (event.getProductAction() != null){
                List<Product> productList = event.getProducts();
                if (event.getProductAction().equalsIgnoreCase(Product.ADD_TO_CART)){
                    if (productList != null) {
                        for (Product product : productList) {
                            cart.add(product, false);
                        }
                    }
                }else if (event.getProductAction().equalsIgnoreCase(Product.REMOVE_FROM_CART)){
                    if (productList != null) {
                        for (Product product : productList) {
                            cart.remove(product, false);
                        }
                    }
                }
            }
            ensureActiveSession();
            mMessageManager.logEvent(event);
            ConfigManager.log(LogLevel.DEBUG, "Logged commerce event - \n", event.toString());
            mEmbeddedKitManager.logCommerceEvent(event);
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
        contextInfo.put(Constants.MessageKey.RESERVED_KEY_LTV, valueIncreased.toPlainString());
        contextInfo.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_LTV);
        logEvent(eventName == null ? "Increase LTV" : eventName, EventType.Transaction, contextInfo);
    }

    /**
     * Log an E-Commerce related event associated to a product
     *
     * @param event
     * @param product
     * @see MPProduct
     * @see MPProduct.Event
     *
     * @deprecated This method has been deprecated in favor of {@link CommerceEvent} and {@link #logEvent(CommerceEvent)}
     */
    public void logProductEvent(final MPProduct.Event event, MPProduct product) {
        if (product == null) {
            ConfigManager.log(LogLevel.ERROR, "MPProduct is required for call to logProductEvent()");
            return;
        }
        if (product.isEmpty()) {
            ConfigManager.log(LogLevel.ERROR, "MPProduct data was null, please check that the MPProduct was built properly.");
            return;
        }
        if (event == null) {
            ConfigManager.log(LogLevel.ERROR, "MPProduct.EVENT is required.");
            return;
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
        MPEvent productEvent = new MPEvent.Builder(event.toString(), EventType.Transaction).info(product).build();
        if (checkEventLimit()) {
            mMessageManager.logEvent(productEvent, mAppStateManager.getCurrentActivity());
            ConfigManager.log(LogLevel.DEBUG, "Logged product event - \n", productEvent.toString());
        }
        if (purchaseEvent) {
            mEmbeddedKitManager.logTransaction(productEvent);
        }

    }

    /**
     * Logs an e-commerce transaction event
     *
     * @param product (required not null)
     * @see MPProduct
     *
     * @deprecated This method has been deprecated in favor of {@link CommerceEvent} and {@link #logEvent(CommerceEvent)}
     */
    public void logTransaction(MPProduct product) {
        logProductEvent(MPProduct.Event.PURCHASE, product);
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
        internalLogScreen(screenName, eventData, true);
    }

    /**
     * Internal logScreen - do not use.
     *
     * @param started true if we're navigating to a screen (onStart), false if we're leaving a screen (onStop)
     */
     public void internalLogScreen(String screenName, Map<String, String> eventData, Boolean started) {
        if (MPUtility.isEmpty(screenName)) {
            ConfigManager.log(LogLevel.ERROR, "screenName is required for logScreen");
            return;
        }
        if (screenName.length() > Constants.LIMIT_NAME) {
            ConfigManager.log(LogLevel.ERROR, "The screen name was too long. Discarding event.");
            return;
        }
        if (checkEventLimit()) {
            ensureActiveSession();
            JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
            if (mConfigManager.isEnabled()) {
                mMessageManager.logScreen(screenName, eventDataJSON, started);

                if (null == eventDataJSON) {
                    ConfigManager.log(LogLevel.DEBUG, "Logged screen: ", screenName);
                } else {
                    ConfigManager.log(LogLevel.DEBUG, "Logged screen: ", screenName, " with data ", eventDataJSON.toString());
                }

            }
            if (started) {
                mEmbeddedKitManager.logScreen(screenName, eventData);
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
            if (breadcrumb.length() > Constants.LIMIT_NAME) {
                ConfigManager.log(LogLevel.ERROR, "The breadcrumb name was too long. Discarding event.");
                return;
            }
            ensureActiveSession();
            mMessageManager.logBreadcrumb(breadcrumb);
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
        if (mConfigManager.isEnabled()) {
            if (MPUtility.isEmpty(message)) {
                ConfigManager.log(LogLevel.ERROR, "message is required for logErrorEvent");
                return;
            }
            ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(message, null, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged error with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString())
                );
            }
        }
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString) {
        if (mConfigManager.isEnabled()) {
            ensureActiveSession();
            if (checkEventLimit()) {
                mMessageManager.logNetworkPerformanceEvent(startTime, method, url, length, bytesSent, bytesReceived, requestString);
            }
        }
    }

    /**
     * Network monitoring is a beta-feature, and is supported by very few 3rd parties (crittercism, new relic). It makes heavy use of
     * reflection, which is an unfortunate necessity. As such, this method is slow, and should *never* be called from the main thread.
     */
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
     * Enable or disable measuring network performance.
     */
    public void setNetworkTrackingEnabled(boolean enabled) {
        setNetworkTrackingEnabled(enabled, true);

    }

    private void setNetworkTrackingEnabled(boolean enabled, boolean userTriggered){
        if (enabled) {
            if (!measuredRequestManager.getEnabled()) {
                if (userTriggered) {
                    ConfigManager.setNetworkingEnabled(true);
                }
                initNetworkMonitoring();
            }
        }else{
            if (measuredRequestManager.getEnabled()) {
                measuredRequestManager.setEnabled(false);
                if (userTriggered) {
                    ConfigManager.setNetworkingEnabled(false);
                }
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
        if (mConfigManager.isEnabled()) {
            ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(message, exception, eventDataJSON);
                ConfigManager.log(LogLevel.DEBUG,
                        "Logged exception with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString()) +
                                " with exception: " + (exception == null ? "<none>" : exception.getMessage())
                );
            }
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

                locationManager.removeUpdates(mLocationListener);
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
        if (mConfigManager.isEnabled()) {
            ensureActiveSession();
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
            ensureActiveSession();
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
            ensureActiveSession();
            ConfigManager.log(LogLevel.DEBUG, "Logging out.");
            mMessageManager.logProfileAction(Constants.ProfileActions.LOGOUT);
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

        if (MPUtility.setCheckedAttribute(mUserAttributes, key, value, false)) {
            mPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).apply();
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

        if (MPUtility.setCheckedAttribute(mUserAttributes, key, value, true)) {
            mPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).apply();
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
        if (mUserAttributes.has(key) || mUserAttributes.has(MPUtility.findCaseInsensitiveKey(mUserAttributes, key))) {
            mUserAttributes.remove(key);
            mPreferences.edit().putString(PrefKeys.USER_ATTRS + mApiKey, mUserAttributes.toString()).apply();
            attributeRemoved(key);
            mEmbeddedKitManager.removeUserAttribute(key);
        }
    }

    private void attributeRemoved(String key) {
        String serializedJsonArray = mPreferences.getString(PrefKeys.DELETED_USER_ATTRS + mApiKey, null);
        JSONArray deletedAtributes;
        try {
            deletedAtributes = new JSONArray(serializedJsonArray);
        } catch (Exception jse) {
            deletedAtributes = new JSONArray();
        }
        deletedAtributes.put(key);

        mPreferences.edit().putString(PrefKeys.DELETED_USER_ATTRS + mApiKey, deletedAtributes.toString()).apply();
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

            JSONArray userIdentities = getUserIdentities();

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

            mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, userIdentities.toString()).apply();
        }
    }


    public JSONArray getUserIdentities(){
        if (mUserIdentities == null){
            String userIds = mPreferences.getString(Constants.PrefKeys.USER_IDENTITIES + mApiKey, null);

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
                    mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, mUserIdentities.toString()).apply();
                }
            } catch (JSONException jse) {
                //swallow this
            }
        }
        return mUserIdentities;

    }

    /**
     * Remove an identity matching this id
     * <p></p>
     * Note: this will only remove the *first* matching id
     *
     * @param id the id to remove
     */
    public void removeUserIdentity(String id) {
        JSONArray userIdentities = getUserIdentities();
        if (id != null && id.length() > 0) {
            try {
                int indexToRemove = -1;
                for (int i = 0; i < userIdentities.length(); i++) {
                    if (userIdentities.getJSONObject(i).getString(MessageKey.IDENTITY_VALUE).equals(id)) {
                        indexToRemove = i;
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
                    mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, userIdentities.toString()).apply();

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
            mMessageManager.optOut(System.currentTimeMillis(), optOutStatus);
            if (optOutStatus && isSessionActive()) {
                endSession();
            }

            mConfigManager.setOptOut(optOutStatus);

            ConfigManager.log(LogLevel.DEBUG, "Set opt-out: " + optOutStatus);
        }
    }

    /**
     * Retrieve a URL to be loaded within a {@link android.webkit.WebView} to show the user a survey
     * or feedback form.
     *
     * @param serviceProviderId The ID of the desired survey/feedback service.
     * @return a fully-formed URI, or null if no URL exists for the given ID.
     *
     * @see com.mparticle.MParticle.ServiceProviders
     */
    public Uri getSurveyUrl(int serviceProviderId) {
        return mEmbeddedKitManager.getSurveyUrl(serviceProviderId, mUserAttributes);
    }

    /**
     *
     * This method is deprecated. Use <code>start()</code> or XML configuration if you need to customize the environment.
     *
     * @see #start(android.content.Context, com.mparticle.MParticle.InstallType, com.mparticle.MParticle.Environment)
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
     * Entry point to the Messaging APIs
     *
     * @return a helper object that allows for interaction with the Messaging APIs
     */
    public MPMessagingAPI Messaging() {
        if (mMessaging == null){
            mMessaging = new MPMessagingAPI(mAppContext, mConfigManager);
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
     * @see {@link ProductBagApi}
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
                    ensureActiveSession();
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
     * {@link com.mparticle.MParticle.ServiceProviders#BROADCAST_ACTIVE} or {@link com.mparticle.MParticle.ServiceProviders#BROADCAST_DISABLED}
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
     * @see com.mparticle.MParticle.ServiceProviders
     */
    public boolean isProviderActive(int serviceProviderId){
        return mEmbeddedKitManager.isProviderActive(serviceProviderId);
    }

    void saveGcmMessage(MPCloudNotificationMessage cloudMessage, String appState) {
        mMessageManager.saveGcmMessage(cloudMessage, appState);
    }

    void saveGcmMessage(ProviderCloudMessage cloudMessage, String appState) {
        mMessageManager.saveGcmMessage(cloudMessage, appState);
    }

    public void logPushRegistration(String registrationId) {
        mMessageManager.setPushRegistrationId(registrationId, true);
    }

    void logNotification(MPCloudNotificationMessage cloudMessage, CloudAction action, boolean startSession, String appState, int behavior) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage.getId(), cloudMessage.getRedactedJsonPayload().toString(), action, appState, behavior);
        }
    }

    void logNotification(ProviderCloudMessage cloudMessage, boolean startSession, String appState) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                ensureActiveSession();
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

    public JSONObject getUserAttributes() {
        return mUserAttributes;
    }

    /**
     * The Environment in which the SDK and hosting app are running. The SDK
     * automatically detects the Environment based on the <code>DEBUGGABLE</code> flag of your application. The <code>DEBUGGABLE</code>  flag of your
     * application will be <code>TRUE</code> when signing with a debug certificate during development, or if you have explicitly set your
     * application to debug within your AndroidManifest.xml.
     *
     * @see com.mparticle.MParticle#start(Context, InstallType, Environment)
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
     * @see #setLogLevel(com.mparticle.MParticle.LogLevel)
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
            mAppStateManager.logStateTransition(Constants.StateTransitionType.STATE_TRANS_EXIT, mAppStateManager.getCurrentActivity());
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
        int FORESEE_ID = 64;
        int APPBOY = 28;
        int ADJUST = 68;
        int KOCHAVA = 37;
        int COMSCORE = 39;
        int KAHUNA = 56;
        int BRANCH_METRICS = 80;
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
