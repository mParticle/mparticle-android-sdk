package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 1/16/14.
 */
class ConfigManager {
    public static final String CONFIG_JSON = "json";
    private static final String KEY_OPT_OUT = "oo";
    public static final String KEY_UNHANDLED_EXCEPTIONS = "cue";
    public static final String KEY_PUSH_MESSAGES = "pmk";
    public static final String KEY_NETWORK_PERFORMANCE = "cnp";
    public static final String KEY_EMBEDDED_KITS = "eks";
    private static final String KEY_ADTRUTH = "atc";
    private static final String KEY_ADTRUTH_URL = "cp";
    private static final String KEY_ADTRUTH_INTERVAL = "ci";
    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    public static final String VALUE_CUE_IGNORE = "forceignore";
    public static final String VALUE_CNP_CAPTURE = "forcetrue";
    public static final String VALUE_CNP_NO_CAPTURE = "forcefalse";
    private static final String PREFERENCES_FILE = "mp_preferences";
    private static final int DEVMODE_UPLOAD_INTERVAL_MILLISECONDS = 5 * 1000;

    private Context mContext;

    private SharedPreferences mPreferences;

    private EmbeddedKitManager mEmbeddedKitManager;
    private AppConfig mLocalPrefs;
    private String[] mPushKeys;
    private String mLogUnhandledExceptions = VALUE_APP_DEFINED;

    private boolean mLoaded = false;

    private boolean mSendOoEvents;
    private JSONObject mProviderPersistence;
    private String mNetworkPerformance = "";
    private Boolean mIsDebugEnvironment = null;

    private AdtruthConfig adtruth;

    private ConfigManager(){

    }

    public ConfigManager(Context context, String key, String secret, EmbeddedKitManager embeddedKitManager) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        mLocalPrefs = new AppConfig(mContext, key, secret);
        mEmbeddedKitManager = embeddedKitManager;
    }

    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {
        //Work-around caching mechanism:
        //Clear out values that change on every config request,
        //so that the cache isn't prematurely busted.
        responseJSON.remove("id");
        responseJSON.remove("ct");
        if (mLoaded && mPreferences.getString(CONFIG_JSON,"").equals(responseJSON.toString())){
            return;
        }

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(CONFIG_JSON, responseJSON.toString());

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            mLogUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES)) {
            JSONArray pushKeyArray = responseJSON.getJSONArray(KEY_PUSH_MESSAGES);
            editor.putString(KEY_PUSH_MESSAGES, pushKeyArray.toString());
            if (pushKeyArray != null){
                mPushKeys = new String[pushKeyArray.length()];
            }
            for (int i = 0; i < pushKeyArray.length(); i++){
                mPushKeys[i] = pushKeyArray.getString(i);
            }
        }

        mNetworkPerformance = responseJSON.optString(KEY_NETWORK_PERFORMANCE, VALUE_APP_DEFINED);

        if (responseJSON.has(KEY_OPT_OUT)){
            mSendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT);
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            setProviderPersistence(new ProviderPersistence(responseJSON, mContext));
        }

        if (responseJSON.has(KEY_ADTRUTH)){
            JSONObject adtruthObject = responseJSON.getJSONObject(KEY_ADTRUTH);
            if (adtruthObject != null) {
                getAdtruth().setUrl(adtruthObject.optString(KEY_ADTRUTH_URL));
                getAdtruth().setInterval(adtruthObject.optInt(KEY_ADTRUTH_INTERVAL, 0));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
        applyConfig();

        if (responseJSON.has(KEY_EMBEDDED_KITS)) {
            mEmbeddedKitManager.updateKits(responseJSON.getJSONArray(KEY_EMBEDDED_KITS));
        }
        
        mLoaded = true;
    }

    public AdtruthConfig getAdtruth(){
        if (adtruth == null){
            adtruth = new AdtruthConfig();
        }
        return adtruth;
    }

    public String[] getPushKeys(){
        return mPushKeys;
    }

    private void applyConfig() {
        if (getLogUnhandledExceptions()) {
            MParticle.getInstance().enableUncaughtExceptionLogging();
        } else {
            MParticle.getInstance().disableUncaughtExceptionLogging();
        }
        if (!VALUE_APP_DEFINED.equals(mNetworkPerformance)){
            if (VALUE_CNP_CAPTURE.equals(mNetworkPerformance)){
                MParticle.getInstance().beginMeasuringNetworkPerformance();
            }else if (VALUE_CNP_NO_CAPTURE.equals(mNetworkPerformance)){
                MParticle.getInstance().endMeasuringNetworkPerformance();
            }
        }
    }

    public boolean getLogUnhandledExceptions() {
        if (mLogUnhandledExceptions.equals(VALUE_APP_DEFINED)) {
            return mLocalPrefs.reportUncaughtExceptions;
        } else {
            return mLogUnhandledExceptions.equals(VALUE_CUE_CATCH);
        }
    }

    public String getApiKey() {
        return mLocalPrefs.mKey;
    }

    public String getApiSecret() {
        return mLocalPrefs.mSecret;
    }

    public long getUploadInterval() {
        if (getEnvironment().equals(MParticle.Environment.Development)) {
            return DEVMODE_UPLOAD_INTERVAL_MILLISECONDS;
        } else {
            return 1000 * mLocalPrefs.uploadInterval;
        }
    }

    public boolean isDebugEnvironment(){
        if (mIsDebugEnvironment == null){
            mIsDebugEnvironment = ( 0 != ( mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
        }
        return mIsDebugEnvironment;
    }

    public MParticle.Environment getEnvironment() {
        if (mLocalPrefs.forcedEnvironment != null){
            return mLocalPrefs.forcedEnvironment;
        }else{
            return isDebugEnvironment() ? MParticle.Environment.Development : MParticle.Environment.Production;
        }
    }

    public void setUploadInterval(int uploadInterval) {
        mLocalPrefs.uploadInterval = uploadInterval;
    }

    public int getSessionTimeout() {
        return mLocalPrefs.sessionTimeout * 1000;
    }

    public void setSessionTimeout(int sessionTimeout) {
        mLocalPrefs.sessionTimeout = sessionTimeout;
    }

    public boolean isPushEnabled() {
        return mLocalPrefs.isPushEnabled ||
                (mPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLED, false) && getPushSenderId() != null);
    }

    public String getPushSenderId() {
        if (mLocalPrefs.pushSenderId != null && mLocalPrefs.pushSenderId.length() > 0)
            return mLocalPrefs.pushSenderId;
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
            if (!mLoaded){
                updateConfig(new JSONObject(mPreferences.getString(ConfigManager.CONFIG_JSON, "")));
            }
        }catch(Exception e){

        }
    }

    void debugLog(String... messages) {
        if (messages != null && getEnvironment().equals(MParticle.Environment.Development)) {
            StringBuilder logMessage = new StringBuilder();
            for (String m : messages){
                logMessage.append(m);
            }
            Log.d(Constants.LOG_TAG, logMessage.toString());
        }
    }

    public String getLicenseKey() {
        return mLocalPrefs.licenseKey;
    }

    public boolean isLicensingEnabled() {
        return mLocalPrefs.licenseKey != null && mLocalPrefs.isLicensingEnabled;
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
            return mSendOoEvents;
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
        return mLocalPrefs.autoTrackingEnabled;
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
        mProviderPersistence = persistence;
    }

    public synchronized JSONObject getProviderPersistence() {
        return mProviderPersistence;
    }

    public boolean isNetworkPerformanceEnabled() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO &&
                mLocalPrefs.networkingEnabled;
    }

    public void setNetworkingEnabled(boolean networkingEnabled) {
        mLocalPrefs.networkingEnabled = networkingEnabled;
    }

    public void setCookies(JSONObject cookies) {
        mPreferences.edit().putString(Constants.PrefKeys.Cookies, cookies.toString()).commit();
    }

    public JSONObject getCookies() throws JSONException {
        return new JSONObject(mPreferences.getString(Constants.PrefKeys.Cookies, ""));
    }

    public void setMpid(long mpid) {
        mPreferences.edit().putLong(Constants.PrefKeys.Mpid, mpid).commit();
    }

    public long getMpid() {
        return mPreferences.getLong(Constants.PrefKeys.Mpid, 0);
    }

    public int getAudienceTimeout() {
        return mLocalPrefs.audienceTimeout;
    }

    public void handleBackgrounded() {
        getAdtruth().process();
    }

    public void setForceEnvironment(MParticle.Environment environment) {
        mLocalPrefs.forcedEnvironment = environment;
    }

    class AdtruthConfig {
        private long interval;
        private String url;
        long lastSuccessful;
        String lastPayload;
        private WebView wv;

        AdtruthConfig(){
            lastPayload = mPreferences.getString(Constants.PrefKeys.ADTRUTH_PAYLOAD, null);
            lastSuccessful = mPreferences.getLong(Constants.PrefKeys.ADTRUTH_LAST_TIMESTAMP, 0);
        }
        private boolean isValid() {
            return (interval > 0 &&
                    url != null &&
                    url.length() > 1 &&
                    ((lastSuccessful + interval) < System.currentTimeMillis()));

        }
        void setInterval(int intervalSeconds){
            interval = (intervalSeconds * 1000);
        }
        void setUrl(String configUrl){
            url = MParticleApiClient.getAbsoluteUrl(configUrl);
        }
        void process(){
            if (isValid()){
                wv = new WebView(mContext);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.addJavascriptInterface(this, "mParticleSDK");
                wv.loadUrl(url);
            }
        }

        @JavascriptInterface
        public void adtruth(String payload){
            if (payload != null && payload.length() > 0){
                lastPayload = payload;
                lastSuccessful = System.currentTimeMillis();
                mPreferences
                        .edit()
                        .putString(Constants.PrefKeys.ADTRUTH_PAYLOAD, lastPayload)
                        .putLong(Constants.PrefKeys.ADTRUTH_LAST_TIMESTAMP, lastSuccessful)
                        .commit();
            }
            if (wv != null){
                wv.destroy();
                wv = null;
            }
        }
    }
}
