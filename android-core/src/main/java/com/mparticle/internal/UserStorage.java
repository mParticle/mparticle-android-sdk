package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.UrlQuerySanitizer;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static com.mparticle.internal.ConfigManager.PREFERENCES_FILE;

public class UserStorage {
    private static final String USER_CONFIG_COLLECTION = "mp::user_config_collection";

    private static final String SESSION_COUNTER = "mp::breadcrumbs::sessioncount";
    private static final String DELETED_USER_ATTRS = "mp::deleted_user_attrs::";
    private static final String BREADCRUMB_LIMIT = "mp::breadcrumbs::limit";
    private static final String LAST_USE = "mp::lastusedate";
    private static final String PREVIOUS_SESSION_FOREGROUND = "mp::time_in_fg";
    private static final String PREVIOUS_SESSION_ID = "mp::session::previous_id";
    private static final String PREVIOUS_SESSION_START = "mp::session::previous_start";
    private static final String LTV = "mp::ltv";
    private static final String TOTAL_RUNS = "mp::totalruns";
    private static final String COOKIES = "mp::cookies";
    private static final String TOTAL_SINCE_UPGRADE = "mp::launch_since_upgrade";
    private static final String USER_IDENTITIES = "mp::user_ids::";
    private static final String CONSENT_STATE = "mp::consent_state::";
    private static final String KNOWN_USER = "mp::known_user";
    private static final String FIRST_SEEN_TIME = "mp::first_seen";
    private static final String LAST_SEEN_TIME = "mp::last_seen";
    private static final String DEFAULT_SEEN_TIME = "mp::default_seen_time";

    static final int DEFAULT_BREADCRUMB_LIMIT = 50;

    private long mpId;
    private SharedPreferences mPreferences;
    private Context mContext;

    SharedPreferences messageManagerSharedPreferences;

    static List<UserStorage> getAllUsers(Context context) {
        Set<Long> userMpIds = getMpIdSet(context);
        List<UserStorage> userStorages = new ArrayList<UserStorage>();
        for (Long mdId: userMpIds) {
            userStorages.add(new UserStorage(context, Long.valueOf(mdId)));
        }
        return userStorages;
    }

    boolean deleteUserConfig(Context context, long mpId) {
        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(getFileName(mpId));
        } else {
            context.getSharedPreferences(getFileName(mpId), Context.MODE_PRIVATE).edit().clear().apply();
        }
        return removeMpId(context, mpId);
    }

    static UserStorage create(Context context, long mpid) {
        return new UserStorage(context, mpid);
    }

    public static void setNeedsToMigrate(Context context, boolean needsToMigrate) {
        SharedPreferencesMigrator.setNeedsToMigrate(context, needsToMigrate);
    }

    private UserStorage(Context context, long mpId) {
        this.mContext = context;
        this.mpId = mpId;
        this.mPreferences = getPreferenceFile(mpId);
        if (SharedPreferencesMigrator.needsToMigrate(context)) {
            SharedPreferencesMigrator.setNeedsToMigrate(context, false);
            new SharedPreferencesMigrator(context).migrate(this);
        }
        this.messageManagerSharedPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        setDefaultSeenTime();
    }

    long getMpid() {
        return mpId;
    }

    int getCurrentSessionCounter(){
        return getCurrentSessionCounter(0);
    }

    int getCurrentSessionCounter(int defaultValue) {
        return mPreferences.getInt(SESSION_COUNTER, defaultValue);
    }

    private void setCurrentSessionCounter(int sessionCounter) {
        mPreferences.edit().putInt(SESSION_COUNTER, sessionCounter).apply();
    }

    private boolean hasCurrentSessionCounter() {
        return mPreferences.contains(SESSION_COUNTER);
    }

    void incrementSessionCounter() {
        int nextCount = getCurrentSessionCounter() + 1;
        if (nextCount >= (Integer.MAX_VALUE / 100)){
            nextCount = 0;
        }
        mPreferences.edit().putInt(SESSION_COUNTER, nextCount).apply();
    }


    String getDeletedUserAttributes() {
        return mPreferences.getString(DELETED_USER_ATTRS, null);
    }

    void deleteDeletedUserAttributes() {
        mPreferences.edit().putString(DELETED_USER_ATTRS, null).apply();
    }

    void setDeletedUserAttributes(String deletedUserAttributes) {
        mPreferences.edit().putString(DELETED_USER_ATTRS, deletedUserAttributes).apply();
    }

    private boolean hasDeletedUserAttributes() {
        return mPreferences.contains(DELETED_USER_ATTRS);
    }

    int getBreadcrumbLimit() {
        if (mPreferences != null){
            return mPreferences.getInt(BREADCRUMB_LIMIT, DEFAULT_BREADCRUMB_LIMIT);
        }
        return DEFAULT_BREADCRUMB_LIMIT;
    }

    void setBreadcrumbLimit(int newLimit) {
        mPreferences.edit().putInt(BREADCRUMB_LIMIT, newLimit).apply();
    }

    private boolean hasBreadcrumbLimit() {
        return mPreferences.contains(BREADCRUMB_LIMIT);
    }

    long getLastUseDate() {
        return getLastUseDate(0);
    }

    long getLastUseDate(long defaultValue) {
        return mPreferences.getLong(LAST_USE, defaultValue);
    }

    void setLastUseDate(long lastUseDate) {
        mPreferences.edit().putLong(LAST_USE, lastUseDate).apply();
    }

    private boolean hasLastUserDate() {
        return mPreferences.contains(LAST_USE);
    }

    long getPreviousSessionForegound() {
        return getPreviousSessionForegound(-1);
    }

    long getPreviousSessionForegound(long defaultValue) {
        return mPreferences.getLong(PREVIOUS_SESSION_FOREGROUND, defaultValue);
    }

    void clearPreviousTimeInForeground() {
        mPreferences.edit().putLong(PREVIOUS_SESSION_FOREGROUND, -1).apply();
    }

    void setPreviousSessionForeground(long previousTimeInForeground) {
        mPreferences.edit().putLong(PREVIOUS_SESSION_FOREGROUND, previousTimeInForeground).apply();
    }

    private boolean hasPreviousSessionForegound() {
        return mPreferences.contains(PREVIOUS_SESSION_FOREGROUND);
    }

    String getPreviousSessionId() {
        return getPreviousSessionId("");
    }

    String getPreviousSessionId(String defaultValue) {
        return mPreferences.getString(PREVIOUS_SESSION_ID, defaultValue);
    }

    void setPreviousSessionId(String previousSessionId) {
        mPreferences.edit().putString(PREVIOUS_SESSION_ID, previousSessionId).apply();
    }

    private boolean hasPreviousSessionId() {
        return mPreferences.contains(PREVIOUS_SESSION_ID);
    }

    long getPreviousSessionStart(long defaultValue) {
        return mPreferences.getLong(PREVIOUS_SESSION_START, defaultValue);
    }

    void setPreviousSessionStart(long previousSessionStart) {
        mPreferences.edit().putLong(PREVIOUS_SESSION_START, previousSessionStart).apply();
    }

    private boolean hasPreviousSessionStart() {
        return mPreferences.contains(PREVIOUS_SESSION_START);
    }

    String getLtv() {
        return mPreferences.getString(LTV, "0");
    }

    void setLtv(String ltv) {
        mPreferences.edit().putString(LTV, ltv).apply();
    }

    private boolean hasLtv() {
        return mPreferences.contains(LTV);
    }

    int getTotalRuns(int defaultValue) {
        return mPreferences.getInt(TOTAL_RUNS, defaultValue);
    }

    void setTotalRuns(int totalRuns) {
        mPreferences.edit().putInt(TOTAL_RUNS, totalRuns).apply();
    }

    private boolean hasTotalRuns() {
        return mPreferences.contains(TOTAL_RUNS);
    }

    String getCookies() {
        return mPreferences.getString(COOKIES, "");
    }

    void setCookies(String cookies) {
        mPreferences.edit().putString(COOKIES, cookies).apply();
    }

    private boolean hasCookies() {
        return mPreferences.contains(COOKIES);
    }

    int getLaunchesSinceUpgrade() {
        return mPreferences.getInt(TOTAL_SINCE_UPGRADE, 0);
    }

    void setLaunchesSinceUpgrade(int launchesSinceUpgrade) {
        mPreferences.edit().putInt(TOTAL_SINCE_UPGRADE, launchesSinceUpgrade).apply();
    }

    private boolean hasLaunchesSinceUpgrade() {
        return mPreferences.contains(TOTAL_SINCE_UPGRADE);
    }

    String getUserIdentities() {
        return mPreferences.getString(USER_IDENTITIES, "");
    }

    void setUserIdentities(String userIdentities) {
        mPreferences.edit().putString(USER_IDENTITIES, userIdentities).apply();
    }

    void setSerializedConsentState(String consentState) {
        mPreferences.edit().putString(CONSENT_STATE, consentState).apply();
    }

    String getSerializedConsentState() {
        return mPreferences.getString(CONSENT_STATE, null);
    }

    private boolean hasConsent() {
        return mPreferences.contains(CONSENT_STATE);
    }

    public boolean isLoggedIn() {
        return mPreferences.getBoolean(KNOWN_USER, false);
    }

    public long getFirstSeenTime() {
        if (!mPreferences.contains(FIRST_SEEN_TIME)) {
            mPreferences.edit().putLong(FIRST_SEEN_TIME, messageManagerSharedPreferences.getLong(Constants.PrefKeys.INSTALL_TIME, getDefaultSeenTime())).apply();
        }
        return mPreferences.getLong(FIRST_SEEN_TIME, getDefaultSeenTime());
    }

    public void setFirstSeenTime(Long time) {
        if (!mPreferences.contains(FIRST_SEEN_TIME)) {
            mPreferences.edit().putLong(FIRST_SEEN_TIME, time).apply();
        }
    }

    public long getLastSeenTime() {
        if (!mPreferences.contains(LAST_SEEN_TIME)) {
            mPreferences.edit().putLong(LAST_SEEN_TIME, getDefaultSeenTime()).apply();
        }
        return mPreferences.getLong(LAST_SEEN_TIME, getDefaultSeenTime());
    }

    public void setLastSeenTime(Long time) {
        mPreferences.edit().putLong(LAST_SEEN_TIME, time).apply();
    }

    //Set a default "lastSeenTime" for migration to SDK versions with MParticleUser.getLastSeenTime(),
    //where some users will not have a value for the field.
    private void setDefaultSeenTime() {
        SharedPreferences preferences = getMParticleSharedPrefs(mContext);
        if (!preferences.contains(DEFAULT_SEEN_TIME)) {
            preferences.edit().putLong(DEFAULT_SEEN_TIME, System.currentTimeMillis());
        }
    }

    private Long getDefaultSeenTime() {
        return getMParticleSharedPrefs(mContext).getLong(DEFAULT_SEEN_TIME, System.currentTimeMillis());
    }

    void setLoggedInUser(boolean knownUser) {
        mPreferences.edit().putBoolean(KNOWN_USER, knownUser).apply();
    }

    private boolean hasUserIdentities() {
        return mPreferences.contains(USER_IDENTITIES);
    }

    private SharedPreferences getPreferenceFile(long mpId) {
        Set<Long> mpIds = getMpIdSet(mContext);
        mpIds.add(mpId);
        setMpIds(mpIds);
        return mContext.getSharedPreferences(getFileName(mpId), Context.MODE_PRIVATE);
    }

    private static boolean removeMpId(Context context, long mpid) {
        Set<Long> mpids = getMpIdSet(context);
        boolean removed = mpids.remove(mpid);
        setMpIds(context, mpids);
        return removed;
    }

    static Set<Long> getMpIdSet(Context context) {
        JSONArray userConfigs = new JSONArray();
        try {
            userConfigs = new JSONArray(getMParticleSharedPrefs(context).getString(USER_CONFIG_COLLECTION, new JSONArray().toString()));
        } catch (JSONException ignore) {}
        Set<Long> mpIds = new TreeSet<Long>();
        for (int i = 0; i < userConfigs.length(); i++) {
            try {
                mpIds.add(userConfigs.getLong(i));
            }
            catch (JSONException ignore) {}
        }
        return mpIds;
    }

    private void setMpIds(Set<Long> mpIds) {
        setMpIds(mContext, mpIds);
    }

    private static void setMpIds(Context context, Set<Long> mpIds) {
        JSONArray jsonArray = new JSONArray();
        for (Long mpId: mpIds) {
            jsonArray.put(mpId);
        }
        getMParticleSharedPrefs(context).edit().putString(USER_CONFIG_COLLECTION, jsonArray.toString()).apply();
    }

    private static String getFileName(long mpId) {
        return PREFERENCES_FILE + ":" + mpId;
    }

    private static SharedPreferences getMParticleSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Used to take any values set in the parameter UserConfig, and apply them to this UserConfig
     *
     * If we have a temporary UserConfig object, and the user sets a number of fields on it, we can
     * use this method to apply those fields to this new UserConfig, by passing the temporary UserConfig
     * object here.
     */
    void merge(UserStorage userStorage) {
        if (userStorage.hasDeletedUserAttributes()) {
            setDeletedUserAttributes(userStorage.getDeletedUserAttributes());
        }
        if (userStorage.hasCurrentSessionCounter()) {
            setCurrentSessionCounter(userStorage.getCurrentSessionCounter());
        }
        if (userStorage.hasBreadcrumbLimit()) {
            setBreadcrumbLimit(userStorage.getBreadcrumbLimit());
        }
        if (userStorage.hasLastUserDate()) {
            setLastUseDate(userStorage.getLastUseDate());
        }
        if (userStorage.hasPreviousSessionForegound()) {
            setPreviousSessionForeground(userStorage.getPreviousSessionForegound());
        }
        if (userStorage.hasPreviousSessionId()) {
            setPreviousSessionId(userStorage.getPreviousSessionId());
        }
        if (userStorage.hasPreviousSessionStart()) {
            setPreviousSessionStart(userStorage.getPreviousSessionStart(0));
        }
        if (userStorage.hasLtv()) {
            setLtv(userStorage.getLtv());
        }
        if (userStorage.hasTotalRuns()) {
            setTotalRuns(userStorage.getTotalRuns(0));
        }
        if (userStorage.hasCookies()) {
            setCookies(userStorage.getCookies());
        }
        if (userStorage.hasLaunchesSinceUpgrade()) {
            setLaunchesSinceUpgrade(userStorage.getLaunchesSinceUpgrade());
        }
        if (userStorage.hasUserIdentities()) {
            setUserIdentities(userStorage.getUserIdentities());
        }
        if (userStorage.hasConsent()) {
            setSerializedConsentState(userStorage.getSerializedConsentState());
        }
    }

    /**
     * Migrate SharedPreferences from old interface, in which all the values in UserStorage were
     * kept application-wide, to the current interface, which stores the values by MPID. The migration
     * process will associate all current values covered by UserStorage to the current MPID, which should
     * be passed into the parameter "currentMpId".
     *
     **/

    private static class SharedPreferencesMigrator {
        private static final String NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT = "mp::needs_to_migrate_to_mpid_dependent";
        private SharedPreferences messageManagerSharedPreferences;
        private SharedPreferences configManagerSharedPreferences;
        private String apiKey;

        /**
         * DO NOT CHANGE THESE VALUES! You don't know when some device is going to update a version
         * and need to migrate from the previous (db version < 7) SharedPreferences schema to the current
         * one. If we change these names, the migration will not work, and we will lose some data.
         */
        private interface LegacySharedPreferencesKeys {
            String SESSION_COUNTER = "mp::breadcrumbs::sessioncount";
            String DELETED_USER_ATTRS = "mp::deleted_user_attrs::";
            String BREADCRUMB_LIMIT = "mp::breadcrumbs::limit";
            String LAST_USE = "mp::lastusedate";
            String PREVIOUS_SESSION_FOREGROUND = "mp::time_in_fg";
            String PREVIOUS_SESSION_ID = "mp::session::previous_id";
            String PREVIOUS_SESSION_START = "mp::session::previous_start";
            String LTV = "mp::ltv";
            String TOTAL_RUNS = "mp::totalruns";
            String COOKIES = "mp::cookies";
            String TOTAL_SINCE_UPGRADE = "mp::launch_since_upgrade";
            String USER_IDENTITIES = "mp::user_ids::";
        }

        SharedPreferencesMigrator(Context context) {
            messageManagerSharedPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            configManagerSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
            this.apiKey = new ConfigManager(context).getApiKey();
        }

        void migrate(UserStorage userStorage) {
            try {
                userStorage.setDeletedUserAttributes(getDeletedUserAttributes());
                userStorage.setPreviousSessionId(getPreviousSessionId());
                String ltv = getLtv();
                if (ltv != null) {
                    userStorage.setLtv(ltv);
                }
                long lastUseDate = getLastUseDate();
                if (lastUseDate != 0) {
                    userStorage.setLastUseDate(getLastUseDate());
                }
                int currentSessionCounter = getCurrentSessionCounter();
                if (currentSessionCounter != 0) {
                    userStorage.setCurrentSessionCounter(getCurrentSessionCounter());
                }
                int breadcrumbLimit = getBreadcrumbLimit();
                if (breadcrumbLimit != 0) {
                    userStorage.setBreadcrumbLimit(breadcrumbLimit);
                }
                long previousTimeInForeground = getPreviousTimeInForeground();
                if (previousTimeInForeground != 0) {
                    userStorage.setPreviousSessionForeground(previousTimeInForeground);
                }
                long previousSessionStart = getPreviousSessionStart();
                if (previousSessionStart != 0) {
                    userStorage.setPreviousSessionStart(previousSessionStart);
                }
                int totalRuns = getTotalRuns();
                if (totalRuns != 0) {
                    userStorage.setTotalRuns(totalRuns);
                }

                //migrate both cookies and device application stamp
                String cookies = getCookies();
                String das = null;
                if (cookies != null) {
                    try {
                        JSONObject jsonCookies = new JSONObject(cookies);
                        String dasParseString = jsonCookies.getJSONObject("uid").getString("c");
                        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(dasParseString);
                        das = sanitizer.getValue("g");
                    }catch (Exception e) {

                    }
                    userStorage.setCookies(cookies);
                }
                if (MPUtility.isEmpty(das)) {
                    das = UUID.randomUUID().toString();
                }
                configManagerSharedPreferences
                        .edit()
                        .putString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, das)
                        .apply();
                int launchesSinceUpgrade = getLaunchesSinceUpgrade();
                if (launchesSinceUpgrade != 0) {
                    userStorage.setLaunchesSinceUpgrade(launchesSinceUpgrade);
                }
                String userIdentities = getUserIdentites();
                if (userIdentities != null) {
                    userStorage.setUserIdentities(userIdentities);
                }
            }
            catch (Exception ex) {
                //do nothing
            }
        }

        /**
         * Check if we have need to migrate from the old SharedPreferences schema. We will only need
         * to trigger a migration, if the flag is explicitly set to true.
         * @param context
         * @return
         */
        static boolean needsToMigrate(Context context) {
            return getMParticleSharedPrefs(context).getBoolean(NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT, false);
        }

        static void setNeedsToMigrate(Context context, boolean needsToMigrate) {
            getMParticleSharedPrefs(context).edit().putBoolean(NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT, needsToMigrate).apply();
        }

        int getCurrentSessionCounter() {
            return messageManagerSharedPreferences.getInt(LegacySharedPreferencesKeys.SESSION_COUNTER, 0);
        }

        String getDeletedUserAttributes() {
            return messageManagerSharedPreferences.getString(LegacySharedPreferencesKeys.DELETED_USER_ATTRS + apiKey, null);
        }

        int getBreadcrumbLimit() {
            return configManagerSharedPreferences.getInt(LegacySharedPreferencesKeys.BREADCRUMB_LIMIT, 0);
        }

        long getLastUseDate() {
            return messageManagerSharedPreferences.getLong(LegacySharedPreferencesKeys.LAST_USE, 0);
        }

        long getPreviousTimeInForeground() {
            return messageManagerSharedPreferences.getLong(LegacySharedPreferencesKeys.PREVIOUS_SESSION_FOREGROUND, 0);
        }

        String getPreviousSessionId() {
            return messageManagerSharedPreferences.getString(LegacySharedPreferencesKeys.PREVIOUS_SESSION_ID, null);
        }

        long getPreviousSessionStart() {
            return messageManagerSharedPreferences.getLong(LegacySharedPreferencesKeys.PREVIOUS_SESSION_START, 0);
        }

        String getLtv() {
            return messageManagerSharedPreferences.getString(LegacySharedPreferencesKeys.LTV, null);
        }

        int getTotalRuns() {
            return messageManagerSharedPreferences.getInt(LegacySharedPreferencesKeys.TOTAL_RUNS, 0);
        }

        String getCookies() {
            return configManagerSharedPreferences.getString(LegacySharedPreferencesKeys.COOKIES, null);
        }

        int getLaunchesSinceUpgrade() {
            return messageManagerSharedPreferences.getInt(LegacySharedPreferencesKeys.TOTAL_SINCE_UPGRADE, 0);
        }

        String getUserIdentites() {
            return configManagerSharedPreferences.getString(LegacySharedPreferencesKeys.USER_IDENTITIES + apiKey, null);
        }
    }

}
