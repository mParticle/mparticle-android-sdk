package com.mparticle;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

public class MParticleOptions {
    private static final String PREFKEY_API_KEY = "mp_key";
    private static final String PREFKEY_API_SECRET = "mp_secret";
    private BaseIdentityTask identityTask;

    private Context mContext;
    private MParticle.InstallType installType = MParticle.InstallType.AutoDetect;
    private MParticle.Environment environment;
    private String apiKey;
    private String apiSecret;
    private IdentityApiRequest identifyRequest;
    private Boolean devicePerformanceMetricsDisabled = false;
    private Boolean androidIdDisabled = false;
    private Integer uploadInterval = 600;  //seconds
    private Integer sessionTimeout = 60; //seconds
    private Boolean unCaughtExceptionLogging = false;
    private MParticle.LogLevel logLevel = MParticle.LogLevel.DEBUG;
    private AttributionListener attributionListener;

    private MParticleOptions(){}

    public MParticleOptions(Builder builder) {
        this.mContext = builder.mContext;
        if (builder.apiKey != null) {
            this.apiKey = builder.apiKey;
        }
        if (builder.apiSecret != null) {
            this.apiSecret = builder.apiSecret;
        }
        if (builder.installType != null) {
            this.installType = builder.installType;
        }
        if (builder.environment != null) {
            this.environment = builder.environment;
        }
        if (builder.identifyRequest != null) {
            this.identifyRequest = builder.identifyRequest;
        }
        if (builder.identityTask != null) {
            this.identityTask = builder.identityTask;
        }
        if (builder.devicePerformanceMetricsDisabled != null) {
            this.devicePerformanceMetricsDisabled = builder.devicePerformanceMetricsDisabled;
        }
        if (builder.androidIdDisabled != null) {
            this.androidIdDisabled = builder.androidIdDisabled;
        }
        if (builder.uploadInterval != null) {
            if (builder.uploadInterval <= 0) {
                Logger.warning("Upload Interval must be a positive number, disregarding value");
            } else {
                this.uploadInterval = builder.uploadInterval;
            }
        }
        if (builder.sessionTimeout != null) {
            if (builder.sessionTimeout <= 0) {
                Logger.warning("Session Timeout must be a positive number, disregarding value");
            } else {
                this.sessionTimeout = builder.sessionTimeout;
            }
        }
        if (builder.unCaughtExceptionLogging != null) {
            this.unCaughtExceptionLogging = builder.unCaughtExceptionLogging;
        }
        if (builder.logLevel != null) {
            this.logLevel = builder.logLevel;
        }
        if (builder.attributionListener != null) {
            this.attributionListener = builder.attributionListener;
        }
    }

    public static MParticleOptions.Builder builder(Context context) {
        return new Builder(context);
    }

    Context getContext() {
        return mContext;
    }

    public MParticle.InstallType getInstallType() {
        return installType;
    }

    public MParticle.Environment getEnvironment() {
        return environment;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public IdentityApiRequest getIdentifyRequest() {
        return identifyRequest;
    }

    public Boolean isDevicePerformanceMetricsDisabled() {
        return devicePerformanceMetricsDisabled;
    }

    public Boolean isAndroidIdDisabled() {
        return androidIdDisabled;
    }

    public Integer getUploadInterval() {
        return uploadInterval;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public Boolean isUncaughtExceptionLoggingEnabled() {
        return unCaughtExceptionLogging;
    }

    public MParticle.LogLevel getLogLevel() {
        return logLevel;
    }

    public BaseIdentityTask getIdentityTask() {
        return identityTask;
    }

    public AttributionListener getAttributionListener() {
        return attributionListener;
    }

    public static class Builder {
        private Context mContext;
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

        private Builder(Context context) {
            this.mContext = context;
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
        public Builder setDevicePerformanceMetricsDisabled(boolean disabled) {
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
        public Builder setAndroidIdDisabled(boolean disabled) {
            this.androidIdDisabled = disabled;
            return this;
        }

        /**
         * Set the upload interval period to control how frequently uploads occur.
         *
         * @param uploadInterval the number of seconds between uploads
         */
        public Builder setUploadInterval(int uploadInterval) {
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
        public Builder setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder enableUncaughtExceptionLogging(boolean enable) {
            this.unCaughtExceptionLogging = enable;
            return this;
        }

        public Builder setLogLevel(MParticle.LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder setAttributionListener(AttributionListener attributionListener){
            this.attributionListener = attributionListener;
            return this;
        }

        public MParticleOptions build() {
            boolean devMode = MParticle.Environment.Development.equals(environment) || MPUtility.isAppDebuggable(mContext);
            String message;
            if (mContext == null) {
                throw new IllegalArgumentException("mParticle failed to start: context is required.");
            }
            if (MPUtility.isEmpty(apiKey)) {
                    apiKey = getString(PREFKEY_API_KEY);
                if (MPUtility.isEmpty(apiKey)) {
                        message = "Configuration issue: No API key passed to start() or configured as mp_key in resources!";
                        if(devMode) {
                            throw new IllegalArgumentException(message);
                        } else {
                            Logger.error(message);
                        }
                    }
                }
            if (MPUtility.isEmpty(apiSecret)) {
                    apiSecret = getString(PREFKEY_API_SECRET);
                    if (MPUtility.isEmpty(apiSecret)) {
                        message = "Configuration issue: No API secret passed to start() or configured as mp_secret in resources!";
                        if(devMode) {
                            throw new IllegalArgumentException(message);
                        } else {
                            Logger.error(message);
                        }
                    }
                }
            return new MParticleOptions(this);
        }

        private String getString(String key) {
            int id =  this.mContext.getResources().getIdentifier(key, "string", this.mContext.getPackageName());
            if (id == 0) {
                return null;
            }
            try {
                return this.mContext.getResources().getString(id);
            }catch (android.content.res.Resources.NotFoundException nfe){
                return null;
            }
        }
    }

}
