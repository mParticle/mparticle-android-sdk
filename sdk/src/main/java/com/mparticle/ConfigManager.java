package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 1/16/14.
 */
class ConfigManager {
    private static volatile ConfigManager instance;

    public static final String KEY_SESSION_UPLOAD_MODE = "su";
    public static final String KEY_UNHANDLED_EXCEPTIONS = "cue";
    public static final String KEY_PUSH_MESSAGES = "pmk";
    public static final String KEY_NETWORK_PERFORMANCE = "cnp";

    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    public static final String VALUE_CUE_IGNORE = "forceignore";
    public static final String VALUE_CNP_CAPTURE = "forcetrue";
    public static final String VALUE_CNP_NO_CAPTURE = "forcefalse";

    private final Context mContext;

    private final SharedPreferences mPreferences;
    private static final String PREFERENCES_FILE = "mp_preferences";
    private boolean mSandboxMode;
    private AppConfig localPrefs;
    public static final String DEBUG_SERVICE_HOST = "api-qa.mparticle.com";

    public ConfigManager(Context context, String key, String secret) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        localPrefs = new AppConfig(mContext, key, secret);
    }

    public void updateConfig(JSONObject responseJSON) throws JSONException {

        SharedPreferences.Editor editor = mPreferences.edit();

        if (responseJSON.has(KEY_SESSION_UPLOAD_MODE)) {
            String sessionUploadMode = responseJSON.getString(KEY_SESSION_UPLOAD_MODE);
            int uploadMode = ("batch".equalsIgnoreCase(sessionUploadMode)) ? Constants.Status.BATCH_READY : Constants.Status.READY;
            editor.putInt(KEY_SESSION_UPLOAD_MODE, uploadMode);
        }else{
            editor.remove(KEY_SESSION_UPLOAD_MODE);
        }

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            String logUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
            editor.putString(KEY_UNHANDLED_EXCEPTIONS, logUnhandledExceptions);
        }else{
            editor.remove(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            editor.apply();
        }else{
            editor.commit();
        }

        applyConfig();
    }

    private void applyConfig() {
        if (getLogUnhandledExceptions()){
            MParticle.getInstance(mContext).enableUncaughtExceptionLogging();
        }else{
            MParticle.getInstance(mContext).disableUncaughtExceptionLogging();
        }
    }

    public void setSandboxMode(boolean sandboxMode) {
        mSandboxMode = sandboxMode;
       /* if(mSandboxMode) {
            mUploadMode = Constants.Status.READY;
        } else {
            mUploadMode = Constants.Status.BATCH_READY;
        }*/
    }

    public boolean getLogUnhandledExceptions(){
        String handleExceptions = mPreferences.getString(KEY_UNHANDLED_EXCEPTIONS, VALUE_APP_DEFINED);
        if (handleExceptions.equals(VALUE_APP_DEFINED)){
            return localPrefs.unhandledExceptions;
        }else{
            return handleExceptions.equals(VALUE_CUE_CATCH);
        }
    }

    public boolean getSandboxMode() {
        return mSandboxMode;
    }

    public int getUploadMode() {
        return mPreferences.getInt(KEY_SESSION_UPLOAD_MODE, Constants.Status.BATCH_READY);
    }

    public String getApiKey() {
        return localPrefs.mKey;
    }

    public String getApiSecret() {
        return localPrefs.mSecret;
    }

    public long getUploadInterval() {
        if (localPrefs.debug){
            return 1000 * localPrefs.debugUploadInterval;
        }else{
            return 1000 * localPrefs.uploadInterval;
        }
    }

    public boolean isDebug() {
        return localPrefs.debug;
    }

    public boolean isCompressionEnabled() {
        return true;
    }

    public String getHttpScheme() {
        return localPrefs.useSecureTransport ? "https" : "http";
    }

    public void setDebug(boolean enabled) {
        localPrefs.debug = enabled;
    }

    public void setUploadInterval(int uploadInterval) {
        localPrefs.uploadInterval = uploadInterval;
    }

    public void setDebugUploadInterval(int uploadInterval) {
        localPrefs.debugUploadInterval = uploadInterval;
    }

    public void setUseSsl(boolean useSsl) {
        localPrefs.useSecureTransport = useSsl;
    }

    public long getSessionTimeout(){
        return 1000 * localPrefs.sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        localPrefs.sessionTimeout = sessionTimeout;
    }

    public boolean isPushEnabled() {
        return localPrefs.isPushEnabled;
    }
}
