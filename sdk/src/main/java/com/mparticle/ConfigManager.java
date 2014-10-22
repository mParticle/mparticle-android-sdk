package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.apache.http.impl.cookie.DateParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mparticle.MParticle.LogLevel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by sdozor on 1/16/14.
 */
class ConfigManager {
    public static final String CONFIG_JSON = "json";
    private static final String KEY_TRIGGER_ITEMS = "tri";
    private static final String KEY_MESSAGE_MATCHES = "mm";
    private static final String KEY_TRIGGER_ITEM_TYPES = "dts";
    private static final String KEY_TRIGGER_ITEM_HASHES = "evts";
    private static final String KEY_OPT_OUT = "oo";
    public static final String KEY_UNHANDLED_EXCEPTIONS = "cue";
    public static final String KEY_PUSH_MESSAGES = "pmk";
    public static final String KEY_NETWORK_PERFORMANCE = "cnp";
    public static final String KEY_EMBEDDED_KITS = "eks";
    private static final String KEY_UPLOAD_INTERVAL = "uitl";
    private static final String KEY_SESSION_TIMEOUT = "stl";
    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    public static final String VALUE_CUE_IGNORE = "forceignore";
    public static final String VALUE_CNP_CAPTURE = "forcetrue";
    public static final String VALUE_CNP_NO_CAPTURE = "forcefalse";
    private static final String PREFERENCES_FILE = "mp_preferences";
    private static final String KEY_RAMP = "rp";
    private static final int DEVMODE_UPLOAD_INTERVAL_MILLISECONDS = 10 * 1000;
    private JSONObject mCurrentCookies;
    private Context mContext;

    private SharedPreferences mPreferences;

    private EmbeddedKitManager mEmbeddedKitManager;
    private static AppConfig sLocalPrefs;
    private static JSONArray sPushKeys;
    private String mLogUnhandledExceptions = VALUE_APP_DEFINED;

    private boolean mSendOoEvents;
    private JSONObject mProviderPersistence;
    private String mNetworkPerformance = "";

    private static boolean sIsDebugEnvironment = false;
    private int mRampValue = -1;

    private int mSessionTimeoutInterval = -1;
    private int mUploadInterval = -1;
    private JSONArray mTriggerMessageMatches, mTriggerMessageHashes;

    private ConfigManager(){

    }

    public ConfigManager(Context context, String key, String secret, EmbeddedKitManager embeddedKitManager) {
        mContext = context.getApplicationContext();
        sIsDebugEnvironment = ( 0 != ( mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
        mPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        sLocalPrefs = new AppConfig(mContext, key, secret);
        if (sIsDebugEnvironment){
            sLocalPrefs.logLevel = LogLevel.DEBUG;
        }
        sLocalPrefs.init(mPreferences);
        mEmbeddedKitManager = embeddedKitManager;
    }



    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(CONFIG_JSON, responseJSON.toString());

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            mLogUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES)) {
            sPushKeys = responseJSON.getJSONArray(KEY_PUSH_MESSAGES);
            editor.putString(KEY_PUSH_MESSAGES, sPushKeys.toString());
        }

        mNetworkPerformance = responseJSON.optString(KEY_NETWORK_PERFORMANCE, VALUE_APP_DEFINED);

        mRampValue = responseJSON.optInt(KEY_RAMP, -1);

        if (responseJSON.has(KEY_OPT_OUT)){
            mSendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT);
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            setProviderPersistence(new ProviderPersistence(responseJSON, mContext));
        }

        mSessionTimeoutInterval = responseJSON.optInt(KEY_SESSION_TIMEOUT, -1);
        mUploadInterval = responseJSON.optInt(KEY_UPLOAD_INTERVAL, -1);

        mTriggerMessageMatches = null;
        if (responseJSON.has(KEY_TRIGGER_ITEMS)){
            try {
                JSONObject items = responseJSON.getJSONObject(KEY_TRIGGER_ITEMS);
                if (items.has(KEY_MESSAGE_MATCHES)){
                    mTriggerMessageMatches = items.getJSONArray(KEY_MESSAGE_MATCHES);
                }
                if (items.has(KEY_TRIGGER_ITEM_HASHES)){
                    mTriggerMessageHashes = items.getJSONArray(KEY_TRIGGER_ITEM_HASHES);
                }
            }catch (JSONException jse){

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
    }

    public JSONArray getTriggerMessageMatches(){
        return mTriggerMessageMatches;
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
            return sLocalPrefs.reportUncaughtExceptions;
        } else {
            return mLogUnhandledExceptions.equals(VALUE_CUE_CATCH);
        }
    }

    public String getApiKey() {
        return sLocalPrefs.mKey;
    }

    public String getApiSecret() {
        return sLocalPrefs.mSecret;
    }

    public long getUploadInterval() {
        if (getEnvironment().equals(MParticle.Environment.Development)) {
            return DEVMODE_UPLOAD_INTERVAL_MILLISECONDS;
        } else {
            if (mUploadInterval > 0){
                return 1000 * mUploadInterval;
            }else {
                return (1000 * sLocalPrefs.uploadInterval);
            }
        }
    }

    public static boolean isDebugEnvironment(){
        return sIsDebugEnvironment;
    }

    public static MParticle.Environment getEnvironment() {
        if (sLocalPrefs.forcedEnvironment != MParticle.Environment.AutoDetect){
            return sLocalPrefs.forcedEnvironment;
        }else{
            return isDebugEnvironment() ? MParticle.Environment.Development : MParticle.Environment.Production;
        }
    }

    public void setUploadInterval(int uploadInterval) {
        sLocalPrefs.uploadInterval = uploadInterval;
    }

    public int getSessionTimeout() {
        if (mSessionTimeoutInterval > 0){
            return mSessionTimeoutInterval * 1000;
        }else{
            return sLocalPrefs.sessionTimeout * 1000;
        }
    }

    public void setSessionTimeout(int sessionTimeout) {
        sLocalPrefs.sessionTimeout = sessionTimeout;
    }

    public boolean isPushEnabled() {
        return sLocalPrefs.isPushEnabled ||
                (mPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLED, false) && getPushSenderId() != null);
    }

    public String getPushSenderId() {
        if (sLocalPrefs.pushSenderId != null && sLocalPrefs.pushSenderId.length() > 0)
            return sLocalPrefs.pushSenderId;
        else return mPreferences.getString(Constants.PrefKeys.PUSH_SENDER_ID, null);
    }

    public void setPushSenderId(String senderId){
        mPreferences.edit()
                .putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                .commit();
    }

    static void log(LogLevel priority, String... messages) {
        log(priority, null, messages);
    }

    static void log(LogLevel priority, Throwable error, String... messages){
        if (messages != null && sLocalPrefs.logLevel.ordinal() >= priority.ordinal() &&
                getEnvironment().equals(MParticle.Environment.Development)) {
            StringBuilder logMessage = new StringBuilder();
            for (String m : messages){
                logMessage.append(m);
            }
            switch (priority){
                case ERROR:
                    if (error != null){
                        Log.e(Constants.LOG_TAG, logMessage.toString(), error);
                    }else{
                        Log.e(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
                case WARNING:
                    if (error != null){
                        Log.w(Constants.LOG_TAG, logMessage.toString(), error);
                    }else{
                        Log.w(Constants.LOG_TAG, logMessage.toString());
                    }

                    break;
                case DEBUG:
                    if (error != null){
                        Log.v(Constants.LOG_TAG, logMessage.toString(), error);
                    }else{
                        Log.v(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
            }
        }
    }

    public String getLicenseKey() {
        return sLocalPrefs.licenseKey;
    }

    public boolean isLicensingEnabled() {
        return sLocalPrefs.licenseKey != null && sLocalPrefs.isLicensingEnabled;
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
        return sLocalPrefs.autoTrackingEnabled;
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

    private static SharedPreferences getPreferences(Context context){
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static JSONArray getPushKeys(Context context){
        if (sPushKeys == null){
            String arrayString = getPreferences(context).getString(KEY_PUSH_MESSAGES, null);
            try {
                sPushKeys = new JSONArray(arrayString);
            } catch (Exception e) {
                sPushKeys = new JSONArray();
            }
        }
        return sPushKeys;
    }

    public static int getPushTitle(Context context) {
        return getPreferences(context)
                .getInt(Constants.PrefKeys.PUSH_TITLE, 0);
    }

    public static int getPushIcon(Context context) {
        return getPreferences(context)
                .getInt(Constants.PrefKeys.PUSH_ICON, 0);
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
                sLocalPrefs.networkingEnabled;
    }

    public void setNetworkingEnabled(boolean networkingEnabled) {
        sLocalPrefs.networkingEnabled = networkingEnabled;
    }

    public void setCookies(JSONObject serverCookies) {
        try {
            JSONObject localCookies = getCookies();
            Iterator<?> keys = serverCookies.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                localCookies.put(key, serverCookies.getJSONObject(key));
            }
            mCurrentCookies = localCookies;
            mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).commit();
        }catch (JSONException jse){

        }
    }

    public JSONObject getCookies() throws JSONException {
        if (mCurrentCookies == null){
            String currentCookies = mPreferences.getString(Constants.PrefKeys.Cookies, null);
            if (TextUtils.isEmpty(currentCookies)){
                mCurrentCookies = new JSONObject();
                mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).commit();
                return mCurrentCookies;
            }else {
                mCurrentCookies = new JSONObject(currentCookies);
            }
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.set(Calendar.YEAR, 1990);
            Date oldDate = nowCalendar.getTime();
            SimpleDateFormat parser = new SimpleDateFormat("yyyy");
            Iterator<?> keys = mCurrentCookies.keys();
            ArrayList<String> keysToRemove = new ArrayList<String>();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if( mCurrentCookies.get(key) instanceof JSONObject ){
                    String expiration = ((JSONObject) mCurrentCookies.get(key)).getString("e");
                    try {
                        Date date = parser.parse(expiration);
                        if (date.before(oldDate)){
                            keysToRemove.add(key);
                        }
                    }catch (ParseException dpe){

                    }
                }
            }
            for (String key : keysToRemove){
                mCurrentCookies.remove(key);
            }
            if (keysToRemove.size() > 0) {
                mPreferences.edit().putString(Constants.PrefKeys.Cookies, mCurrentCookies.toString()).commit();
            }
            return mCurrentCookies;
        }else{
            return mCurrentCookies;
        }
    }

    public void setMpid(long mpid) {
        mPreferences.edit().putLong(Constants.PrefKeys.Mpid, mpid).commit();
    }

    public long getMpid() {
        return mPreferences.getLong(Constants.PrefKeys.Mpid, 0);
    }

    public int getAudienceTimeout() {
        return sLocalPrefs.audienceTimeout;
    }

    public void setForceEnvironment(MParticle.Environment environment) {
        sLocalPrefs.forcedEnvironment = environment;
    }

    public void setLogLevel(LogLevel level) {
        sLocalPrefs.logLevel = level;
    }

    public int getCurrentRampValue() {
        return mRampValue;
    }

    public JSONArray getTriggerMessageHashes() {
        return mTriggerMessageHashes;
    }
}
