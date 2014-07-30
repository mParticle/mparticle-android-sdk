package com.mparticle;

import android.content.Context;
import android.util.Log;

/**
 * Created by sdozor on 1/21/14.
 */
class AppConfig {
    public static final String PREFKEY_API_KEY = "mp_key";
    public static final String PREFKEY_API_SECRET = "mp_secret";
    public static final String PREFKEY_EXCEPTIONS = "mp_reportUncaughtExceptions";
    public static final String PREFKEY_SESSION_TIMEOUT = "mp_sessionTimeout";
    public static final String PREFKEY_PROD_UPLOAD_INTERVAL = "mp_productionUploadInterval";
    public static final String PREFKEY_PUSH_ENABLED = "mp_enablePush";
    public static final String PREFKEY_PUSH_SENDER_ID = "mp_pushSenderId";
    public static final String PREFKEY_APP_LICENSE_KEY = "mp_appLicenseKey";
    public static final String PREFKEY_LICENSING_ENABLED = "mp_enableLicenseCheck";
    private static final String PREFKEY_AUTOTRACKING = "mp_enableAutoTracking";
    private static final String PREFKEY_NETWORK_MEASUREMENT = "mp_enableNetworkPerformanceMeasurement";
    private static final String PREFKEY_FORCE_ENVIRONMENT= "mp_forceEnvironment";

    public static final int DEFAULT_SESSION_TIMEOUT = 60;
    public static final int DEFAULT_UPLOAD_INTERVAL = 600;
    public static final boolean DEFAULT_ENABLE_PUSH = false;
    public static final boolean DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS = false;
    public static final boolean DEFAULT_ENABLE_LICENSING = false;
    public static final boolean DEFAULT_ENABLE_AUTO_TRACKING = false;
    public static final boolean DEFAULT_ENABLE_PUSH_SOUND = false;
    public static final boolean DEFAULT_ENABLE_PUSH_VIBRATION = false;
    public static final int DEFAULT_BREADCRUMB_LIMIT = 50;
    public static final boolean DEFAULT_NETWORK_MEASUREMENT = false;


    private final Context mContext;

    public String mKey = null;
    public String mSecret = null;

    public boolean reportUncaughtExceptions;
    public int sessionTimeout;
    public int uploadInterval;
    public boolean isPushEnabled;
    public String pushSenderId;
    public String licenseKey;
    public boolean isLicensingEnabled;
    public boolean autoTrackingEnabled;
    public volatile boolean networkingEnabled;
    public int audienceTimeout = 100;
    public MParticle.Environment forcedEnvironment = null;


    public AppConfig(Context context, String key, String secret) {
        mContext = context;
        if (key == null || secret == null) {
            parseLocalCredentials();
        } else {
            mKey = key;
            mSecret = secret;
        }
        parseLocalSettings();
    }

    private void parseLocalCredentials() {
        mKey = getString(PREFKEY_API_KEY);
        if (mKey == null) {
            Log.e(Constants.LOG_TAG, String.format("Configuration issue: Missing required key: %s", PREFKEY_API_KEY));
            throw new IllegalArgumentException("Configuration issue: Missing API key.");
        }
        mSecret = getString(PREFKEY_API_SECRET);
        if (mSecret == null) {
            Log.e(Constants.LOG_TAG, String.format("Configuration issue: Missing required key: %s", PREFKEY_API_SECRET));
            throw new IllegalArgumentException("Configuration issue: Missing API secret.");
        }
    }

    private void parseLocalSettings() {
        reportUncaughtExceptions = getBoolean(PREFKEY_EXCEPTIONS, DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS);
        sessionTimeout = getInteger(PREFKEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
        uploadInterval = getInteger(PREFKEY_PROD_UPLOAD_INTERVAL, DEFAULT_UPLOAD_INTERVAL);
        isPushEnabled = getBoolean(PREFKEY_PUSH_ENABLED, DEFAULT_ENABLE_PUSH);
        if (isPushEnabled){
            pushSenderId = getString(PREFKEY_PUSH_SENDER_ID);
            if (pushSenderId == null){
                Log.w(Constants.LOG_TAG, "Configuration issue: Push is enabled but no sender id is specified.");
            }
        }

        isLicensingEnabled = getBoolean(PREFKEY_LICENSING_ENABLED, DEFAULT_ENABLE_LICENSING);
        if (isLicensingEnabled){
            licenseKey = getString(PREFKEY_APP_LICENSE_KEY);
            if (licenseKey == null){
                Log.w(Constants.LOG_TAG, "Configuration issue: Licensing enabled but no license key specified.");
            }
        }
        autoTrackingEnabled = getBoolean(PREFKEY_AUTOTRACKING, DEFAULT_ENABLE_AUTO_TRACKING);
        networkingEnabled = getBoolean(PREFKEY_NETWORK_MEASUREMENT, DEFAULT_NETWORK_MEASUREMENT);
        String mode = getString(PREFKEY_FORCE_ENVIRONMENT);
        if (mode != null){
            if (mode.toLowerCase().contains("dev")){
                Log.w(Constants.LOG_TAG, "Forcing SDK into development mode based on configuration XML key: " + PREFKEY_FORCE_ENVIRONMENT + " and value: " + mode);
                forcedEnvironment = MParticle.Environment.Development;
            }else if (mode.toLowerCase().contains("prod")){
                Log.w(Constants.LOG_TAG, "Forcing SDK into production mode based on configuration XML key: " + PREFKEY_FORCE_ENVIRONMENT + " and value: " + mode);
                forcedEnvironment = MParticle.Environment.Production;
            }
        }
    }

    private int getResourceId(String key, String type) {
        return this.mContext.getResources().getIdentifier(key, type, this.mContext.getPackageName());
    }

    public String getString(String key) {
        int id = getResourceId(key, "string");
        if (id == 0) {
            Log.i(Constants.LOG_TAG, String.format("Configuration: Missing key: %s", key));
            return null;
        }
        return this.mContext.getString(id);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        int id = getResourceId(key, "bool");
        if (id == 0) {
            Log.i(Constants.LOG_TAG, String.format("Configuration: Missing key: %s", key));
            return defaultValue;
        }
        return this.mContext.getResources().getBoolean(id);
    }

    public int getInteger(String key, int defaultValue) {
        int id = getResourceId(key, "integer");
        if (id == 0) {
            Log.i(Constants.LOG_TAG, String.format("Configuration: Missing key: %s", key));
            return defaultValue;
        }
        return mContext.getResources().getInteger(id);
    }
}
