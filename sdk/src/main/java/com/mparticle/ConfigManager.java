package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 1/16/14.
 */
class ConfigManager {
    public static final String CONFIG_JSON = "json";
    private static final String KEY_OPT_OUT = "oo";
    private static volatile ConfigManager instance;

    public static final String KEY_SESSION_UPLOAD_MODE = "su";
    public static final String KEY_UNHANDLED_EXCEPTIONS = "cue";
    public static final String KEY_PUSH_MESSAGES = "pmk";
    public static final String KEY_NETWORK_PERFORMANCE = "cnp";
    public static final String KEY_EMBEDDED_KITS = "eks";

    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    public static final String VALUE_CUE_IGNORE = "forceignore";
    public static final String VALUE_CNP_CAPTURE = "forcetrue";
    public static final String VALUE_CNP_NO_CAPTURE = "forcefalse";

    private final Context mContext;

    private final SharedPreferences mPreferences;
    private static final String PREFERENCES_FILE = "mp_preferences";
    private final EmbeddedKitManager embeddedKitManager;
    private AppConfig localPrefs;
    public static final String DEBUG_SERVICE_HOST = "api-qa.mparticle.com";
    private String[] pushKeys;
    private int uploadMode = Constants.Status.BATCH_READY;
    private String logUnhandledExceptions = VALUE_APP_DEFINED;

    private boolean loaded = false;

    private boolean sendOoEvents;
    private JSONObject providerPersistence;

    public ConfigManager(Context context, String key, String secret, boolean sandboxMode, EmbeddedKitManager embeddedKitManager) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        localPrefs = new AppConfig(mContext, key, secret, sandboxMode);
        this.embeddedKitManager = embeddedKitManager;
    }

    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {
        if (loaded && mPreferences.getString(CONFIG_JSON,"").equals(responseJSON)){
            return;
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(CONFIG_JSON, responseJSON.toString());

        if (responseJSON.has(KEY_SESSION_UPLOAD_MODE)) {
            String sessionUploadMode = responseJSON.getString(KEY_SESSION_UPLOAD_MODE);
            uploadMode = ("batch".equalsIgnoreCase(sessionUploadMode)) ? Constants.Status.BATCH_READY : Constants.Status.READY;
        }

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            logUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES)) {
            JSONArray pushKeyArray = responseJSON.getJSONArray(KEY_PUSH_MESSAGES);
            editor.putString(KEY_PUSH_MESSAGES, pushKeyArray.toString());
            if (pushKeyArray != null){
                pushKeys = new String[pushKeyArray.length()];
            }
            for (int i = 0; i < pushKeyArray.length(); i++){
                pushKeys[i] = pushKeyArray.getString(i);
            }
        }

        if (responseJSON.has(KEY_OPT_OUT)){
            sendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT);
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            setProviderPersistence(new ProviderPersistence(responseJSON, mContext));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
        applyConfig();

        if (responseJSON.has(KEY_EMBEDDED_KITS)) {
            embeddedKitManager.updateKits(responseJSON.getJSONArray(KEY_EMBEDDED_KITS));
        }

        if (responseJSON != null){
            loaded = true;
        }
    }

    public String[] getPushKeys(){
        return pushKeys;
    }

    private void applyConfig() {
        if (getLogUnhandledExceptions()) {
            MParticle.getInstance().enableUncaughtExceptionLogging();
        } else {
            MParticle.getInstance().disableUncaughtExceptionLogging();
        }
    }

    public boolean getLogUnhandledExceptions() {
        if (logUnhandledExceptions.equals(VALUE_APP_DEFINED)) {
            return localPrefs.reportUncaughtExceptions;
        } else {
            return logUnhandledExceptions.equals(VALUE_CUE_CATCH);
        }
    }

    public boolean getSandboxMode() {
        return localPrefs.sandboxMode;
    }

    public int getUploadMode() {
        if (getSandboxMode()){
            return Constants.Status.READY;
        }else{
            return uploadMode;
        }
    }

    public String getApiKey() {
        return localPrefs.mKey;
    }

    public String getApiSecret() {
        return localPrefs.mSecret;
    }

    public long getUploadInterval() {
        if (localPrefs.debug) {
            return 1000 * localPrefs.debugUploadInterval;
        } else {
            return 1000 * localPrefs.uploadInterval;
        }
    }

    public boolean isDebug() {
        return localPrefs.debug;
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

    public int getSessionTimeout() {
        return localPrefs.sessionTimeout * 1000;
    }

    public void setSessionTimeout(int sessionTimeout) {
        localPrefs.sessionTimeout = sessionTimeout;
    }

    public boolean isPushEnabled() {
        return localPrefs.isPushEnabled ||
                (mPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLED, false) && getPushSenderId() != null);
    }

    public String getPushSenderId() {
        if (localPrefs.pushSenderId != null && localPrefs.pushSenderId.length() > 0)
            return localPrefs.pushSenderId;
        else return mPreferences.getString(Constants.PrefKeys.PUSH_SENDER_ID, null);
    }

    public void setPushSenderId(String senderId){
        mPreferences.edit()
                .putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                .commit();
    }

    public void restoreFromCache() {
        try{
            if (!loaded){
                updateConfig(new JSONObject(mPreferences.getString(ConfigManager.CONFIG_JSON, "")));
            }
        }catch(Exception e){

        }
    }

    public String getLicenseKey() {
        return localPrefs.licenseKey;
    }

    public boolean isLicensingEnabled() {
        return localPrefs.licenseKey != null && localPrefs.isLicensingEnabled;
    }

    public void setPushSoundEnabled(boolean pushSoundEnabled) {
        mPreferences.edit()
                .putBoolean(Constants.PrefKeys.PUSH_ENABLE_SOUND, pushSoundEnabled)
                .commit();
    }

    public void setPushVibrationEnabled(boolean pushVibrationEnabled) {
        mPreferences.edit()
                .putBoolean(Constants.PrefKeys.PUSH_ENABLE_VIBRATION, pushVibrationEnabled)
                .commit();
    }

    public boolean getSendOoEvents(){
        boolean optedOut = this.getOptedOut();
        if (!optedOut){
            return true;
        }else{
            return sendOoEvents;
        }

    }

    public void setOptOut(boolean optOut){
        mPreferences
                .edit().putBoolean(Constants.PrefKeys.OPTOUT, optOut).commit();
    }

    public boolean getOptedOut(){
        return mPreferences.getBoolean(Constants.PrefKeys.OPTOUT, false);
    }

    public boolean isAutoTrackingEnabled() {
        return localPrefs.autoTrackingEnabled;
    }

    public boolean isPushSoundEnabled() {
        return mPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLE_SOUND, AppConfig.DEFAULT_ENABLE_PUSH_SOUND);
    }
    public boolean isPushVibrationEnabled() {
        return mPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLE_VIBRATION, AppConfig.DEFAULT_ENABLE_PUSH_VIBRATION);
    }

    public void setPushNotificationIcon(int pushNotificationIcon) {
        mPreferences.edit()
                .putInt(Constants.PrefKeys.PUSH_ICON, pushNotificationIcon)
                .commit();
    }

    public void setPushNotificationTitle(int pushNotificationTitle) {
        mPreferences.edit()
                .putInt(Constants.PrefKeys.PUSH_TITLE, pushNotificationTitle)
                .commit();
    }

    public int getPushTitle() {
        return mPreferences.getInt(Constants.PrefKeys.PUSH_TITLE, 0);
    }

    public int getPushIcon() {
        return mPreferences.getInt(Constants.PrefKeys.PUSH_ICON, 0);
    }

    public int getBreadcrumbLimit() {
        return mPreferences.getInt(Constants.PrefKeys.BREADCRUMB_LIMIT, AppConfig.DEFAULT_BREADCRUMB_LIMIT);
    }

    public void setBreadcrumbLimit(int newLimit){
        mPreferences.edit()
                .putInt(Constants.PrefKeys.BREADCRUMB_LIMIT, newLimit)
                .commit();
    }

    private synchronized void setProviderPersistence(JSONObject persistence){
        providerPersistence = persistence;
    }

    public synchronized JSONObject getProviderPersistence() {
        return providerPersistence;
    }

    public boolean isNetworkPerformanceEnabled() {
        return localPrefs.networkingEnabled;
    }

    public void setNetworkingEnabled(boolean networkingEnabled) {
        this.localPrefs.networkingEnabled = networkingEnabled;
    }

    public void setCookies(JSONObject cookies) {
        mPreferences.edit().putString(Constants.PrefKeys.Cookies, cookies.toString()).commit();
    }

    public JSONObject getCookies() throws JSONException {
        return new JSONObject(mPreferences.getString(Constants.PrefKeys.Cookies, ""));
    }

    public void setMpid(long mpid) {
        mPreferences.edit().putFloat(Constants.PrefKeys.Mpid, mpid).commit();
    }
}
