package com.mparticle.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.telephony.TelephonyManager
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mparticle.InstallReferrerHelper
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.AliasRequest
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.Constants.MessageType
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.internal.messages.BaseMPMessageBuilder
import com.mparticle.internal.messages.MPAliasMessage
import com.mparticle.messaging.ProviderCloudMessage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.Locale

/**
 * This class is primarily responsible for generating BaseMPMessage objects, and then adding them to a
 * queue which is then processed in a background thread for further processing and database storage.
 */
open class MessageManager :
    MessageManagerCallbacks,
    ReportingManager {
    private val mDeviceAttributes: DeviceAttributes
    private var mAppStateManager: AppStateManager? = null
    private var mConfigManager: ConfigManager? = null
    private var mMParticleDBManager: MParticleDBManager? = null
    private var mOperatingSystem: MParticle.OperatingSystem? = null

    /**
     * These are the handlers which manage the queues and threads mentioned above.
     */
    @JvmField
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var mMessageHandler: MessageHandler? = null

    @JvmField
    var mUploadHandler: UploadHandler? = null

    /**
     * Used to communicate the current location at the time of message generation. Can be set
     * manually by the customer, or automatically via our our location listener, if enabled.
     */
    private var mLocation: Location? = null

    /**
     * Every state-transition message needs to know if this was an upgrade or an install.
     */
    @JvmField
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var mInstallType: MParticle.InstallType? = MParticle.InstallType.AutoDetect

    private var delayedStartOccurred = false

    /**
     * Used solely for unit testing
     */
    constructor() {
        mDeviceAttributes = DeviceAttributes(mOperatingSystem)
    }

    /**
     * Used solely for unit testing
     */
    constructor(
        appContext: Context?,
        configManager: ConfigManager?,
        installType: MParticle.InstallType?,
        appStateManager: AppStateManager?,
        dbManager: MParticleDBManager?,
        messageHandler: MessageHandler?,
        uploadHandler: UploadHandler?,
    ) {
        mDeviceAttributes = DeviceAttributes(mOperatingSystem)
        sContext = appContext!!.applicationContext
        mConfigManager = configManager
        mAppStateManager = appStateManager
        mMParticleDBManager = dbManager
        mMessageHandler = messageHandler
        mUploadHandler = uploadHandler
        sPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mInstallType = installType
    }

    constructor(
        configManager: ConfigManager?,
        appStateManager: AppStateManager?,
        kitFrameworkWrapper: KitFrameworkWrapper?,
        devicePerformanceMetricsDisabled: Boolean,
        dbManager: MParticleDBManager?,
        options: MParticleOptions?,
    ) {
        MessageManager.devicePerformanceMetricsDisabled = devicePerformanceMetricsDisabled
        mOperatingSystem = options!!.operatingSystem
        mDeviceAttributes = DeviceAttributes(mOperatingSystem)
        sContext = options.context.applicationContext
        mConfigManager = configManager
        mAppStateManager = appStateManager
        mAppStateManager!!.setMessageManager(this)
        mMParticleDBManager = dbManager
        mMessageHandler =
            MessageHandler(
                sMessageHandlerThread!!.looper,
                this,
                options.context,
                dbManager,
                options.dataplanId,
                options.dataplanVersion,
            )
        mUploadHandler =
            UploadHandler(
                options.context,
                sUploadHandlerThread!!.looper,
                configManager,
                appStateManager,
                this,
                dbManager,
                kitFrameworkWrapper,
            )
        sPreferences = options.context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mInstallType = options.installType
    }

    open var isDevicePerformanceMetricsDisabled: Boolean
        get() = devicePerformanceMetricsDisabled
        set(disabled) {
            devicePerformanceMetricsDisabled = disabled
        }

    @Throws(JSONException::class)
    open fun createFirstRunMessage(): BaseMPMessage = BaseMPMessage
        .Builder(MessageType.FIRST_RUN)
        .timestamp(mAppStateManager!!.session.mSessionStartTime)
        .dataConnection(sActiveNetworkName)
        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

    open fun startSession(session: InternalSession?): BaseMPMessage? {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.SESSION_START)
                    .timestamp(mAppStateManager!!.session.mSessionStartTime)
                    .build(session, mLocation, mConfigManager!!.mpid)

            val editor = sPreferences!!.edit()
            val timeInFg = mConfigManager!!.userStorage.previousSessionForegound
            if (timeInFg > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_LENGTH, timeInFg / 1000)
                mConfigManager!!.userStorage.clearPreviousTimeInForeground()
            }
            val prevSessionId = mConfigManager!!.userStorage.previousSessionId
            mConfigManager!!.userStorage.previousSessionId = session!!.mSessionID
            if (!MPUtility.isEmpty(prevSessionId)) {
                message.put(MessageKey.PREVIOUS_SESSION_ID, prevSessionId)
            }

            val prevSessionStart = mConfigManager!!.userStorage.getPreviousSessionStart(-1)
            mConfigManager!!.userStorage.setPreviousSessionStart(session.mSessionStartTime)

            if (prevSessionStart > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_START, prevSessionStart)
            }

            editor.apply()

            if (isFirstRunForMessage) {
                isFirstRunForMessage = false
                try {
                    val firstRunMessage: JSONObject = createFirstRunMessage()
                    mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, firstRunMessage))
                } catch (e: JSONException) {
                    Logger.warning("Failed to create First Run Message.")
                }
            } else {
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.END_ORPHAN_SESSIONS, mConfigManager!!.mpid))
            }

            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))

            mConfigManager!!.userStorage.incrementSessionCounter()
            return message
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle start session message.")
            return null
        }
    }

    open fun updateSessionEnd(session: InternalSession?) {
        try {
            mConfigManager!!.userStorage.setPreviousSessionForeground(session!!.foregroundTime)
            mMessageHandler!!
                .sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.UPDATE_SESSION_END, session))
        } catch (e: Exception) {
            Logger.warning("Failed to send update session end message.")
        }
    }

    open fun endSession(session: InternalSession?) {
        updateSessionEnd(session)
        val entry: Map.Entry<String?, Set<Long>?> =
            java.util.AbstractMap.SimpleEntry<String?, Set<Long>?>(session!!.mSessionID, session.mpids)
        mMessageHandler!!
            .sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, 1, 1, entry))
    }

    // check if a session has been started, if it has, then it means that the InstallReferrer was received
    // after the current session was started, is out of date, and needs to be updated. If the current
    // session has been started, disregard.
    open fun installReferrerUpdated() {
        val sessionId = mAppStateManager!!.session.mSessionID
        if (mAppStateManager!!.session.isActive) {
            val message = mMessageHandler!!.obtainMessage(MessageHandler.INSTALL_REFERRER_UPDATED, sessionId)
            mMessageHandler!!.sendMessage(message)
        }
    }

    open fun logEvent(event: MPEvent?, currentActivity: String?): BaseMPMessage? {
        if (event != null) {
            try {
                val message =
                    event
                        .getMessage()
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
                message.put(Constants.MessageKey.EVENT_START_TIME, mAppStateManager!!.session.mLastEventTime)
                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity)
                }

                var count = sPreferences!!.getInt(Constants.PrefKeys.EVENT_COUNTER, 0)
                message.put(MessageKey.EVENT_COUNTER, count)
                sPreferences!!.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, ++count).apply()

                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle log event message.")
            }
        }
        return null
    }

    open fun logEvent(event: CommerceEvent?): BaseMPMessage? {
        if (event != null) {
            try {
                val builder: BaseMPMessageBuilder = event.getMessage()
                val message =
                    builder
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle log event message.")
            }
        }
        return null
    }

    open fun logScreen(event: MPEvent?, started: Boolean): BaseMPMessage? {
        if (event != null && event.eventName != null) {
            try {
                val message =
                    BaseMPMessage
                        .Builder(MessageType.SCREEN_VIEW)
                        .timestamp(mAppStateManager!!.session.mLastEventTime)
                        .name(event.eventName)
                        .flags(event.customFlags)
                        .attributes(MPUtility.enforceAttributeConstraints(event.customAttributeStrings))
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager!!.session.mLastEventTime)
                message.put(MessageKey.EVENT_DURATION, 0)
                message.put(MessageKey.SCREEN_STARTED, if (started) "activity_started" else "activity_stopped")
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle log event message.")
            }
        }
        return null
    }

    open fun logBreadcrumb(breadcrumb: String?): BaseMPMessage? {
        if (breadcrumb != null) {
            try {
                val message =
                    BaseMPMessage
                        .Builder(MessageType.BREADCRUMB)
                        .timestamp(mAppStateManager!!.session.mLastEventTime)
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager!!.session.mLastEventTime)
                message.put(MessageKey.BREADCRUMB_SESSION_COUNTER, mConfigManager!!.userStorage.currentSessionCounter)
                message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb)
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_BREADCRUMB, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle breadcrumb message.")
            }
        }
        return null
    }

    open fun optOut(time: Long, optOutStatus: Boolean): BaseMPMessage? {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.OPT_OUT)
                    .timestamp(time)
                    .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus)
            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
            return message
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle opt out message.")
            return null
        }
    }

    open fun logErrorEvent(errorMessage: String?, t: Throwable?, attributes: JSONObject?): BaseMPMessage? =
        logErrorEvent(errorMessage, t, attributes, true)

    open fun logErrorEvent(errorMessage: String?, t: Throwable?, attributes: JSONObject?, caught: Boolean): BaseMPMessage? {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.ERROR)
                    .timestamp(mAppStateManager!!.session.mLastEventTime)
                    .attributes(attributes)
                    .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
            if (t != null) {
                message.put(MessageKey.ERROR_MESSAGE, t.message)
                message.put(MessageKey.ERROR_SEVERITY, if (caught) "error" else "fatal")
                message.put(MessageKey.ERROR_CLASS, t.javaClass.canonicalName)
                val stringWriter = StringWriter()
                t.printStackTrace(PrintWriter(stringWriter))
                message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString())
                message.put(MessageKey.ERROR_UNCAUGHT, caught.toString())
                message.put(MessageKey.ERROR_SESSION_COUNT, mConfigManager!!.userStorage.currentSessionCounter)
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
            } else if (errorMessage != null) {
                message.put(MessageKey.ERROR_SEVERITY, "error")
                message.put(MessageKey.ERROR_MESSAGE, errorMessage)
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
            }
            return message
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle error message.")
        }
        return null
    }

    open fun logNetworkPerformanceEvent(
        time: Long,
        method: String?,
        url: String?,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
    ): BaseMPMessage? {
        if (!MPUtility.isEmpty(url) && !MPUtility.isEmpty(method)) {
            try {
                val message =
                    BaseMPMessage
                        .Builder(MessageType.NETWORK_PERFORMNACE)
                        .timestamp(time)
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
                message.put(MessageKey.NPE_METHOD, method)
                message.put(MessageKey.NPE_URL, url)
                message.put(MessageKey.NPE_LENGTH, length)
                message.put(MessageKey.NPE_SENT, bytesSent)
                message.put(MessageKey.NPE_REC, bytesReceived)
                if (requestString != null) {
                    message.put(MessageKey.NPE_POST_DATA, requestString)
                }
                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle network performance message.")
            }
        }
        return null
    }

    open fun setPushRegistrationId(token: String?, registeringFlag: Boolean): BaseMPMessage? {
        if (!MPUtility.isEmpty(token)) {
            try {
                mConfigManager!!.setPushInstanceId(token)
                val message =
                    BaseMPMessage
                        .Builder(MessageType.PUSH_REGISTRATION)
                        .timestamp(System.currentTimeMillis())
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)
                message.put(MessageKey.PUSH_TOKEN, token)
                message.put(MessageKey.PUSH_TOKEN_TYPE, "google")
                message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag)

                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle push registration message.")
            }
        }
        return null
    }

    open fun setSessionAttributes() {
        val session = mAppStateManager!!.session
        if (session.mSessionAttributes != null) {
            try {
                val sessionAttributes = JSONObject()
                sessionAttributes.put(MessageKey.SESSION_ID, mAppStateManager!!.session.mSessionID)
                sessionAttributes.put(MessageKey.ATTRIBUTES, session.mSessionAttributes)
                mMessageHandler!!.sendMessage(
                    mMessageHandler!!.obtainMessage(
                        MessageHandler.UPDATE_SESSION_ATTRIBUTES,
                        sessionAttributes,
                    ),
                )
            } catch (e: JSONException) {
                Logger.warning("Failed to send update session attributes message.")
            }
        }
    }

    /**
     * We will attempt to upload by default every 10 minutes until the session times out.
     */
    open fun startUploadLoop() {
        mUploadHandler!!.removeMessages(UploadHandler.UPLOAD_MESSAGES, mConfigManager!!.mpid)
        mUploadHandler!!.sendMessageDelayed(
            mUploadHandler!!.obtainMessage(UploadHandler.UPLOAD_MESSAGES, mConfigManager!!.mpid),
            Constants.INITIAL_UPLOAD_DELAY,
        )
    }

    open fun doUpload() {
        mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.CLEAR_MESSAGES_FOR_UPLOAD))
    }

    override fun messagesClearedForUpload() {
        mUploadHandler!!.sendMessage(mUploadHandler!!.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0, mConfigManager!!.mpid))
    }

    open var location: Location?
        get() = mLocation
        set(value) {
            mLocation = value
            Logger.debug("Received location update: $value")
        }

    open fun logStateTransition(
        stateTransInit: String?,
        currentActivity: String?,
        launchUri: String?,
        launchExtras: String?,
        launchSourcePackage: String?,
        previousForegroundTime: Long,
        suspendedTime: Long,
        interruptions: Int,
    ): BaseMPMessage? {
        if (!MPUtility.isEmpty(stateTransInit)) {
            try {
                val message =
                    BaseMPMessage
                        .Builder(MessageType.APP_STATE_TRANSITION)
                        .timestamp(System.currentTimeMillis())
                        .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

                message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit)
                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity)
                }

                val crashedInForeground = sPreferences!!.getBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false)

                if (stateTransInit!! == Constants.StateTransitionType.STATE_TRANS_INIT ||
                    stateTransInit == Constants.StateTransitionType.STATE_TRANS_FORE
                ) {
                    sPreferences!!.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, true).apply()
                    message.put(MessageKey.ST_LAUNCH_REFERRER, launchUri)
                    message.put(MessageKey.ST_LAUNCH_PARAMS, launchExtras)
                    message.put(MessageKey.ST_LAUNCH_SOURCE_PACKAGE, launchSourcePackage)
                    if (previousForegroundTime > 0) {
                        message.put(MessageKey.ST_LAUNCH_PRV_FORE_TIME, previousForegroundTime)
                    }
                    if (suspendedTime > 0) {
                        message.put(MessageKey.ST_LAUNCH_TIME_SUSPENDED, suspendedTime)
                    }
                    if (interruptions >= 0) {
                        message.put(MessageKey.ST_INTERRUPTIONS, interruptions)
                    }
                }

                if (stateTransInit == Constants.StateTransitionType.STATE_TRANS_INIT) {
                    val editor = sPreferences!!.edit()

                    if (!isFirstRunForAST) {
                        message.put(MessageKey.APP_INIT_CRASHED, crashedInForeground)
                    }

                    var versionCode = 0
                    try {
                        val pInfo = sContext!!.packageManager.getPackageInfo(sContext!!.packageName, 0)
                        versionCode = pInfo.versionCode
                    } catch (nnfe: PackageManager.NameNotFoundException) {
                    }
                    // if we've seen this device before, and the versionCode is different, then we know it's an upgrade
                    var upgrade = (versionCode != sPreferences!!.getInt(Constants.PrefKeys.INITUPGRADE, versionCode))
                    editor.putInt(Constants.PrefKeys.INITUPGRADE, versionCode).apply()

                    if (!upgrade) {
                        if (mInstallType == MParticle.InstallType.KnownUpgrade) {
                            upgrade = true
                        } else if (mInstallType == MParticle.InstallType.KnownInstall) {
                            upgrade = false
                        }
                    }

                    message.put(MessageKey.APP_INIT_FIRST_RUN, isFirstRunForAST)
                    if (isFirstRunForAST) {
                        isFirstRunForAST = false
                    }
                    message.put(MessageKey.APP_INIT_UPGRADE, upgrade)
                }

                if (stateTransInit == Constants.StateTransitionType.STATE_TRANS_BG) {
                    sPreferences!!.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).apply()
                }

                mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
                return message
            } catch (e: JSONException) {
                Logger.warning("Failed to create mParticle state transition message.")
            }
        }
        return null
    }

    open fun logNotification(cloudMessage: ProviderCloudMessage?, appState: String?) {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.PUSH_RECEIVED)
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

            message.put(MessageKey.PAYLOAD, cloudMessage!!.redactedJsonPayload.toString())
            message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED)

            val instanceId = mConfigManager!!.pushRegistration?.instanceId
            if (!instanceId.isNullOrEmpty()) {
                message.put(MessageKey.PUSH_TOKEN, instanceId)
            }
            message.put(MessageKey.APP_STATE, appState)
            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
        } catch (e: JSONException) {
        }
    }

    override fun logNotification(contentId: Int, payload: String?, appState: String?, behavior: Int) {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.PUSH_RECEIVED)
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

            message.put(MessageKey.PAYLOAD, payload)
            message.put(MessageKey.PUSH_BEHAVIOR, behavior)
            message.put(MessageKey.CONTENT_ID, contentId)
            message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED)

            val instanceId = mConfigManager!!.pushRegistration?.instanceId
            if (!instanceId.isNullOrEmpty()) {
                message.put(MessageKey.PUSH_TOKEN, instanceId)
            }
            message.put(MessageKey.APP_STATE, appState)
            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
        } catch (e: JSONException) {
        }
    }

    open fun logProfileAction(action: String?) {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.PROFILE)
                    .timestamp(System.currentTimeMillis())
                    .build(mAppStateManager!!.session, mLocation, mConfigManager!!.mpid)

            message.put(Constants.ProfileActions.KEY, action)

            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle log event message.")
        }
    }

    open fun logUserIdentityChangeMessage(
        newIdentity: JSONObject?,
        oldIdentity: JSONObject?,
        userIdentities: JSONArray?,
        mpId: Long,
    ): BaseMPMessage? {
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.USER_IDENTITY_CHANGE)
                    .timestamp(System.currentTimeMillis())
                    .build(mAppStateManager!!.session, mLocation, mpId)
            if (newIdentity != null) {
                message.put(MessageKey.NEW_USER_IDENTITY, newIdentity)
            } else {
                message.put(MessageKey.NEW_USER_IDENTITY, JSONObject.NULL)
            }
            if (oldIdentity != null) {
                message.put(MessageKey.OLD_USER_IDENTITY, oldIdentity)
            } else {
                message.put(MessageKey.OLD_USER_IDENTITY, JSONObject.NULL)
            }
            message.put(MessageKey.USER_IDENTITIES, userIdentities)
            mMessageHandler!!.sendMessage(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
            val seenIdentities = mConfigManager!!.markIdentitiesAsSeen(userIdentities, mpId)
            if (seenIdentities != null) {
                mConfigManager!!.saveUserIdentityJson(seenIdentities, mpId)
            }
            return message
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle user-identity-change message.")
        } finally {
            mConfigManager!!.saveUserIdentityJson(userIdentities, mpId)
        }

        return null
    }

    override fun logUserAttributeChangeMessage(
        userAttributeKey: String?,
        newValue: Any?,
        oldValue: Any?,
        deleted: Boolean,
        isNewAttribute: Boolean,
        time: Long,
        mpId: Long,
    ): BaseMPMessage? {
        var newAttributeValue = newValue
        var oldAttributeValue = oldValue
        try {
            val message =
                BaseMPMessage
                    .Builder(MessageType.USER_ATTRIBUTE_CHANGE)
                    .timestamp(time)
                    .build(mAppStateManager!!.session, mLocation, mpId)
            message.put(MessageKey.NAME, userAttributeKey)

            if (newAttributeValue == null || deleted) {
                newAttributeValue = JSONObject.NULL
            } else if (newAttributeValue is List<*>) {
                val newValueArray = JSONArray()
                for (i in newAttributeValue.indices) {
                    val value = newAttributeValue[i] as String?
                    newValueArray.put(value)
                }
                newAttributeValue = newValueArray
            }
            message.put(MessageKey.NEW_ATTRIBUTE_VALUE, newAttributeValue)

            if (oldAttributeValue == null) {
                oldAttributeValue = JSONObject.NULL
            } else if (oldAttributeValue is List<*>) {
                val oldValueArray = JSONArray()
                for (i in oldAttributeValue.indices) {
                    val value = oldAttributeValue[i] as String?
                    oldValueArray.put(value)
                }
                oldAttributeValue = oldValueArray
            }
            message.put(MessageKey.OLD_ATTRIBUTE_VALUE, oldAttributeValue)

            message.put(MessageKey.ATTRIBUTE_DELETED, deleted)
            message.put(MessageKey.IS_NEW_ATTRIBUTE, isNewAttribute)
            message.put(MessageKey.USER_ATTRIBUTES, mMParticleDBManager!!.getAllUserAttributesJson(mpId))
            mMessageHandler!!.handleMessageImpl(mMessageHandler!!.obtainMessage(MessageHandler.STORE_MESSAGE, message))
            return message
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle user-attribute-change message.")
        }
        return null
    }

    @get:Throws(MParticleApiClientImpl.MPNoConfigException::class)
    override val apiKey: String?
        get() {
            val apiKey = mConfigManager!!.apiKey
            if (MPUtility.isEmpty(apiKey)) {
                throw MParticleApiClientImpl.MPNoConfigException()
            }
            return apiKey
        }

    @get:Throws(MParticleApiClientImpl.MPNoConfigException::class)
    override val uploadSettings: UploadSettings?
        get() {
            val uploadSettings = mConfigManager!!.uploadSettings
            if (uploadSettings == null) {
                throw MParticleApiClientImpl.MPNoConfigException()
            }
            return uploadSettings
        }

    @SuppressLint("MissingPermission")
    override fun delayedStart() {
        try {
            if (!delayedStartOccurred) {
                delayedStartOccurred = true
                // get the previous Intent otherwise the first few messages will have 0 for battery level
                val batteryIntent =
                    sContext!!.applicationContext.registerReceiver(
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    )
                val level = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                sBatteryLevel = level / scale.toDouble()

                sStatusBroadcastReceiver = StatusBroadcastReceiver()
                // NOTE: if permissions are not correct all messages will be tagged as 'offline'
                val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                if (MPUtility.checkPermission(sContext, Manifest.permission.ACCESS_NETWORK_STATE)) {
                    // same as with battery, get current connection so we don't have to wait for the next change
                    val connectivityManager =
                        sContext!!
                            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    setDataConnection(activeNetwork)
                    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
                sContext!!.registerReceiver(sStatusBroadcastReceiver, filter)

                InstallReferrerHelper.fetchInstallReferrer(
                    sContext!!,
                    object : InstallReferrerHelper.InstallReferrerCallback {
                        override fun onReceived(installReferrer: String?) {
                            if (sContext != null) {
                                InstallReferrerHelper.setInstallReferrer(sContext!!.applicationContext, installReferrer)
                            }
                        }

                        override fun onFailed() {
                            // do nothing, it very may well be the case that the InstallReferrer API is not available
                        }
                    },
                )
            }
        } catch (e: Exception) {
            // this can sometimes fail due to wonky-device reasons.
        }
    }

    override fun endUploadLoop() {
        mUploadHandler!!.removeMessages(UploadHandler.UPLOAD_MESSAGES)
        val instance = MParticle.getInstance()!!
        MParticle.withoutRoktApiUsage(instance::upload)
    }

    override fun checkForTrigger(message: BaseMPMessage?) {
        if (mConfigManager!!.shouldTrigger(message)) {
            mUploadHandler!!.removeMessages(UploadHandler.UPLOAD_TRIGGER_MESSAGES, mConfigManager!!.mpid)
            mUploadHandler!!.sendMessageDelayed(
                mUploadHandler!!.obtainMessage(UploadHandler.UPLOAD_TRIGGER_MESSAGES, 1, 0, mConfigManager!!.mpid),
                Constants.TRIGGER_MESSAGE_DELAY,
            )
        }
    }

    open fun refreshConfiguration() {
        mUploadHandler!!.sendEmptyMessage(UploadHandler.UPDATE_CONFIG)
    }

    open fun initConfigDelayed() {
        mUploadHandler!!.sendEmptyMessageDelayed(UploadHandler.INIT_CONFIG, (10 * 1000).toLong())
    }

    override fun log(reportingMessage: JsonReportingMessage?) {
        if (reportingMessage != null) {
            val reportingMessageList: MutableList<JsonReportingMessage> = ArrayList(1)
            reportingMessageList.add(reportingMessage)
            logAll(reportingMessageList)
        }
    }

    override fun logAll(messageList: List<JsonReportingMessage>?) {
        if (messageList != null && messageList.size > 0) {
            val development = MPUtility.isDevEnv()
            val sessionId = mAppStateManager!!.session.mSessionID
            for (i in messageList.indices) {
                messageList[i].setDevMode(development)
                messageList[i].setSessionId(sessionId)
            }
            val message =
                mMessageHandler!!.obtainMessage(
                    MessageHandler.STORE_REPORTING_MESSAGE_LIST,
                    ReportingMpidMessage(messageList, mConfigManager!!.mpid),
                )
            mMessageHandler!!.sendMessage(message)
        }
    }

    open fun getUserAttributes(listener: UserAttributeListenerWrapper?, mpId: Long): Map<String, Any?>? =
        mMParticleDBManager!!.getUserAttributes(listener, mpId)

    open fun removeUserAttribute(key: String?, mpId: Long) {
        val container = MParticleDBManager.UserAttributeRemoval()
        container.key = key
        container.time = System.currentTimeMillis()
        container.mpId = mpId
        val message = mMessageHandler!!.obtainMessage(MessageHandler.REMOVE_USER_ATTRIBUTE, container)
        mMessageHandler!!.sendMessage(message)
    }

    override fun attributeRemoved(key: String?, mpId: Long) {
        val serializedJsonArray = mConfigManager!!.getUserStorage(mpId).deletedUserAttributes
        var deletedAtributes: JSONArray
        try {
            deletedAtributes = JSONArray(serializedJsonArray)
        } catch (jse: Exception) {
            deletedAtributes = JSONArray()
        }
        deletedAtributes.put(key)
        mConfigManager!!.getUserStorage(mpId).deletedUserAttributes = deletedAtributes.toString()
    }

    open fun setUserAttribute(key: String?, value: Any?, mpId: Long) {
        setUserAttribute(key, value, mpId, false)
    }

    /**
     * this should almost always be called with "synchronously" == false, do so unless you are
     * really sure you really need to.
     */
    @Suppress("UNCHECKED_CAST")
    open fun setUserAttribute(key: String?, value: Any?, mpId: Long, synchronously: Boolean) {
        val container = MParticleDBManager.UserAttributeResponse()
        container.time = System.currentTimeMillis()
        container.mpId = mpId
        if (value is List<*>) {
            // a HashMap takes a null key, as the Java form did
            val attributeLists = HashMap<String?, List<String>>()
            container.attributeLists = attributeLists as MutableMap<String, MutableList<String>>
            attributeLists[key] = value as List<String>
        } else {
            val attributeSingles = HashMap<String?, Any?>()
            container.attributeSingles = attributeSingles as MutableMap<String, Any?>
            attributeSingles[key] = value
        }
        if (synchronously) {
            mMessageHandler!!.setUserAttributes(container)
        } else {
            val message = mMessageHandler!!.obtainMessage(MessageHandler.SET_USER_ATTRIBUTE, container)
            mMessageHandler!!.sendMessage(message)
        }
    }

    open fun incrementUserAttribute(key: String?, value: Number?, mpId: Long) {
        val incrementUserAttributeMessage = IncrementUserAttributeMessage(key, mpId, value)
        val message = mMessageHandler!!.obtainMessage(MessageHandler.INCREMENT_USER_ATTRIBUTE, incrementUserAttributeMessage)
        mMessageHandler!!.sendMessage(message)
    }

    @get:Synchronized
    override val deviceAttributes: DeviceAttributes?
        get() = mDeviceAttributes

    open fun getUserIdentities(mpId: Long): Map<MParticle.IdentityType, String>? = mConfigManager!!.getUserIdentities(mpId)

    open fun getUserIdentityJson(mpId: Long): JSONArray? = mConfigManager!!.getUserIdentityJson(mpId)

    internal fun postToMessageThread(runnable: Runnable?) {
        mMessageHandler!!.post(runnable!!)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getUserAttributes(mpId: Long): MParticleDBManager.UserAttributeResponse {
        val response = MParticleDBManager.UserAttributeResponse()
        response.attributeSingles = mMParticleDBManager!!.getUserAttributeSingles(mpId) as MutableMap<String, Any?>?
        response.attributeLists = mMParticleDBManager!!.getUserAttributeLists(mpId) as MutableMap<String, MutableList<String>>?
        return response
    }

    internal fun toStringListMap(map: Map<String, String>?): Map<String, List<String>>? {
        if (map == null) {
            return null
        }
        val listMap: MutableMap<String, List<String>> = HashMap()
        for ((key, value) in map) {
            listMap[key] = Collections.singletonList(value)
        }
        return listMap
    }

    // this return valuable is significant if it is false. That means that a legacy SDK has on record
    // that this is not the first run for this instance of the application
    internal val isFirstRunForMessageLegacy: Boolean
        get() = sPreferences!!.getBoolean("${Constants.PrefKeys.FIRSTRUN_OBSELETE}${mConfigManager!!.apiKey}", true)

    internal var isFirstRunForMessage: Boolean
        get() =
            sPreferences!!.getBoolean("${Constants.PrefKeys.FIRSTRUN_MESSAGE}${mConfigManager!!.apiKey}", true) &&
                isFirstRunForMessageLegacy
        set(firstRun) {
            sPreferences!!
                .edit()
                .putBoolean("${Constants.PrefKeys.FIRSTRUN_MESSAGE}${mConfigManager!!.apiKey}", firstRun)
                .remove("${Constants.PrefKeys.FIRSTRUN_OBSELETE}${mConfigManager!!.apiKey}")
                .apply()
        }

    internal var isFirstRunForAST: Boolean
        get() =
            sPreferences!!.getBoolean("${Constants.PrefKeys.FIRSTRUN_AST}${mConfigManager!!.apiKey}", true) &&
                isFirstRunForMessageLegacy
        set(firstRun) {
            sPreferences!!
                .edit()
                .putBoolean("${Constants.PrefKeys.FIRSTRUN_AST}${mConfigManager!!.apiKey}", firstRun)
                .remove("${Constants.PrefKeys.FIRSTRUN_OBSELETE}${mConfigManager!!.apiKey}")
                .apply()
        }

    @SuppressLint("MissingPermission")
    private inner class StatusBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(appContext: Context?, intent: Intent?) {
            try {
                if (ConnectivityManager.CONNECTIVITY_ACTION == intent!!.action) {
                    val connectivityManager =
                        appContext!!
                            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    this@MessageManager.setDataConnection(activeNetwork)
                } else if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    sBatteryLevel = level / scale.toDouble()
                }
            } catch (e: Exception) {
                // sometimes we're given a null intent,
                // or even if we have permissions to ACCESS_NETWORK_STATE, the call may fail.
            }
        }
    }

    open fun setDataConnection(activeNetwork: NetworkInfo?) {
        if (activeNetwork != null) {
            var activeNetworkName = activeNetwork.typeName
            if (0 != activeNetwork.subtype) {
                activeNetworkName = "$activeNetworkName/${activeNetwork.subtypeName}"
            }
            sActiveNetworkName = activeNetworkName.lowercase(Locale.US)
            mUploadHandler!!.setConnected(activeNetwork.isConnectedOrConnecting)
        } else {
            sActiveNetworkName = "offline"
            mUploadHandler!!.setConnected(false)
        }
    }

    open val messageHandler: Handler?
        get() = mMessageHandler

    open val mParticleDBManager: MParticleDBManager?
        get() = mMParticleDBManager

    open fun disable() {
        if (mMessageHandler != null) {
            mMessageHandler!!.disable(true)
        }
        if (mUploadHandler != null) {
            mUploadHandler!!.disable(true)
        }
    }

    open fun logAliasRequest(aliasRequest: AliasRequest?) {
        try {
            val aliasMessage = MPAliasMessage(aliasRequest, mConfigManager!!.deviceApplicationStamp, mConfigManager!!.apiKey)
            mMessageHandler!!.sendMessage(mUploadHandler!!.obtainMessage(MessageHandler.STORE_ALIAS_MESSAGE, aliasMessage))
        } catch (e: JSONException) {
            Logger.warning("Failed to create mParticle opt out message")
        }
    }

    open class ReportingMpidMessage(reportingMessages: List<JsonReportingMessage>?, mpid: Long) {
        @JvmField
        internal var mpid: Long = mpid

        @JvmField
        internal var reportingMessages: List<JsonReportingMessage>? = reportingMessages
    }

    internal fun hasDelayedStartOccurred(): Boolean = delayedStartOccurred

    internal class IncrementUserAttributeMessage(key: String?, mpid: Long?, incrementBy: Number?) {
        @JvmField
        var key: String? = key

        @JvmField
        var mpid: Long? = mpid

        @JvmField
        var incrementBy: Number? = incrementBy
    }

    companion object {
        private var sContext: Context? = null

        @JvmField
        internal var sPreferences: SharedPreferences? = null

        @JvmField
        @Volatile
        internal var devicePerformanceMetricsDisabled: Boolean = false

        /**
         * These two threads are used to do the heavy lifting.
         * The Message Handler primarly stores messages in the database.
         */
        private var sMessageHandlerThread: HandlerThread? = null

        /**
         * The upload handler thread primarily queries the database for messages to upload, and then handles network communication.
         */
        private var sUploadHandlerThread: HandlerThread? = null

        /**
         * Ideally these threads would not be started in a static initializer
         * block. but this is cleaner than checking if they have been started in
         * the constructor.
         */
        init {
            startThreads()
        }

        private fun startThreads() {
            sMessageHandlerThread =
                HandlerThread(
                    "mParticleMessageHandler",
                    Process.THREAD_PRIORITY_BACKGROUND,
                )
            sUploadHandlerThread =
                HandlerThread(
                    "mParticleUploadHandler",
                    Process.THREAD_PRIORITY_BACKGROUND,
                )
            sMessageHandlerThread!!.start()
            sUploadHandlerThread!!.start()
        }

        /**
         * This broadcast receiver is used to determine the current state of connectivity and battery level.
         * We could query these things every time a message is generated, but this should be more performant.
         */
        private var sStatusBroadcastReceiver: BroadcastReceiver? = null

        /**
         * Used to communicate the current network state at the time of message generation.
         * Also, if we don't have a network communicate, the SDK will not even try to query and upload a batch message
         * to try and save on battery life and memory.
         */
        private var sActiveNetworkName: String = "offline"

        /**
         * Keep a reference to the current battery life as populated by the BroadcastReceiver described above.
         */
        private var sBatteryLevel = 0.0

        /**
         * The app-customAttributes dictionary in each batch need to know the runtime of the SDK/app itself.
         */
        private var sStartTime: Long = MPUtility.millitime()

        /**
         * Batches/messages need to communicate the current telephony status when available.
         */
        private var sTelephonyManager: TelephonyManager? = null

        private fun getTelephonyManager(): TelephonyManager? {
            if (sTelephonyManager == null) {
                sTelephonyManager = sContext!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            }
            return sTelephonyManager
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun getStateInfo(): JSONObject {
            val infoJson = JSONObject()
            try {
                if (!devicePerformanceMetricsDisabled) {
                    infoJson.put(MessageKey.STATE_INFO_AVAILABLE_DISK, MPUtility.getAvailableInternalDisk(sContext))
                    infoJson.put(MessageKey.STATE_INFO_AVAILABLE_EXT_DISK, MPUtility.getAvailableExternalDisk(sContext))
                    val rt = Runtime.getRuntime()
                    infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_USAGE, rt.totalMemory())
                    infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_AVAIL, rt.freeMemory())
                    infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_MAX, rt.maxMemory())
                }
                infoJson.put(MessageKey.STATE_INFO_AVAILABLE_MEMORY, MPUtility.getAvailableMemory(sContext))
                infoJson.put(MessageKey.STATE_INFO_TOTAL_MEMORY, getTotalMemory())
                infoJson.put(MessageKey.STATE_INFO_BATTERY_LVL, sBatteryLevel)
                infoJson.put(MessageKey.STATE_INFO_TIME_SINCE_START, MPUtility.millitime() - sStartTime)

                val gps = MPUtility.getGpsEnabled(sContext)
                if (gps != null) {
                    infoJson.put(MessageKey.STATE_INFO_GPS, gps.toBoolean())
                }
                infoJson.put(MessageKey.STATE_INFO_DATA_CONNECTION, sActiveNetworkName)
                val orientation = MPUtility.getOrientation(sContext)
                infoJson.put(MessageKey.STATE_INFO_ORIENTATION, orientation)
                infoJson.put(MessageKey.STATE_INFO_BAR_ORIENTATION, orientation)
                infoJson.put(MessageKey.STATE_INFO_MEMORY_LOW, MPUtility.isSystemMemoryLow(sContext))
                infoJson.put(MessageKey.STATE_INFO_MEMORY_THRESHOLD, getSystemMemoryThreshold())
                val networkType = MPUtility.getNetworkType(sContext, getTelephonyManager())
                if (networkType != null) {
                    infoJson.put(MessageKey.STATE_INFO_NETWORK_TYPE, networkType)
                }
            } catch (error: OutOfMemoryError) {
                Logger.error("Out of memory")
            }
            return infoJson
        }

        @JvmStatic
        fun getTotalMemory(): Long {
            var total = sPreferences!!.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1)
            if (total < 0) {
                total = MPUtility.getTotalMemory(sContext)
                val edit = sPreferences!!.edit()
                edit.putLong(Constants.MiscStorageKeys.TOTAL_MEMORY, total)
                edit.apply()
            }
            return total
        }

        @JvmStatic
        fun getSystemMemoryThreshold(): Long {
            var threshold = sPreferences!!.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1)
            if (threshold < 0) {
                threshold = MPUtility.getSystemMemoryThreshold(sContext)
                val edit = sPreferences!!.edit()
                edit.putLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, threshold)
                edit.apply()
            }
            return threshold
        }

        @JvmStatic
        fun destroy() {
            if (sMessageHandlerThread != null) {
                sMessageHandlerThread!!.quit()
            }
            if (sUploadHandlerThread != null) {
                sUploadHandlerThread!!.quit()
            }
            startThreads()
        }
    }
}
