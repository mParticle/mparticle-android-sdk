package com.mparticle;

import android.content.Context;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.PushRegistrationHelper;

import java.util.ArrayList;
import java.util.List;

public class MParticleOptions {
    private static final String PREFKEY_API_KEY = "mp_key";
    private static final String PREFKEY_API_SECRET = "mp_secret";
    private BaseIdentityTask mIdentityTask;

    private Context mContext;
    private MParticle.InstallType mInstallType = MParticle.InstallType.AutoDetect;
    private MParticle.Environment mEnvironment;
    private String mApiKey;
    private String mApiSecret;
    private IdentityApiRequest mIdentifyRequest;
    private Boolean mDevicePerformanceMetricsDisabled = false;
    private Boolean mAndroidIdDisabled = false;
    private Integer mUploadInterval = 600;  //seconds
    private Integer mSessionTimeout = 60; //seconds
    private Boolean mUnCaughtExceptionLogging = false;
    private MParticle.LogLevel mLogLevel = MParticle.LogLevel.DEBUG;
    private AttributionListener mAttributionListener;
    private LocationTracking mLocationTracking;
    private PushRegistrationHelper.PushRegistration mPushRegistration;

    private MParticleOptions(){}

    public MParticleOptions(Builder builder) {
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
                Logger.warning("Upload Interval must be a positive number, disregarding value");
            } else {
                this.mUploadInterval = builder.uploadInterval;
            }
        }
        if (builder.sessionTimeout != null) {
            if (builder.sessionTimeout <= 0) {
                Logger.warning("Session Timeout must be a positive number, disregarding value");
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
     }

    public static MParticleOptions.Builder builder(Context context) {
        return new Builder(context);
    }

    Context getContext() {
        return mContext;
    }

    public MParticle.InstallType getInstallType() {
        return mInstallType;
    }

    public MParticle.Environment getEnvironment() {
        return mEnvironment;
    }

    public String getApiKey() {
        return mApiKey;
    }

    public String getApiSecret() {
        return mApiSecret;
    }

    public IdentityApiRequest getIdentifyRequest() {
        return mIdentifyRequest;
    }

    public Boolean isDevicePerformanceMetricsDisabled() {
        return mDevicePerformanceMetricsDisabled;
    }

    public Boolean isAndroidIdDisabled() {
        return mAndroidIdDisabled;
    }

    public Integer getUploadInterval() {
        return mUploadInterval;
    }

    public Integer getSessionTimeout() {
        return mSessionTimeout;
    }

    public Boolean isUncaughtExceptionLoggingEnabled() {
        return mUnCaughtExceptionLogging;
    }

    public MParticle.LogLevel getLogLevel() {
        return mLogLevel;
    }

    public BaseIdentityTask getIdentityTask() {
        return mIdentityTask;
    }

    public AttributionListener getAttributionListener() {
        return mAttributionListener;
    }

    public boolean hasLocationTracking() {
        return mLocationTracking != null;
    }

    public LocationTracking getLocationTracking() {
        return mLocationTracking;
    }

    public boolean hasPushRegistration() {
        return mPushRegistration != null;
    }

    public PushRegistrationHelper.PushRegistration getPushRegistration() {
        return mPushRegistration;
    }

    public static class Builder {
        private Context context;
        private String apiKey;
        private String apiSecret;
        private MParticle.InstallType installType;
        private MParticle.Environment environment;
        private IdentityApiRequest identifyRequest;
        private Boolean devicePerformanceMetricsDisabled = null;
        private Boolean androidIdDisabled = null;
        private Integer uploadInterval = null;
        private Integer sessionTimeout = null;
        private Boolean unCaughtExceptionLogging = null;
        private MParticle.LogLevel logLevel = null;
        private BaseIdentityTask identityTask;
        private AttributionListener attributionListener;
        private ConfigManager configManager;
        private LocationTracking locationTracking;
        private PushRegistrationHelper.PushRegistration pushRegistration;

        private Builder(Context context) {
            this.context = context;
        }

        public Builder credentials(@NonNull String apiKey, @NonNull String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
            return this;
        }

        public Builder installType(@NonNull MParticle.InstallType installType) {
            this.installType = installType;
            return this;
        }

        public Builder environment(@NonNull MParticle.Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder identify(@NonNull IdentityApiRequest identifyRequest) {
            this.identifyRequest = identifyRequest;
            return this;
        }

        public Builder identifyTask(@NonNull BaseIdentityTask task) {
            this.identityTask = task;
            return this;
        }

        /**
         * Disable CPU and memory usage collection.
         *
         * @param disabled
         */
        public Builder devicePerformanceMetricsDisabled(boolean disabled) {
            this.devicePerformanceMetricsDisabled = disabled;
            return this;
        }

        /**
         *
         * By default, the SDK will collect <a href="http://developer.android.com/reference/android/provider/Settings.Secure.html#ANDROID_ID">Android Id</a> for the purpose
         * of anonymous analytics. If you're not using an mParticle integration that consumes Android ID, the value will be sent to the mParticle
         * servers and then immediately discarded. Use this API if you would like to additionally disable it from being collected entirely.
         *
         * @param disabled true to disable collection (false by default)
         */
        public Builder androidIdDisabled(boolean disabled) {
            this.androidIdDisabled = disabled;
            return this;
        }

        /**
         * Set the upload interval period to control how frequently uploads occur.
         *
         * @param uploadInterval the number of seconds between uploads
         */
        public Builder uploadInterval(int uploadInterval) {
            this.uploadInterval = uploadInterval;
            return this;
        }

        /**
         * Set the user session timeout interval.
         * <p></p>
         * A session has ended once the application has been in the background for more than this timeout
         *
         * @param sessionTimeout Session timeout in seconds
         */
        public Builder sessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder enableUncaughtExceptionLogging(boolean enable) {
            this.unCaughtExceptionLogging = enable;
            return this;
        }

        public Builder logLevel(MParticle.LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder attributionListener(AttributionListener attributionListener){
            this.attributionListener = attributionListener;
            return this;
        }

        public Builder locationTrackingDisabled() {
            this.locationTracking = new LocationTracking(false);
            return this;
        }

        public Builder locationTrackingEnabled(String provider, long minTime, long minDistance) {
            this.locationTracking = new LocationTracking(provider, minTime, minDistance);
            return this;
        }

        public Builder pushRegistration(String instanceId, String senderId) {
            this.pushRegistration = new PushRegistrationHelper.PushRegistration(instanceId, senderId);
            return this;
        }

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
            int id =  this.context.getResources().getIdentifier(key, "string", this.context.getPackageName());
            if (id == 0) {
                return null;
            }
            try {
                return this.context.getResources().getString(id);
            }catch (android.content.res.Resources.NotFoundException nfe){
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
}
