package com.mparticle;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by sdozor on 1/21/14.
 */
public class AppConfig {
    public static final String PREFKEY_API_KEY = "mp_key";
    public static final String PREFKEY_API_SECRET = "mp_secret";
    public static final String PREFKEY_EXCEPTIONS = "mp_reportUncaughtExceptions";
    public static final String PREFKEY_SESSION_TIMEOUT = "mp_sessionTimeout";
    public static final String PREFKEY_DBG_UPLOAD_INTERVAL= "mp_debugUploadInterval";
    public static final String PREFKEY_PROD_UPLOAD_INTERVAL = "mp_productionUploadInterval";
    public static final String PREFKEY_USE_SSL = "mp_useSecureTransport";
    public static final String PREFKEY_DBG_ENABLED = "mp_enableDebug";
    public static final String PREFKEY_PUSH_ENABLED = "mp_enablePush";
    private final Context mContext;

    public String mKey = null;
    public String mSecret = null;

    public boolean unhandledExceptions;
    public boolean reportUncaughtExceptions = false;
    public boolean useSecureTransport = true;
    public boolean debug = false;
    public int sessionTimeout = 120;
    public int debugUploadInterval = 10;
    public int uploadInterval = 60;
    public boolean isPushEnabled;

    public AppConfig(Context context) {
        this(context, null, null);
    }

    public AppConfig(Context context, String key, String secret){
        mContext = context;
        if ((key == null) || secret == null){
            Log.d(Constants.LOG_TAG, context.getString(R.string.error_noservercredentials));
            parseLocalCredentials();
        }else{
            mKey = key;
            mSecret = secret;
        }
        parseLocalSettings();
    }

    private void parseLocalCredentials(){
        mKey = getString(PREFKEY_API_KEY);
        if (mKey == null){
            Log.d(Constants.LOG_TAG, String.format(mContext.getString(R.string.error_missingrequiredkey), PREFKEY_API_KEY));
        }
        mSecret = getString(PREFKEY_API_SECRET);
        if (mKey == null){
            Log.d(Constants.LOG_TAG, String.format(mContext.getString(R.string.error_missingrequiredkey), PREFKEY_API_KEY));
        }
    }

    private void parseLocalSettings() {
        debug = getBoolean(PREFKEY_DBG_ENABLED, false);
        reportUncaughtExceptions = getBoolean(PREFKEY_EXCEPTIONS, false);
        useSecureTransport = getBoolean(PREFKEY_USE_SSL, true);
        sessionTimeout = getInteger(PREFKEY_SESSION_TIMEOUT, 120);
        debugUploadInterval = getInteger(PREFKEY_DBG_UPLOAD_INTERVAL, 10);
        uploadInterval = getInteger(PREFKEY_PROD_UPLOAD_INTERVAL, 60);
    }

    private int getResourceId(String key, String type) {
        return this.mContext.getResources().getIdentifier(key, type, this.mContext.getPackageName());
    }

    private void debugLog(String message){
        if (debug){
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

    public int getInteger(String key, int defaultValue){
        int id = getResourceId(key, "integer");
        if (id == 0){
            debugLog(String.format(mContext.getString(R.string.error_missingkey), key));
            return defaultValue;
        }
        return mContext.getResources().getInteger(id);
    }
}
