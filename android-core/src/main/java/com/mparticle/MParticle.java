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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.webkit.WebView;

import com.mparticle.commerce.CommerceEvent;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.DeviceAttributes;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPLocationListener;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.internal.listeners.ApiClass;
import com.mparticle.internal.listeners.InternalListenerManager;
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
import java.util.Map;
import java.util.Set;

/**
 * The primary access point to the mParticle SDK. In order to use this class, you must first call {@link #start(MParticleOptions)}. You can then retrieve a reference
 * to an instance of this class via {@link #getInstance()}.
 *
 */
@ApiClass
public class MParticle {

    /**
     * Used to delegate messages, events, user actions, etc on to embedded kits.
     */

    @NonNull protected KitFrameworkWrapper mKitManager;
    /**
     * The state manager is primarily concerned with Activity lifecycle and app visibility in order to manage sessions,
     * automatically log screen views, and pass lifecycle information on top embedded kits.
     */
    @NonNull protected AppStateManager mAppStateManager;

    @NonNull protected ConfigManager mConfigManager;
    @NonNull protected MessageManager mMessageManager;
    private static volatile MParticle instance;
    @NonNull protected SharedPreferences mPreferences;
    @NonNull protected MPLocationListener mLocationListener;
    @NonNull protected Context mAppContext;
    @NonNull protected MPMessagingAPI mMessaging;
    @NonNull protected MPMediaAPI mMedia;
    @NonNull protected MParticleDBManager mDatabaseManager;
    @NonNull protected volatile AttributionListener mAttributionListener;
    @NonNull protected IdentityApi mIdentityApi;
    static volatile boolean sAndroidIdDisabled;
    static volatile boolean sDevicePerformanceMetricsDisabled;
    @NonNull protected boolean locationTrackingEnabled = false;
    @NonNull protected Internal mInternal = new Internal();
    private IdentityStateListener mDeferredModifyPushRegistrationListener;

    protected MParticle() { }
    
    private MParticle(MParticleOptions options) {
        ConfigManager configManager = new ConfigManager(options.getContext(), options.getEnvironment(), options.getApiKey(), options.getApiSecret(), options.getDataplanOptions(), options.getDataplanId(), options.getDataplanVersion(), options);
        configManager.setUploadInterval(options.getUploadInterval());
        configManager.setSessionTimeout(options.getSessionTimeout());
        configManager.setIdentityConnectionTimeout(options.getConnectionTimeout());
        AppStateManager appStateManager = new AppStateManager(options.getContext());
        appStateManager.setConfigManager(configManager);

        mAppContext = options.getContext();
        mConfigManager = configManager;
        mAppStateManager = appStateManager;
        mDatabaseManager = new MParticleDBManager(mAppContext);
        if (options.isUncaughtExceptionLoggingEnabled()) {
            enableUncaughtExceptionLogging();
        } else {
            disableUncaughtExceptionLogging();
        }
        mMessageManager = new MessageManager(configManager, appStateManager, mKitManager, sDevicePerformanceMetricsDisabled, mDatabaseManager, options);
        mConfigManager.setNetworkOptions(options.getNetworkOptions());
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
    @NonNull
    private static MParticle getInstance(@NonNull Context context, @NonNull MParticleOptions options) {
        if (instance == null) {
            synchronized (MParticle.class) {
                if (instance == null) {
                    sDevicePerformanceMetricsDisabled = options.isDevicePerformanceMetricsDisabled();
                    sAndroidIdDisabled = options.isAndroidIdDisabled();
                    setLogLevel(options.getLogLevel());

                    Context originalContext = context;
                    context = context.getApplicationContext();
                    if (!MPUtility.checkPermission(context, Manifest.permission.INTERNET)) {
                        Logger.error("mParticle requires android.permission.INTERNET permission.");
                    }

                    instance = new MParticle(options);
                    instance.mKitManager = new KitFrameworkWrapper(options.getContext(), instance.mMessageManager, instance.Internal().getConfigManager(), instance.Internal().getAppStateManager(), instance.mMessageManager.getTaskHandler(), options);
                    instance.mIdentityApi = new IdentityApi(options.getContext(), instance.mInternal.getAppStateManager(), instance.mMessageManager, instance.mConfigManager, instance.mKitManager, options.getOperatingSystem());
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

                    //There are a number of settings that don't need to be enabled right away
                    //queue up a delayed init and let the start() call return ASAP.
                    instance.mMessageManager.initConfigDelayed();
                    instance.mInternal.getAppStateManager().init(Build.VERSION.SDK_INT);
                    //We ask to be initialized in Application#onCreate, but
                    //if the Context is an Activity, we know we weren't, so try
                    //to salvage session management via simulating onActivityResume.
                    if (originalContext instanceof Activity) {
                        instance.mAppStateManager.onActivityResumed((Activity) originalContext);
                    }
                    PushRegistrationHelper.PushRegistration pushRegistration = options.getPushRegistration();
                    if (pushRegistration != null) {
                        instance.logPushRegistration(pushRegistration.instanceId, pushRegistration.senderId);
                    } else {
                        //Check if Push InstanceId was updated since we last started the SDK and send corresponding modify() request.
                        String oldInstanceId = instance.mConfigManager.getPushInstanceIdBackground();
                        if (oldInstanceId != null) {
                            String newInstanceId = instance.mConfigManager.getPushInstanceId();
                            instance.updatePushToken(newInstanceId, oldInstanceId);
                            instance.mConfigManager.clearPushRegistrationBackground();
                        }
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Retrieve an instance of the MParticle class. {@link #start(MParticleOptions)} must
     * be called prior to this.
     *
     * @return An instance of the mParticle SDK configured with your API key
     */
    @Nullable
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
    public static void setInstance(@Nullable MParticle instance) {
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
     * Set the device's current IMEI.
     *
     * The mParticle SDK does not collect IMEI but you may use this API to provide it.
     *  Collecting the IMEI is generally unnecessary and discouraged for apps in Google Play. For
     *  apps distributed outside of Google Play, IMEI may be necessary for accurate install attribution.
     *
     *  The mParticle SDK does not persist this value - it must be set whenever the SDK is initialized.
     *
     * @param deviceImei a string representing the device's current IMEI, or null to remove clear it.
     */
    public static void setDeviceImei(@Nullable String deviceImei) {
        DeviceAttributes.setDeviceImei(deviceImei);
    }

    /**
     * Get the device's current IMEI.
     *
     * The mParticle SDK does not collect IMEI but you may use the {@link #setDeviceImei(String)} API to provide it..
     *  Collecting the IMEI is generally unnecessary and discouraged for apps in Google Play. For
     *  apps distributed outside of Google Play, IMEI may be necessary for accurate install attribution.
     *
     *  The mParticle SDK does not persist this value - it must be set whenever the SDK is initialized.
     *
     * @return a string representing the device's current IMEI, or null if not set.
     */
    @Nullable
    public static String getDeviceImei() {
        return DeviceAttributes.getDeviceImei();
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
        if (mConfigManager.isEnabled()) {
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    /**
     * Query for a read-only Session API object.
     *
     */
    @Nullable
    public Session getCurrentSession() {
        InternalSession session = mAppStateManager.getSession();
        if (session == null || !session.isActive() || session.isTimedOut(mConfigManager.getSessionTimeout())) {
            return null;
        } else {
            return new Session(session.mSessionID, session.mSessionStartTime);
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
    public void setInstallReferrer(@Nullable String referrer) {
        InstallReferrerHelper.setInstallReferrer(mAppContext, referrer);
    }

    /**
     * Retrieve the current install referrer, if it has been set.
     *
     * @return The current Install Referrer
     */
    @Nullable
    public String getInstallReferrer() {
        return InstallReferrerHelper.getInstallReferrer(mAppContext);
    }

    public void logEvent(@NonNull BaseEvent event) {
        if (event instanceof MPEvent) {
            logMPEvent((MPEvent)event);
        } else if (event instanceof CommerceEvent) {
            logCommerceEvent((CommerceEvent)event);
        } else {
            if (mConfigManager.isEnabled()) {
                mAppStateManager.ensureActiveSession();
                Logger.debug("Logged event - \n", event.toString());
                mKitManager.logEvent(event);
            }
        }
    }

    /**
     * Log an event with an {@link MPEvent} object.
     *
     * @param event the event object to log
     */
    private void logMPEvent(@NonNull MPEvent event) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event, mAppStateManager.getCurrentActivityName());
            Logger.debug("Logged event - \n", event.toString());
            mKitManager.logEvent(event);

        }
    }

    /**
     * Log an e-Commerce related event with a {@link CommerceEvent} object.
     *
     * @param event the event to log
     *
     * @see CommerceEvent
     */
    private void logCommerceEvent(@NonNull CommerceEvent event) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            mMessageManager.logEvent(event);
            Logger.debug("Logged commerce event - \n", event.toString());
            mKitManager.logEvent(event);
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
    public void logLtvIncrease(@NonNull BigDecimal valueIncreased, @Nullable String eventName, @Nullable Map<String, String> contextInfo) {
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
                        .customAttributes(contextInfo)
                        .build()
        );
    }

    /**
     * Logs a screen view event.
     *
     * @param screenName the name of the screen to be tracked
     */
    public void logScreen(@NonNull String screenName) {
        logScreen(screenName, null);
    }

    /**
     * Logs a screen view event.
     *
     * @param screenName the name of the screen to be tracked
     * @param eventData  a Map of data attributes to associate with this screen view
     */
    public void logScreen(@NonNull String screenName, @Nullable Map<String, String> eventData) {
        logScreen(new MPEvent.Builder(screenName).customAttributes(eventData).build().setScreenEvent(true));
    }


    /**
     * Logs a screen view event.
     *
     * @param screenEvent an event object, the name of the event will be used as the screen name
     */
    public void logScreen(@NonNull MPEvent screenEvent) {
        screenEvent.setScreenEvent(true);
        if (MPUtility.isEmpty(screenEvent.getEventName())) {
            Logger.error("screenName is required for logScreen.");
            return;
        }
        if (screenEvent.getEventName().length() > Constants.LIMIT_ATTR_KEY) {
            Logger.error("The screen name was too long. Discarding event.");
            return;
        }
        mAppStateManager.ensureActiveSession();
        if (mConfigManager.isEnabled()) {
            mMessageManager.logScreen(screenEvent, screenEvent.getNavigationDirection());
            Logger.debug("Logged screen: ", screenEvent.toString());
        }
        if (screenEvent.getNavigationDirection()) {
            mKitManager.logScreen(screenEvent);
        }
    }

    /**
     * Leave a breadcrumb to be included with error and exception logging, as well as
     * with regular session events.
     *
     * @param breadcrumb
     */
    public void leaveBreadcrumb(@NonNull String breadcrumb) {
        if (mConfigManager.isEnabled()) {
            if (MPUtility.isEmpty(breadcrumb)) {
                Logger.error( "breadcrumb is required for leaveBreadcrumb.");
                return;
            }
            if (breadcrumb.length() > Constants.LIMIT_ATTR_KEY) {
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
     * Logs an error event.
     *
     * @param message the name of the error event to be tracked
     */
    public void logError(@NonNull String message) {
        logError(message, null);
    }

    /**
     * Logs an error event
     *
     * @param message   the name of the error event to be tracked
     * @param errorAttributes a Map of data attributes to associate with this error
     */
    public void logError(@NonNull String message, @Nullable Map<String, String> errorAttributes) {
        if (mConfigManager.isEnabled()) {
            if (MPUtility.isEmpty(message)) {
                Logger.error("message is required for logErrorEvent.");
                return;
            }
            mAppStateManager.ensureActiveSession();
            JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(errorAttributes);
            mMessageManager.logErrorEvent(message, null, eventDataJSON);
            Logger.debug("Logged error with message: " + (message == null ? "<none>" : message) +
                    " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString())
            );
            mKitManager.logError(message, errorAttributes);
        }
    }

    public void logNetworkPerformance(@NonNull String url, long startTime, @NonNull String method, long length, long bytesSent, long bytesReceived, @Nullable String requestString, int responseCode) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            mMessageManager.logNetworkPerformanceEvent(startTime, method, url, length, bytesSent, bytesReceived, requestString);
            mKitManager.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);
        }
    }

    /**
     * Logs an Exception.
     *
     * @param exception an Exception
     */
    public void logException(@NonNull Exception exception) {
        logException(exception, null, null);
    }

    /**
     * Logs an Exception.
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     */
    public void logException(@NonNull Exception exception, @Nullable Map<String, String> eventData) {
        logException(exception, eventData, null);
    }

    /**
     * Clears the current Attribution Listener.
     */
    public void removeAttributionListener() {
        mAttributionListener = null;
    }

    /**
     * Retrieve the current Attribution Listener.
     *
     */
    @Nullable
    public AttributionListener getAttributionListener() {
        return mAttributionListener;
    }

    /**
     * Queries the attribution results.
     *
     * @return the current attribution results
     */
    @NonNull
    public Map<Integer, AttributionResult> getAttributionResults() {
        return mKitManager.getAttributionResults();
    }


    /**
     * Logs an Exception.
     *
     * @param exception an Exception
     * @param eventData a Map of data attributes
     * @param message   the name of the error event to be tracked
     */
    public void logException(@NonNull Exception exception, @Nullable Map<String, String> eventData, @Nullable String message) {
        if (mConfigManager.isEnabled()) {
            mAppStateManager.ensureActiveSession();
            JSONObject eventDataJSON = MPUtility.enforceAttributeConstraints(eventData);
            mMessageManager.logErrorEvent(message, exception, eventDataJSON);
            Logger.debug(
                    "Logged exception with message: " + (message == null ? "<none>" : message) +
                            " with data: " + (eventDataJSON == null ? "<none>" : eventDataJSON.toString()) +
                            " with exception: " + (exception == null ? "<none>" : exception.getMessage())
            );
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
    public void enableLocationTracking(@NonNull String provider, long minTime, long minDistance) {
        if (mConfigManager.isEnabled()) {
            try {
                LocationManager locationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(provider)) {
                    Logger.error( "That requested location provider is not available.");
                    return;
                }

                try {
                    if (null == mLocationListener) {
                        mLocationListener = new MPLocationListener(this);
                    } else {
                        // Clear the location listener, so it can be added again
                        //noinspection MissingPermission.
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
                Logger.error( "The app must require the appropriate permissions to track location using this provider.");
            }
        }
    }

    /**
     * Disables any mParticle location tracking that had been started.
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
     * Retrieves the current setting of location tracking.
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
    public void setLocation(@Nullable Location location) {
        mMessageManager.setLocation(location);
        mKitManager.setLocation(location);

    }

    /**
     * Set a single <i>session</i> attribute. The attribute will combined with any existing session attributes.
     *
     * @param key   the attribute key
     * @param value the attribute value. This value will be converted to its String representation as dictated by its <code>toString()</code> method.
     */
    public void setSessionAttribute(@NonNull String key, @Nullable Object value) {
        if (key == null) {
            Logger.warning("setSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (value != null){
            value = value.toString();
        }
        if (mConfigManager.isEnabled()) {
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
    public void incrementSessionAttribute(@NonNull String key, int value) {
        if (key == null) {
            Logger.warning("incrementSessionAttribute called with null key. Ignoring...");
            return;
        }
        if (mConfigManager.isEnabled()) {
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
    @NonNull
    public Boolean getOptOut() {
        return mConfigManager.getOptedOut();
    }

    /**
     * Control the opt-in/opt-out status for the application.
     *
     * @param optOutStatus set to <code>true</code> to opt out of event tracking
     */
    public void setOptOut(@NonNull Boolean optOutStatus) {
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
    @Nullable
    public Uri getSurveyUrl(final int kitId) {
        return mKitManager.getSurveyUrl(kitId, null, null);
    }

    /**
     * Get the current Environment that the SDK has interpreted. Will never return AutoDetect.
     *
     * @return the current environment, either production or development
     */
    @NonNull
    public Environment getEnvironment() {
        return ConfigManager.getEnvironment();
    }

    /**
     * Enable mParticle exception handling to automatically log events on uncaught exceptions.
     */
    public void enableUncaughtExceptionLogging() {
        mConfigManager.enableUncaughtExceptionLogging(true);
    }

    /**
     * Disables mParticle exception handling and restores the original UncaughtExceptionHandler.
     */
    public void disableUncaughtExceptionLogging() {
        mConfigManager.disableUncaughtExceptionLogging(true);
    }

    /**
     * Retrieves the current setting of automatic screen tracking.
     *
     * @return The current setting of automatic screen tracking.
     */
    @NonNull
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

    public void getUserSegments(long timeout, @NonNull String endpointId, @NonNull SegmentListener listener) {
        if (mMessageManager != null && mMessageManager.mUploadHandler != null) {
            mMessageManager.mUploadHandler.fetchSegments(timeout, endpointId, listener);
        }
    }

    /**
     * Instrument a WebView so that communication can be facilitated between this Native SDK instance and any
     * instance of the mParticle Javascript SDK is loaded within the provided WebView.
     *
     * @param webView
     */
    @SuppressLint("AddJavascriptInterface")
    @RequiresApi(17)
    public void registerWebView(@NonNull WebView webView) {
        registerWebView(webView, null);
    }

    /**
     * Instrument a WebView so that communication can be facilitated between this Native SDK instance and any
     * instance of the mParticle Javascript SDK is loaded within the provided WebView. Additionally,
     * the "reqiredBridgeName" value will override the generated token which is used by the Javascript SDK
     * to ensure communication only happens across matching Workspaces.
     *
     * Make sure you know what you are doing when you provide a requiredBridgeName, since a mismatch
     * with the Javascript SDK's bridgeName will result in a failure for it to forward calls to the Native SDK
     *
     * @param webView
     * @param requiredBridgeName
     */
    @SuppressLint("AddJavascriptInterface")
    @RequiresApi(17)
    public void registerWebView(@NonNull WebView webView, String requiredBridgeName) {
        MParticleJSInterface.registerWebView(webView, requiredBridgeName);
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
    public static void setLogLevel(@NonNull LogLevel level) {
        if (level != null) {
            Logger.setMinLogLevel(level, true);
        }
    }

    /**
     * Entry point to the Messaging APIs.
     *
     * @return a helper object that allows for interaction with the Messaging APIs
     */
    @NonNull
    public MPMessagingAPI Messaging() {
        if (mMessaging == null){
            mMessaging = new MPMessagingAPI(mAppContext);
        }
        return mMessaging;
    }

    static String getAppState() {
        String appState = AppStateManager.APP_STATE_NOTRUNNING;
        if (AppStateManager.mInitialized) {
            MParticle instance = MParticle.getInstance();
            if (instance != null) {
                if (instance.mAppStateManager.isBackgrounded()) {
                    appState = AppStateManager.APP_STATE_BACKGROUND;
                } else {
                    appState = AppStateManager.APP_STATE_FOREGROUND;
                }
            }
        }
        return appState;
    }

    /**
     * Entry point to the Media APIs.
     *
     * @return a helper object that allows for interaction with the Media APIs
     */
    @NonNull
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
     * kit is not active, or if the application just started, and the kit has not yet been initialized.
     *
     * @param kitId
     * @return The Kit object, may be null if kit is not available
     */
    @Nullable
    public Object getKitInstance(int kitId) {
        return mKitManager.getKitInstance(kitId);
    }

    public void logPushRegistration(@Nullable String instanceId, @Nullable String senderId) {
        mAppStateManager.ensureActiveSession();
        PushRegistrationHelper.PushRegistration registration = new PushRegistrationHelper.PushRegistration(instanceId, senderId);
        String oldInstanceId = mConfigManager.getPushInstanceId();
        mConfigManager.setPushRegistration(registration);
        mMessageManager.setPushRegistrationId(instanceId, true);
        mKitManager.onPushRegistration(instanceId, senderId);
        updatePushToken(instanceId, oldInstanceId);
    }

    /**
     * Logs a Push Notification displayed to the User.
     * @param intent
     */
    public void logNotification(@NonNull Intent intent) {
        if (mConfigManager.isEnabled()) {
            ProviderCloudMessage message = ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mAppContext));
            mMessageManager.logNotification(message, getAppState());
        }
    }

    void logNotification(@NonNull ProviderCloudMessage cloudMessage, boolean startSession, @NonNull String appState, int behavior) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage.getId(), cloudMessage.getRedactedJsonPayload().toString(), appState, behavior);
        }
    }

    void logNotification(@NonNull ProviderCloudMessage cloudMessage, boolean startSession, @NonNull String appState) {
        if (mConfigManager.isEnabled()) {
            if (startSession){
                mAppStateManager.ensureActiveSession();
            }
            mMessageManager.logNotification(cloudMessage, appState);
        }
    }

    /**
     * Logs a Push Notification has been tapped or opened.
     * @param intent
     */
    public void logNotificationOpened(@NonNull Intent intent) {
        logNotification(ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mAppContext)),
                true, MParticle.getAppState(), ProviderCloudMessage.FLAG_READ | ProviderCloudMessage.FLAG_DIRECT_OPEN);
    }

    @NonNull
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
     * and halt any upload or download behavior that may be in process.
     *
     * If you have any reference to the MParticle instance, you must remove your reference by setting it to "null",
     * in order to avoid any unexpected behavior.
     *
     * The SDK will be shut down and MParticle.getInstance() will return null. MParticle can be restarted by
     * calling MParticle.start().
     *
     * @param context
     */
    public static void reset(@NonNull Context context) {
        reset(context, true);
    }

    static void reset(@NonNull Context context, boolean deleteDatabase) {
        synchronized (MParticle.class) {
            //"commit" will force all async writes stemming from an "apply" call to finish. We need to do this
            //because we need to ensure that the "getMpids()" call is returning all calls that have been made
            // up to this point, otherwise we will miss deleting some files.
            context.getSharedPreferences(ConfigManager.PREFERENCES_FILE, Context.MODE_PRIVATE).edit().commit();
            if (instance != null) {
                if (instance.isLocationTrackingEnabled()) {
                    instance.disableLocationTracking();
                }
                instance.mMessageManager.disable();
                instance.mIdentityApi.Internal().reset();
                MParticle.setInstance(null);
            }
            MessageManager.destroy();

            //Delete all SharedPreferences files.
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
                    if (sharedPreferences != null) {
                        sharedPreferences.edit()
                                .clear()
                                .commit();
                    }
                    file.delete();
                }
            }
            if (deleteDatabase) {
                context.deleteDatabase(MParticleDatabaseHelper.getDbName());
            }
        }
    }

    /**
     * This method will permanently remove ALL MParticle data from the device, included SharedPreferences and Database,
     * and halt any upload or download behavior that may be in process.
     *
     * If you have any reference to the MParticle instance, you must remove your reference by setting it to "null",
     * in order to avoid any unexpected behavior.
     *
     * The SDK will be shut down and MParticle.getInstance() will return null. MParticle can be restarted by
     * calling MParticle.start().
     *
     * @param context
     * @param callback A callback that will trigger when the SDK has been fully reset
     */
    public static void reset(@NonNull final Context context, @Nullable final ResetListener callback) {
        final HandlerThread handlerThread = new HandlerThread("mParticleShutdownHandler");
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    reset(context);
                    if (callback != null) {
                        try {
                            callback.onReset();
                        } catch (Exception e) {
                        }
                    }
                    handlerThread.quit();
                }
            });
    }

    /**
     * NOTE: Experimental feature, adding a Listener will slow down the SDK, and should be used only
     * for development purposes. By default, InternalListeners will automatically disable in release builds.
     * In order to override this behavior, use the following adb shell command:
     *
     * {@code `adb shell setprop debug.mparticle.listener {YOUR.PACKAGE.NAME}`}
     *
     * Register an instance of {@link SdkListener} to receive callbacks about SDK events
     * @param context
     * @param listener the SdkListener implementation
     */
    public static void addListener(@NonNull Context context, @NonNull SdkListener listener) {
        InternalListenerManager manager = InternalListenerManager.start(context);
        if (manager != null) {
            manager.addListener(listener);
        }
    }

    /**
     * Remove an instance of {@link SdkListener}.
     */
    public static void removeListener(SdkListener listener) {
        InternalListenerManager manager = InternalListenerManager.start(null);
        if (manager != null) {
            manager.removeListener(listener);
        }
    }

    /**
     * Event type to use when logging {@link MPEvent}s.
     *
     * @see #logEvent(BaseEvent)
     */
    public enum EventType {
        Unknown, Navigation, Location, Search, Transaction, UserContent, UserPreference, Social, Other, Media;

        @NonNull
        public String toString() {
            return name();
        }
    }

    /**
     * To be used when initializing MParticle.
     *
     * @see MParticleOptions
     */
    public enum InstallType {
        /**
         * This is the default value. Using this value will rely on the mParticle SDK to differentiate a new install vs. an upgrade.
         */
        AutoDetect,
        /**
         * In the case where your app has never seen this user before.
         */
        KnownInstall,
        /**
         * In the case where you app has seen this user before.
         */
        KnownUpgrade;

        @NonNull
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
        Other4(12),
        Other5(13),
        Other6(14),
        Other7(15),
        Other8(16),
        Other9(17),
        Other10(18),
        MobileNumber(19),
        PhoneNumber2(20),
        PhoneNumber3(21);


        private final int value;

        IdentityType(int value) {
            this.value = value;
        }

        @NonNull
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
                case 13:
                    return Other5;
                case 14:
                    return Other6;
                case 15:
                    return Other7;
                case 16:
                    return Other8;
                case 17:
                    return Other9;
                case 18:
                    return Other10;
                case 19:
                    return MobileNumber;
                case 20:
                    return PhoneNumber2;
                case 21:
                    return PhoneNumber3;
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
         * Used to relay fine-grained issues with the usage of the SDK.
         */
        VERBOSE(Log.VERBOSE),
        /*
         * Used to communicate.
         */
        INFO(Log.INFO);

        public int logLevel;
        LogLevel(int logLevel) {
            this.logLevel = logLevel;
        }
    }

    public enum OperatingSystem {
        ANDROID,
        FIRE_OS
    }

    void logUnhandledError(Throwable t) {
        if (mConfigManager.isEnabled()) {
            mMessageManager.logErrorEvent(t != null ? t.getMessage() : null, t, null, false);
            //we know that the app is about to crash and therefore exit.
            mAppStateManager.logStateTransition(Constants.StateTransitionType.STATE_TRANS_EXIT, mAppStateManager.getCurrentActivityName());
            mAppStateManager.getSession().mLastEventTime = System.currentTimeMillis();
            mAppStateManager.endSession();
        }
    }

    void installReferrerUpdated() {
        mMessageManager.installReferrerUpdated();
        mKitManager.installReferrerUpdated();
    }

    private void updatePushToken(@Nullable final String newInstanceId, @Nullable final String oldInstanceId) {
        MParticleUser user = Identity().getCurrentUser();
        if (user != null) {
            sendPushTokenModifyRequest(user, newInstanceId, oldInstanceId);
        } else {
            if (mDeferredModifyPushRegistrationListener != null) {
                Identity().removeIdentityStateListener(mDeferredModifyPushRegistrationListener);
                Logger.verbose("Removed deferred logPushRegistration Modify request.");
            }
            mDeferredModifyPushRegistrationListener = new IdentityStateListener() {
                @Override
                public void onUserIdentified(MParticleUser user, MParticleUser previousUser) {
                    if (user != null) {
                        Identity().removeIdentityStateListener(this);
                        Logger.verbose("Sending previously deferred logPushRegistration Modify request.");
                        sendPushTokenModifyRequest(user, newInstanceId, oldInstanceId);
                    }
                }
            };
            Identity().addIdentityStateListener(mDeferredModifyPushRegistrationListener);
            Logger.verbose("Deferred logPushRegistration Modify request, MParticleUser not present.");
        }
    }

    private void sendPushTokenModifyRequest(MParticleUser user, @Nullable String instanceId, @Nullable String oldInstanceId) {
        Identity().modify(new Builder(user)
                .pushToken(instanceId, oldInstanceId)
                .build());
    }

    class Builder extends IdentityApiRequest.Builder {
        Builder(MParticleUser user) {
            super(user);
        }
        Builder() { super(); }

        @Override
        @NonNull protected IdentityApiRequest.Builder pushToken(@Nullable String newPushToken, @Nullable String oldPushToken) {
            return super.pushToken(newPushToken, oldPushToken);
        }
    }

    /**
     * Set or remove the integration attributes for a given integration ID.
     *
     * Integration attributes are keys and values specific to a given integration. For example,
     * many integrations have their own internal user/device ID. mParticle will store integration attributes
     * for a given device, and will be able to use these values for server-to-server communication to services.
     * This is often useful when used in combination with a server-to-server feed, allowing the feed to be enriched
     * with the necessary integration attributes to be properly forwarded to the given integration.
     *
     * @param integrationId mParticle integration ID. This may be a {@link ServiceProviders} value if it's a kit, or
     *                      it could be any mParticle integration.
     * @param attributes a map of attributes that will replace any current attributes. The keys are predefined by mParticle.
     *                   Please consult with the mParticle docs or your solutions consultant for the correct value. You may
     *                   also pass a null or empty map here to remove all of the attributes.
     */
    public void setIntegrationAttributes(int integrationId, @Nullable Map<String, String> attributes) {
        this.Internal().getConfigManager().setIntegrationAttributes(integrationId, attributes);
    }

    /**
     * Queries the current integration attributes for a given integration ID.
     *
     * Integration attributes are keys and values specific to a given integration. For example,
     * many integrations have their own internal user/device ID. mParticle will store integration attributes
     * for a given device, and will be able to use these values for server-to-server communication to services.
     * This is often useful when used in combination with a server-to-server feed, allowing the feed to be enriched
     * with the necessary integration attributes to be properly forwarded to the given integration.
     *
     * @param integrationId mParticle integration ID. This may be a {@link ServiceProviders} value if it's a kit, or
     *                      it could be any mParticle integration.
     */
    @NonNull public Map<String, String> getIntegrationAttributes(int integrationId) {
        return this.Internal().getConfigManager().getIntegrationAttributes(integrationId);
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
        int APPSEE = 126;
        int TAPLYTICS = 129;
        int OPTIMIZELY = 54;
        int RESPONSYS = 102;
        int CLEVERTAP = 135;
        int ONETRUST = 134;
        int GOOGLE_ANALYTICS_FIREBASE = 136;
        int PILGRIM = 211;
        int SWRVE = 1145;
        int BLUESHIFT = 1144;
        int NEURA = 147;
        @NonNull String BROADCAST_ACTIVE = "MPARTICLE_SERVICE_PROVIDER_ACTIVE_";
        @NonNull String BROADCAST_DISABLED = "MPARTICLE_SERVICE_PROVIDER_DISABLED_";
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
         * A special attribute string to specify the mobile number of the consumer's device.
         */
        @NonNull String MOBILE_NUMBER = "$Mobile";
        /**
         * A special attribute string to specify the user's gender.
         */
        @NonNull String GENDER = "$Gender";
        /**
         * A special attribute string to specify the user's age.
         */
        @NonNull String AGE = "$Age";
        /**
         * A special attribute string to specify the user's country.
         */
        @NonNull String COUNTRY = "$Country";
        /**
         * A special attribute string to specify the user's zip code.
         */
        @NonNull String ZIPCODE = "$Zip";
        /**
         * A special attribute string to specify the user's city.
         */
        @NonNull String CITY = "$City";
        /**
         * A special attribute string to specify the user's state or region.
         */
        @NonNull String STATE = "$State";
        /**
         * A special attribute string to specify the user's street address and apartment number.
         */
        @NonNull String ADDRESS = "$Address";
        /**
         * A special attribute string to specify the user's first name.
         */
        @NonNull String FIRSTNAME = "$FirstName";
        /**
         * A special attribute string to specify the user's last name.
         */
        @NonNull String LASTNAME = "$LastName";
    }

    public interface ResetListener {
        void onReset();
    }

    public class Internal {
        protected Internal() { }

        /**
         * The ConfigManager is tasked with incorporating server-based, run-time, and XML configuration,
         * and surfacing the result/winner.
         */
        @NonNull
        public ConfigManager getConfigManager() {
            return mConfigManager;
        }

        @NonNull
        public AppStateManager getAppStateManager() {
            return MParticle.this.mAppStateManager;
        }

        @NonNull
        public KitFrameworkWrapper getKitManager() {
            return mKitManager;
        }

        @NonNull
        public MessageManager getMessageManager() {
            return mMessageManager;
        }

    }
}
