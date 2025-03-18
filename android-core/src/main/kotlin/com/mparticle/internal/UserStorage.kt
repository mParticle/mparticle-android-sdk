package com.mparticle.internal

import android.content.Context
import android.content.SharedPreferences
import android.net.UrlQuerySanitizer
import android.os.Build
import com.mparticle.internal.database.UploadSettings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.TreeSet
import java.util.UUID

class UserStorage private constructor(private val mContext: Context, val mpid: Long) {
    private val mPreferences: SharedPreferences

    var messageManagerSharedPreferences: SharedPreferences

    fun deleteUserConfig(context: Context, mpId: Long): Boolean {
        if (Build.VERSION.SDK_INT >= 24) {
            context.deleteSharedPreferences(getFileName(mpId))
        } else {
            context.getSharedPreferences(getFileName(mpId), Context.MODE_PRIVATE).edit().clear()
                .apply()
        }
        return removeMpId(context, mpId)
    }

    init {
        this.mPreferences = getPreferenceFile(mpid)
        if (SharedPreferencesMigrator.needsToMigrate(mContext)) {
            SharedPreferencesMigrator.setNeedsToMigrate(mContext, false)
            SharedPreferencesMigrator(mContext).migrate(this)
        }
        this.messageManagerSharedPreferences =
            mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        setDefaultSeenTime()
    }

    var currentSessionCounter: Int
        get() = getCurrentSessionCounter(0)
        private set(sessionCounter) {
            mPreferences.edit().putInt(SESSION_COUNTER, sessionCounter).apply()
        }

    fun getCurrentSessionCounter(defaultValue: Int): Int {
        return mPreferences.getInt(SESSION_COUNTER, defaultValue)
    }

    private fun hasCurrentSessionCounter(): Boolean {
        return mPreferences.contains(SESSION_COUNTER)
    }

    fun incrementSessionCounter() {
        var nextCount = currentSessionCounter + 1
        if (nextCount >= (Int.MAX_VALUE / 100)) {
            nextCount = 0
        }
        mPreferences.edit().putInt(SESSION_COUNTER, nextCount).apply()
    }


    var deletedUserAttributes: String?
        get() = mPreferences.getString(DELETED_USER_ATTRS, null)
        set(deletedUserAttributes) {
            mPreferences.edit().putString(DELETED_USER_ATTRS, deletedUserAttributes).apply()
        }

    fun deleteDeletedUserAttributes() {
        mPreferences.edit().putString(DELETED_USER_ATTRS, null).apply()
    }

    private fun hasDeletedUserAttributes(): Boolean {
        return mPreferences.contains(DELETED_USER_ATTRS)
    }

    var breadcrumbLimit: Int
        get() {
            if (mPreferences != null) {
                return mPreferences.getInt(BREADCRUMB_LIMIT, DEFAULT_BREADCRUMB_LIMIT)
            }
            return DEFAULT_BREADCRUMB_LIMIT
        }
        set(newLimit) {
            mPreferences.edit().putInt(BREADCRUMB_LIMIT, newLimit).apply()
        }

    private fun hasBreadcrumbLimit(): Boolean {
        return mPreferences.contains(BREADCRUMB_LIMIT)
    }

    var lastUseDate: Long
        get() = getLastUseDate(0)
        set(lastUseDate) {
            mPreferences.edit().putLong(LAST_USE, lastUseDate).apply()
        }

    fun getLastUseDate(defaultValue: Long): Long {
        return mPreferences.getLong(LAST_USE, defaultValue)
    }

    private fun hasLastUserDate(): Boolean {
        return mPreferences.contains(LAST_USE)
    }

    val previousSessionForegound: Long
        get() = getPreviousSessionForegound(-1)

    fun getPreviousSessionForegound(defaultValue: Long): Long {
        return mPreferences.getLong(PREVIOUS_SESSION_FOREGROUND, defaultValue)
    }

    fun clearPreviousTimeInForeground() {
        mPreferences.edit().putLong(PREVIOUS_SESSION_FOREGROUND, -1).apply()
    }

    fun setPreviousSessionForeground(previousTimeInForeground: Long) {
        mPreferences.edit().putLong(PREVIOUS_SESSION_FOREGROUND, previousTimeInForeground).apply()
    }

    private fun hasPreviousSessionForegound(): Boolean {
        return mPreferences.contains(PREVIOUS_SESSION_FOREGROUND)
    }

    var previousSessionId: String?
        get() = getPreviousSessionId("")
        set(previousSessionId) {
            mPreferences.edit().putString(PREVIOUS_SESSION_ID, previousSessionId).apply()
        }

    fun getPreviousSessionId(defaultValue: String?): String? {
        return mPreferences.getString(PREVIOUS_SESSION_ID, defaultValue)
    }

    private fun hasPreviousSessionId(): Boolean {
        return mPreferences.contains(PREVIOUS_SESSION_ID)
    }

    fun getPreviousSessionStart(defaultValue: Long): Long {
        return mPreferences.getLong(PREVIOUS_SESSION_START, defaultValue)
    }

    fun setPreviousSessionStart(previousSessionStart: Long) {
        mPreferences.edit().putLong(PREVIOUS_SESSION_START, previousSessionStart).apply()
    }

    private fun hasPreviousSessionStart(): Boolean {
        return mPreferences.contains(PREVIOUS_SESSION_START)
    }

    var ltv: String?
        get() = mPreferences.getString(LTV, "0")
        set(ltv) {
            mPreferences.edit().putString(LTV, ltv).apply()
        }

    private fun hasLtv(): Boolean {
        return mPreferences.contains(LTV)
    }

    fun getTotalRuns(defaultValue: Int): Int {
        return mPreferences.getInt(TOTAL_RUNS, defaultValue)
    }

    fun setTotalRuns(totalRuns: Int) {
        mPreferences.edit().putInt(TOTAL_RUNS, totalRuns).apply()
    }

    private fun hasTotalRuns(): Boolean {
        return mPreferences.contains(TOTAL_RUNS)
    }

    var cookies: String?
        get() = mPreferences.getString(COOKIES, "")
        set(cookies) {
            mPreferences.edit().putString(COOKIES, cookies).apply()
        }

    private fun hasCookies(): Boolean {
        return mPreferences.contains(COOKIES)
    }

    var launchesSinceUpgrade: Int
        get() = mPreferences.getInt(TOTAL_SINCE_UPGRADE, 0)
        set(launchesSinceUpgrade) {
            mPreferences.edit().putInt(TOTAL_SINCE_UPGRADE, launchesSinceUpgrade).apply()
        }

    private fun hasLaunchesSinceUpgrade(): Boolean {
        return mPreferences.contains(TOTAL_SINCE_UPGRADE)
    }

    var userIdentities: String?
        get() = mPreferences.getString(USER_IDENTITIES, "")
        set(userIdentities) {
            mPreferences.edit().putString(USER_IDENTITIES, userIdentities).apply()
        }

    var serializedConsentState: String?
        get() = mPreferences.getString(CONSENT_STATE, null)
        set(consentState) {
            mPreferences.edit().putString(CONSENT_STATE, consentState).apply()
        }

    private fun hasConsent(): Boolean {
        return mPreferences.contains(CONSENT_STATE)
    }

    val isLoggedIn: Boolean
        get() = mPreferences.getBoolean(KNOWN_USER, false)

    var firstSeenTime: Long?
        get() {
            if (!mPreferences.contains(FIRST_SEEN_TIME)) {
                mPreferences.edit().putLong(
                    FIRST_SEEN_TIME, messageManagerSharedPreferences.getLong(
                        Constants.PrefKeys.INSTALL_TIME, defaultSeenTime
                    )
                ).apply()
            }
            return mPreferences.getLong(FIRST_SEEN_TIME, defaultSeenTime)
        }
        set(time) {
            if (!mPreferences.contains(FIRST_SEEN_TIME)) {
                time?.let { mPreferences.edit().putLong(FIRST_SEEN_TIME, it).apply() }
            }
        }

    var lastSeenTime: Long?
        get() {
            if (!mPreferences.contains(LAST_SEEN_TIME)) {
                mPreferences.edit().putLong(LAST_SEEN_TIME, defaultSeenTime).apply()
            }
            return mPreferences.getLong(LAST_SEEN_TIME, defaultSeenTime)
        }
        set(time) {
            time?.let { mPreferences.edit().putLong(LAST_SEEN_TIME, it).apply() }
        }

    //Set a default "lastSeenTime" for migration to SDK versions with MParticleUser.getLastSeenTime(),
    //where some users will not have a value for the field.
    private fun setDefaultSeenTime() {
        val preferences = getMParticleSharedPrefs(mContext)
        if (!preferences.contains(DEFAULT_SEEN_TIME)) {
            preferences.edit().putLong(DEFAULT_SEEN_TIME, System.currentTimeMillis())
        }
    }

    private val defaultSeenTime: Long
        get() = getMParticleSharedPrefs(mContext).getLong(
            DEFAULT_SEEN_TIME,
            System.currentTimeMillis()
        )

    fun getLastUploadSettings(): UploadSettings? {
        val lastUploadSettingsJson = mPreferences.getString(LAST_UPLOAD_SETTINGS, null)
        if (lastUploadSettingsJson != null) {
            return UploadSettings.withJson(lastUploadSettingsJson)
        }
        return null
    }

    fun setLastUploadSettings(uploadSettings: UploadSettings) {
        val lastUploadSettingsJson = uploadSettings.toJson()
        if (lastUploadSettingsJson != null) {
            mPreferences.edit().putString(LAST_UPLOAD_SETTINGS, lastUploadSettingsJson).apply()
        }
    }

    fun setLoggedInUser(knownUser: Boolean) {
        mPreferences.edit().putBoolean(KNOWN_USER, knownUser).apply()
    }

    private fun hasUserIdentities(): Boolean {
        return mPreferences.contains(USER_IDENTITIES)
    }

    private fun getPreferenceFile(mpId: Long): SharedPreferences {
        val mpIds = getMpIdSet(mContext)
        mpIds.add(mpId)
        setMpIds(mpIds)
        return mContext.getSharedPreferences(getFileName(mpId), Context.MODE_PRIVATE)
    }

    private fun setMpIds(mpIds: Set<Long>) {
        setMpIds(mContext, mpIds)
    }

    /**
     * Used to take any values set in the parameter UserConfig, and apply them to this UserConfig
     *
     * If we have a temporary UserConfig object, and the user sets a number of fields on it, we can
     * use this method to apply those fields to this new UserConfig, by passing the temporary UserConfig
     * object here.
     */
    fun merge(userStorage: UserStorage) {
        if (userStorage.hasDeletedUserAttributes()) {
            deletedUserAttributes = userStorage.deletedUserAttributes
        }
        if (userStorage.hasCurrentSessionCounter()) {
            currentSessionCounter = userStorage.currentSessionCounter
        }
        if (userStorage.hasBreadcrumbLimit()) {
            breadcrumbLimit = userStorage.breadcrumbLimit
        }
        if (userStorage.hasLastUserDate()) {
            lastUseDate = userStorage.lastUseDate
        }
        if (userStorage.hasPreviousSessionForegound()) {
            setPreviousSessionForeground(userStorage.previousSessionForegound)
        }
        if (userStorage.hasPreviousSessionId()) {
            previousSessionId = userStorage.previousSessionId
        }
        if (userStorage.hasPreviousSessionStart()) {
            setPreviousSessionStart(userStorage.getPreviousSessionStart(0))
        }
        if (userStorage.hasLtv()) {
            ltv = userStorage.ltv
        }
        if (userStorage.hasTotalRuns()) {
            setTotalRuns(userStorage.getTotalRuns(0))
        }
        if (userStorage.hasCookies()) {
            cookies = userStorage.cookies
        }
        if (userStorage.hasLaunchesSinceUpgrade()) {
            launchesSinceUpgrade = userStorage.launchesSinceUpgrade
        }
        if (userStorage.hasUserIdentities()) {
            userIdentities = userStorage.userIdentities
        }
        if (userStorage.hasConsent()) {
            serializedConsentState = userStorage.serializedConsentState
        }
    }

    /**
     * Migrate SharedPreferences from old interface, in which all the values in UserStorage were
     * kept application-wide, to the current interface, which stores the values by MPID. The migration
     * process will associate all current values covered by UserStorage to the current MPID, which should
     * be passed into the parameter "currentMpId".
     */
    private class SharedPreferencesMigrator(context: Context) {
        private val messageManagerSharedPreferences: SharedPreferences =
            context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        private val configManagerSharedPreferences: SharedPreferences =
            context.getSharedPreferences(ConfigManager.PREFERENCES_FILE, Context.MODE_PRIVATE)
        private val apiKey: String = ConfigManager(context).apiKey

        /**
         * DO NOT CHANGE THESE VALUES! You don't know when some device is going to update a version
         * and need to migrate from the previous (db version < 7) SharedPreferences schema to the current
         * one. If we change these names, the migration will not work, and we will lose some data.
         */
        private interface LegacySharedPreferencesKeys {
            companion object {
                const val SESSION_COUNTER: String = "mp::breadcrumbs::sessioncount"
                const val DELETED_USER_ATTRS: String = "mp::deleted_user_attrs::"
                const val BREADCRUMB_LIMIT: String = "mp::breadcrumbs::limit"
                const val LAST_USE: String = "mp::lastusedate"
                const val PREVIOUS_SESSION_FOREGROUND: String = "mp::time_in_fg"
                const val PREVIOUS_SESSION_ID: String = "mp::session::previous_id"
                const val PREVIOUS_SESSION_START: String = "mp::session::previous_start"
                const val LTV: String = "mp::ltv"
                const val TOTAL_RUNS: String = "mp::totalruns"
                const val COOKIES: String = "mp::cookies"
                const val TOTAL_SINCE_UPGRADE: String = "mp::launch_since_upgrade"
                const val USER_IDENTITIES: String = "mp::user_ids::"
            }
        }

        fun migrate(userStorage: UserStorage) {
            try {
                userStorage.deletedUserAttributes = getDeletedUserAttributes
                userStorage.previousSessionId = previousSessionId
                val ltv: String? = ltv
                if (ltv != null) {
                    userStorage.ltv = ltv
                }
                val lastUseDate: Long = lastUseDate
                if (lastUseDate != 0L) {
                    userStorage.lastUseDate = lastUseDate
                }
                val currentSessionCounter: Int = currentSessionCounter
                if (currentSessionCounter != 0) {
                    userStorage.currentSessionCounter = currentSessionCounter
                }
                val breadcrumbLimit: Int = breadcrumbLimit
                if (breadcrumbLimit != 0) {
                    userStorage.breadcrumbLimit = breadcrumbLimit
                }
                val previousTimeInForeground: Long = previousTimeInForeground
                if (previousTimeInForeground != 0L) {
                    userStorage.setPreviousSessionForeground(previousTimeInForeground)
                }
                val previousSessionStart: Long = previousSessionStart
                if (previousSessionStart != 0L) {
                    userStorage.setPreviousSessionStart(previousSessionStart)
                }
                val totalRuns: Int = totalRuns
                if (totalRuns != 0) {
                    userStorage.setTotalRuns(totalRuns)
                }

                //migrate both cookies and device application stamp
                val cookies: String? = cookies
                var das: String? = null
                if (cookies != null) {
                    try {
                        val jsonCookies = JSONObject(cookies)
                        val dasParseString = jsonCookies.getJSONObject("uid").getString("c")
                        val sanitizer = UrlQuerySanitizer(dasParseString)
                        das = sanitizer.getValue("g")
                    } catch (e: Exception) {
                    }
                    userStorage.cookies = cookies
                }
                if (MPUtility.isEmpty(das)) {
                    das = UUID.randomUUID().toString()
                }
                configManagerSharedPreferences
                    .edit()
                    .putString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, das)
                    .apply()
                val launchesSinceUpgrade: Int = launchesSinceUpgrade
                if (launchesSinceUpgrade != 0) {
                    userStorage.launchesSinceUpgrade = launchesSinceUpgrade
                }
                val userIdentities: String = userIdentites.toString()
                if (userIdentities != null) {
                    userStorage.userIdentities = userIdentities
                }
            } catch (ex: Exception) {
                //do nothing
            }
        }

        val currentSessionCounter: Int
            get() = messageManagerSharedPreferences.getInt(
                LegacySharedPreferencesKeys.SESSION_COUNTER,
                0
            )

        val getDeletedUserAttributes: String?
            get() = messageManagerSharedPreferences.getString(
                LegacySharedPreferencesKeys.DELETED_USER_ATTRS + apiKey,
                null
            )

        val breadcrumbLimit: Int
            get() = configManagerSharedPreferences.getInt(
                LegacySharedPreferencesKeys.BREADCRUMB_LIMIT,
                0
            )

        val lastUseDate: Long
            get() = messageManagerSharedPreferences.getLong(LegacySharedPreferencesKeys.LAST_USE, 0)

        val previousTimeInForeground: Long
            get() = messageManagerSharedPreferences.getLong(
                LegacySharedPreferencesKeys.PREVIOUS_SESSION_FOREGROUND,
                0
            )

        val previousSessionId: String?
            get() = messageManagerSharedPreferences.getString(
                LegacySharedPreferencesKeys.PREVIOUS_SESSION_ID,
                null
            )

        val previousSessionStart: Long
            get() = messageManagerSharedPreferences.getLong(
                LegacySharedPreferencesKeys.PREVIOUS_SESSION_START,
                0
            )

        val ltv: String?
            get() = messageManagerSharedPreferences.getString(LegacySharedPreferencesKeys.LTV, null)

        val totalRuns: Int
            get() = messageManagerSharedPreferences.getInt(
                LegacySharedPreferencesKeys.TOTAL_RUNS,
                0
            )

        val cookies: String?
            get() = configManagerSharedPreferences.getString(
                LegacySharedPreferencesKeys.COOKIES,
                null
            )

        val launchesSinceUpgrade: Int
            get() = messageManagerSharedPreferences.getInt(
                LegacySharedPreferencesKeys.TOTAL_SINCE_UPGRADE,
                0
            )

        val userIdentites: String?
            get() = configManagerSharedPreferences.getString(
                LegacySharedPreferencesKeys.USER_IDENTITIES + apiKey,
                null
            )

        companion object {
            private const val NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT =
                "mp::needs_to_migrate_to_mpid_dependent"

            /**
             * Check if we have need to migrate from the old SharedPreferences schema. We will only need
             * to trigger a migration, if the flag is explicitly set to true.
             *
             * @param context
             * @return
             */
            fun needsToMigrate(context: Context): Boolean {
                return getMParticleSharedPrefs(context).getBoolean(
                    NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT, false
                )
            }

            fun setNeedsToMigrate(context: Context, needsToMigrate: Boolean) {
                getMParticleSharedPrefs(context).edit().putBoolean(
                    NEEDS_TO_MIGRATE_TO_MPID_DEPENDENT, needsToMigrate
                ).apply()
            }
        }
    }

    companion object {
        private const val USER_CONFIG_COLLECTION = "mp::user_config_collection"

        private const val SESSION_COUNTER = "mp::breadcrumbs::sessioncount"
        private const val DELETED_USER_ATTRS = "mp::deleted_user_attrs::"
        private const val BREADCRUMB_LIMIT = "mp::breadcrumbs::limit"
        private const val LAST_USE = "mp::lastusedate"
        private const val PREVIOUS_SESSION_FOREGROUND = "mp::time_in_fg"
        private const val PREVIOUS_SESSION_ID = "mp::session::previous_id"
        private const val PREVIOUS_SESSION_START = "mp::session::previous_start"
        private const val LTV = "mp::ltv"
        private const val TOTAL_RUNS = "mp::totalruns"
        private const val COOKIES = "mp::cookies"
        private const val TOTAL_SINCE_UPGRADE = "mp::launch_since_upgrade"
        private const val USER_IDENTITIES = "mp::user_ids::"
        private const val CONSENT_STATE = "mp::consent_state::"
        private const val KNOWN_USER = "mp::known_user"
        private const val FIRST_SEEN_TIME = "mp::first_seen"
        private const val LAST_SEEN_TIME = "mp::last_seen"
        private const val DEFAULT_SEEN_TIME = "mp::default_seen_time"
        private const val LAST_UPLOAD_SETTINGS = "mp::last_upload_settings"

        const val DEFAULT_BREADCRUMB_LIMIT: Int = 50

        fun getAllUsers(context: Context): List<UserStorage> {
            val userMpIds: Set<Long> = getMpIdSet(context)
            val userStorages: MutableList<UserStorage> = ArrayList()
            for (mdId in userMpIds) {
                userStorages.add(UserStorage(context, mdId))
            }
            return userStorages
        }

        @JvmStatic
        fun create(context: Context, mpid: Long): UserStorage {
            return UserStorage(context, mpid)
        }

        @JvmStatic
        fun setNeedsToMigrate(context: Context, needsToMigrate: Boolean) {
            SharedPreferencesMigrator.setNeedsToMigrate(context, needsToMigrate)
        }

        private fun removeMpId(context: Context, mpid: Long): Boolean {
            val mpids = getMpIdSet(context)
            val removed = mpids.remove(mpid)
            setMpIds(context, mpids)
            return removed
        }

        @JvmStatic
        fun getMpIdSet(context: Context): MutableSet<Long> {
            var userConfigs = JSONArray()
            try {
                userConfigs = JSONArray(
                    getMParticleSharedPrefs(context).getString(
                        USER_CONFIG_COLLECTION, JSONArray().toString()
                    )
                )
            } catch (ignore: JSONException) {
            }
            val mpIds: MutableSet<Long> = TreeSet()
            for (i in 0 until userConfigs.length()) {
                try {
                    mpIds.add(userConfigs.getLong(i))
                } catch (ignore: JSONException) {
                }
            }
            return mpIds
        }

        private fun setMpIds(context: Context, mpIds: Set<Long>) {
            val jsonArray = JSONArray()
            for (mpId in mpIds) {
                jsonArray.put(mpId)
            }
            getMParticleSharedPrefs(context).edit()
                .putString(USER_CONFIG_COLLECTION, jsonArray.toString()).apply()
        }

        private fun getFileName(mpId: Long): String {
            return ConfigManager.PREFERENCES_FILE + ":" + mpId
        }

        private fun getMParticleSharedPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                ConfigManager.PREFERENCES_FILE,
                Context.MODE_PRIVATE
            )
        }
    }
}
