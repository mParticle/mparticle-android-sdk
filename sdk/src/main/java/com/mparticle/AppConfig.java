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
    public static final String PREFKEY_DBG_UPLOAD_INTERVAL = "mp_debugUploadInterval";
    public static final String PREFKEY_PROD_UPLOAD_INTERVAL = "mp_productionUploadInterval";
    public static final String PREFKEY_DBG_ENABLED = "mp_enableDebugMode";
    public static final String PREFKEY_PUSH_ENABLED = "mp_enablePush";
    public static final String PREFKEY_PUSH_SENDER_ID = "mp_pushSenderId";
    public static final String PREFKEY_SANDBOX_MODE = "mp_enableSandboxMode";
    public static final String PREFKEY_APP_LICENSE_KEY = "mp_appLicenseKey";
    public static final String PREFKEY_LICENSING_ENABLED = "mp_enableLicenseCheck";
    private static final String PREFKEY_AUTOTRACKING = "mp_enableAutoTracking";

    public static final int DEFAULT_SESSION_TIMEOUT = 60;
    public static final int DEFAULT_UPLOAD_INTERVAL = 600;
    public static final int DEFAULT_DEBUG_UPLOAD_INTERVAL = 10;
    public static final boolean DEFAULT_ENABLE_PUSH = false;
    public static final boolean DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS = false;
    public static final boolean DEFAULT_SANDBOX_MODE = false;
    public static final boolean DEFAULT_ENABLE_DEBUG_MODE = false;
    public static final boolean DEFAULT_ENABLE_LICENSING = false;
    public static final boolean DEFAULT_ENABLE_AUTO_TRACKING = false;

    private final Context mContext;

    public String mKey = null;
    public String mSecret = null;

    public boolean reportUncaughtExceptions;
    public boolean debug;
    public int sessionTimeout;
    public int debugUploadInterval;
    public int uploadInterval;
    public boolean isPushEnabled;
    public String pushSenderId;
    public boolean sandboxMode;
    public String licenseKey;
    public boolean isLicensingEnabled;
    public boolean autoTrackingEnabled;

    public AppConfig(Context context, String key, String secret, boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
        mContext = context;
        if ((key == null) || secret == null) {
            Log.d(Constants.LOG_TAG, context.getString(R.string.error_noservercredentials));
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
            Log.d(Constants.LOG_TAG, String.format(mContext.getString(R.string.error_missingrequiredkey), PREFKEY_API_KEY));
            throw new IllegalArgumentException(mContext.getString(R.string.missing_apikey));
        }
        mSecret = getString(PREFKEY_API_SECRET);
        if (mSecret == null) {
            Log.d(Constants.LOG_TAG, String.format(mContext.getString(R.string.error_missingrequiredkey), PREFKEY_API_KEY));
            throw new IllegalArgumentException(mContext.getString(R.string.missing_apisecret));
        }
    }

    private void parseLocalSettings() {
        debug = getBoolean(PREFKEY_DBG_ENABLED, DEFAULT_ENABLE_DEBUG_MODE);
        reportUncaughtExceptions = getBoolean(PREFKEY_EXCEPTIONS, DEFAULT_REPORT_UNCAUGHT_EXCEPTIONS);
        sessionTimeout = getInteger(PREFKEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
        debugUploadInterval = getInteger(PREFKEY_DBG_UPLOAD_INTERVAL, DEFAULT_DEBUG_UPLOAD_INTERVAL);
        uploadInterval = getInteger(PREFKEY_PROD_UPLOAD_INTERVAL, DEFAULT_UPLOAD_INTERVAL);
        isPushEnabled = getBoolean(PREFKEY_PUSH_ENABLED, DEFAULT_ENABLE_PUSH);
        if (isPushEnabled){
            pushSenderId = getString(PREFKEY_PUSH_SENDER_ID);
            if (pushSenderId == null){
                Log.w(Constants.LOG_TAG, mContext.getString(R.string.error_nosenderid));
            }
        }
        sandboxMode = getBoolean(PREFKEY_SANDBOX_MODE, DEFAULT_SANDBOX_MODE);
        isLicensingEnabled = getBoolean(PREFKEY_LICENSING_ENABLED, DEFAULT_ENABLE_LICENSING);
        if (isLicensingEnabled){
            licenseKey = getString(PREFKEY_APP_LICENSE_KEY);
            if (licenseKey == null){
                Log.d(Constants.LOG_TAG, String.format(mContext.getString(R.string.error_licensingmissingkey), PREFKEY_API_KEY));
            }
        }
        autoTrackingEnabled = getBoolean(PREFKEY_AUTOTRACKING, DEFAULT_ENABLE_AUTO_TRACKING);

    }

    private int getResourceId(String key, String type) {
        return this.mContext.getResources().getIdentifier(key, type, this.mContext.getPackageName());
    }

    private void debugLog(String message) {
        if (debug) {
            Log.d(Constants.LOG_TAG, message);
        }
    }

    public String getString(String key) {
        int id = getResourceId(key, "string");
        if (id == 0) {
            debugLog(String.format(mContext.getString(R.string.error_missingkey), key));
            return null;
        }
        return this.mContext.getString(id);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        int id = getResourceId(key, "bool");
        if (id == 0) {
            debugLog(String.format(mContext.getString(R.string.error_missingkey), key));
            return defaultValue;
        }
        return this.mContext.getResources().getBoolean(id);
    }

    public int getInteger(String key, int defaultValue) {
        int id = getResourceId(key, "integer");
        if (id == 0) {
            debugLog(String.format(mContext.getString(R.string.error_missingkey), key));
            return defaultValue;
        }
        return mContext.getResources().getInteger(id);
    }
}
