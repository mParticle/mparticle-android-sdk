package com.mparticle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.MParticleUser;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPLocationListener;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.media.MediaCallbacks;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The primary access point to the mParticle SDK. In order to use this class, you must first call {@link #start(MParticleOptions)}. You can then retrieve a reference
 * to an instance of this class via {@link #getInstance()}
 *
 */
public class MParticle {

    /**
     * Used to delegate messages, events, user actions, etc on to embedded kits.
     */

    protected KitFrameworkWrapper mKitManager;
    /**
     * The state manager is primarily concerned with Activity lifecycle and app visibility in order to manage sessions,
     * automatically log screen views, and pass lifecycle information on top embedded kits.
     */
    protected AppStateManager mAppStateManager;

    protected MessageManager mMessageManager;
    private static volatile MParticle instance;
    protected SharedPreferences mPreferences;
    protected MPLocationListener mLocationListener;
    private Context mAppContext;
    protected MPMessagingAPI mMessaging;
    protected MPMediaAPI mMedia;
    protected CommerceApi mCommerce;
    protected MParticleDBManager mDatabaseManager;
    protected volatile AttributionListener mAttributionListener;
    protected IdentityApi mIdentityApi;
    static volatile boolean sAndroidIdDisabled;
    static volatile boolean sDevicePerformanceMetricsDisabled;
    protected boolean locationTrackingEnabled = false;
    protected Internal mInternal;


    protected MParticle() { }
    
    private MParticle(MParticleOptions options) {
        ConfigManager configManager = new ConfigManager(options.getContext(), options.getEnvironment(), options.getApiKey(), options.getApiSecret());
        configManager.setUploadInterval(options.getUploadInterval());
        configManager.setSessionTimeout(options.getSessionTimeout());
        configManager.setIdentityConnectionTimeout(options.getConnectionTimeout());
        configManager.setNetworkOptions(options.getNetworkOptions());
        AppStateManager appStateManager = new AppStateManager(options.getContext());
        appStateManager.setConfigManager(configManager);
        
        
        mAppContext = options.getContext();
        mInternal = new Internal(configManager);
        mAppStateManager = appStateManager;
        mDatabaseManager = new MParticleDBManager(mAppContext);
        if (options.isUncaughtExceptionLoggingEnabled()) {
            enableUncaughtExceptionLogging();
        } else {
            disableUncaughtExceptionLogging();
        }
        mCommerce = new CommerceApi(options.getContext());
        mMessageManager = new MessageManager(options.getContext(), configManager, options.getInstallType(), appStateManager, sDevicePerformanceMetricsDisabled, mDatabaseManager);
        mPreferences = options.getContext().getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Start the mParticle SDK and begin tracking a user session. This method must be called prior to {@link #getInstance()}.
     *
     * @param options Required to initialize the SDK properly
     */
    public static void start(@NonNull MParticleOptions options) {
        MParticle.getInstance(options.getContext(), options);
    }

    /**
     * Initialize or return a thread-safe instance of the mParticle SDK, specifying the API credentials to use. If this
     * or any other {@link #getInstance()} has already been called in the application's lifecycle, the
     * API credentials will be ignored and the current instance will be returned.
     *
     * @param context the Activity that is creating the instance
     * @return An instance of the mParticle SDK configured with your API key
     */
    private static MParticle getInstance(Context context, MParticleOptions options) {
        if (instance == null) {
            synchronized (MParticle.class) {
                if (instance == null) {
                    sDevicePerformanceMetricsDisabled = options.isDevicePerformanceMetricsDisabled();
                    sAndroidIdDisabled = options.isAndroidIdDisabled();
                    setLogLevel(options.getLogLevel());

                    Context originalContext = context;
                    context = context.getApplicationContext();
                    if (!MPUtility.checkPermission(context, Manifest.permission.INTERNET)) {
                        Logger.error("mParticle requires android.permission.INTERNET permission");
                    }

                    instance = new MParticle(options);
                    instance.mKitManager = new KitFrameworkWrapper(options.getContext(), instance.mMessageManager, instance.Internal().getConfigManager(), instance.getAppStateManager(), instance.mMessageManager.getTaskHandler());
                    instance.mIdentityApi = new IdentityApi(options.getContext(), instance.mAppStateManager, instance.mMessageManager, instance.Internal().mConfigManager, instance.mKitManager);
                    instance.mMessageManager.refreshConfiguration();
                    instance.identify(options);
                    if (options.hasLocationTracking()) {
                        MParticleOptions.LocationTracking locationTracking = options.getLocationTracking();
                        if (locationTracking.enabled) {
                            instance.enableLocationTracking(locationTracking.provider, locationTracking.minTime, locationTracking.minDistance);
                        } else {
                            instance.disableLocationTracking();
                        }
                    }

                    if (instance.Internal().getConfigManager().getLogUnhandledExceptions()) {
                        instance.enableUncaughtExceptionLogging();
                    }

                    if (options.getAttributionListener() != null) {
                        instance.mAttributionListener = options.getAttributionListener();
                    }

                    //there are a number of settings that don't need to be enabled right away
                    //queue up a delayed init and let the start() call return ASAP.
                    instance.mMessageManager.initConfigDelayed();
                    instance.mAppStateManager.init(Build.VERSION.SDK_INT);
                    //We ask to be initialized in Application#onCreate, but
                    //if the Context is an Activity, we know we weren't, so try
                    //to salvage session management via simulating onActivityResume.
                    if (originalContext instanceof Activity) {
                        instance.mAppStateManager.onActivityResumed((Activity) originalContext);
                    }
                    if (options.hasPushRegistration()) {
                        PushRegistrationHelper.PushRegistration pushRegistration = options.getPushRegistration();
                        instance.logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId);
                    }
                    InstallReferrerHelper.fetchInstallReferrer(context, new InstallReferrerHelper.InstallReferrerCallback() {
                        @Override
                        public void onReceived(String installReferrer) {
                            if (MParticle.getInstance() != null) {
                                InstallReferrerHelper.setInstallReferrer(MParticle.getInstance().mAppContext, installReferrer);
                            }
                        }

                        @Override
                        public void onFailed() {
                            //do nothing, it very may well be the case that the InstallReferrer API
                            //is not available, and the user will have to rely upon the ReferrerReceiver
                        }
                    });
                }
            }
        }
        return instance;
    }

    public KitFrameworkWrapper getKitManager() {
        return mKitManager;
    }

    public AppStateManager getAppStateManager() {
        return mAppStateManager;
    }

    /**
     * Retrieve an instance of the MParticle class. {@link #start(MParticleOptions)} must
     * be called prior to this.
     *
     * @return An instance of the mParticle SDK configured with your API key
     */
    public static MParticle getInstance() {
        if (instance == null) {
            Logger.error("Failed to get MParticle instance, getInstance() called prior to start().");
            return null;
        }
        return getInstance(null, null);
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

     * @see MParticleOptions.Builder#androidIdDisabled(boolean)
     */
    public static boolean isAndroidIdDisabled() {
        return sAndroidIdDisabled;
    }

    /**
     * Query whether device performance metrics are disabled
     *
     * @return true if Device Performance Metrics are disabled
     */
    public boolean isDevicePerformanceMetricsDisabled() {
        return mMessageManager.isDevicePerformanceMetricsDisabled();
    }

    /**
     * Explicitly terminate the current user's session.
     */
    private void endSession() {
        if (mInternal.getConfigManager().isEnabled()) {
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    /**
     * Query for a read-only Session API object.
     *
     */
    public Session getCurrentSession() {
        InternalSession session = mAppStateManager.getSession();
        if (session == null || !session.isActive() || session.isTimedOut(mInternal.getConfigManager().getSessionTimeout())) {
            return null;
        } else {
            return new Session(session.mSessionID);
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
        InstallReferrerHelper.setInstallReferrer(mAppContext, referrer);
    }

    /**
     * Retrieve the current install referrer, if it has been set
     *
     * @return The current Install Referrer
     */
    public String getInstallReferrer() {
        return InstallReferrerHelper.getInstallReferrer(mAppContext);
    }

    /**
     * Log an event with an {@link MPEvent} object
     *
     * @param event the event object to log
     */
    public void logEvent(MPEvent event) {
        if (mInternal.getConfigManager().isEnabled() && checkEventLimit()) {
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event, mAppStateManager.getCurrentActivityName());
            Logger.debug("Logged event - \n", event.toString());
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
        if (mInternal.getConfigManager().isEnabled() && checkEventLimit()) {
            MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
            if (user != null) {
                Cart cart = user.getCart();
                if (event.getProductAction() != null) {
                    List<Product> productList = event.getProducts();
                    if (productList != null) {
                        if (event.getProductAction().equalsIgnoreCase(Product.ADD_TO_CART)) {
                            for (Product product : productList) {
                                cart.add(product, false);
                            }
                        } else if (event.getProductAction().equalsIgnoreCase(Product.REMOVE_FROM_CART)) {
                            for (Product product : productList) {
                                cart.remove(product, false);
                            }
                        }
                    }
                }
            }
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event);
            Logger.debug("Logged commerce event - \n", event.toString());
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
            Logger.error( "ValueIncreased must not be null.");
            return;
        }
        if (contextInfo == null) {
            contextInfo = new HashMap<String, String>();
        }
        contextInfo.put(MessageKey.RESERVED_KEY_LTV, valueIncreased.toPlainString());
        contextInfo.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_LTV);
        logEvent(
                new MPEvent.Builder(eventName == null ? "Increase LTV" : eventName, EventType.Transaction)
                        .info(contextInfo)
                        .build()
        );
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
            Logger.error( "screenName is required for logScreen");
            return;
        }
        if (screenEvent.getEventName().length() > Constants.LIMIT_EVENT_NAME) {
            Logger.error( "The screen name was too long. Discarding event.");
            return;
        }
        if (checkEventLimit()) {
            mAppStateManager.ensureActiveSession();
            if (mInternal.getConfigManager().isEnabled()) {
                mMessageManager.logScreen(screenEvent, screenEvent.getNavigationDirection());

                if (null == screenEvent.getInfo()) {
                    Logger.debug("Logged screen: ", screenEvent.toString());
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
        if (mInternal.getConfigManager().isEnabled()) {
            if (MPUtility.isEmpty(breadcrumb)) {
                Logger.error( "breadcrumb is required for leaveBreadcrumb");
                return;
            }
            if (breadcrumb.length() > Constants.LIMIT_EVENT_NAME) {
                Logger.error( "The breadcrumb name was too long. Discarding event.");
                return;
            }
            mAppStateManager.ensureActiveSession();
            mMessageManager.logBreadcrumb(breadcrumb);
            Logger.debug("Logged breadcrumb: " + breadcrumb);
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
        if (mInternal.getConfigManager().isEnabled()) {
            if (MPUtility.isEmpty(message)) {
                Logger.error( "message is required for logErrorEvent");
                return;
            }
            mAppStateManager.ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(errorAttributes);
                mMessageManager.logErrorEvent(message, null, eventDataJSON);
                Logger.debug("Logged error with message: " + (message == null ? "<none>" : message) +
                                " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString())
                );
            }
            mKitManager.logError(message, errorAttributes);
        }
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        if (mInternal.getConfigManager().isEnabled()) {
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
     * Clears the current Attribution Listener
     */
    public void removeAttributionListener() {
        mAttributionListener = null;
    }

    /**
     * Retrieve the current attribution listener
     *
     */
    public AttributionListener getAttributionListener() {
        return mAttributionListener;
    }

    /**
     * Queries the attribution results
     *
     * @return the current attribution results
     */
    public Map<Integer, AttributionResult> getAttributionResults() {
        return mKitManager.getAttributionResults();
    }


    /**
     * Logs an Exception
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     * @param message   the name of the error event to be tracked
     */
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        if (mInternal.getConfigManager().isEnabled()) {
            mAppStateManager.ensureActiveSession();
            if (checkEventLimit()) {
                JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
                mMessageManager.logErrorEvent(message, exception, eventDataJSON);
                Logger.debug(
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
    @SuppressLint("MissingPermission")
    public void enableLocationTracking(String provider, long minTime, long minDistance) {
        if (mInternal.getConfigManager().isEnabled()) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(provider)) {
                    Logger.error( "That requested location provider is not available");
                    return;
                }

                try {
                    if (null == mLocationListener) {
                        mLocationListener = new MPLocationListener(this);
                    } else {
                        // clear the location listener, so it can be added again
                        //noinspection MissingPermission
                        locationManager.removeUpdates(mLocationListener);
                    }
                    //noinspection MissingPermission
                    locationManager.requestLocationUpdates(provider, minTime, minDistance, mLocationListener);
                    locationTrackingEnabled = true;
                }catch (SecurityException se) {

                }
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(PrefKeys.LOCATION_PROVIDER, provider)
                        .putLong(PrefKeys.LOCATION_MINTIME, minTime)
                        .putLong(PrefKeys.LOCATION_MINDISTANCE, minDistance)
                        .apply();

            } catch (SecurityException e) {
                Logger.error( "The app must require the appropriate permissions to track location using this provider");
            }
        }
    }

    /**
     * Disables any mParticle location tracking that had been started
     */
    @SuppressLint("MissingPermission")
    public void disableLocationTracking() {
        if (mLocationListener != null) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);

                if (MPUtility.checkPermission(mAppContext, Manifest.permission.ACCESS_FINE_LOCATION) ||
                        MPUtility.checkPermission(mAppContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    try {
                        //noinspection MissingPermission
                        locationManager.removeUpdates(mLocationListener);
                        locationTrackingEnabled = false;
                    } catch (SecurityException se) {

                    }
                }
                mLocationListener = null;
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.remove(PrefKeys.LOCATION_PROVIDER)
                        .remove(PrefKeys.LOCATION_MINTIME)
                        .remove(PrefKeys.LOCATION_MINDISTANCE)
                        .apply();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Retrieves the current setting of location tracking
     *
     */
    public boolean isLocationTrackingEnabled() {
        return locationTrackingEnabled;
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
            Logger.warning("setSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (value != null){
            value = value.toString();
        }
        if (mInternal.getConfigManager().isEnabled()) {
            mAppStateManager.ensureActiveSession();
            Logger.debug("Set session attribute: " + key + "=" + value);

            if (MPUtility.setCheckedAttribute(mAppStateManager.getSession().mSessionAttributes, key, value, false, false)) {
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
            Logger.warning("incrementSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (mInternal.getConfigManager().isEnabled()) {
            mAppStateManager.ensureActiveSession();
            Logger.debug("Incrementing session attribute: " + key + "=" + value);

            if (MPUtility.setCheckedAttribute(mAppStateManager.getSession().mSessionAttributes, key, value, true, true)) {
                mMessageManager.setSessionAttributes();
            }
        }
    }

    /**
     * Get the current opt-out status for the application.
     *
     * @return the opt-out status
     */
    public Boolean getOptOut() {
        return mInternal.getConfigManager().getOptedOut();
    }

    /**
     * Control the opt-in/opt-out status for the application.
     *
     * @param optOutStatus set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(Boolean optOutStatus) {
        if (optOutStatus != null) {
            if (optOutStatus != mInternal.getConfigManager().getOptedOut()) {
                if (!optOutStatus) {
                    mAppStateManager.ensureActiveSession();
                }
                mMessageManager.optOut(System.currentTimeMillis(), optOutStatus);
                if (optOutStatus && isSessionActive()) {
                    endSession();
                }

                mInternal.getConfigManager().setOptOut(optOutStatus);

                Logger.debug("Set opt-out: " + optOutStatus);
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
        return mKitManager.getSurveyUrl(kitId, null, null);
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
     * Enable mParticle exception handling to automatically log events on uncaught exceptions
     */
    public void enableUncaughtExceptionLogging() {
        mInternal.getConfigManager().enableUncaughtExceptionLogging(true);
    }

    /**
     * Disables mParticle exception handling and restores the original UncaughtExceptionHandler
     */
    public void disableUncaughtExceptionLogging() {
        mInternal.getConfigManager().disableUncaughtExceptionLogging(true);
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
        return mInternal.getConfigManager().isAutoTrackingEnabled();
    }

    /**
     * Retrieves the current session timeout setting in seconds
     *
     * @return The current session timeout setting in seconds
     */
    public int getSessionTimeout() {
        return mInternal.getConfigManager().getSessionTimeout() / 1000;
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
     * Set the minimum log level before the SDK is initialized. The log level
     * is used to moderate the amount of messages that are printed by the SDK
     * to the console. Note that while the SDK is in the Production,
     * <i>log messages at or above this level will be printed</i>.
     *
     * @see MParticle.LogLevel
     *
     * @param level
     */
    public static void setLogLevel(LogLevel level) {
        if (level != null) {
            Logger.setMinLogLevel(level, true);
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

    static String getAppState() {
        String appState = AppStateManager.APP_STATE_NOTRUNNING;
        if (AppStateManager.mInitialized) {
            if (MParticle.getInstance().mAppStateManager.isBackgrounded()) {
                appState = AppStateManager.APP_STATE_BACKGROUND;
            } else {
                appState = AppStateManager.APP_STATE_FOREGROUND;
            }
        }
        return appState;
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
     * Retrieve the underlying object for the given Kit Id for direct calls. Results will be null if
     * kit is not active, or if the application just started, and the kit has not yet been initialized
     *
     * @param kitId
     * @return The Kit object, may be null if kit is not available
     */
    @Nullable
    public Object getKitInstance(int kitId) {
        return mKitManager.getKitInstance(kitId);
    }

    public void logPushRegistration(String instanceId, String senderId) {
        mAppStateManager.ensureActiveSession();
        PushRegistrationHelper.PushRegistration registration = new PushRegistrationHelper.PushRegistration(instanceId, senderId);
        PushRegistrationHelper.setInstanceId(mAppContext, registration);
        String oldInstanceId = mInternal.getConfigManager().getPushToken();
        mMessageManager.setPushRegistrationId(instanceId, true);
        mKitManager.onPushRegistration(instanceId, senderId);

        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        Builder builder;
        if (user != null) {
            builder = new Builder(user);
        } else {
            builder = new Builder();
        }
        Identity().modify(builder
                .pushToken(instanceId, oldInstanceId)
                .build());
    }

    /**
     * Logs a Push Notification displayed to the User
     * @param intent
     */
    public void logNotification(Intent intent) {
        if (mInternal.getConfigManager().isEnabled()) {
            try {
                ProviderCloudMessage message = ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mAppContext));
                mMessageManager.logNotification(message, getAppState());
            } catch (ProviderCloudMessage.InvalidGcmMessageException e) {
                e.printStackTrace();
            }
        }
    }

    void logNotification(ProviderCloudMessage cloudMessage, boolean startSession, String appState, int behavior) {
        if (mInternal.getConfigManager().isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage.getId(), cloudMessage.getRedactedJsonPayload().toString(), appState, behavior);
        }
    }

    void logNotification(ProviderCloudMessage cloudMessage, boolean startSession, String appState) {
        if (mInternal.getConfigManager().isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage, appState);
        }
    }

    /**
     * Logs a Push Notification has been tapped or opened
     * @param intent
     */
    public void logNotificationOpened(Intent intent) {
        try {
            logNotification(ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mAppContext)),
                    true, MParticle.getAppState(), ProviderCloudMessage.FLAG_READ | ProviderCloudMessage.FLAG_DIRECT_OPEN);
        } catch (ProviderCloudMessage.InvalidGcmMessageException e) {
            e.printStackTrace();
        }
    }

    public Internal Internal() {
        return mInternal;
    }

    void refreshConfiguration() {
        Logger.debug("Refreshing configuration...");
        mMessageManager.refreshConfiguration();
    }

    private void identify(final MParticleOptions options) {
        IdentityApiRequest request = options.getIdentifyRequest();
        if (request == null) {
            MParticleUser currentUser = Identity().getCurrentUser();
            if (currentUser != null) {
                request = IdentityApiRequest.withUser(currentUser).build();
            } else {
                request = IdentityApiRequest.withEmptyUser().build();
            }
        }
        MParticleTask<IdentityApiResult> task = instance.mIdentityApi.identify(request);
        if (options.getIdentityTask() != null) {
            task.addFailureListener(new TaskFailureListener() {
               @Override
               public void onFailure(IdentityHttpResponse result) {
                   options.getIdentityTask().setFailed(result);
               }
            });
            task.addSuccessListener(new TaskSuccessListener() {
                @Override
                public void onSuccess(IdentityApiResult result) {
                    options.getIdentityTask().setSuccessful(result);
                }
            });
        }
    }

    @NonNull
    public IdentityApi Identity() {
        return mIdentityApi;
    }

    /**
     * This method will permanently remove ALL MParticle data from the device, included SharedPreferences and Database,
     * and halt any upload or download behavior that may be in process
     *
     * If you have any reference to the MParticle instance, you must remove your reference by setting it to "null",
     * in order to avoid any unexpected behavior
     *
     * The SDK will be shut down and MParticle.getInstance() will return null. MParticle can be restarted by
     * calling MParticle.start()
     *
     * @param context
     */
    public static void reset(Context context) {
        synchronized (MParticle.class) {
            //"commit" will force all async writes stemming from an "apply" call to finish. We need to do this
            //because we need to ensure that the "getMpids()" call is returning all calls that have been made
            // up to this point, otherwise we will miss deleting some files
            context.getSharedPreferences(ConfigManager.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().commit();
            if (instance != null) {
                if (instance.isLocationTrackingEnabled()) {
                    instance.disableLocationTracking();
                }
                instance.mMessageManager.disableHandlers();
                instance.mIdentityApi.reset();
                MParticle.setInstance(null);
            }

            //delete all SharedPreferences files
            Set<String> prefFiles = new HashSet<String>();
            prefFiles.add(ConfigManager.PREFERENCES_FILE);
            prefFiles.add(Constants.PREFS_FILE);
            //urban airship kit shared preference file
            prefFiles.add("com.mparticle.kits.urbanairship");

            String sharedPrefsDirectory = context.getFilesDir().getPath().replace("files", "shared_prefs/");
            File[] files = new File(sharedPrefsDirectory).listFiles();
            for (File file : files) {
                String sharedPreferenceName = file.getPath().replace(sharedPrefsDirectory, "").replace(".xml", "");
                // it is going to be difficult/impossible to come up with a finite list of Kit SharedPreference files, with the custom kits
                // coming, so we will look for any kit shared preference files by their prefix "mp::kit::"
                if (sharedPreferenceName.startsWith("mp::kit::")
                        || sharedPreferenceName.startsWith(ConfigManager.PREFERENCES_FILE + ":")
                        || prefFiles.contains(sharedPreferenceName)) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE);
                    sharedPreferences.edit().commit();
                    if (sharedPreferences != null) {
                        sharedPreferences.edit()
                                .clear()
                                .commit();
                    }
                    file.delete();
                }
            }
            context.getApplicationContext().deleteDatabase(MParticleDatabaseHelper.DB_NAME);
            Logger.debug("MParticle destroyed");
        }
    }

    /**
     *  This method will permanently remove ALL MParticle data from the device, included SharedPreferences and Database,
     * and halt any upload or download behavior that may be in process
     *
     * If you have any reference to the MParticle instance, you must remove your reference by setting it to "null",
     * in order to avoid any unexpected behavior
     *
     * The SDK will be shut down and MParticle.getInstance() will return null. MParticle can be restarted by
     * calling MParticle.start()
     *
     * @param context
     * @param callback A callback that will trigger when the SDK has been fully reset
     */
    public static void reset(final Context context, final ResetListener callback) {
        final HandlerThread handlerThread = new HandlerThread("mParticleShutdownHandler");
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    reset(context);
                    callback.onReset();
                    handlerThread.quit();
                }
            });
    }
    /**
     * Event type to use when logging events.
     *
     * @see #logEvent(MPEvent)
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
     * @see MParticleOptions
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
     * @see IdentityApiRequest
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
        FacebookCustomAudienceId(9),
        Other2(10),
        Other3(11),
        Other4(12);

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
                case 10:
                    return Other2;
                case 11:
                    return Other3;
                case 12:
                    return Other4;
                default:
                    return Other;
            }
        }

        public int getValue() {
            return value;
        }

    }

    /**
     * The Environment in which the SDK and hosting app are running. The SDK
     * automatically detects the Environment based on the <code>DEBUGGABLE</code> flag of your application. The <code>DEBUGGABLE</code>  flag of your
     * application will be <code>TRUE</code> when signing with a debug certificate during development, or if you have explicitly set your
     * application to debug within your AndroidManifest.xml.
     *
     * @see MParticle#start(MParticleOptions)
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
         * @see MParticleOptions.Builder().setUploadInterval(int)
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
        NONE(Integer.MAX_VALUE),
        /**
         * Used for critical issues with the SDK or its configuration.
         */
        ERROR(Log.ERROR),
        /**
         * (default) Used to warn developers of potentially unintended consequences of their use of the SDK.
         */
        WARNING(Log.WARN),
        /**
         * Used to communicate the internal state and processes of the SDK.
         */
        DEBUG(Log.DEBUG),
        /*
         * Used to relay fine-grained issues with the usage of the SDK
         */
        VERBOSE(Log.VERBOSE),
        /*
         * Used to communicate
         */
        INFO(Log.INFO);

        public int logLevel;
        LogLevel(int logLevel) {
            this.logLevel = logLevel;
        }
    }

    void logUnhandledError(Throwable t) {
        if (mInternal.getConfigManager().isEnabled()) {
            mMessageManager.logErrorEvent(t != null ? t.getMessage() : null, t, null, false);
            //we know that the app is about to crash and therefore exit
            mAppStateManager.logStateTransition(Constants.StateTransitionType.STATE_TRANS_EXIT, mAppStateManager.getCurrentActivityName());
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    void installReferrerUpdated() {
        mMessageManager.installReferrerUpdated();
        mKitManager.installReferrerUpdated();
    }

    class Builder extends IdentityApiRequest.Builder {
        Builder(MParticleUser user) {
            super(user);
        }
        Builder() {
            super();
        }

        @Override
        protected IdentityApiRequest.Builder pushToken(String newPushToken, String oldPushToken) {
            return super.pushToken(newPushToken, oldPushToken);
        }
    }

    /**
     * This interface defines constants that can be used to interact with specific 3rd-party services.
     *
     * @see #getSurveyUrl(int)
     */
    public interface ServiceProviders {
        int URBAN_AIRSHIP = 25;
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
        int APPTIMIZE = 105;
        int BUTTON = 1022;
        int LEANPLUM = 98;
        int REVEAL_MOBILE = 112;
        int RADAR = 117;
        int ITERABLE = 1003;
        int SKYHOOK = 121;
        int SINGULAR = 119;
        int ADOBE = 124;
        int INSTABOT = 123;
        int APPSEE = 126;
        int TAPLYTICS = 129;
        int RESPONSYS = 102;
        String BROADCAST_ACTIVE = "MPARTICLE_SERVICE_PROVIDER_ACTIVE_";
        String BROADCAST_DISABLED = "MPARTICLE_SERVICE_PROVIDER_DISABLED_";
    }

    /**
     * This interface defines a series of constants that can be used to specify certain characteristics of a user. There are many 3rd party services
     * that support, for example, specifying a gender of a user. The mParticle platform will look for these constants within the user attributes that
     * you have set for a given user, and forward any attributes to the services that support them.
     *
     * @see com.mparticle.identity.MParticleUser
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

    public interface ResetListener {
        void onReset();
    }

    public class Internal {


        protected ConfigManager mConfigManager;

        protected Internal(ConfigManager configManager) {
            mConfigManager = configManager;
        }

        /**
         * The ConfigManager is tasked with incorporating server-based, run-time, and XML configuration,
         * and surfacing the result/winner.
         */
        public ConfigManager getConfigManager() {
            return mConfigManager;
        }
    }
}
