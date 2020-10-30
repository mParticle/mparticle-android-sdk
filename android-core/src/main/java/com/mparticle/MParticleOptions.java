package com.mparticle;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.networking.NetworkOptions;
import com.mparticle.networking.NetworkOptionsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * class used for passing optional settings to the SDK when it is started.
 */
public class MParticleOptions {
    private static final String PREFKEY_API_KEY = "mp_key";
    private static final String PREFKEY_API_SECRET = "mp_secret";
    private BaseIdentityTask mIdentityTask;

    private Context mContext;
    private MParticle.InstallType mInstallType = MParticle.InstallType.AutoDetect;
    private MParticle.Environment mEnvironment = MParticle.Environment.AutoDetect;
    private String mApiKey;
    private String mApiSecret;
    private IdentityApiRequest mIdentifyRequest;
    private Boolean mDevicePerformanceMetricsDisabled = false;
    private Boolean mAndroidIdDisabled = false;
    private Integer mUploadInterval = ConfigManager.DEFAULT_UPLOAD_INTERVAL;  //seconds
    private Integer mSessionTimeout = ConfigManager.DEFAULT_SESSION_TIMEOUT_SECONDS; //seconds
    private Boolean mUnCaughtExceptionLogging = false;
    private MParticle.LogLevel mLogLevel = MParticle.LogLevel.DEBUG;
    private AttributionListener mAttributionListener;
    private LocationTracking mLocationTracking;
    private PushRegistrationHelper.PushRegistration mPushRegistration;
    private Integer mIdentityConnectionTimeout = ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS;
    private NetworkOptions mNetworkOptions;
    private String mDataplanId;
    private Integer mDataplanVersion;
    private MParticle.OperatingSystem mOperatingSystem = MParticle.OperatingSystem.ANDROID;
    private DataplanOptions mDataplanOptions;

    private MParticleOptions() {
    }

    public MParticleOptions(@NonNull Builder builder) {
        this.mContext = builder.context;
        if (builder.apiKey != null) {
            this.mApiKey = builder.apiKey;
        }
        if (builder.apiSecret != null) {
            this.mApiSecret = builder.apiSecret;
        }
        if (builder.installType != null) {
            this.mInstallType = builder.installType;
        }
        if (builder.environment != null) {
            this.mEnvironment = builder.environment;
        }
        if (builder.identifyRequest != null) {
            this.mIdentifyRequest = builder.identifyRequest;
        }
        if (builder.identityTask != null) {
            this.mIdentityTask = builder.identityTask;
        }
        if (builder.devicePerformanceMetricsDisabled != null) {
            this.mDevicePerformanceMetricsDisabled = builder.devicePerformanceMetricsDisabled;
        }
        if (builder.androidIdDisabled != null) {
            this.mAndroidIdDisabled = builder.androidIdDisabled;
        }
        if (builder.uploadInterval != null) {
            if (builder.uploadInterval <= 0) {
                Logger.warning("Upload Interval must be a positive number, disregarding value.");
            } else {
                this.mUploadInterval = builder.uploadInterval;
            }
        }
        if (builder.sessionTimeout != null) {
            if (builder.sessionTimeout <= 0) {
                Logger.warning("Session Timeout must be a positive number, disregarding value.");
            } else {
                this.mSessionTimeout = builder.sessionTimeout;
            }
        }
        if (builder.unCaughtExceptionLogging != null) {
            this.mUnCaughtExceptionLogging = builder.unCaughtExceptionLogging;
        }
        if (builder.logLevel != null) {
            this.mLogLevel = builder.logLevel;
        }
        if (builder.attributionListener != null) {
            this.mAttributionListener = builder.attributionListener;
        }
        if (builder.locationTracking != null) {
            this.mLocationTracking = builder.locationTracking;
        }
        if (builder.pushRegistration != null) {
            this.mPushRegistration = builder.pushRegistration;
        }
        if (builder.identityConnectionTimeout != null && builder.identityConnectionTimeout >= ConfigManager.MINIMUM_CONNECTION_TIMEOUT_SECONDS) {
            this.mIdentityConnectionTimeout = builder.identityConnectionTimeout;
        } else if (builder.identityConnectionTimeout != null) {
            Logger.warning(String.format("Connection Timeout milliseconds must be a positive number, greater than %s second. Defaulting to %s seconds", String.valueOf(ConfigManager.MINIMUM_CONNECTION_TIMEOUT_SECONDS), String.valueOf(ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS)));
        }
        if (builder.operatingSystem != null) {
            this.mOperatingSystem = builder.operatingSystem;
        }
        this.mNetworkOptions = NetworkOptionsManager.validateAndResolve(builder.networkOptions);
        this.mDataplanId = builder.dataplanId;
        this.mDataplanVersion = builder.dataplanVersion;
        this.mDataplanOptions = builder.dataplanOptions;
    }

    /**
     * @param context
     * @return
     */
    @NonNull
    public static MParticleOptions.Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Query the InstallType.
     */
    @NonNull
    public MParticle.InstallType getInstallType() {
        return mInstallType;
    }

    /**
     * Query the Environment.
     * @return
     */
    @NonNull
    public MParticle.Environment getEnvironment() {
        return mEnvironment;
    }

    /**
     * Query the API Key.
     * @return
     */
    @NonNull
    public String getApiKey() {
        return mApiKey;
    }

    /**
     * Query the API Secret.
     * @return
     */
    @NonNull
    public String getApiSecret() {
        return mApiSecret;
    }

    /**
     * Query the Identify Request.
     * @return
     */
    @Nullable
    public IdentityApiRequest getIdentifyRequest() {
        return mIdentifyRequest;
    }

    /**
     * Query whether device performance metrics are enabled or disabled.
     * @return true if the are disabled, false if they are enabled
     */
    @NonNull
    public Boolean isDevicePerformanceMetricsDisabled() {
        return mDevicePerformanceMetricsDisabled;
    }

    /**
     * Query whether Android Id collection is enabled or disabled.
     * @return true if collection is disabled, false if it is enabled
     */
    @NonNull
    public Boolean isAndroidIdDisabled() {
        return mAndroidIdDisabled;
    }

    /**
     * Query the uploadInterval.
     * @return the upload interval, in seconds
     * @return the upload interval, in seconds
     */
    @NonNull
    public Integer getUploadInterval() {
        return mUploadInterval;
    }

    @NonNull
    public Integer getSessionTimeout() {
        return mSessionTimeout;
    }

    @NonNull
    public Boolean isUncaughtExceptionLoggingEnabled() {
        return mUnCaughtExceptionLogging;
    }

    @NonNull
    public MParticle.LogLevel getLogLevel() {
        return mLogLevel;
    }

    @Nullable
    public BaseIdentityTask getIdentityTask() {
        return mIdentityTask;
    }

    @Nullable
    public AttributionListener getAttributionListener() {
        return mAttributionListener;
    }

    public boolean hasLocationTracking() {
        return mLocationTracking != null;
    }

    @Nullable
    public LocationTracking getLocationTracking() {
        return mLocationTracking;
    }

    @Nullable
    public PushRegistrationHelper.PushRegistration getPushRegistration() {
        return mPushRegistration;
    }

    public int getConnectionTimeout() {
        return mIdentityConnectionTimeout;
    }

    @NonNull
    public NetworkOptions getNetworkOptions() {
        return mNetworkOptions;
    }

    @Nullable
    public String getDataplanId() {
        return mDataplanId;
    }

    @Nullable
    public Integer getDataplanVersion() {
        return mDataplanVersion;
    }

    @NonNull
    public MParticle.OperatingSystem getOperatingSystem() {
        return mOperatingSystem;
    }

    @Nullable
    public DataplanOptions getDataplanOptions() {
        return mDataplanOptions;
    }

    public static class Builder {
        private Context context;
        String apiKey;
        String apiSecret;
        private MParticle.InstallType installType;
        private MParticle.Environment environment;
        private IdentityApiRequest identifyRequest;
        private Boolean devicePerformanceMetricsDisabled = null;
        private Boolean androidIdDisabled = null;
        private Integer uploadInterval = null;
        private Integer sessionTimeout = null;
        private Boolean unCaughtExceptionLogging = null;
        private MParticle.LogLevel logLevel = null;
        BaseIdentityTask identityTask;
        private AttributionListener attributionListener;
        private ConfigManager configManager;
        private LocationTracking locationTracking;
        private PushRegistrationHelper.PushRegistration pushRegistration;
        private Integer identityConnectionTimeout = null;
        private NetworkOptions networkOptions;
        private String dataplanId;
        private Integer dataplanVersion;
        private MParticle.OperatingSystem operatingSystem;
        private DataplanOptions dataplanOptions;

        private Builder(Context context) {
            this.context = context;
        }

        /**
         * Register an Api Key and Secret to be used for the SDK. This is a required field, and your
         * app will not function properly if you do not provide a valid Key and Secret.
         * @param apiKey the Api Key
         * @param apiSecret the Api Secret
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder credentials(@NonNull String apiKey, @NonNull String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            return this;
        }

        /**
         * Indicate a known {@link com.mparticle.MParticle.InstallType}. If this method is not used,
         * a default type of MParticle.InstallType.AutoDetect will be used.
         *
         * @param installType
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see com.mparticle.MParticle.InstallType
         */
        @NonNull
        public Builder installType(@NonNull MParticle.InstallType installType) {
            this.installType = installType;
            return this;
        }

        /**
         * Indicate a known {@link com.mparticle.MParticle.Environment} the Application will be running in. If this method is not used.
         * a default Environment of MParticle.Environment.AutoDetect will be used.
         * @param environment
         * @return
         */
        @NonNull
        public Builder environment(@NonNull MParticle.Environment environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Register an IdentityApiRequest which will be passed to an {@link com.mparticle.identity.IdentityApi#identify(IdentityApiRequest)}
         * request when the SDK starts in order to interact with the results of this call, without registering
         * a global listener in {@link com.mparticle.identity.IdentityApi#addIdentityStateListener(IdentityStateListener)}, register
         * a BaseIdentityTask with {@link #identifyTask(BaseIdentityTask)}. If this method is not called,
         * an Identify request using the most recent current user will be used, or if this is a first-run,
         * and empty request will be used.
         *
         * @param identifyRequest
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see IdentityApiRequest
         */
        @NonNull
        public Builder identify(@NonNull IdentityApiRequest identifyRequest) {
            this.identifyRequest = identifyRequest;
            return this;
        }

        /**
         * Register an BaseIdentityTask, which can be used to interact with the asynchronous results
         * of an {@link #identify(IdentityApiRequest)} request.
         *
         * @param task
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see BaseIdentityTask
         */
        @NonNull
        public Builder identifyTask(@NonNull BaseIdentityTask task) {
            this.identityTask = task;
            return this;
        }

        /**
         * Disable CPU and memory usage collection.
         *
         * @param disabled
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder devicePerformanceMetricsDisabled(boolean disabled) {
            this.devicePerformanceMetricsDisabled = disabled;
            return this;
        }

        /**
         * By default, the SDK will collect <a href="http://developer.android.com/reference/android/provider/Settings.Secure.html#ANDROID_ID">Android Id</a> for the purpose
         * of anonymous analytics. If you're not using an mParticle integration that consumes Android ID, the value will be sent to the mParticle
         * servers and then immediately discarded. Use this API if you would like to additionally disable it from being collected entirely.
         *
         * @param disabled true to disable collection (false by default)
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder androidIdDisabled(boolean disabled) {
            this.androidIdDisabled = disabled;
            return this;
        }

        /**
         * Set the upload interval period to control how frequently uploads occur.
         *
         * @param uploadInterval the number of seconds between uploads
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder uploadInterval(int uploadInterval) {
            this.uploadInterval = uploadInterval;
            return this;
        }

        /**
         * Set the user session timeout interval.
         * <p></p>
         * A session has ended once the application has been in the background for more than this timeout.
         *
         * @param sessionTimeout Session timeout in seconds
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder sessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        /**
         * Enable or disable mParticle exception handling to automatically log events on uncaught exceptions.
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder enableUncaughtExceptionLogging(boolean enable) {
            this.unCaughtExceptionLogging = enable;
            return this;
        }

        /**
         * Set the minimum log level for the SDK. The log level
         * is used to moderate the amount of messages that are printed by the SDK
         * to the console. Note that while the SDK is in the Production,
         * <i>log messages at or above this level will be printed</i>.
         *
         * @param logLevel the preferred level of logging
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see MParticle.LogLevel
         */
        @NonNull
        public Builder logLevel(@NonNull MParticle.LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Register a callback for when an attribution is received.
         * @param attributionListener an instance of the AttributionListener callback
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see AttributionListener
         */
        @NonNull
        public Builder attributionListener(@Nullable AttributionListener attributionListener) {
            this.attributionListener = attributionListener;
            return this;
        }

        /**
         * Disables Location tracking.
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder locationTrackingDisabled() {
            this.locationTracking = new LocationTracking(false);
            return this;
        }

        /**
         * Enables location tracking given a provider and update frequency criteria. The provider must
         * be available and the correct permissions must have been requested within your application's manifest XML file.
         *
         * @param provider    the provider key
         * @param minTime     the minimum time (in milliseconds) to trigger an update
         * @param minDistance the minimum distance (in meters) to trigger an update
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder locationTrackingEnabled(@NonNull String provider, long minTime, long minDistance) {
            this.locationTracking = new LocationTracking(provider, minTime, minDistance);
            return this;
        }

        /**
         * Manually log a push registration.
         * @param instanceId the Instance Id of the push token
         * @param senderId the Sender Id of the push token
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder pushRegistration(@NonNull String instanceId, @NonNull String senderId) {
            this.pushRegistration = new PushRegistrationHelper.PushRegistration(instanceId, senderId);
            return this;
        }

        /**
         * Set the user connection timeout interval.
         * <p></p>
         * A connection to the server closes after this timeout expires, for each call.
         *
         * @param  identityConnectionTimeout the connection timeout for Identity server calls, in seconds
         *
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder identityConnectionTimeout(int identityConnectionTimeout) {
            this.identityConnectionTimeout = identityConnectionTimeout;
            return this;
        }

        @NonNull
        public Builder networkOptions(@Nullable NetworkOptions networkOptions) {
            this.networkOptions = networkOptions;
            return this;
        }

        @NonNull
        public Builder dataplan(@Nullable String dataplanId, @Nullable Integer dataplanVersion) {
            this.dataplanId = dataplanId;
            this.dataplanVersion = dataplanVersion;
            return this;
        }

        /**
         * Set the Operating System. Defaults to {@link MParticle.OperatingSystem#ANDROID}
         * @param operatingSystem
         * @return
         */
        @NonNull
        public Builder operatingSystem(MParticle.OperatingSystem operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        /**
         * Set the {@link com.mparticle.MParticleOptions.DataplanOptions}. This object is used to
         * load a dataplan for the purpose of blocking unplanned attributes and/or events from being forwarded to kit integrations.
         * When set, this will override any block settings that have been configured in the mParticle dashboard.
         * @param dataplanOptions
         * @return
         */
        @NonNull
        public Builder dataplanOptions(DataplanOptions dataplanOptions) {
            this.dataplanOptions = dataplanOptions;
            return this;
        }

        /**
         * Builds this Builder into an MParticleOptions object which can be used to start the SDK.
         *
         * @return MParticleOptions instance
         */
        @NonNull
        public MParticleOptions build() {
            boolean devMode = MParticle.Environment.Development.equals(environment) || MPUtility.isAppDebuggable(context);
            String message;
            if (context == null) {
                throw new IllegalArgumentException("mParticle failed to start: context is required.");
            }
            if (MPUtility.isEmpty(apiKey)) {
                apiKey = getString(PREFKEY_API_KEY);
                if (MPUtility.isEmpty(apiKey)) {
                    apiKey = getConfigManager().getApiKey();
                    if (MPUtility.isEmpty(apiKey)) {
                        message = "Configuration issue: No API key passed to start() or configured as mp_key in resources!";
                        if (devMode) {
                            throw new IllegalArgumentException(message);
                        } else {
                            Logger.error(message);
                        }
                    }
                }
            }
            if (MPUtility.isEmpty(apiSecret)) {
                apiSecret = getString(PREFKEY_API_SECRET);
                if (MPUtility.isEmpty(apiSecret)) {
                    apiSecret = getConfigManager().getApiSecret();
                    if (MPUtility.isEmpty(apiSecret)) {
                        message = "Configuration issue: No API secret passed to start() or configured as mp_secret in resources!";
                        if (devMode) {
                            throw new IllegalArgumentException(message);
                        } else {
                            Logger.error(message);
                        }
                    }
                }
            }
            return new MParticleOptions(this);
        }

        private String getString(String key) {
            int id = this.context.getResources().getIdentifier(key, "string", this.context.getPackageName());
            if (id == 0) {
                return null;
            }
            try {
                return this.context.getResources().getString(id);
            } catch (android.content.res.Resources.NotFoundException nfe) {
                return null;
            }
        }

        private ConfigManager getConfigManager() {
            if (configManager == null) {
                configManager = new ConfigManager(context);
            }
            return configManager;
        }
    }

    static class LocationTracking {
        boolean enabled = true;
        String provider;
        long minTime;
        long minDistance;

        protected LocationTracking(boolean enabled) {
            this.enabled = enabled;
        }

        protected LocationTracking(String provider, long minTime, long minDistance) {
            this.provider = provider;
            this.minTime = minTime;
            this.minDistance = minDistance;
        }
    }

    public static class DataplanOptions {
        private JSONObject dataplan;
        private boolean blockUserAttributes;
        private boolean blockUserIdentities;
        private boolean blockEventAttributes;
        private boolean blockEvents;

        private DataplanOptions(@NonNull Builder builder) {
            dataplan = builder.dataplanVersion;
            blockUserAttributes = builder.blockUserAttributes;
            blockUserIdentities = builder.blockUserIdentities;
            blockEventAttributes = builder.blockEventAttributes;
            blockEvents = builder.blockEvents;
        }

        /**
         * Query the dataplan version document
         * @return the dataplan version as a JSONObject
         */
        @NonNull
        public JSONObject getDataplan() {
            return dataplan;
        }

        /**
         * Query whether unplanned user attributes should be blocked
         * @return boolean where true indicates blocking should occur
         */
        public boolean isBlockUserAttributes() {
            return blockUserAttributes;
        }

        /**
         * Query whether unplanned user identities should be blocked
         * @return boolean where true indicates blocking should occur
         */
        public boolean isBlockUserIdentities() {
            return blockUserIdentities;
        }

        /**
         * Query whether unplanned event attributes should be blocked
         * @return boolean where true indicates blocking should occur
         */
        public boolean isBlockEventAttributes() {
            return blockEventAttributes;
        }

        /**
         * Query whether unplanned events should be blocked
         * @return boolean where true indicates blocking should occur
         */
        public boolean isBlockEvents() {
            return blockEvents;
        }

        @Override
        public String toString() {
            String dataplanString = null;
            try {
                dataplanString = dataplan.toString(4);
            } catch (JSONException e) {
                dataplanString = "Unable to print Dataplan";
            }
            return "DataplanOptions {" +
                    "\n\tblockUserAttributes=" + blockUserAttributes +
                    ", \n\tblockUserIdentities=" + blockUserIdentities +
                    ", \n\tblockEventAttributes=" + blockEventAttributes +
                    ", \n\tblockEvents=" + blockEvents +
                    ",\n\tdataplan=" + dataplanString +
                    "\n}";
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private JSONObject dataplanVersion;
            private boolean blockUserAttributes;
            private boolean blockUserIdentities;
            private boolean blockEventAttributes;
            private boolean blockEvents;

            private Builder() {}

            /**
             * Sets/Gets the Data Plan Version to use when evaluating block and transformation settings
             * @param dataplanVersion
             * @return the Builder instance
             */
            public Builder dataplanVersion(String dataplanVersion) {
                try {
                    this.dataplanVersion = new JSONObject(dataplanVersion);
                } catch (JSONException e) {
                    Logger.error("Unable to parse dataplan json. Dataplan will not be applied");
                }
                return this;
            }

            /**
             * Sets/Gets the Data Plan Version to use when evaluating block and transformation settings
             * @param dataplanVersion
             * @return the Builder instance
             */
            public Builder dataplanVersion(JSONObject dataplanVersion) {
                this.dataplanVersion = dataplanVersion;
                return this;
            }

            /**
             * Sets/Gets the Data Plan Version to use when evaluating block and transformation settings
             * @param dataplanVersion
             * @return the Builder instance
             */
            public Builder dataplanVersion(Map<String, Object> dataplanVersion) {
                try {
                    this.dataplanVersion = new JSONObject(dataplanVersion);
                } catch (Exception e) {
                    Logger.error("Unable to parse dataplan json. Dataplan will not be applied");
                }
                return this;
            }

            /**
             * This flag determines if unplanned user attributes should be blocked
             * @param blockUserAttributes
             * @return the Builder instance
             */
            public Builder blockUserAttributes(boolean blockUserAttributes) {
                this.blockUserAttributes = blockUserAttributes;
                return this;
            }

            /**
             * This flag determines if unplanned user identities should be blocked
             * @param blockUserIdentities
             * @return the Builder instance
             */
            public Builder blockUserIdentities(boolean blockUserIdentities) {
                this.blockUserIdentities = blockUserIdentities;
                return this;
            }

            /**
             * This flag determines if unplanned event attributes should be blocked
             * @param blockEventAttributes
             * @return the Builder instance
             */
            public Builder blockEventAttributes(boolean blockEventAttributes) {
                this.blockEventAttributes = blockEventAttributes;
                return this;
            }

            /**
             * This flag determines if unplanned events should be blocked
             * @param blockEvents
             * @return the Builder instance
             */
            public Builder blockEvents(boolean blockEvents) {
                this.blockEvents = blockEvents;
                return this;
            }

            /**
             * Transform the Builder instance into an immutable {@link DataplanOptions} instance.
             * This step will check that a valid dataplan verion has been set and will return null if
             * it has not
             * @return the DataplanOptions instance, or null if a valid dataplan version was not present
             */
            @Nullable
            public DataplanOptions build() {
                if (MPUtility.isEmpty(dataplanVersion)) {
                    String message = "Configuration issue: dataplan is not required, but it may not be empty. Ignoring Dataplan";
                    if (MPUtility.isDevEnv()) {
                        throw new IllegalArgumentException(message);
                    } else {
                        Logger.error(message);
                    }
                    return null;
                }
                return new DataplanOptions(this);
            }
        }
    }
}
