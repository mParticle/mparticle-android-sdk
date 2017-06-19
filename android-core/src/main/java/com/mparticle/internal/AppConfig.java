package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MParticle;

/**
 * This class is primarily responsible for parsing and representing XML/resource-based configuration.
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
    private static final String PREFKEY_FORCE_ENVIRONMENT= "mp_environment";

    public static final int DEFAULT_SESSION_TIMEOUT = 60;
    public static final int DEFAULT_UPLOAD_INTERVAL = 600;
    public static final boolean DEFAULT_ENABLE_PUSH = false;
    public static final boolean DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS = false;
    public static final boolean DEFAULT_ENABLE_LICENSING = false;
    public static final boolean DEFAULT_ENABLE_AUTO_TRACKING = false;
    public static final boolean DEFAULT_ENABLE_PUSH_SOUND = false;
    public static final boolean DEFAULT_ENABLE_PUSH_VIBRATION = false;

    private final Context mContext;

    public String mKey = null;
    public String mSecret = null;

    public boolean reportUncaughtExceptions;
    public int sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    public int uploadInterval = DEFAULT_UPLOAD_INTERVAL;
    public boolean isPushEnabled = DEFAULT_ENABLE_PUSH;
    private String pushSenderId = null;
    public String licenseKey = null;
    public boolean isLicensingEnabled = DEFAULT_ENABLE_LICENSING;
    public boolean autoTrackingEnabled = DEFAULT_ENABLE_AUTO_TRACKING;
    public int audienceTimeout = 100;
    private static MParticle.Environment sEnvironment = MParticle.Environment.Production;

    public AppConfig(Context context, MParticle.Environment environment, SharedPreferences preferences, String apiKey, String apiSecret) {
        mContext = context;
        if (environment == null || environment == MParticle.Environment.AutoDetect){
            if (MPUtility.isAppDebuggable(context)){
               sEnvironment = MParticle.Environment.Development;
            }else{
               sEnvironment = MParticle.Environment.Production;
            }
        }else{
            sEnvironment = environment;
        }
        if (!MPUtility.isEmpty(apiKey)) {
            mKey = apiKey;
        }else {
            mKey = getString(PREFKEY_API_KEY, mKey);
            if (mKey == null &&
                    (mKey = preferences.getString(Constants.PrefKeys.API_KEY, mKey)) == null) {
                Logger.error( "Configuration issue: No API key passed to start() or configured as mp_key in resources!");
                mSecret = "";
            }
        }

        if (!MPUtility.isEmpty(apiSecret)) {
            mSecret = apiSecret;
        }else {
            mSecret = getString(PREFKEY_API_SECRET, mSecret);
            if (mSecret == null &&
                    (mSecret = preferences.getString(Constants.PrefKeys.API_SECRET, mSecret)) == null) {
                Logger.error("Configuration issue: No API secret passed to start() or configured as mp_secret in resources!");
                mSecret = "";
            }

        }
        preferences.edit()
                .putString(Constants.PrefKeys.API_KEY, mKey)
                .putString(Constants.PrefKeys.API_SECRET, mSecret)
                .apply();

        reportUncaughtExceptions = getBoolean(PREFKEY_EXCEPTIONS, DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS);

        String mode = getString(PREFKEY_FORCE_ENVIRONMENT, null);
        if (mode != null) {
            if (mode.toLowerCase().contains("dev")) {
                Logger.warning("Forcing SDK into development mode based on configuration XML key: " + PREFKEY_FORCE_ENVIRONMENT + " and value: " + mode);
                sEnvironment = MParticle.Environment.Development;
            } else if (mode.toLowerCase().contains("prod")) {
                Logger.warning("Forcing SDK into production mode based on configuration XML key: " + PREFKEY_FORCE_ENVIRONMENT + " and value: " + mode);
                sEnvironment = MParticle.Environment.Production;
            }
        }
        autoTrackingEnabled = getBoolean(PREFKEY_AUTOTRACKING, DEFAULT_ENABLE_AUTO_TRACKING);
        sessionTimeout = getInteger(PREFKEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
    }

    public void delayedInit() {
        uploadInterval = getInteger(PREFKEY_PROD_UPLOAD_INTERVAL, DEFAULT_UPLOAD_INTERVAL);
        isPushEnabled = getBoolean(PREFKEY_PUSH_ENABLED, DEFAULT_ENABLE_PUSH);
        if (isPushEnabled){
            pushSenderId = getString(PREFKEY_PUSH_SENDER_ID, null);
            if (pushSenderId == null){
                Logger.error("Configuration issue: Push is enabled but no sender id is specified.");
            }
        }

        isLicensingEnabled = getBoolean(PREFKEY_LICENSING_ENABLED, DEFAULT_ENABLE_LICENSING);
        if (isLicensingEnabled){
            licenseKey = getString(PREFKEY_APP_LICENSE_KEY, "");
            if (licenseKey == null){
                Logger.error("Configuration issue: Licensing enabled but no license key specified.");
            }
        }
    }

    private int getResourceId(String key, String type) {
        return this.mContext.getResources().getIdentifier(key, type, this.mContext.getPackageName());
    }

    public String getString(String key, String defaultString) {
        int id = getResourceId(key, "string");
        if (id == 0) {
            if (defaultString != null) {
                Logger.debug(String.format("Configuration: No string resource for: %s, using default: %s", key, defaultString));
            }
            return defaultString;
        }
        try {
            return this.mContext.getResources().getString(id);
        }catch (android.content.res.Resources.NotFoundException nfe){
            return defaultString;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        int id = getResourceId(key, "bool");
        if (id == 0) {
            Logger.debug(String.format("Configuration: No string resource for: %s, using default: %b", key, defaultValue));
            return defaultValue;
        }
        try {
            return this.mContext.getResources().getBoolean(id);
        }catch (android.content.res.Resources.NotFoundException nfe){
            return defaultValue;
        }
    }

    public int getInteger(String key, int defaultValue) {
        int id = getResourceId(key, "integer");
        if (id == 0) {
            Logger.debug(String.format("Configuration: No string resource for: %s, using default: %d", key, defaultValue));
            return defaultValue;
        }
        try {
            return this.mContext.getResources().getInteger(id);
        }catch (android.content.res.Resources.NotFoundException nfe){
            return defaultValue;
        }
    }

    public static MParticle.Environment getEnvironment() {
        return sEnvironment;
    }

    public String getPushSenderId() {
        if (MPUtility.isEmpty(pushSenderId)){
            pushSenderId = getString(PREFKEY_PUSH_SENDER_ID, null);
        }
        return pushSenderId;
    }
}
