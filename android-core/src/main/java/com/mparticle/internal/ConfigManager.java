package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.Configuration;
import com.mparticle.ExceptionHandler;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.networking.NetworkOptions;
import com.mparticle.networking.NetworkOptionsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    public static final String CONFIG_JSON = "json";
    public static final String CONFIG_JSON_TIMESTAMP = "json_timestamp";
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
    public static final String PREFERENCES_FILE = "mp_preferences";
    public static final String KEY_INCLUDE_SESSION_HISTORY = "inhd";
    private static final String KEY_DEVICE_PERFORMANCE_METRICS_DISABLED = "dpmd";
    public static final String WORKSPACE_TOKEN = "wst";
    static final String ALIAS_MAX_WINDOW = "alias_max_window";
    static final String KEY_RAMP = "rp";
    static final String DATAPLAN_KEY = "dpr";
    static final String DATAPLAN_OBJ = "dtpn";
    static final String DATAPLAN_BLOCKING = "blok";
    static final String DATAPLAN_VERSION = "vers";
    static final String DATAPLAN_BLOCK_EVENTS = "ev";
    static final String DATAPLAN_BLOCK_EVENT_ATTRIBUTES = "ea";
    static final String DATAPLAN_BLOCK_USER_ATTRIBUTES = "ua";
    static final String DATAPLAN_BLOCK_USER_IDENTITIES = "id";


    private static final int DEVMODE_UPLOAD_INTERVAL_MILLISECONDS = 10 * 1000;
    private static final int DEFAULT_MAX_ALIAS_WINDOW_DAYS = 90;
    private Context mContext;
    private static NetworkOptions sNetworkOptions;
    private boolean mIgnoreDataplanOptionsFromConfig = false;
    private MParticleOptions.DataplanOptions mDataplanOptions;

    static SharedPreferences sPreferences;

    private static JSONArray sPushKeys;
    private UserStorage mUserStorage;
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
    private String mDataplanId;
    private Integer mDataplanVersion;
    private Integer mMaxConfigAge;
    public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
    public static final int MINIMUM_CONNECTION_TIMEOUT_SECONDS = 1;
    public static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 60;
    public static final int DEFAULT_UPLOAD_INTERVAL = 600;
    private List<ConfigLoadedListener> configUpdatedListeners = new ArrayList<ConfigLoadedListener>();

    private ConfigManager() {
        super();
    }

    public static ConfigManager getInstance(Context context) {
        ConfigManager configManager = null;
        MParticle mParticle = MParticle.getInstance();
        if (mParticle != null) {
            configManager = MParticle.getInstance().Internal().getConfigManager();
        }
        if (configManager == null) {
            configManager = new ConfigManager(context);
        }
        return configManager;
    }

    public ConfigManager(Context context) {
        this(context, null, null, null, null, null, null, null, null);
    }

    public ConfigManager(@NonNull MParticleOptions options) {
        this(options.getContext(), options.getEnvironment(), options.getApiKey(), options.getApiSecret(), options.getDataplanOptions(), options.getDataplanId(), options.getDataplanVersion(), options.getConfigMaxAge(), options.getConfigurationsForTarget(ConfigManager.class));
    }

    public ConfigManager(@NonNull Context context, @Nullable MParticle.Environment environment, @Nullable String apiKey, @Nullable String apiSecret, @Nullable MParticleOptions.DataplanOptions dataplanOptions, @Nullable String dataplanId, @Nullable Integer dataplanVersion, @Nullable Integer configMaxAge, @Nullable List<Configuration<ConfigManager>> configurations) {
        mContext = context.getApplicationContext();
        sPreferences = getPreferences(mContext);
        if (apiKey != null || apiSecret != null) {
            setCredentials(apiKey, apiSecret);
        }
        if (environment != null) {
            setEnvironment(environment);
        }
        mUserStorage = UserStorage.create(mContext, getMpid());
        //if we are initialized with a DataplanOptions instance, then we will ignore values from remote config
        mIgnoreDataplanOptionsFromConfig = dataplanOptions != null;
        mDataplanOptions = dataplanOptions;
        mDataplanVersion = dataplanVersion;
        mDataplanId = dataplanId;
        mMaxConfigAge = configMaxAge;
        checkConfigStaleness();
        restoreOldConfig();
        if (configurations != null) {
            for (Configuration configuration : configurations) {
                configuration.apply(this);
            }

        }
    }

    private void restoreOldConfig() {
        String oldConfig = getConfig();
        if (!MPUtility.isEmpty(oldConfig)) {
            try {
                JSONObject oldConfigJson = new JSONObject(oldConfig);
                reloadConfig(oldConfigJson);
            } catch (Exception jse) {

            }
        }
    }

    /**
     * This called on startup. The only thing that's completely necessary is that we fire up kits.
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

    public MParticleOptions.DataplanOptions getDataplanOptions() {
        return mDataplanOptions;
    }

    public UserStorage getUserStorage() {
        return getUserStorage(getMpid());
    }

    public UserStorage getUserStorage(long mpId) {
        if (mUserStorage == null || mUserStorage.getMpid() != mpId) {
            mUserStorage = UserStorage.create(mContext, mpId);
        }
        return mUserStorage;
    }

    public static UserStorage getUserStorage(Context context) {
        return UserStorage.create(context, getMpid(context));
    }

    public static UserStorage getUserStorage(Context context, long mpid) {
        return UserStorage.create(context, mpid);
    }

    public void deleteUserStorage(Context context, long mpid) {
        if (mUserStorage != null) {
            mUserStorage.deleteUserConfig(context, mpid);
        }
    }

    public void deleteUserStorage(long mpId) {
        deleteUserStorage(mContext, mpId);
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

    /**
     * detrmine if the stored config age is greater than mMaxConfigAge and clear it from storage if it is
     */
    void checkConfigStaleness() {
        Long storageDate = getConfigTimestamp();
        if (storageDate == null) {
            // migration step: if the current config does not have a timestamp, set one to the current time
            setConfigTimestamp(System.currentTimeMillis());
            storageDate = getConfigTimestamp();
        }
        if (mMaxConfigAge == null || mMaxConfigAge < 0) {
            return;
        }
        if (mMaxConfigAge == 0) {
            clearConfig();
        } else {
            if (System.currentTimeMillis() >= storageDate + TimeUnit.SECONDS.toMillis(mMaxConfigAge)) {
                clearConfig();
            }
        }
    }

    String getConfig() {
        return sPreferences.getString(CONFIG_JSON, null);
    }

    void setConfigTimestamp(Long timestamp) {
        sPreferences.edit()
                .putLong(CONFIG_JSON_TIMESTAMP, timestamp)
                .apply();
    }

    @Nullable
    Long getConfigTimestamp() {
        if (sPreferences.contains(CONFIG_JSON_TIMESTAMP)) {
            return sPreferences.getLong(CONFIG_JSON_TIMESTAMP, 0);
        } else {
            return null;
        }
    }

    void saveConfigJson(JSONObject json, String etag, String lastModified) throws JSONException {
        if (json != null) {
            Logger.debug("Updating config to:\n" + json.toString());
            sPreferences.edit()
                    .putLong(CONFIG_JSON_TIMESTAMP, System.currentTimeMillis())
                    .putString(CONFIG_JSON, json.toString())
                    .putString(Constants.PrefKeys.ETAG, etag)
                    .putString(Constants.PrefKeys.IF_MODIFIED, lastModified)
                    .apply();
        } else {
            Logger.debug("clearing current configurations");
            clearConfig();
        }
    }

    void clearConfig() {
        sPreferences.edit()
                .remove(CONFIG_JSON_TIMESTAMP)
                .remove(CONFIG_JSON)
                .remove(Constants.PrefKeys.ETAG)
                .remove(Constants.PrefKeys.IF_MODIFIED)
                .apply();
    }

    public synchronized void updateConfig(JSONObject responseJSON) throws JSONException {
        updateConfig(responseJSON, true, null, null);
    }

    public synchronized void updateConfig(JSONObject responseJSON, String etag, String lastModified) throws JSONException {
        updateConfig(responseJSON, true, etag, lastModified);
    }

    public synchronized void reloadConfig(JSONObject responseJSON) throws JSONException  {
        updateConfig(responseJSON, false, null, null);
    }

    private synchronized void updateConfig(JSONObject responseJSON, boolean newConfig, String etag, String lastModified) throws JSONException {
        MParticle instance = MParticle.getInstance();
        KitFrameworkWrapper kitManager = null;
        if (instance != null) {
            kitManager = instance.Internal().getKitManager();
        }
        SharedPreferences.Editor editor = sPreferences.edit();
        if (newConfig) {
            saveConfigJson(responseJSON, etag, lastModified);
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
        if (responseJSON.has(WORKSPACE_TOKEN)) {
            editor.putString(WORKSPACE_TOKEN, responseJSON.getString(WORKSPACE_TOKEN));
        } else {
            editor.remove(WORKSPACE_TOKEN);
        }
        if (responseJSON.has(ALIAS_MAX_WINDOW)) {
            editor.putInt(ALIAS_MAX_WINDOW, responseJSON.getInt(ALIAS_MAX_WINDOW));
        } else {
            editor.remove(ALIAS_MAX_WINDOW);
        }
        if (!mIgnoreDataplanOptionsFromConfig) {
            mDataplanOptions = parseDataplanOptions(responseJSON);
            if (kitManager != null) {
                kitManager.updateDataplan(mDataplanOptions);
            }
        }
        editor.apply();
        applyConfig();
        if (newConfig && kitManager != null) {
            kitManager.updateKits(responseJSON.optJSONArray(KEY_EMBEDDED_KITS));
        }
        for (ConfigLoadedListener listener: new ArrayList<ConfigLoadedListener>(configUpdatedListeners)) {
            if (listener != null){
                listener.onConfigUpdated(newConfig ? ConfigType.NEW : ConfigType.RELOADED, responseJSON);
            }
        }
    }

    public String getActiveModuleIds() {
        return MParticle.getInstance().Internal().getKitManager().getActiveModuleIds();
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
        String senderId = getPushSenderId();
        if (isPushEnabled() && senderId != null) {
            MParticle.getInstance().Messaging().enablePushNotifications(senderId);
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
        if (userTriggered) {
            setLogUnhandledExceptions(true);
        }
        if (null == mExHandler) {
            Thread.UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (!(currentUncaughtExceptionHandler instanceof ExceptionHandler)) {
                mExHandler = new ExceptionHandler(currentUncaughtExceptionHandler);
                Thread.setDefaultUncaughtExceptionHandler(mExHandler);
            }
        }
    }

    public void disableUncaughtExceptionLogging(boolean userTriggered) {
        if (userTriggered) {
            setLogUnhandledExceptions(false);
        }
        if (null != mExHandler) {
            Thread.UncaughtExceptionHandler currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (currentUncaughtExceptionHandler instanceof ExceptionHandler) {
                Thread.setDefaultUncaughtExceptionHandler(mExHandler.getOriginalExceptionHandler());
                mExHandler = null;
            }
        }
    }

    public boolean getLogUnhandledExceptions() {
        if (VALUE_APP_DEFINED.equals(mLogUnhandledExceptions)) {
            return sPreferences.getBoolean(Constants.PrefKeys.REPORT_UNCAUGHT_EXCEPTIONS, false);
        } else {
            return VALUE_CUE_CATCH.equals(mLogUnhandledExceptions);
        }
    }

    public void setLogUnhandledExceptions(boolean log) {
        sPreferences.edit().putBoolean(Constants.PrefKeys.REPORT_UNCAUGHT_EXCEPTIONS, log).apply();
    }

    public String getApiKey() {
        return sPreferences.getString(Constants.PrefKeys.API_KEY, null);
    }

    public String getApiSecret() {
        return sPreferences.getString(Constants.PrefKeys.API_SECRET,null);
    }

    public void setCredentials(String apiKey, String secret) {
        sPreferences.edit()
                .putString(Constants.PrefKeys.API_KEY, apiKey)
                .putString(Constants.PrefKeys.API_SECRET, secret)
                .apply();
    }

    public long getUploadInterval() {
        if (getEnvironment().equals(MParticle.Environment.Development)) {
            return DEVMODE_UPLOAD_INTERVAL_MILLISECONDS;
        } else {
            if (mUploadInterval > 0) {
                return 1000 * mUploadInterval;
            } else {
                return (1000 * sPreferences.getInt(Constants.PrefKeys.UPLOAD_INTERVAL, DEFAULT_UPLOAD_INTERVAL));
            }
        }
    }

    public void setEnvironment(MParticle.Environment environment) {
        if (environment != null) {
            sPreferences.edit().putInt(Constants.PrefKeys.ENVIRONMENT, environment.getValue()).apply();
        } else {
            sPreferences.edit().remove(Constants.PrefKeys.ENVIRONMENT).apply();
        }
    }

    public static MParticle.Environment getEnvironment() {
        if (sPreferences != null) {
            int env = sPreferences.getInt(Constants.PrefKeys.ENVIRONMENT, MParticle.Environment.Production.getValue());
            for(MParticle.Environment environment: MParticle.Environment.values()) {
                if (environment.getValue() == env) {
                    return environment;
                }
            }
        }
        return MParticle.Environment.Production;
    }

    public void setUploadInterval(int uploadInterval) {
        sPreferences.edit().putInt(Constants.PrefKeys.UPLOAD_INTERVAL, uploadInterval).apply();
    }

    public int getSessionTimeout() {
        if (mSessionTimeoutInterval > 0) {
            return mSessionTimeoutInterval * 1000;
        } else {
            return sPreferences.getInt(Constants.PrefKeys.SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_SECONDS) * 1000;
        }
    }

    public void setSessionTimeout(int sessionTimeout) {
        sPreferences.edit().putInt(Constants.PrefKeys.SESSION_TIMEOUT, sessionTimeout).apply();
    }

    public boolean isPushEnabled() {
        return sPreferences.getBoolean(Constants.PrefKeys.PUSH_ENABLED, false) && getPushSenderId() != null;
    }

    public String getPushSenderId() {
        PushRegistrationHelper.PushRegistration pushRegistration = getPushRegistration();
        if (pushRegistration != null) {
            return pushRegistration.senderId;
        } else {
            return null;
        }
    }

    public @Nullable String getPushInstanceId() {
        PushRegistrationHelper.PushRegistration pushRegistration = getPushRegistration();
        if (pushRegistration != null) {
            return pushRegistration.instanceId;
        } else {
            return null;
        }
    }

    public PushRegistrationHelper.PushRegistration getPushRegistration() {
        String senderId = sPreferences.getString(Constants.PrefKeys.PUSH_SENDER_ID, null);
        String instanceId = sPreferences.getString(Constants.PrefKeys.PUSH_INSTANCE_ID, null);
        return new PushRegistrationHelper.PushRegistration(instanceId, senderId);
    }

    public void setPushRegistrationFetched() {
        int appVersion = getAppVersion();
        sPreferences.edit()
                .putInt(Constants.PrefKeys.PROPERTY_APP_VERSION, appVersion)
                .putInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Build.VERSION.SDK_INT)
                .apply();
    }

    public boolean isPushRegistrationFetched() {
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = sPreferences.getInt(Constants.PrefKeys.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        int osVersion = sPreferences.getInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Integer.MIN_VALUE);
        return registeredVersion == currentVersion && osVersion == Build.VERSION.SDK_INT;
    }

    @Nullable
    public String getPushInstanceIdBackground() {
        return sPreferences.getString(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND, null);
    }

    public void setPushSenderId(String senderId) {
        sPreferences.edit().putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                .putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                .apply();
    }

    public void setPushInstanceId(@Nullable String token) {
        sPreferences.edit().putString(Constants.PrefKeys.PUSH_INSTANCE_ID, token).apply();
    }

    public void setPushRegistration(PushRegistrationHelper.PushRegistration pushRegistration) {
        if (pushRegistration == null || MPUtility.isEmpty(pushRegistration.senderId)) {
            clearPushRegistration();
        } else {
            setPushSenderId(pushRegistration.senderId);
            setPushInstanceId(pushRegistration.instanceId);
        }
    }

    public void setPushRegistrationInBackground(PushRegistrationHelper.PushRegistration pushRegistration) {
        String oldInstanceId = getPushInstanceId();
        if (oldInstanceId == null) {
            oldInstanceId = "";
        }
        sPreferences.edit()
                .putString(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND, oldInstanceId)
                .apply();
        setPushRegistration(pushRegistration);
    }

    public void clearPushRegistration() {
        sPreferences.edit()
                .remove(Constants.PrefKeys.PUSH_SENDER_ID)
                .remove(Constants.PrefKeys.PUSH_INSTANCE_ID)
                .remove(Constants.PrefKeys.PUSH_ENABLED)
                .remove(Constants.PrefKeys.PROPERTY_APP_VERSION)
                .remove(Constants.PrefKeys.PROPERTY_OS_VERSION)
                .apply();
    }

    public void clearPushRegistrationBackground() {
        sPreferences.edit()
                .remove(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND)
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

    public void setDisplayPushNotifications(Boolean display) {
        sPreferences.edit()
                .putBoolean(Constants.PrefKeys.DISPLAY_PUSH_NOTIFICATIONS, display)
                .apply();
    }

    public static Boolean isDisplayPushNotifications(Context context) {
        return getPreferences(context).getBoolean(Constants.PrefKeys.DISPLAY_PUSH_NOTIFICATIONS, false);
    }

    private static SharedPreferences getPreferences(Context context){
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
        return getUserStorage(context).getBreadcrumbLimit();
    }

    public static int getBreadcrumbLimit(Context context, long mpId) {
        return getUserStorage(context, mpId).getBreadcrumbLimit();
    }

    public static String getCurrentUserLtv(Context context) {
        return getUserStorage(context).getLtv();
    }

    public void setBreadcrumbLimit(int newLimit) {
        setBreadcrumbLimit(newLimit, getMpid());
    }

    public void setBreadcrumbLimit(int newLimit, long mpId) {
        getUserStorage(mpId).setBreadcrumbLimit(newLimit);
    }

    public static void setNeedsToMigrate(Context context, boolean needsToMigrate) {
        UserStorage.setNeedsToMigrate(context, needsToMigrate);
    }

    public static void clear() {
        sPreferences.edit().clear().apply();
    }

    private synchronized void setProviderPersistence(JSONObject persistence) {
        mProviderPersistence = persistence;
    }

    public synchronized JSONObject getProviderPersistence() {
        return mProviderPersistence;
    }

    public void setMpid(long newMpid, boolean isLoggedInUser) {
        long previousMpid = getMpid();
        boolean currentLoggedInUser = false;
        UserStorage currentUserStorage = getUserStorage();
        if (currentUserStorage != null) {
            currentUserStorage.setLastSeenTime(System.currentTimeMillis());
            currentLoggedInUser = currentUserStorage.isLoggedIn();
        }
        UserStorage userStorage = UserStorage.create(mContext, newMpid);
        userStorage.setLoggedInUser(isLoggedInUser);

        sPreferences.edit().putLong(Constants.PrefKeys.MPID, newMpid).apply();
        if (mUserStorage == null || mUserStorage.getMpid() != newMpid) {
            mUserStorage = userStorage;
            mUserStorage.setFirstSeenTime(System.currentTimeMillis());
        }
        if ((previousMpid != newMpid || currentLoggedInUser != isLoggedInUser)) {
            triggerMpidChangeListenerCallbacks(newMpid, previousMpid);
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

    public boolean mpidExists(long mpid) {
        return UserStorage.getMpIdSet(mContext).contains(mpid);
    }

    public Set<Long> getMpids() {
        return UserStorage.getMpIdSet(mContext);
    }

    private static boolean sInProgress;
    public static void setIdentityRequestInProgress(boolean inProgress) {
        sInProgress = inProgress;
    }

    public void mergeUserConfigs(long subjectMpId, long targetMpId) {
        UserStorage subjectUserStorage = getUserStorage(subjectMpId);
        UserStorage targetUserStorage = getUserStorage(targetMpId);
        targetUserStorage.merge(subjectUserStorage);
    }

    public int getCurrentRampValue() {
        return mRampValue;
    }

    public JSONArray getTriggerMessageHashes() {
        return mTriggerMessageHashes;
    }

    public boolean shouldTrigger(BaseMPMessage message) {
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

    public void setIntegrationAttributes(int integrationId, Map<String, String> newAttributes) {
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
            currentJsonAttributes.put(Integer.toString(integrationId), newJsonAttributes);
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

    public Map<String, String> getIntegrationAttributes(int integrationId) {
        Map<String, String> integrationAttributes = new HashMap<String, String>();
        JSONObject jsonAttributes = getIntegrationAttributes();
        if (jsonAttributes != null) {
            JSONObject kitAttributes = jsonAttributes.optJSONObject(Integer.toString(integrationId));
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

    @NonNull
    public JSONArray getUserIdentityJson(long mpId) {
        JSONArray userIdentities = null;
        String userIds = getUserStorage(mpId).getUserIdentities();

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
        getUserStorage(mpId).setUserIdentities(userIdentities.toString());
    }

    public String getDataplanId() {
        return mDataplanId;
    }

    public Integer getDataplanVersion() {
        return mDataplanVersion;
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
            String currentCookies = getUserStorage(mpId).getCookies();
            if (MPUtility.isEmpty(currentCookies)) {
                mCurrentCookies = new JSONObject();
                getUserStorage(mpId).setCookies(mCurrentCookies.toString());
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
                getUserStorage(mpId).setCookies(mCurrentCookies.toString());
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

    public String getPreviousAdId() {
        MPUtility.AdIdInfo adInfo = MPUtility.getAdIdInfo(mContext);
        String currentAdId = null;
        if (adInfo != null) {
            currentAdId = adInfo.id;
        }
        return sPreferences.getString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, currentAdId);
    }

    public void setPreviousAdId() {
        MPUtility.AdIdInfo adInfo = MPUtility.getAdIdInfo(mContext);
        String currentAdId = null;
        if (adInfo != null) {
            currentAdId = adInfo.id;
        }
        sPreferences.edit().putString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, currentAdId).apply();
    }

    public int getIdentityConnectionTimeout() {
        return sPreferences.getInt(Constants.PrefKeys.IDENTITY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_SECONDS) * 1000;
    }

    public int getConnectionTimeout() {
        return DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000;
    }

    public void setIdentityConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout >= MINIMUM_CONNECTION_TIMEOUT_SECONDS) {
            sPreferences.edit().putInt(Constants.PrefKeys.IDENTITY_CONNECTION_TIMEOUT, connectionTimeout).apply();
        }
    }

    private static Set<IdentityApi.MpIdChangeListener> mpIdChangeListeners = new HashSet<IdentityApi.MpIdChangeListener>();

    public static void addMpIdChangeListener(IdentityApi.MpIdChangeListener listener) {
        mpIdChangeListeners.add(listener);
    }

    private void triggerMpidChangeListenerCallbacks(long mpid, long previousMpid) {
        if (MPUtility.isEmpty(mpIdChangeListeners)) {
            return;
        }
        for (IdentityApi.MpIdChangeListener listenerRef: new ArrayList<IdentityApi.MpIdChangeListener>(mpIdChangeListeners)) {
            if (listenerRef != null) {
                listenerRef.onMpIdChanged(mpid, previousMpid);
            }
        }
    }

    public ConsentState getConsentState(long mpid) {
        String serializedConsent = getUserStorage(mpid).getSerializedConsentState();
        return ConsentState.withConsentState(serializedConsent).build();
    }

    public void setConsentState(ConsentState state, long mpid) {
        String serializedConsent = null;
        if (state != null) {
            serializedConsent = state.toString();
        }
        getUserStorage(mpid).setSerializedConsentState(serializedConsent);
    }

    public NetworkOptions getNetworkOptions() {
        if (sNetworkOptions == null) {
            sNetworkOptions = NetworkOptionsManager.validateAndResolve(null);
        }
        return sNetworkOptions;
    }

    public synchronized void setNetworkOptions(NetworkOptions networkOptions) {
        sNetworkOptions = networkOptions;
        sPreferences.edit().remove(Constants.PrefKeys.NETWORK_OPTIONS).apply();
    }

    @NonNull
    public String getWorkspaceToken() {
        return sPreferences.getString(WORKSPACE_TOKEN, "");
    }

    /**
     * the maximum allowed age of "start_time" in an AliasRequest, in days
     * @return
     */
    public int getAliasMaxWindow() {
        return sPreferences.getInt(ALIAS_MAX_WINDOW, DEFAULT_MAX_ALIAS_WINDOW_DAYS);
    }

    public String getEtag() {
        return sPreferences.getString(Constants.PrefKeys.ETAG, null);
    }

    public String getIfModified() {
        return sPreferences.getString(Constants.PrefKeys.IF_MODIFIED, null);
    }

    public void addConfigUpdatedListener(ConfigLoadedListener listener) {
        configUpdatedListeners.add(listener);
    }

    @Nullable
    MParticleOptions.DataplanOptions parseDataplanOptions(JSONObject jsonObject) {
        if (jsonObject != null) {
            JSONObject dataplanConfig = jsonObject.optJSONObject(DATAPLAN_KEY);
            if (dataplanConfig != null) {
                JSONObject dataplanContanier = dataplanConfig.optJSONObject(DATAPLAN_OBJ);
                if (dataplanContanier != null) {
                    JSONObject block = dataplanContanier.optJSONObject(DATAPLAN_BLOCKING);
                    JSONObject dataplanVersion = dataplanContanier.optJSONObject(DATAPLAN_VERSION);
                    if (block != null) {
                        return MParticleOptions.DataplanOptions.builder()
                                .dataplanVersion(dataplanVersion)
                                .blockEvents(block.optBoolean(DATAPLAN_BLOCK_EVENTS, false))
                                .blockEventAttributes(block.optBoolean(DATAPLAN_BLOCK_EVENT_ATTRIBUTES, false))
                                .blockUserAttributes(block.optBoolean(DATAPLAN_BLOCK_USER_ATTRIBUTES, false))
                                .blockUserIdentities(block.optBoolean(DATAPLAN_BLOCK_USER_IDENTITIES, false))
                                .build();
                    }
                }
            }
        }
        return null;
    }

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    enum ConfigType {
        NEW,
        RELOADED
    }

    interface ConfigLoadedListener {
        public void onConfigUpdated(ConfigType configType, JSONObject config);
    }
}