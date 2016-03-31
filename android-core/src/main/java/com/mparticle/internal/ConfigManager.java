package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mparticle.ExceptionHandler;
import com.mparticle.MParticle;
import com.mparticle.MParticle.LogLevel;
import com.mparticle.messaging.MessagingConfigCallbacks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class ConfigManager {
    public static final String CONFIG_JSON = "json";
    private static final String KEY_TRIGGER_ITEMS = "tri";
    private static final String KEY_MESSAGE_MATCHES = "mm";
    private static final String KEY_TRIGGER_ITEM_HASHES = "evts";
    private static final String KEY_INFLUENCE_OPEN = "pio";
    static final String KEY_OPT_OUT = "oo";
    public static final String KEY_UNHANDLED_EXCEPTIONS = "cue";
    public static final String KEY_PUSH_MESSAGES = "pmk";
    public static final String KEY_EMBEDDED_KITS = "eks";
    static final String KEY_UPLOAD_INTERVAL = "uitl";
    static final String KEY_SESSION_TIMEOUT = "stl";
    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    private static final String PREFERENCES_FILE = "mp_preferences";
    static final String KEY_RAMP = "rp";

    private static final int DEVMODE_UPLOAD_INTERVAL_MILLISECONDS = 10 * 1000;
    private Context mContext;

    static SharedPreferences mPreferences;

    AppConfig sLocalPrefs;
    private static JSONArray sPushKeys;
    private String mLogUnhandledExceptions = VALUE_APP_DEFINED;

    private boolean mSendOoEvents;
    private JSONObject mProviderPersistence;

    private int mRampValue = -1;
    private int mUserBucket = -1;

    private int mSessionTimeoutInterval = -1;
    private int mUploadInterval = -1;
    private long mInfluenceOpenTimeout = 3600 * 1000;
    private JSONArray mTriggerMessageMatches, mTriggerMessageHashes = null;
    private ExceptionHandler mExHandler;

    private ConfigManager(){

    }

    public ConfigManager(Context context, MParticle.Environment environment, String apiKey, String apiSecret) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        sLocalPrefs = new AppConfig(mContext, environment, mPreferences, apiKey, apiSecret);
    }

    /**
     * The is called on startup. The only thing that's completely necessary is that we fire up kits.
     */
    public JSONArray getLatestKitConfiguration(){
        String oldConfig = mPreferences.getString(CONFIG_JSON, null);
        if (!MPUtility.isEmpty(oldConfig)){
            try{
                JSONObject oldConfigJson = new JSONObject(oldConfig);
                return oldConfigJson.optJSONArray(KEY_EMBEDDED_KITS);
            }catch (Exception jse){

            }
        }
        return null;
    }

    void saveConfigJson(JSONObject json){
        if (json != null) {
            mPreferences.edit().putString(CONFIG_JSON, json.toString()).apply();
        }
    }

    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {
        updateConfig(responseJSON, true);
    }
    public synchronized void updateConfig(JSONObject responseJSON, boolean persistJson) throws JSONException {
        SharedPreferences.Editor editor = mPreferences.edit();
        if (persistJson) {
            saveConfigJson(responseJSON);
        }

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            mLogUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES)) {
            sPushKeys = responseJSON.getJSONArray(KEY_PUSH_MESSAGES);
            editor.putString(KEY_PUSH_MESSAGES, sPushKeys.toString());
        }

        mRampValue = responseJSON.optInt(KEY_RAMP, -1);

        if (responseJSON.has(KEY_OPT_OUT)){
            mSendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT);
        }else{
            mSendOoEvents = false;
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            setProviderPersistence(new ProviderPersistence(responseJSON, mContext));
        }else{
            setProviderPersistence(null);
        }

        mSessionTimeoutInterval = responseJSON.optInt(KEY_SESSION_TIMEOUT, -1);
        mUploadInterval = responseJSON.optInt(KEY_UPLOAD_INTERVAL, -1);

        mTriggerMessageMatches = null;
        mTriggerMessageHashes = null;
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

        if (responseJSON.has(KEY_INFLUENCE_OPEN)){
            mInfluenceOpenTimeout = responseJSON.getLong(KEY_INFLUENCE_OPEN) * 60 * 1000;
        }else{
            mInfluenceOpenTimeout = 30 * 60 * 1000;
        }

        editor.apply();
        applyConfig();

        MParticle.getInstance().getKitManager().updateKits(responseJSON.optJSONArray(KEY_EMBEDDED_KITS));
    }

    @Nullable
    public String getActiveModuleIds(){
        return MParticle.getInstance().getKitManager().getActiveModuleIds();
    }

    /**
     * When the Config manager starts up, we don't want to enable everything immediately to save on app-load time.
     * This method will be called from a background thread after startup is already complete.

     */
    public void delayedStart(){
        sLocalPrefs.delayedInit();
        if (isPushEnabled()) {
            MParticle.getInstance().Messaging().enablePushNotifications(getPushSenderId());
        }
    }

    public JSONArray getTriggerMessageMatches(){
        return mTriggerMessageMatches;
    }

    public long getInfluenceOpenTimeoutMillis(){
        return mInfluenceOpenTimeout;
    }

    private void applyConfig() {
        if (getLogUnhandledExceptions()) {
            enableUncaughtExceptionLogging(false);
        } else {
            disableUncaughtExceptionLogging(false);
        }
    }

    public void enableUncaughtExceptionLogging(boolean userTriggered) {
        if (null == mExHandler) {
            Thread.UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (!(currentUncaughtExceptionHandler instanceof ExceptionHandler)) {
                mExHandler = new ExceptionHandler(currentUncaughtExceptionHandler);
                Thread.setDefaultUncaughtExceptionHandler(mExHandler);
                if (userTriggered) {
                    setLogUnhandledExceptions(true);
                }
            }
        }
    }

    public void disableUncaughtExceptionLogging(boolean userTriggered) {
        if (null != mExHandler) {
            Thread.UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (currentUncaughtExceptionHandler instanceof ExceptionHandler) {
                Thread.setDefaultUncaughtExceptionHandler(mExHandler.getOriginalExceptionHandler());
                mExHandler = null;
                if (userTriggered) {
                    setLogUnhandledExceptions(false);
                }
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

    public void setLogUnhandledExceptions(boolean log){
        sLocalPrefs.reportUncaughtExceptions = log;
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

    public static MParticle.Environment getEnvironment() {
        return AppConfig.getEnvironment();
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
        String senderId = sLocalPrefs.getPushSenderId();
        if (!MPUtility.isEmpty(senderId))
            return senderId;
        else return mPreferences.getString(Constants.PrefKeys.PUSH_SENDER_ID, null);
    }

    public void setPushSenderId(String senderId){
        mPreferences.edit()
                .putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                .apply();
    }

    public static void log(LogLevel priority, String... messages) {
        log(priority, null, messages);
    }

    public static void log(LogLevel priority, Throwable error, String... messages){
        if (messages != null && AppConfig.logLevel.ordinal() >= priority.ordinal() &&
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
            .apply();
    }

    public void setPushVibrationEnabled(boolean pushVibrationEnabled) {
        mPreferences.edit()
            .putBoolean(Constants.PrefKeys.PUSH_ENABLE_VIBRATION, pushVibrationEnabled)
            .apply();
    }

    public boolean isEnabled(){
        boolean optedOut = this.getOptedOut();
        return !optedOut || mSendOoEvents;
    }

    public void setOptOut(boolean optOut){
        mPreferences
                .edit().putBoolean(Constants.PrefKeys.OPTOUT, optOut).apply();
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
                .apply();
    }

    public void setPushNotificationTitle(int pushNotificationTitle) {
        mPreferences.edit()
                .putInt(Constants.PrefKeys.PUSH_TITLE, pushNotificationTitle)
                .apply();
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

    public static int getBreadcrumbLimit() {
        if (mPreferences != null){
            return mPreferences.getInt(Constants.PrefKeys.BREADCRUMB_LIMIT, AppConfig.DEFAULT_BREADCRUMB_LIMIT);
        }
        return AppConfig.DEFAULT_BREADCRUMB_LIMIT;
    }

    public void setBreadcrumbLimit(int newLimit){
        mPreferences.edit()
                .putInt(Constants.PrefKeys.BREADCRUMB_LIMIT, newLimit)
                .apply();
    }

    private synchronized void setProviderPersistence(JSONObject persistence){
        mProviderPersistence = persistence;
    }

    public synchronized JSONObject getProviderPersistence() {
        return mProviderPersistence;
    }

    public void setMpid(long mpid) {
        mPreferences.edit().putLong(Constants.PrefKeys.Mpid, mpid).apply();
    }

    public long getMpid() {
        if (mPreferences.contains(Constants.PrefKeys.Mpid)){
            return mPreferences.getLong(Constants.PrefKeys.Mpid, 0);
        }else{
            long mpid = MPUtility.generateMpid();
            mPreferences.edit().putLong(Constants.PrefKeys.Mpid, mpid).apply();
            return mpid;
        }
    }

    public int getAudienceTimeout() {
        return sLocalPrefs.audienceTimeout;
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

    public boolean shouldTrigger(MPMessage message) {
        JSONArray messageMatches = getTriggerMessageMatches();
        JSONArray triggerHashes = getTriggerMessageHashes();

        //always trigger for PUSH_RECEIVED
        boolean shouldTrigger = message.getMessageType().equals(Constants.MessageType.PUSH_RECEIVED)
                || message.getMessageType().equals(Constants.MessageType.COMMERCE_EVENT);

        if (!shouldTrigger && messageMatches != null && messageMatches.length() > 0){
            shouldTrigger = true;
            int i = 0;
            while (shouldTrigger && i < messageMatches.length()){
                try {
                    JSONObject messageMatch = messageMatches.getJSONObject(i);
                    Iterator<?> keys = messageMatch.keys();
                    while(shouldTrigger && keys.hasNext() ){
                        String key = (String)keys.next();
                        shouldTrigger = message.has(key);
                        if (shouldTrigger){
                            try {
                                shouldTrigger = messageMatch.getString(key).equalsIgnoreCase(message.getString(key));
                            }catch (JSONException stringex){
                                try {
                                    shouldTrigger = message.getBoolean(key) == messageMatch.getBoolean(key);
                                }catch (JSONException boolex){
                                    try{
                                        shouldTrigger = message.getDouble(key) == messageMatch.getDouble(key);
                                    }catch (JSONException doubleex){
                                        shouldTrigger = false;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                }
                i++;
            }
        }
        if (!shouldTrigger && triggerHashes != null){
            for (int i = 0; i < triggerHashes.length(); i++){
                try {
                    if (triggerHashes.getInt(i) == message.getTypeNameHash()) {
                        shouldTrigger = true;
                        break;
                    }
                }catch (JSONException jse){

                }
            }
        }
        return shouldTrigger;
    }

    public int getUserBucket() {
        if (mUserBucket < 0){
            mUserBucket = (int)(Math.abs(getMpid() >> 8) % 100);
        }
        return mUserBucket;
    }

}
