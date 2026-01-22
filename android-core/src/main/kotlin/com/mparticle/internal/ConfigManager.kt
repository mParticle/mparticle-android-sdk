package com.mparticle.internal

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.WorkerThread
import com.mparticle.Configuration
import com.mparticle.ExceptionHandler
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.consent.ConsentState
import com.mparticle.identity.IdentityApi.MpIdChangeListener
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.internal.MPUtility.AdIdInfo
import com.mparticle.internal.MPUtility.getAdIdInfo
import com.mparticle.internal.MPUtility.isEmpty
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.internal.SideloadedKitsUtils.combineConfig
import com.mparticle.internal.UserStorage.Companion.create
import com.mparticle.internal.UserStorage.Companion.getMpIdSet
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.networking.NetworkOptions
import com.mparticle.networking.NetworkOptionsManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

open class ConfigManager {
    private var mContext: Context? = null
    private var mIgnoreDataplanOptionsFromConfig: Boolean = false
    var dataplanOptions: DataplanOptions? = null
        private set

    var isDirectUrlRoutingEnabled: Boolean = false
        private set
    private var mUserStorage: UserStorage? = null
    private var mLogUnhandledExceptions: String = VALUE_APP_DEFINED

    private var mSendOoEvents: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var providerPersistence: JSONObject? = null
        private set
    var currentRampValue: Int = -1
        private set
    private var mUserBucket: Int = -1

    private var mSessionTimeoutInterval: Int = -1
    private var mUploadInterval: Int = -1
    var influenceOpenTimeoutMillis: Long = (3600 * 1000).toLong()
        private set
    var triggerMessageMatches: JSONArray? = null
        private set
    var triggerMessageHashes: JSONArray? = null
        private set
    private var mExHandler: ExceptionHandler? = null
    private var mCurrentCookies: JSONObject? = null
    var dataplanId: String? = null
        private set
    var dataplanVersion: Int? = null
        private set
    private var mMaxConfigAge: Int? = null
    private val configUpdatedListeners: MutableList<ConfigLoadedListener> = ArrayList()
    private var sideloadedKits: List<SideloadedKit> = ArrayList()

    private constructor() : super()

    constructor(context: Context) {
        mContext = context
        mContext?.let { sPreferences = getPreferences(it) }
    }

    constructor(options: MParticleOptions) : this(
        options.context,
        options.environment,
        options.apiKey,
        options.apiSecret,
        options.dataplanOptions,
        options.dataplanId,
        options.dataplanVersion,
        options.configMaxAge,
        options.getConfigurationsForTarget<ConfigManager?>(
            ConfigManager::class.java
        ),
        options.sideloadedKits
    )

    constructor(
        context: Context,
        environment: MParticle.Environment?,
        apiKey: String?,
        apiSecret: String?,
        dataplanOptions: DataplanOptions?,
        dataplanId: String?,
        dataplanVersion: Int?,
        configMaxAge: Int?,
        configurations: List<Configuration<ConfigManager?>>?,
        sideloadedKits: List<SideloadedKit>?
    ) {
        mContext = context.applicationContext
        mContext?.let { sPreferences = getPreferences(it) }
        if (apiKey != null || apiSecret != null) {
            setCredentials(apiKey, apiSecret)
        }
        if (environment != null) {
            setEnvironment(environment)
        }
        mContext?.let { mUserStorage = create(it, mpid) }
        // if we are initialized with a DataplanOptions instance, then we will ignore values from remote config
        mIgnoreDataplanOptionsFromConfig = dataplanOptions != null
        this.dataplanOptions = dataplanOptions
        this.dataplanVersion = dataplanVersion
        this.dataplanId = dataplanId
        mMaxConfigAge = configMaxAge
        if (sideloadedKits != null) {
            this.sideloadedKits = sideloadedKits
        } else {
            this.sideloadedKits = ArrayList()
        }
        configurations?.let {
            for (configuration in configurations) {
                configuration.apply(this)
            }
        }
    }

    fun onMParticleStarted() {
        checkConfigStaleness()
        migrateConfigIfNeeded()
        restoreCoreConfig()
    }

    private fun restoreCoreConfig() {
        val oldConfig: String? = config
        if (!isEmpty(oldConfig)) {
            try {
                val oldConfigJson: JSONObject = JSONObject(oldConfig)
                reloadCoreConfig(oldConfigJson)
            } catch (jse: Exception) {
            }
        }
    }

    @get:WorkerThread
    open val latestKitConfiguration: JSONArray?
        /**
         * This called on startup. The only thing that's completely necessary is that we fire up kits.
         */
        get() {
            val oldConfig: String? = kitConfigPreferences?.getString(KIT_CONFIG_KEY, null)
            if (!isEmpty(oldConfig)) {
                try {
                    return JSONArray(oldConfig)
                } catch (jse: Exception) {
                }
            }
            return null
        }

    open val userStorage: UserStorage?
        get() = getUserStorage(mpid)

    fun getUserStorage(mpId: Long): UserStorage? {
        if (mUserStorage == null || mUserStorage?.mpid != mpId) {
            mUserStorage = mContext?.let { create(it, mpId) }
        }
        return mUserStorage
    }

    fun deleteUserStorage(context: Context, mpid: Long) {
        if (mUserStorage != null) {
            mUserStorage?.deleteUserConfig(context, mpid)
        }
    }

    fun deleteUserStorage(mpId: Long) {
        mContext?.let { deleteUserStorage(it, mpId) }
    }

    fun migrateConfigIfNeeded() {
        if (sPreferences?.getBoolean(MIGRATED_TO_KIT_SHARED_PREFS, false) == false) {
            sPreferences?.edit()?.putBoolean(MIGRATED_TO_KIT_SHARED_PREFS, true)?.apply()
            val configString: String? = sPreferences?.getString(CONFIG_JSON, null)
            if (!isEmpty(configString)) {
                try {
                    // save ourselves some time and only parse the JSONObject if might contain the embedded kits key
                    if (configString?.contains("\"" + KEY_EMBEDDED_KITS + "\":") == true) {
                        Logger.info("Migrating kit configuration")
                        saveConfigJson(JSONObject(configString), etag, ifModified, configTimestamp)
                    }
                } catch (jse: JSONException) {
                }
            }
        }
    }

    /**
     * detrmine if the stored config age is greater than mMaxConfigAge and clear it from storage if it is
     */
    fun checkConfigStaleness() {
        var storageDate: Long? = configTimestamp
        if (storageDate == null) {
            // migration step: if the current config does not have a timestamp, set one to the current time
            configTimestamp = System.currentTimeMillis()
            storageDate = configTimestamp
        }
        mMaxConfigAge?.let {
            if (it < 0) {
                return
            }
        } ?: return
        if (mMaxConfigAge == 0) {
            clearConfig()
        } else {
            val currentTime = System.currentTimeMillis()
            val storageDateMillis = storageDate ?: 0L
            if (currentTime >= storageDateMillis + TimeUnit.SECONDS.toMillis(mMaxConfigAge?.toLong() ?: 0L)) {
                clearConfig()
            }
        }
    }

    val config: String?
        get() = sPreferences?.getString(CONFIG_JSON, "")

    var configTimestamp: Long?
        get() {
            if (sPreferences?.contains(CONFIG_JSON_TIMESTAMP) == true) {
                return sPreferences?.getLong(CONFIG_JSON_TIMESTAMP, 0)
            } else {
                return null
            }
        }
        set(timestamp) {
            sPreferences?.edit()
                ?.putLong(CONFIG_JSON_TIMESTAMP, timestamp ?: 0L)
                ?.apply()
        }

    @JvmOverloads
    @Throws(JSONException::class)
    fun saveConfigJson(combinedConfig: JSONObject?, etag: String? = null, lastModified: String? = null, timestamp: Long? = null) {
        if (combinedConfig != null) {
            val kitConfig: JSONArray? = if (combinedConfig.has(KEY_EMBEDDED_KITS)) combinedConfig.remove(KEY_EMBEDDED_KITS) as JSONArray else null
            saveConfigJson(combinedConfig, kitConfig, etag, lastModified, timestamp)
        } else {
            saveConfigJson(combinedConfig, null, etag, lastModified, timestamp)
        }
    }

    @Throws(JSONException::class)
    fun saveConfigJson(coreConfig: JSONObject?, kitConfig: JSONArray?, etag: String?, lastModified: String?, timestamp: Long?) {
        if (coreConfig != null) {
            val kitConfigString: String? = if (kitConfig != null) kitConfig.toString() else null
            Logger.debug("Updating core config to:\n$coreConfig")
            Logger.debug("Updating kit config to:\n$kitConfigString")
            sPreferences?.edit()
                ?.putString(CONFIG_JSON, coreConfig.toString())
                ?.putLong(CONFIG_JSON_TIMESTAMP, if (timestamp != null) timestamp else System.currentTimeMillis())
                ?.putString(Constants.PrefKeys.ETAG, etag)
                ?.putString(Constants.PrefKeys.IF_MODIFIED, lastModified)
                ?.apply()
            kitConfigPreferences
                ?.edit()
                ?.putString(KIT_CONFIG_KEY, combineConfig(kitConfig, sideloadedKits).toString())
                ?.apply()
        } else {
            Logger.debug("clearing current configurations")
            clearConfig()
        }
    }

    fun clearConfig() {
        sPreferences?.edit()
            ?.remove(CONFIG_JSON)
            ?.remove(CONFIG_JSON_TIMESTAMP)
            ?.remove(Constants.PrefKeys.ETAG)
            ?.remove(Constants.PrefKeys.IF_MODIFIED)
            ?.apply()
        kitConfigPreferences
            ?.edit()
            ?.remove(KIT_CONFIG_KEY)
            ?.apply()
    }

    @Synchronized
    @Throws(JSONException::class)
    fun updateConfig(responseJSON: JSONObject?) {
        updateConfig(responseJSON, null, null)
    }

    @Synchronized
    @Throws(JSONException::class)
    fun configUpToDate() {
        try {
            val config: String? = kitConfigPreferences?.getString(KIT_CONFIG_KEY, "")
            if (!config.isNullOrEmpty()) {
                val kitConfig: JSONArray = JSONArray(config)
                val combined: JSONArray = combineConfig(kitConfig, sideloadedKits)
                kitConfigPreferences
                    ?.edit()
                    ?.putString(KIT_CONFIG_KEY, combined.toString())
                    ?.apply()
                onConfigLoaded(ConfigType.KIT, kitConfig !== combined)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    open fun updateConfig(responseJSON: JSONObject?, etag: String?, lastModified: String?) {
        var responseJSON: JSONObject? = responseJSON
        if (responseJSON == null) {
            responseJSON = JSONObject()
        }
        val kitConfig: JSONArray? = if (responseJSON.has(KEY_EMBEDDED_KITS)) responseJSON.remove(KEY_EMBEDDED_KITS) as JSONArray else null
        saveConfigJson(responseJSON, kitConfig, etag, lastModified, System.currentTimeMillis())
        updateCoreConfig(responseJSON, true)
        updateKitConfig(kitConfig)
    }

    @Synchronized
    @Throws(JSONException::class)
    fun reloadCoreConfig(responseJSON: JSONObject) {
        updateCoreConfig(responseJSON, false)
    }

    @Synchronized
    private fun updateKitConfig(kitConfigs: JSONArray?) {
        val instance: MParticle? = MParticle.getInstance()
        instance?.let {
            instance.Internal().kitManager
                .updateKits(kitConfigs)
                .onKitsLoaded(object : OnKitManagerLoaded {
                    override fun onKitManagerLoaded() {
                        onConfigLoaded(ConfigType.KIT, true)
                    }
                })
        }
    }

    @Synchronized
    @Throws(JSONException::class)
    private fun updateCoreConfig(responseJSON: JSONObject, newConfig: Boolean) {
        val editor: SharedPreferences.Editor? = sPreferences?.edit()
        if (responseJSON.has(KEY_UNHANDLED_EXCEPTIONS)) {
            mLogUnhandledExceptions = responseJSON.getString(KEY_UNHANDLED_EXCEPTIONS)
        }

        if (responseJSON.has(KEY_PUSH_MESSAGES) && newConfig) {
            sPushKeys = responseJSON.getJSONArray(KEY_PUSH_MESSAGES)
            editor?.putString(KEY_PUSH_MESSAGES, sPushKeys.toString())
        }

        if (responseJSON.has(KEY_DIRECT_URL_ROUTING)) {
            isDirectUrlRoutingEnabled = responseJSON.optBoolean(KEY_DIRECT_URL_ROUTING)
            editor?.putBoolean(KEY_DIRECT_URL_ROUTING, isDirectUrlRoutingEnabled)
        }

        currentRampValue = responseJSON.optInt(KEY_RAMP, -1)

        if (responseJSON.has(KEY_OPT_OUT)) {
            mSendOoEvents = responseJSON.getBoolean(KEY_OPT_OUT)
        } else {
            mSendOoEvents = false
        }

        if (responseJSON.has(ProviderPersistence.KEY_PERSISTENCE)) {
            providerPersistence = ProviderPersistence(responseJSON, mContext)
        } else {
            providerPersistence = null
        }

        mSessionTimeoutInterval = responseJSON.optInt(KEY_SESSION_TIMEOUT, -1)
        mUploadInterval = responseJSON.optInt(KEY_UPLOAD_INTERVAL, -1)

        triggerMessageMatches = null
        triggerMessageHashes = null
        if (responseJSON.has(KEY_TRIGGER_ITEMS)) {
            try {
                val items: JSONObject = responseJSON.getJSONObject(KEY_TRIGGER_ITEMS)
                if (items.has(KEY_MESSAGE_MATCHES)) {
                    triggerMessageMatches = items.getJSONArray(KEY_MESSAGE_MATCHES)
                }
                if (items.has(KEY_TRIGGER_ITEM_HASHES)) {
                    triggerMessageHashes = items.getJSONArray(KEY_TRIGGER_ITEM_HASHES)
                }
            } catch (jse: JSONException) {
            }
        }

        if (responseJSON.has(KEY_INFLUENCE_OPEN)) {
            influenceOpenTimeoutMillis = responseJSON.getLong(KEY_INFLUENCE_OPEN) * 60 * 1000
        } else {
            influenceOpenTimeoutMillis = (30 * 60 * 1000).toLong()
        }

        if (responseJSON.has(KEY_DEVICE_PERFORMANCE_METRICS_DISABLED)) {
            MessageManager.devicePerformanceMetricsDisabled = responseJSON.optBoolean(KEY_DEVICE_PERFORMANCE_METRICS_DISABLED, false)
        }
        if (responseJSON.has(WORKSPACE_TOKEN)) {
            editor?.putString(WORKSPACE_TOKEN, responseJSON.getString(WORKSPACE_TOKEN))
        } else {
            editor?.remove(WORKSPACE_TOKEN)
        }
        if (responseJSON.has(ALIAS_MAX_WINDOW)) {
            editor?.putInt(ALIAS_MAX_WINDOW, responseJSON.getInt(ALIAS_MAX_WINDOW))
        } else {
            editor?.remove(ALIAS_MAX_WINDOW)
        }
        if (!mIgnoreDataplanOptionsFromConfig) {
            dataplanOptions = parseDataplanOptions(responseJSON)
            val instance: MParticle? = MParticle.getInstance()
            if (instance != null) {
                instance.Internal().kitManager.updateDataplan(dataplanOptions)
            }
        }
        editor?.apply()
        applyConfig()
        onConfigLoaded(ConfigType.CORE, newConfig)
    }

    val activeModuleIds: String
        get() {
            val kitStatusMap: MutableMap<Int, KitStatus>? = MParticle.getInstance()?.Internal()?.kitManager?.kitStatus
            val activeKits: MutableList<Int> = ArrayList()
            kitStatusMap?.let {
                for (kitStatus: Map.Entry<Int, KitStatus> in kitStatusMap.entries) {
                    val status: KitStatus = kitStatus.value
                    when (status) {
                        KitStatus.ACTIVE, KitStatus.STOPPED -> activeKits.add(kitStatus.key)
                        else -> {
                        }
                    }
                }
            }
            Collections.sort(activeKits)
            if (activeKits.size == 0) {
                return ""
            } else {
                val builder: StringBuilder = StringBuilder(activeKits.size * 3)
                for (kitId: Int? in activeKits) {
                    builder.append(kitId)
                    builder.append(",")
                }
                builder.deleteCharAt(builder.length - 1)
                return builder.toString()
            }
        }

    /**
     * When the Config manager starts up, we don't want to enable everything immediately to save on app-load time.
     * This method will be called from a background thread after startup is already complete.
     */
    fun delayedStart() {
        val senderId: String? = pushSenderId
        if (isPushEnabled && senderId != null) {
            MParticle.getInstance()?.Messaging()?.enablePushNotifications(senderId)
        }
    }

    private fun applyConfig() {
        if (logUnhandledExceptions) {
            enableUncaughtExceptionLogging(false)
        } else {
            disableUncaughtExceptionLogging(false)
        }
    }

    fun enableUncaughtExceptionLogging(userTriggered: Boolean) {
        if (userTriggered) {
            logUnhandledExceptions = true
        }
        if (null == mExHandler) {
            val currentUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
            if (currentUncaughtExceptionHandler !is ExceptionHandler) {
                mExHandler = ExceptionHandler(currentUncaughtExceptionHandler)
                Thread.setDefaultUncaughtExceptionHandler(mExHandler)
            }
        }
    }

    fun disableUncaughtExceptionLogging(userTriggered: Boolean) {
        if (userTriggered) {
            logUnhandledExceptions = false
        }
        if (null != mExHandler) {
            val currentUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
            if (currentUncaughtExceptionHandler is ExceptionHandler) {
                Thread.setDefaultUncaughtExceptionHandler(mExHandler?.originalExceptionHandler)
                mExHandler = null
            }
        }
    }

    var logUnhandledExceptions: Boolean
        get() {
            if (VALUE_APP_DEFINED == mLogUnhandledExceptions) {
                return sPreferences?.getBoolean(Constants.PrefKeys.REPORT_UNCAUGHT_EXCEPTIONS, false) == true
            } else {
                return VALUE_CUE_CATCH == mLogUnhandledExceptions
            }
        }
        set(log) {
            sPreferences?.edit()?.putBoolean(Constants.PrefKeys.REPORT_UNCAUGHT_EXCEPTIONS, log)?.apply()
        }
    open val apiKey: String
        get() = sPreferences?.getString(Constants.PrefKeys.API_KEY, null) ?: ""

    open val apiSecret: String
        get() = sPreferences?.getString(Constants.PrefKeys.API_SECRET, null) ?: ""

    fun setCredentials(apiKey: String?, secret: String?) {
        sPreferences?.edit()
            ?.putString(Constants.PrefKeys.API_KEY, apiKey)
            ?.putString(Constants.PrefKeys.API_SECRET, secret)
            ?.apply()
    }

    open val uploadInterval: Long
        get() {
            return if (getEnvironment() == MParticle.Environment.Development) {
                DEVMODE_UPLOAD_INTERVAL_MILLISECONDS.toLong()
            } else {
                if (mUploadInterval > 0) {
                    (1000 * mUploadInterval).toLong()
                } else {
                    ((1000 * (sPreferences?.getInt(Constants.PrefKeys.UPLOAD_INTERVAL, DEFAULT_UPLOAD_INTERVAL) ?: DEFAULT_UPLOAD_INTERVAL)).toLong())
                }
            }
        }

    fun setEnvironment(environment: MParticle.Environment?) {
        if (environment != null) {
            sPreferences?.edit()?.putInt(Constants.PrefKeys.ENVIRONMENT, environment.value)?.apply()
        } else {
            sPreferences?.edit()?.remove(Constants.PrefKeys.ENVIRONMENT)?.apply()
        }
    }

    fun setUploadInterval(uploadInterval: Int) {
        sPreferences?.edit()?.putInt(Constants.PrefKeys.UPLOAD_INTERVAL, uploadInterval)?.apply()
    }

    var sessionTimeout: Int
        get() {
            if (mSessionTimeoutInterval > 0) {
                return mSessionTimeoutInterval * 1000
            } else {
                return sPreferences?.getInt(
                    Constants.PrefKeys.SESSION_TIMEOUT,
                    DEFAULT_SESSION_TIMEOUT_SECONDS
                )?.times(1000) ?: 0
            }
        }
        set(sessionTimeout) {
            sPreferences?.edit()?.putInt(Constants.PrefKeys.SESSION_TIMEOUT, sessionTimeout)?.apply()
        }

    open val isPushEnabled: Boolean
        get() = sPreferences?.getBoolean(
            Constants.PrefKeys.PUSH_ENABLED,
            false
        ) == true && pushSenderId != null

    open var pushSenderId: String?
        get() {
            val pushRegistration: PushRegistration? = pushRegistration
            if (pushRegistration != null) {
                return pushRegistration.senderId
            } else {
                return null
            }
        }
        set(senderId) {
            sPreferences?.edit()?.putString(Constants.PrefKeys.PUSH_SENDER_ID, senderId)
                ?.putBoolean(Constants.PrefKeys.PUSH_ENABLED, true)
                ?.apply()
        }

    open var pushInstanceId: String?
        get() {
            val pushRegistration: PushRegistration? = pushRegistration
            if (pushRegistration != null) {
                return pushRegistration.instanceId
            } else {
                return null
            }
        }
        set(token) {
            sPreferences?.edit()?.putString(Constants.PrefKeys.PUSH_INSTANCE_ID, token)?.apply()
        }

    open var pushRegistration: PushRegistration?
        get() {
            val senderId: String? =
                sPreferences?.getString(Constants.PrefKeys.PUSH_SENDER_ID, null)
            val instanceId: String? =
                sPreferences?.getString(Constants.PrefKeys.PUSH_INSTANCE_ID, null)
            return PushRegistration(instanceId, senderId)
        }
        set(pushRegistration) {
            if (pushRegistration == null || isEmpty(pushRegistration.senderId)) {
                clearPushRegistration()
            } else {
                pushSenderId = pushRegistration.senderId
                pushInstanceId = pushRegistration.instanceId
            }
        }

    fun setPushRegistrationFetched() {
        val appVersion: Int = appVersion
        sPreferences?.edit()
            ?.putInt(Constants.PrefKeys.PROPERTY_APP_VERSION, appVersion)
            ?.putInt(Constants.PrefKeys.PROPERTY_OS_VERSION, Build.VERSION.SDK_INT)
            ?.apply()
    }

    val isPushRegistrationFetched: Boolean
        get() {
            // Check if app was updated; if so, it must clear the registration ID
            // since the existing regID is not guaranteed to work with the new
            // app version.
            val registeredVersion: Int = sPreferences?.getInt(
                Constants.PrefKeys.PROPERTY_APP_VERSION,
                Int.MIN_VALUE
            ) ?: Int.MIN_VALUE
            val currentVersion: Int = appVersion
            val osVersion: Int = sPreferences?.getInt(
                Constants.PrefKeys.PROPERTY_OS_VERSION,
                Int.MIN_VALUE
            ) ?: Int.MIN_VALUE
            return registeredVersion == currentVersion && osVersion == Build.VERSION.SDK_INT
        }

    val pushInstanceIdBackground: String?
        get() {
            return sPreferences?.getString(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND, null)
        }

    fun setPushRegistrationInBackground(pushRegistration: PushRegistration?) {
        var oldInstanceId: String? = pushInstanceId
        if (oldInstanceId == null) {
            oldInstanceId = ""
        }
        sPreferences?.edit()
            ?.putString(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND, oldInstanceId)
            ?.apply()
        this.pushRegistration = pushRegistration
    }

    fun clearPushRegistration() {
        sPreferences?.edit()
            ?.remove(Constants.PrefKeys.PUSH_SENDER_ID)
            ?.remove(Constants.PrefKeys.PUSH_INSTANCE_ID)
            ?.remove(Constants.PrefKeys.PUSH_ENABLED)
            ?.remove(Constants.PrefKeys.PROPERTY_APP_VERSION)
            ?.remove(Constants.PrefKeys.PROPERTY_OS_VERSION)
            ?.apply()
    }

    fun clearPushRegistrationBackground() {
        sPreferences?.edit()
            ?.remove(Constants.PrefKeys.PUSH_INSTANCE_ID_BACKGROUND)
            ?.apply()
    }

    open val isEnabled: Boolean
        get() {
            val optedOut: Boolean = this.optedOut
            return !optedOut || mSendOoEvents
        }

    fun setOptOut(optOut: Boolean) {
        sPreferences?.edit()?.putBoolean(Constants.PrefKeys.OPTOUT, optOut)?.apply()
    }

    val optedOut: Boolean
        get() {
            return sPreferences?.getBoolean(Constants.PrefKeys.OPTOUT, false) == true
        }

    fun setPushNotificationIcon(pushNotificationIcon: Int) {
        sPreferences?.edit()
            ?.putInt(Constants.PrefKeys.PUSH_ICON, pushNotificationIcon)
            ?.apply()
    }

    fun setPushNotificationTitle(pushNotificationTitle: Int) {
        sPreferences?.edit()
            ?.putInt(Constants.PrefKeys.PUSH_TITLE, pushNotificationTitle)
            ?.apply()
    }

    fun setDisplayPushNotifications(display: Boolean) {
        sPreferences?.edit()
            ?.putBoolean(Constants.PrefKeys.DISPLAY_PUSH_NOTIFICATIONS, display)
            ?.apply()
    }

    val kitConfigPreferences: SharedPreferences?
        get() {
            return mContext?.getSharedPreferences(KIT_CONFIG_PREFERENCES, Context.MODE_PRIVATE)
        }

    fun setBreadcrumbLimit(newLimit: Int) {
        setBreadcrumbLimit(newLimit, mpid)
    }

    fun setBreadcrumbLimit(newLimit: Int, mpId: Long) {
        getUserStorage(mpId)?.breadcrumbLimit = newLimit
    }

    fun setMpid(newMpid: Long, isLoggedInUser: Boolean) {
        val previousMpid: Long = mpid
        var currentLoggedInUser: Boolean = false
        val currentUserStorage: UserStorage? = userStorage
        if (currentUserStorage != null) {
            currentUserStorage.lastSeenTime = System.currentTimeMillis()
            currentLoggedInUser = currentUserStorage.isLoggedIn
        }
        val userStorage: UserStorage? = mContext?.let { create(it, newMpid) }
        userStorage?.setLoggedInUser(isLoggedInUser)

        sPreferences?.edit()?.putLong(Constants.PrefKeys.MPID, newMpid)?.apply()
        if (mUserStorage == null || mUserStorage?.mpid != newMpid) {
            mUserStorage = userStorage
            mUserStorage?.firstSeenTime = System.currentTimeMillis()
        }
        if ((previousMpid != newMpid || currentLoggedInUser != isLoggedInUser)) {
            triggerMpidChangeListenerCallbacks(newMpid, previousMpid)
        }
    }

    open val mpid: Long
        get() {
            return getMpid(false)
        }

    fun getMpid(allowTemporary: Boolean): Long {
        if (allowTemporary && sInProgress) {
            return Constants.TEMPORARY_MPID
        } else {
            return sPreferences?.getLong(Constants.PrefKeys.MPID, Constants.TEMPORARY_MPID) ?: Constants.TEMPORARY_MPID
        }
    }

    fun mpidExists(mpid: Long): Boolean {
        return mContext?.let { getMpIdSet(it).contains(mpid) } ?: false
    }

    val mpids: Set<Long>
        get() {
            return mContext?.let { getMpIdSet(it) } ?: emptySet()
        }

    fun mergeUserConfigs(subjectMpId: Long, targetMpId: Long) {
        val subjectUserStorage: UserStorage? = getUserStorage(subjectMpId)
        val targetUserStorage: UserStorage? = getUserStorage(targetMpId)
        subjectUserStorage?.let {
            targetUserStorage?.merge(subjectUserStorage)
        }
    }

    fun shouldTrigger(message: BaseMPMessage): Boolean {
        val messageMatches: JSONArray? = triggerMessageMatches
        val triggerHashes: JSONArray? = triggerMessageHashes

        var isBackgroundAst: Boolean = false
        try {
            isBackgroundAst =
                (message.messageType == Constants.MessageType.APP_STATE_TRANSITION && message.get(Constants.MessageKey.STATE_TRANSITION_TYPE) == Constants.StateTransitionType.STATE_TRANS_BG)
        } catch (ex: JSONException) {
        }
        var shouldTrigger: Boolean = message.messageType == Constants.MessageType.PUSH_RECEIVED ||
            message.messageType == Constants.MessageType.COMMERCE_EVENT ||
            isBackgroundAst

        if (!shouldTrigger && messageMatches != null && messageMatches.length() > 0) {
            shouldTrigger = true
            var i: Int = 0
            while (shouldTrigger && i < messageMatches.length()) {
                try {
                    val messageMatch: JSONObject = messageMatches.getJSONObject(i)
                    val keys: Iterator<*> = messageMatch.keys()
                    while (shouldTrigger && keys.hasNext()) {
                        val key: String = keys.next() as String
                        shouldTrigger = message.has(key)
                        if (shouldTrigger) {
                            try {
                                shouldTrigger = messageMatch.getString(key).equals(message.getString(key), ignoreCase = true)
                            } catch (stringex: JSONException) {
                                try {
                                    shouldTrigger = message.getBoolean(key) == messageMatch.getBoolean(key)
                                } catch (boolex: JSONException) {
                                    try {
                                        shouldTrigger = message.getDouble(key) == messageMatch.getDouble(key)
                                    } catch (doubleex: JSONException) {
                                        shouldTrigger = false
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                }
                i++
            }
        }
        if (!shouldTrigger && triggerHashes != null) {
            for (i in 0 until triggerHashes.length()) {
                try {
                    if (triggerHashes.getInt(i) == message.typeNameHash) {
                        shouldTrigger = true
                        break
                    }
                } catch (jse: JSONException) {
                }
            }
        }
        return shouldTrigger
    }

    val userBucket: Int
        get() {
            if (mUserBucket < 0) {
                mUserBucket = (abs((mpid shr 8).toDouble()) % 100).toInt()
            }
            return mUserBucket
        }

    fun setIntegrationAttributes(integrationId: Int, newAttributes: Map<String, String?>?) {
        try {
            var newJsonAttributes: JSONObject? = null
            if (newAttributes != null && !newAttributes.isEmpty()) {
                newJsonAttributes = JSONObject()
                for (entry: Map.Entry<String?, String?> in newAttributes.entries) {
                    newJsonAttributes.put(entry.key, entry.value)
                }
            }
            var currentJsonAttributes: JSONObject? = integrationAttributes
            if (currentJsonAttributes == null) {
                currentJsonAttributes = JSONObject()
            }
            currentJsonAttributes.put(integrationId.toString(), newJsonAttributes)
            if (currentJsonAttributes.length() > 0) {
                sPreferences?.edit()
                    ?.putString(Constants.PrefKeys.INTEGRATION_ATTRIBUTES, currentJsonAttributes.toString())
                    ?.apply()
            } else {
                sPreferences?.edit()
                    ?.remove(Constants.PrefKeys.INTEGRATION_ATTRIBUTES)
                    ?.apply()
            }
        } catch (jse: JSONException) {
        }
    }

    fun getIntegrationAttributes(integrationId: Int): Map<String, String> {
        val integrationAttributes: MutableMap<String, String> = HashMap()
        val jsonAttributes: JSONObject? = this.integrationAttributes
        if (jsonAttributes != null) {
            val kitAttributes: JSONObject? = jsonAttributes.optJSONObject(integrationId.toString())
            if (kitAttributes != null) {
                try {
                    val keys: Iterator<String> = kitAttributes.keys()
                    while (keys.hasNext()) {
                        val key: String = keys.next()
                        if (kitAttributes.get(key) is String) {
                            integrationAttributes[key] = kitAttributes.getString(key)
                        }
                    }
                } catch (e: JSONException) {
                }
            }
        }
        return integrationAttributes
    }

    val integrationAttributes: JSONObject?
        get() {
            var jsonAttributes: JSONObject? = null
            val allAttributes: String? =
                sPreferences?.getString(Constants.PrefKeys.INTEGRATION_ATTRIBUTES, null)
            if (allAttributes != null) {
                try {
                    jsonAttributes = JSONObject(allAttributes)
                } catch (e: JSONException) {
                }
            }
            return jsonAttributes
        }

    fun getUserIdentities(mpId: Long): Map<IdentityType, String> {
        val userIdentitiesJson: JSONArray = getUserIdentityJson(mpId)
        val identityTypeStringMap: MutableMap<IdentityType, String> = HashMap(userIdentitiesJson.length())

        for (i in 0 until userIdentitiesJson.length()) {
            try {
                val identity: JSONObject = userIdentitiesJson.getJSONObject(i)
                identityTypeStringMap[IdentityType.parseInt(identity.getInt(Constants.MessageKey.IDENTITY_NAME))] =
                    identity.getString(Constants.MessageKey.IDENTITY_VALUE)
            } catch (jse: JSONException) {
            }
        }

        return identityTypeStringMap
    }

    val userIdentityJson: JSONArray
        get() {
            return getUserIdentityJson(mpid)
        }

    fun getUserIdentityJson(mpId: Long): JSONArray {
        var userIdentities: JSONArray? = null
        val userIds: String? = getUserStorage(mpId)?.userIdentities

        try {
            userIdentities = JSONArray(userIds)
            val changeMade: Boolean = fixUpUserIdentities(userIdentities)
            if (changeMade) {
                saveUserIdentityJson(userIdentities, mpId)
            }
        } catch (e: Exception) {
            userIdentities = JSONArray()
        }
        return userIdentities ?: JSONArray()
    }

    @JvmOverloads
    fun saveUserIdentityJson(userIdentities: JSONArray, mpId: Long = mpid) {
        getUserStorage(mpId)?.userIdentities = userIdentities.toString()
    }

    open val deviceApplicationStamp: String?
        get() {
            var das: String? =
                sPreferences?.getString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, null)
            if (isEmpty(das)) {
                das = UUID.randomUUID().toString()
                sPreferences?.edit()
                    ?.putString(Constants.PrefKeys.DEVICE_APPLICATION_STAMP, das)
                    ?.apply()
            }
            return das
        }

    fun getCookies(mpId: Long): JSONObject? {
        if (mCurrentCookies == null) {
            val currentCookies: String? = getUserStorage(mpId)?.cookies
            if (isEmpty(currentCookies)) {
                mCurrentCookies = JSONObject()
                getUserStorage(mpId)?.cookies = mCurrentCookies.toString()
                return mCurrentCookies
            } else {
                try {
                    mCurrentCookies = JSONObject(currentCookies)
                } catch (e: JSONException) {
                    mCurrentCookies = JSONObject()
                }
            }
            val nowCalendar: Calendar = Calendar.getInstance()
            nowCalendar.set(Calendar.YEAR, 1990)
            val oldDate: Date = nowCalendar.time
            val parser: SimpleDateFormat = SimpleDateFormat("yyyy")
            val keys: MutableIterator<String>? = mCurrentCookies?.keys()
            val keysToRemove: ArrayList<String> = ArrayList()
            while (keys?.hasNext() == true) {
                try {
                    val key: String = keys.next()
                    if (mCurrentCookies?.get(key) is JSONObject) {
                        val expiration: String = (mCurrentCookies?.get(key) as JSONObject).getString("e")
                        try {
                            val date: Date? = parser.parse(expiration)
                            if (date?.before(oldDate) == true) {
                                keysToRemove.add(key)
                            }
                        } catch (dpe: ParseException) {
                        }
                    }
                } catch (jse: JSONException) {
                }
            }
            for (key: String? in keysToRemove) {
                mCurrentCookies?.remove(key)
            }
            if (keysToRemove.size > 0) {
                getUserStorage(mpId)?.cookies = mCurrentCookies.toString()
            }
            return mCurrentCookies
        } else {
            return mCurrentCookies
        }
    }

    fun markIdentitiesAsSeen(uploadedIdentities: JSONArray): JSONArray? {
        return markIdentitiesAsSeen(uploadedIdentities, mpid)
    }

    fun markIdentitiesAsSeen(uploadedIdentities: JSONArray, mpId: Long): JSONArray? {
        var uploadedIdentities: JSONArray = uploadedIdentities
        try {
            val currentIdentities: JSONArray = getUserIdentityJson(mpId)
            if (currentIdentities.length() == 0) {
                return null
            }
            uploadedIdentities = JSONArray(uploadedIdentities.toString())
            val identityTypes: MutableSet<Int> = HashSet()
            for (i in 0 until uploadedIdentities.length()) {
                if (uploadedIdentities.getJSONObject(i).optBoolean(Constants.MessageKey.IDENTITY_FIRST_SEEN)) {
                    identityTypes.add(uploadedIdentities.getJSONObject(i).getInt(Constants.MessageKey.IDENTITY_NAME))
                }
            }
            if (identityTypes.size > 0) {
                for (i in 0 until currentIdentities.length()) {
                    val identity: Int = currentIdentities.getJSONObject(i).getInt(Constants.MessageKey.IDENTITY_NAME)
                    if (identityTypes.contains(identity)) {
                        currentIdentities.getJSONObject(i).put(Constants.MessageKey.IDENTITY_FIRST_SEEN, false)
                    }
                }
                return currentIdentities
            }
        } catch (jse: JSONException) {
        }
        return null
    }

    var identityApiContext: String?
        get() {
            return sPreferences?.getString(Constants.PrefKeys.IDENTITY_API_CONTEXT, null)
        }
        set(context) {
            sPreferences?.edit()?.putString(Constants.PrefKeys.IDENTITY_API_CONTEXT, context)?.apply()
        }

    val previousAdId: String?
        get() {
            val adInfo: AdIdInfo? = mContext?.let { getAdIdInfo(it) }
            if (adInfo != null && !adInfo.isLimitAdTrackingEnabled) {
                return sPreferences?.getString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, null)
            }
            return null
        }

    fun setPreviousAdId() {
        val adInfo: AdIdInfo? = mContext?.let { getAdIdInfo(it) }
        if (adInfo != null && !adInfo.isLimitAdTrackingEnabled) {
            sPreferences?.edit()?.putString(Constants.PrefKeys.PREVIOUS_ANDROID_ID, adInfo.id)?.apply()
        } else {
            sPreferences?.edit()?.remove(Constants.PrefKeys.PREVIOUS_ANDROID_ID)?.apply()
        }
    }

    var identityConnectionTimeout: Int
        get() {
            return (
                sPreferences?.getInt(
                    Constants.PrefKeys.IDENTITY_CONNECTION_TIMEOUT,
                    DEFAULT_CONNECTION_TIMEOUT_SECONDS
                ) ?: DEFAULT_CONNECTION_TIMEOUT_SECONDS
                ) * 1000
        }
        set(connectionTimeout) {
            if (connectionTimeout >= MINIMUM_CONNECTION_TIMEOUT_SECONDS) {
                sPreferences?.edit()
                    ?.putInt(Constants.PrefKeys.IDENTITY_CONNECTION_TIMEOUT, connectionTimeout)?.apply()
            }
        }

    val connectionTimeout: Int
        get() {
            return DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000
        }

    private fun triggerMpidChangeListenerCallbacks(mpid: Long, previousMpid: Long) {
        if (isEmpty(mpIdChangeListeners)) {
            return
        }
        for (listenerRef: MpIdChangeListener? in ArrayList(mpIdChangeListeners)) {
            if (listenerRef != null) {
                listenerRef.onMpIdChanged(mpid, previousMpid)
            }
        }
    }

    fun getConsentState(mpid: Long): ConsentState {
        val serializedConsent: String? = getUserStorage(mpid)?.serializedConsentState
        return ConsentState.withConsentState(serializedConsent ?: "").build()
    }

    val podPrefix: String
        /* This function is called to get the specific pod/silo prefix when the `directUrlRouting` is `true`. mParticle API keys are prefixed with the
             silo and a hyphen (ex. "us1-", "us2-", "eu1-").  us1 was the first silo,and before other silos existed, there were no prefixes and all apiKeys
             were us1. As such, if we split on a '-' and the resulting array length is 1, then it is an older APIkey that should route to us1.
             When splitKey.length is greater than 1, then splitKey[0] will be us1, us2, eu1, au1, or st1, etc as new silos are added */
        get() {
            var prefix: String = "us1"
            try {
                val prefixFromApi: Array<String> = apiKey.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (prefixFromApi.size > 1) {
                    prefix = prefixFromApi.get(0)
                }
            } catch (e: Exception) {
                prefix = "us1"
                Logger.error("Error while getting pod prefix for direct URL routing : $e")
            }
            return prefix
        }

    fun setConsentState(state: ConsentState?, mpid: Long) {
        var serializedConsent: String? = null
        if (state != null) {
            serializedConsent = state.toString()
        }
        getUserStorage(mpid)?.serializedConsentState = serializedConsent
    }

    @set:Synchronized
    open var networkOptions: NetworkOptions?
        get() {
            if (sNetworkOptions == null) {
                sNetworkOptions = NetworkOptionsManager.validateAndResolve(null)
            }
            return sNetworkOptions
        }
        set(networkOptions) {
            sNetworkOptions = networkOptions
            sPreferences?.edit()?.remove(Constants.PrefKeys.NETWORK_OPTIONS)?.apply()
        }

    open val workspaceToken: String
        get() {
            return sPreferences?.getString(WORKSPACE_TOKEN, "") ?: ""
        }
    open val aliasMaxWindow: Int
        /**
         * the maximum allowed age of "start_time" in an AliasRequest, in days
         *
         * @return
         */
        get() {
            return sPreferences?.getInt(
                ALIAS_MAX_WINDOW,
                DEFAULT_MAX_ALIAS_WINDOW_DAYS
            ) ?: DEFAULT_MAX_ALIAS_WINDOW_DAYS
        }

    val etag: String?
        get() {
            return sPreferences?.getString(Constants.PrefKeys.ETAG, null)
        }

    val ifModified: String?
        get() {
            return sPreferences?.getString(Constants.PrefKeys.IF_MODIFIED, null)
        }

    fun addConfigUpdatedListener(listener: ConfigLoadedListener) {
        configUpdatedListeners.add(listener)
    }

    private fun onConfigLoaded(configType: ConfigType, isNew: Boolean) {
        Logger.debug("Loading " + (if (isNew) "new " else "cached ") + configType.name.lowercase() + " config")
        val listeners = configUpdatedListeners
        for (listener in listeners) {
            listener.onConfigUpdated(configType, isNew)
        }
    }

    fun parseDataplanOptions(jsonObject: JSONObject?): DataplanOptions? {
        if (jsonObject != null) {
            val dataplanConfig: JSONObject? = jsonObject.optJSONObject(DATAPLAN_KEY)
            if (dataplanConfig != null) {
                val dataplanContanier: JSONObject? = dataplanConfig.optJSONObject(DATAPLAN_OBJ)
                if (dataplanContanier != null) {
                    val block: JSONObject? = dataplanContanier.optJSONObject(DATAPLAN_BLOCKING)
                    val dataplanVersion: JSONObject? = dataplanContanier.optJSONObject(DATAPLAN_VERSION)
                    if (block != null) {
                        return DataplanOptions.builder()
                            .dataplanVersion(dataplanVersion)
                            .blockEvents(block.optBoolean(DATAPLAN_BLOCK_EVENTS, false))
                            .blockEventAttributes(block.optBoolean(DATAPLAN_BLOCK_EVENT_ATTRIBUTES, false))
                            .blockUserAttributes(block.optBoolean(DATAPLAN_BLOCK_USER_ATTRIBUTES, false))
                            .blockUserIdentities(block.optBoolean(DATAPLAN_BLOCK_USER_IDENTITIES, false))
                            .build()
                    }
                }
            }
        }
        return null
    }

    private val appVersion: Int
        get() {
            try {
                val packageInfo = mContext?.packageName?.let { mContext?.packageManager?.getPackageInfo(it, 0) }
                return packageInfo?.versionCode ?: throw RuntimeException("Could not get package info.")
            } catch (e: PackageManager.NameNotFoundException) {
                // should never happen
                throw RuntimeException("Could not get package name: $e")
            }
        }

    enum class ConfigType {
        CORE,
        KIT
    }

    interface ConfigLoadedListener {
        fun onConfigUpdated(configType: ConfigType, isNew: Boolean)
    }

    companion object {
        const val CONFIG_JSON: String = "json"
        const val KIT_CONFIG_PREFERENCES: String = "mparticle_config.json"
        const val CONFIG_JSON_TIMESTAMP: String = "json_timestamp"
        private const val KEY_TRIGGER_ITEMS: String = "tri"
        private const val KEY_MESSAGE_MATCHES: String = "mm"
        private const val KEY_TRIGGER_ITEM_HASHES: String = "evts"
        private const val KEY_INFLUENCE_OPEN: String = "pio"
        const val KEY_OPT_OUT: String = "oo"
        const val KEY_UNHANDLED_EXCEPTIONS: String = "cue"
        const val KEY_PUSH_MESSAGES: String = "pmk"
        const val KEY_DIRECT_URL_ROUTING: String = "dur"
        const val KEY_EMBEDDED_KITS: String = "eks"
        const val KEY_UPLOAD_INTERVAL: String = "uitl"
        const val KEY_SESSION_TIMEOUT: String = "stl"
        const val VALUE_APP_DEFINED: String = "appdefined"
        const val VALUE_CUE_CATCH: String = "forcecatch"
        const val PREFERENCES_FILE: String = "mp_preferences"
        private const val KEY_DEVICE_PERFORMANCE_METRICS_DISABLED: String = "dpmd"
        const val WORKSPACE_TOKEN: String = "wst"
        const val ALIAS_MAX_WINDOW: String = "alias_max_window"
        const val KEY_RAMP: String = "rp"
        const val DATAPLAN_KEY: String = "dpr"
        const val DATAPLAN_OBJ: String = "dtpn"
        const val DATAPLAN_BLOCKING: String = "blok"
        const val DATAPLAN_VERSION: String = "vers"
        const val DATAPLAN_BLOCK_EVENTS: String = "ev"
        const val DATAPLAN_BLOCK_EVENT_ATTRIBUTES: String = "ea"
        const val DATAPLAN_BLOCK_USER_ATTRIBUTES: String = "ua"
        const val DATAPLAN_BLOCK_USER_IDENTITIES: String = "id"
        const val KIT_CONFIG_KEY: String = "kit_config"
        const val MIGRATED_TO_KIT_SHARED_PREFS: String = "is_mig_kit_sp"

        private val DEVMODE_UPLOAD_INTERVAL_MILLISECONDS: Int = 10 * 1000
        private const val DEFAULT_MAX_ALIAS_WINDOW_DAYS: Int = 90
        private var sNetworkOptions: NetworkOptions? = null
        var sPreferences: SharedPreferences? = null

        private var sPushKeys: JSONArray? = null
        const val DEFAULT_CONNECTION_TIMEOUT_SECONDS: Int = 30
        const val MINIMUM_CONNECTION_TIMEOUT_SECONDS: Int = 1
        const val DEFAULT_SESSION_TIMEOUT_SECONDS: Int = 60
        const val DEFAULT_UPLOAD_INTERVAL: Int = 600

        @JvmStatic
        fun getInstance(context: Context): ConfigManager {
            var configManager: ConfigManager? = null
            val mParticle: MParticle? = MParticle.getInstance()
            if (mParticle != null) {
                configManager = MParticle.getInstance()?.Internal()?.configManager
            }
            if (configManager == null) {
                configManager = ConfigManager(context)
            }
            return configManager
        }

        fun getUserStorage(context: Context): UserStorage {
            return create(context, getMpid(context))
        }

        fun getUserStorage(context: Context, mpid: Long): UserStorage {
            return create(context, mpid)
        }

        @JvmStatic
        fun deleteConfigManager(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(PREFERENCES_FILE)
                sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            } else {
                if (sPreferences == null) {
                    sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
                }
                sPreferences?.edit()?.clear()?.commit()
            }
        }

        @JvmStatic
        fun getEnvironment(): MParticle.Environment {
            if (sPreferences != null) {
                val env = sPreferences?.getInt(Constants.PrefKeys.ENVIRONMENT, MParticle.Environment.Production.value)
                for (environment in MParticle.Environment.entries.toTypedArray()) {
                    if (environment.value == env) {
                        return environment
                    }
                }
            }
            return MParticle.Environment.Production
        }

        @JvmStatic
        fun isDisplayPushNotifications(context: Context): Boolean {
            return getPreferences(context).getBoolean(Constants.PrefKeys.DISPLAY_PUSH_NOTIFICATIONS, false)
        }

        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        }

        @JvmStatic
        fun getPushKeys(context: Context): JSONArray {
            if (sPushKeys == null) {
                val arrayString: String? = getPreferences(context).getString(KEY_PUSH_MESSAGES, null)
                try {
                    sPushKeys = JSONArray(arrayString)
                } catch (e: Exception) {
                    sPushKeys = JSONArray()
                }
            }
            return sPushKeys ?: JSONArray()
        }

        @JvmStatic
        fun getPushTitle(context: Context): Int {
            return getPreferences(context)
                .getInt(Constants.PrefKeys.PUSH_TITLE, 0)
        }

        @JvmStatic
        fun getPushIcon(context: Context): Int {
            return getPreferences(context)
                .getInt(Constants.PrefKeys.PUSH_ICON, 0)
        }

        fun getBreadcrumbLimit(context: Context): Int {
            return getUserStorage(context).breadcrumbLimit
        }

        @JvmStatic
        fun getBreadcrumbLimit(context: Context, mpId: Long): Int {
            return getUserStorage(context, mpId).breadcrumbLimit
        }

        fun getCurrentUserLtv(context: Context): String? {
            return getUserStorage(context).ltv
        }

        @JvmStatic
        fun setNeedsToMigrate(context: Context, needsToMigrate: Boolean) {
            UserStorage.setNeedsToMigrate(context, needsToMigrate)
        }

        fun clear() {
            sPreferences?.edit()?.clear()?.apply()
        }

        // for testing
        @JvmStatic
        fun clearMpid(context: Context) {
            if (sPreferences == null) {
                sPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            }
            sPreferences?.edit()?.remove(Constants.PrefKeys.MPID)?.apply()
        }

        @JvmStatic
        fun getMpid(context: Context?): Long {
            return getMpid(context, false)
        }

        fun getMpid(context: Context?, allowTemporary: Boolean): Long {
            if (sPreferences == null) {
                sPreferences = context?.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            }
            if (allowTemporary && sInProgress) {
                return Constants.TEMPORARY_MPID
            } else {
                return sPreferences?.getLong(Constants.PrefKeys.MPID, Constants.TEMPORARY_MPID) ?: Constants.TEMPORARY_MPID
            }
        }

        private var sInProgress: Boolean = false

        @JvmStatic
        fun setIdentityRequestInProgress(inProgress: Boolean) {
            sInProgress = inProgress
        }

        private fun fixUpUserIdentities(identities: JSONArray): Boolean {
            var changeMade: Boolean = false
            try {
                for (i in 0 until identities.length()) {
                    val identity: JSONObject = identities.getJSONObject(i)
                    if (!identity.has(Constants.MessageKey.IDENTITY_DATE_FIRST_SEEN)) {
                        identity.put(Constants.MessageKey.IDENTITY_DATE_FIRST_SEEN, 0)
                        changeMade = true
                    }
                    if (!identity.has(Constants.MessageKey.IDENTITY_FIRST_SEEN)) {
                        identity.put(Constants.MessageKey.IDENTITY_FIRST_SEEN, true)
                        changeMade = true
                    }
                }
            } catch (jse: JSONException) {
            }
            return changeMade
        }

        private val mpIdChangeListeners: MutableSet<MpIdChangeListener?> = HashSet()

        @JvmStatic
        fun addMpIdChangeListener(listener: MpIdChangeListener?) {
            mpIdChangeListeners.add(listener)
        }
    }
}
