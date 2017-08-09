package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.UrlQuerySanitizer;
import android.os.Build;

import com.mparticle.ExceptionHandler;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    public static final String KEY_AAID_LAT = "rdlat";
    public static final String VALUE_APP_DEFINED = "appdefined";
    public static final String VALUE_CUE_CATCH = "forcecatch";
    static final String PREFERENCES_FILE = "mp_preferences";
    private static final String KEY_INCLUDE_SESSION_HISTORY = "inhd";
    private static final String KEY_DEVICE_PERFORMANCE_METRICS_DISABLED = "dpmd";
    static final String KEY_RAMP = "rp";

    private static final int DEVMODE_UPLOAD_INTERVAL_MILLISECONDS = 10 * 1000;
    private Context mContext;

    static SharedPreferences sPreferences;

    AppConfig mLocalPrefs;

    private static JSONArray sPushKeys;
    private static UserConfig sUserConfig;
    private String mLogUnhandledExceptions = VALUE_APP_DEFINED;

    private boolean mSendOoEvents;
    private JSONObject mProviderPersistence;
    private boolean mRestrictAAIDfromLAT = true;
    private int mRampValue = -1;
    private int mUserBucket = -1;

    private int mSessionTimeoutInterval = -1;
    private int mUploadInterval = -1;
    private long mInfluenceOpenTimeout = 3600 * 1000;
    private JSONArray mTriggerMessageMatches, mTriggerMessageHashes = null;
    private ExceptionHandler mExHandler;
    private boolean mIncludeSessionHistory = false;
    private JSONObject mCurrentCookies;

    private ConfigManager() {

    }

    public ConfigManager(Context context, MParticle.Environment environment, String apiKey, String apiSecret) {
        mContext = context.getApplicationContext();
        sPreferences = mContext.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        mLocalPrefs = new AppConfig(mContext, environment, sPreferences, apiKey, apiSecret);
        sUserConfig = UserConfig.getUserConfig(mContext, getMpid());
        restoreOldConfig();
    }

    private void restoreOldConfig() {
        String oldConfig = sPreferences.getString(CONFIG_JSON, null);
        if (!MPUtility.isEmpty(oldConfig)) {
            try {
                JSONObject oldConfigJson = new JSONObject(oldConfig);
                updateConfig(oldConfigJson, false);
            } catch (Exception jse) {

            }
        }
    }

    /**
     * The is called on startup. The only thing that's completely necessary is that we fire up kits.
     */
    public JSONArray getLatestKitConfiguration() {
        String oldConfig = sPreferences.getString(CONFIG_JSON, null);
        if (!MPUtility.isEmpty(oldConfig)) {
            try {
                JSONObject oldConfigJson = new JSONObject(oldConfig);
                return oldConfigJson.optJSONArray(KEY_EMBEDDED_KITS);
            } catch (Exception jse) {

            }
        }
        return null;
    }

    UserConfig getUserConfig() {
        return sUserConfig;
    }

    UserConfig getUserConfig(long mpId) {
        if (sUserConfig == null || sUserConfig.getMpid() != mpId) {
            sUserConfig = UserConfig.getUserConfig(mContext, mpId);
        }
        return sUserConfig;
    }

    public static UserConfig getUserConfig(Context context) {
        if (sUserConfig == null) {
            sUserConfig = UserConfig.getUserConfig(context, getMpid(context));
        }
        return sUserConfig;
    }

    private static UserConfig getUserConfig(Context context, long mpid) {
        if (sUserConfig == null || sUserConfig.getMpid() != mpid) {
            sUserConfig = UserConfig.getUserConfig(context, mpid);
        }
        return sUserConfig;
    }

    public static void deleteUserConfig(Context context, long mpid) {
        if (sUserConfig != null && sUserConfig.getMpid() == mpid) {
            sUserConfig = null;
        }
        UserConfig.deleteUserConfig(context, mpid);
    }

    public void deleteUserConfig(long mpId) {
        deleteUserConfig(mContext, mpId);
    }

    static void deleteConfigManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(PREFERENCES_FILE);
            sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        } else {
            if (sPreferences == null) {
                sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
            }
            sPreferences.edit().clear().commit();
        }
    }

    void saveConfigJson(JSONObject json) {
        if (json != null) {
            sPreferences.edit().putString(CONFIG_JSON, json.toString()).apply();
        }
    }

    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {
        updateConfig(responseJSON, true);
    }

    public synchronized void updateConfig(JSONObject responseJSON, boolean newConfig) throws JSONException {
        SharedPreferences.Editor editor = sPreferences.edit();
        if (newConfig) {
            saveConfigJson(responseJSON);
        }

        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            mLogUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS);
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES) && newConfig) {
            sPushKeys = responseJSON.getJSONArray(KEY_PUSH_MESSAGES);
            editor.putString(KEY_PUSH_MESSAGES, sPushKeys.toString());
        }

        mRampValue = responseJSON.optInt(KEY_RAMP, -1);

        if (responseJSON.has(KEY_OPT_OUT)) {
            mSendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT);
        } else {
            mSendOoEvents = false;
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            setProviderPersistence(new ProviderPersistence(responseJSON, mContext));
        } else {
            setProviderPersistence(null);
        }

        mSessionTimeoutInterval = responseJSON.optInt(KEY_SESSION_TIMEOUT, -1);
        mUploadInterval = responseJSON.optInt(KEY_UPLOAD_INTERVAL, -1);

        mTriggerMessageMatches = null;
        mTriggerMessageHashes = null;
        if (responseJSON.has(KEY_TRIGGER_ITEMS)) {
            try {
                JSONObject items = responseJSON.getJSONObject(KEY_TRIGGER_ITEMS);
                if (items.has(KEY_MESSAGE_MATCHES)) {
                    mTriggerMessageMatches = items.getJSONArray(KEY_MESSAGE_MATCHES);
                }
                if (items.has(KEY_TRIGGER_ITEM_HASHES)) {
                    mTriggerMessageHashes = items.getJSONArray(KEY_TRIGGER_ITEM_HASHES);
                }
            } catch (JSONException jse) {

            }

        }

        if (responseJSON.has(KEY_INFLUENCE_OPEN)) {
            mInfluenceOpenTimeout = responseJSON.getLong(KEY_INFLUENCE_OPEN) * 60 * 1000;
        } else {
            mInfluenceOpenTimeout = 30 * 60 * 1000;
        }

        mRestrictAAIDfromLAT = responseJSON.optBoolean(KEY_AAID_LAT, true);
        mIncludeSessionHistory = responseJSON.optBoolean(KEY_INCLUDE_SESSION_HISTORY, true);
        if (responseJSON.has(KEY_DEVICE_PERFORMANCE_METRICS_DISABLED)) {
            MessageManager.devicePerformanceMetricsDisabled = responseJSON.optBoolean(KEY_DEVICE_PERFORMANCE_METRICS_DISABLED, false);
        }

        editor.apply();
        applyConfig();
        if (newConfig) {
            MParticle.getInstance().getKitManager().updateKits(responseJSON.optJSONArray(KEY_EMBEDDED_KITS));
        }
    }

    public String getActiveModuleIds() {
        return MParticle.getInstance().getKitManager().getActiveModuleIds();
    }

    public boolean getIncludeSessionHistory() {
        return mIncludeSessionHistory;
    }

    /**
     * Indicates if the Android Advertising ID should be collected regardless of the limit ad tracking
     * setting. Google allows the usage of AAID regardless of the LAT setting for cases of anonymous analytics,
     * attribution, etc. By default, this will return True, which means that that SDK should *not* collect AAID
     * when the user has enable limit ad tracking.
     *
     * @return true if AAID should only be send when LAT is disabled.
     */
    public boolean getRestrictAAIDBasedOnLAT() {
        return mRestrictAAIDfromLAT;
    }

    /**
     * When the Config manager starts up, we don't want to enable everything immediately to save on app-load time.
     * This method will be called from a background thread after startup is already complete.
     */
    public void delayedStart() {
        mLocalPrefs.delayedInit();
        if (isPushEnabled() && PushRegistrationHelper.getLatestPushRegistration(mContext) == null) {
            MParticle.getInstance().Messaging().enablePushNotifications(getPushSenderId());
        }
    }

    public JSONArray getTriggerMessageMatches() {
        return mTriggerMessageMatches;
    }

    public long getInfluenceOpenTimeoutMillis() {
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
            return mLocalPrefs.reportUncaughtExceptions;
        } else {
            return mLogUnhandledExceptions.equals(VALUE_CUE_CATCH);
        }
    }

    public void setLogUnhandledExceptions(boolean log) {
        mLocalPrefs.reportUncaughtExceptions = log;
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
            if (mUploadInterval > 0) {
                return 1000 * mUploadInterval;
            } else {
                return (1000 * mLocalPrefs.uploadInterval);
            }
        }
    }

    public static MParticle.Environment getEnvironment() {
        return AppConfig.getEnvironment();
    }

    public void setUploadInterval(int uploadInterval) {
        mLocalPrefs.uploadInterval = uploadInterval;
    }

    public int getSessionTimeout() {
        if (mSessionTimeoutInterval > 0) {
            return mSessionTimeoutInterval * 1000;
        } else {
            return mLocalPrefs.sessionTimeout * 1000;
        }
    }

    public void setSessionTimeout(int sessionTimeout) {
        mLocalPrefs.sessionTimeout = sessionTimeout;
    }

    public boolean isPushEnabled() {
        return mLocalPrefs.isPushEnabled ||
                (sPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLED, false) && getPushSenderId() != null);
    }

    public String getPushSenderId() {
        String senderId = mLocalPrefs.getPushSenderId();
        if (!MPUtility.isEmpty(senderId))
            return senderId;
        else return sPreferences.getString(Constants.PrefKeys.PUSH_SENDER_ID, null);
    }

    public void setPushSenderId(String senderId) {
        sPreferences.edit()
                .putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                .apply();
    }

    public String getLicenseKey() {
        return mLocalPrefs.licenseKey;
    }

    public boolean isLicensingEnabled() {
        return mLocalPrefs.licenseKey != null && mLocalPrefs.isLicensingEnabled;
    }

    public void setPushSoundEnabled(boolean pushSoundEnabled) {
        sPreferences.edit()
                .putBoolean(Constants.PrefKeys.PUSH_ENABLE_SOUND, pushSoundEnabled)
                .apply();
    }

    public void setPushVibrationEnabled(boolean pushVibrationEnabled) {
        sPreferences.edit()
                .putBoolean(Constants.PrefKeys.PUSH_ENABLE_VIBRATION, pushVibrationEnabled)
                .apply();
    }

    public boolean isEnabled() {
        boolean optedOut = this.getOptedOut();
        return !optedOut || mSendOoEvents;
    }

    public void setOptOut(boolean optOut) {
        sPreferences
                .edit().putBoolean(Constants.PrefKeys.OPTOUT, optOut).apply();
    }

    public boolean getOptedOut() {
        return sPreferences.getBoolean(Constants.PrefKeys.OPTOUT, false);
    }

    public boolean isAutoTrackingEnabled() {
        return mLocalPrefs.autoTrackingEnabled;
    }

    public boolean isPushSoundEnabled() {
        return sPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLE_SOUND, AppConfig.DEFAULT_ENABLE_PUSH_SOUND);
    }

    public boolean isPushVibrationEnabled() {
        return sPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLE_VIBRATION, AppConfig.DEFAULT_ENABLE_PUSH_VIBRATION);
    }

    public void setPushNotificationIcon(int pushNotificationIcon) {
        sPreferences.edit()
                .putInt(Constants.PrefKeys.PUSH_ICON, pushNotificationIcon)
                .apply();
    }

    public void setPushNotificationTitle(int pushNotificationTitle) {
        sPreferences.edit()
                .putInt(Constants.PrefKeys.PUSH_TITLE, pushNotificationTitle)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    public static JSONArray getPushKeys(Context context) {
        if (sPushKeys == null) {
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

    public static int getBreadcrumbLimit(Context context) {
        return getUserConfig(context).getBreadcrumbLimit();
    }

    public static int getBreadcrumbLimit(Context context, long mpId) {
        return getUserConfig(context, mpId).getBreadcrumbLimit();
    }

    public static String getCurrentUserLtv(Context context) {
        return getUserConfig(context).getLtv();
    }

    public void setBreadcrumbLimit(int newLimit) {
        setBreadcrumbLimit(newLimit, getMpid());
    }

    public void setBreadcrumbLimit(int newLimit, long mpId) {
        getUserConfig(mpId).setBreadcrumbLimit(newLimit);
    }

    public static void setNeedsToMigrate(Context context, boolean needsToMigrate) {
        UserConfig.setNeedsToMigrate(context, needsToMigrate);
    }

    private synchronized void setProviderPersistence(JSONObject persistence) {
        mProviderPersistence = persistence;
    }

    public synchronized JSONObject getProviderPersistence() {
        return mProviderPersistence;
    }

    public void setMpid(long mpid) {
        if (getMpid() != mpid && mpIdChangeListeners != null) {
            triggerMpidChangeListenerCallbacks(mpid);
        }
        sPreferences.edit().putLong(Constants.PrefKeys.MPID, mpid).apply();
        if (sUserConfig == null || sUserConfig.getMpid() != mpid) {
            sUserConfig = UserConfig.getUserConfig(mContext, mpid);
        }
    }

    //for testing
    static void clearMpid(Context context) {
        if (sPreferences == null) {
            sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        }
        sPreferences.edit().remove(Constants.PrefKeys.MPID).apply();
    }

    public long getMpid() {
        return getMpid(false);
    }

    public long getMpid(boolean allowTemporary) {
        if (allowTemporary && sInProgress) {
            return Constants.TEMPORARY_MPID;
        } else {
            return sPreferences.getLong(Constants.PrefKeys.MPID, Constants.TEMPORARY_MPID);
        }
    }

    public static long getMpid(Context context) {
        return getMpid(context, false);
    }

    public static long getMpid(Context context, boolean allowTemporary) {
        if (sPreferences == null) {
            sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        }
        if (allowTemporary && sInProgress) {
            return Constants.TEMPORARY_MPID;
        } else {
            return sPreferences.getLong(Constants.PrefKeys.MPID, Constants.TEMPORARY_MPID);
        }
    }

    private static boolean sInProgress;
    public static void setIdentityRequestInProgress(boolean inProgress) {
        sInProgress = inProgress;
    }

    public void mergeUserConfigs(long subjectMpId, long targetMpId) {
        UserConfig subjectUserConfig = getUserConfig(subjectMpId);
        UserConfig targetUserConfig = getUserConfig(targetMpId);
        targetUserConfig.merge(subjectUserConfig);
    }

    public int getAudienceTimeout() {
        return mLocalPrefs.audienceTimeout;
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

        boolean isBackgroundAst = false;
        try {
            isBackgroundAst = (message.getMessageType().equals(Constants.MessageType.APP_STATE_TRANSITION) && message.get(Constants.MessageKey.STATE_TRANSITION_TYPE).equals(Constants.StateTransitionType.STATE_TRANS_BG));
        }
        catch (JSONException ex) {}
        boolean shouldTrigger = message.getMessageType().equals(Constants.MessageType.PUSH_RECEIVED)
                || message.getMessageType().equals(Constants.MessageType.COMMERCE_EVENT)
                || isBackgroundAst;

        if (!shouldTrigger && messageMatches != null && messageMatches.length() > 0) {
            shouldTrigger = true;
            int i = 0;
            while (shouldTrigger && i < messageMatches.length()) {
                try {
                    JSONObject messageMatch = messageMatches.getJSONObject(i);
                    Iterator<?> keys = messageMatch.keys();
                    while (shouldTrigger && keys.hasNext()) {
                        String key = (String) keys.next();
                        shouldTrigger = message.has(key);
                        if (shouldTrigger) {
                            try {
                                shouldTrigger = messageMatch.getString(key).equalsIgnoreCase(message.getString(key));
                            } catch (JSONException stringex) {
                                try {
                                    shouldTrigger = message.getBoolean(key) == messageMatch.getBoolean(key);
                                } catch (JSONException boolex) {
                                    try {
                                        shouldTrigger = message.getDouble(key) == messageMatch.getDouble(key);
                                    } catch (JSONException doubleex) {
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
        if (!shouldTrigger && triggerHashes != null) {
            for (int i = 0; i < triggerHashes.length(); i++) {
                try {
                    if (triggerHashes.getInt(i) == message.getTypeNameHash()) {
                        shouldTrigger = true;
                        break;
                    }
                } catch (JSONException jse) {

                }
            }
        }
        return shouldTrigger;
    }

    public int getUserBucket() {
        if (mUserBucket < 0) {
            mUserBucket = (int) (Math.abs(getMpid() >> 8) % 100);
        }
        return mUserBucket;
    }

    public void setIntegrationAttributes(int kitId, Map<String, String> newAttributes) {
        try {
            JSONObject newJsonAttributes = null;
            if (newAttributes != null && !newAttributes.isEmpty()) {
                newJsonAttributes = new JSONObject();
                for (Map.Entry<String, String> entry : newAttributes.entrySet()) {
                    newJsonAttributes.put(entry.getKey(), entry.getValue());
                }
            }
            JSONObject currentJsonAttributes = getIntegrationAttributes();
            if (currentJsonAttributes == null) {
                currentJsonAttributes = new JSONObject();
            }
            currentJsonAttributes.put(Integer.toString(kitId), newJsonAttributes);
            if (currentJsonAttributes.length() > 0) {
                sPreferences.edit()
                        .putString(Constants.PrefKeys.INTEGRATION_ATTRIBUTES, currentJsonAttributes.toString())
                        .apply();
            } else {
                sPreferences.edit()
                        .remove(Constants.PrefKeys.INTEGRATION_ATTRIBUTES)
                        .apply();
            }
        } catch (JSONException jse) {

        }
    }

    public Map<String, String> getIntegrationAttributes(int kitId) {
        Map<String, String> integrationAttributes = new HashMap<String, String>();
        JSONObject jsonAttributes = getIntegrationAttributes();
        if (jsonAttributes != null) {
            JSONObject kitAttributes = jsonAttributes.optJSONObject(Integer.toString(kitId));
            if (kitAttributes != null) {
                try {
                    Iterator<String> keys = kitAttributes.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (kitAttributes.get(key) instanceof String) {
                            integrationAttributes.put(key, kitAttributes.getString(key));
                        }
                    }
                } catch (JSONException e) {

                }
            }
        }
        return integrationAttributes;
    }

    public JSONObject getIntegrationAttributes() {
        JSONObject jsonAttributes = null;
        String allAttributes = sPreferences.getString(Constants.PrefKeys.INTEGRATION_ATTRIBUTES, null);
        if (allAttributes != null) {
            try {
                jsonAttributes = new JSONObject(allAttributes);
            } catch (JSONException e) {

            }
        }
        return jsonAttributes;
    }

    public Map<MParticle.IdentityType, String> getUserIdentities(long mpId) {
        JSONArray userIdentitiesJson = getUserIdentityJson(mpId);
        Map<MParticle.IdentityType, String> identityTypeStringMap = new HashMap<MParticle.IdentityType, String>(userIdentitiesJson.length());

        for (int i = 0; i < userIdentitiesJson.length(); i++) {
            try {
                JSONObject identity = userIdentitiesJson.getJSONObject(i);
                identityTypeStringMap.put(
                        MParticle.IdentityType.parseInt(identity.getInt(Constants.MessageKey.IDENTITY_NAME)),
                        identity.getString(Constants.MessageKey.IDENTITY_VALUE)
                );
            } catch (JSONException jse) {

            }
        }

        return identityTypeStringMap;
    }

    public JSONArray getUserIdentityJson() {
        return getUserIdentityJson(getMpid());
    }

    public JSONArray getUserIdentityJson(long mpId) {
        JSONArray userIdentities = null;
        String userIds = getUserConfig(mpId).getUserIdentities();

        try {
            userIdentities = new JSONArray(userIds);
            boolean changeMade = fixUpUserIdentities(userIdentities);
            if (changeMade) {
                saveUserIdentityJson(userIdentities, mpId);
            }
        } catch (Exception e) {
            userIdentities = new JSONArray();
        }
        return userIdentities;
    }

    public void saveUserIdentityJson(JSONArray userIdentities) {
        saveUserIdentityJson(userIdentities, getMpid());
    }

    public void saveUserIdentityJson(JSONArray userIdentities, long mpId) {
        getUserConfig(mpId).setUserIdentities(userIdentities.toString());
    }

    private static boolean fixUpUserIdentities(JSONArray identities) {
        boolean changeMade = false;
        try {
            for (int i = 0; i < identities.length(); i++) {
                JSONObject identity = identities.getJSONObject(i);
                if (!identity.has(Constants.MessageKey.IDENTITY_DATE_FIRST_SEEN)) {
                    identity.put(Constants.MessageKey.IDENTITY_DATE_FIRST_SEEN, 0);
                    changeMade = true;
                }
                if (!identity.has(Constants.MessageKey.IDENTITY_FIRST_SEEN)) {
                    identity.put(Constants.MessageKey.IDENTITY_FIRST_SEEN, true);
                    changeMade = true;
                }
            }

        } catch (JSONException jse) {

        }
        return changeMade;
    }

    public String getDeviceApplicationStamp() {
        String das = sPreferences.getString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, null);
        if (MPUtility.isEmpty(das)) {
            das = UUID.randomUUID().toString();
            sPreferences.edit()
                    .putString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, das)
                    .apply();
        }
        return das;
    }

    public JSONObject getCookies(long mpId) {
        if (mCurrentCookies == null) {
            String currentCookies = getUserConfig(mpId).getCookies();
            if (MPUtility.isEmpty(currentCookies)) {
                mCurrentCookies = new JSONObject();
                getUserConfig(mpId).setCookies(mCurrentCookies.toString());
                return mCurrentCookies;
            } else {
                try {
                    mCurrentCookies = new JSONObject(currentCookies);
                } catch (JSONException e) {
                    mCurrentCookies = new JSONObject();
                }
            }
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.set(Calendar.YEAR, 1990);
            Date oldDate = nowCalendar.getTime();
            SimpleDateFormat parser = new SimpleDateFormat("yyyy");
            Iterator<?> keys = mCurrentCookies.keys();
            ArrayList<String> keysToRemove = new ArrayList<String>();
            while (keys.hasNext()) {
                try {
                    String key = (String) keys.next();
                    if (mCurrentCookies.get(key) instanceof JSONObject) {
                        String expiration = ((JSONObject) mCurrentCookies.get(key)).getString("e");
                        try {
                            Date date = parser.parse(expiration);
                            if (date.before(oldDate)) {
                                keysToRemove.add(key);
                            }
                        } catch (ParseException dpe) {

                        }
                    }
                } catch (JSONException jse) {

                }
            }
            for (String key : keysToRemove) {
                mCurrentCookies.remove(key);
            }
            if (keysToRemove.size() > 0) {
                getUserConfig(mpId).setCookies(mCurrentCookies.toString());
            }
            return mCurrentCookies;
        } else {
            return mCurrentCookies;
        }
    }

    JSONArray markIdentitiesAsSeen(JSONArray uploadedIdentities) {
        return markIdentitiesAsSeen(uploadedIdentities, getMpid());
    }

    JSONArray markIdentitiesAsSeen(JSONArray uploadedIdentities, long mpId) {
        try {

            JSONArray currentIdentities = getUserIdentityJson(mpId);
            if (currentIdentities.length() == 0) {
                return null;
            }
            uploadedIdentities = new JSONArray(uploadedIdentities.toString());
            Set<Integer> identityTypes = new HashSet<Integer>();
            for (int i = 0; i < uploadedIdentities.length(); i++) {
                if (uploadedIdentities.getJSONObject(i).optBoolean(Constants.MessageKey.IDENTITY_FIRST_SEEN)) {
                    identityTypes.add(uploadedIdentities.getJSONObject(i).getInt(Constants.MessageKey.IDENTITY_NAME));
                }
            }
            if (identityTypes.size() > 0) {
                for (int i = 0; i < currentIdentities.length(); i++) {
                    int identity = currentIdentities.getJSONObject(i).getInt(Constants.MessageKey.IDENTITY_NAME);
                    if (identityTypes.contains(identity)) {
                        currentIdentities.getJSONObject(i).put(Constants.MessageKey.IDENTITY_FIRST_SEEN, false);
                    }
                }
                return currentIdentities;
            }
        } catch (JSONException jse) {

        }
        return null;
    }

    public String getIdentityApiContext() {
        return sPreferences.getString(Constants.PrefKeys.IDENTITY_API_CONTEXT, null);
    }

    public void setIdentityApiContext(String context) {
        sPreferences.edit().putString(Constants.PrefKeys.IDENTITY_API_CONTEXT, context).apply();
    }

    public String getPushToken() {
        return sPreferences.getString(Constants.PrefKeys.PUSH_TOKEN, null);
    }

    public void setPushToken(String token) {
        sPreferences.edit().putString(Constants.PrefKeys.PUSH_TOKEN, token).apply();
    }

    public String getPreviousGoogleAdId() {
        MPUtility.AndroidAdIdInfo adInfo = MPUtility.getGoogleAdIdInfo(mContext);
        String currentAdId = null;
        if (adInfo != null) {
            currentAdId = adInfo.id;
        }
        return sPreferences.getString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, currentAdId);
    }

    public void setPreviousGoogleAdId() {
        MPUtility.AndroidAdIdInfo adInfo = MPUtility.getGoogleAdIdInfo(mContext);
        String currentAdId = null;
        if (adInfo != null) {
            currentAdId = adInfo.id;
        }
        sPreferences.edit().putString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, currentAdId).apply();
    }

    private static Set<IdentityApi.MpIdChangeListener> mpIdChangeListeners = new HashSet<IdentityApi.MpIdChangeListener>();

    public static void addMpIdChangeListener(IdentityApi.MpIdChangeListener listener) {
        mpIdChangeListeners.add(listener);
    }

    private void triggerMpidChangeListenerCallbacks(long mpid) {
        if (MPUtility.isEmpty(mpIdChangeListeners)) {
            return;
        }
        for (IdentityApi.MpIdChangeListener listenerRef: mpIdChangeListeners) {
            if (listenerRef != null) {
                listenerRef.onMpIdChanged(mpid);
            }
        }
    }
}