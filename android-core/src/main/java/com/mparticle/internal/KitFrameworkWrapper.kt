package com.mparticle.internal

import android.app.Activity
import android.content.Context
import com.mparticle.MParticleOptions
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.mparticle.internal.KitsLoadedCallback
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.AttributionResult
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.consent.ConsentState
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.internal.listeners.InternalListenerManager
import org.json.JSONArray
import java.lang.Exception
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.*

open class KitFrameworkWrapper(
    context: Context?,
    reportingManager: ReportingManager?,
    configManager: ConfigManager,
    appStateManager: AppStateManager,
    testing: Boolean,
    private val mOptions: MParticleOptions?
) : KitManager {
    private val mContext: Context?
    val mCoreCallbacks: CoreCallbacks
    private val mReportingManager: ReportingManager?
    @JvmField
    var mKitManager: KitManager? = null

    @Volatile
    var frameworkLoadAttempted = false
        private set
    var eventQueue: Queue<Any>? = null
        private set
    var attributeQueue: Queue<AttributeChange>? = null
        private set

    @Volatile
    private var registerForPush = false

    constructor(
        context: Context?,
        reportingManager: ReportingManager?,
        configManager: ConfigManager,
        appStateManager: AppStateManager,
        options: MParticleOptions?
    ) : this(context, reportingManager, configManager, appStateManager, false, options) {
    }

    init {
        mContext = if (testing) context else KitContext(context)
        mReportingManager = reportingManager
        mCoreCallbacks = CoreCallbacksImpl(this, configManager, appStateManager)
        Companion.kitsLoaded = false
    }

    @WorkerThread
    open fun loadKitLibrary() {
        if (!frameworkLoadAttempted) {
            Logger.debug("Loading Kit Framework.")
            frameworkLoadAttempted = true
            try {
                val clazz = Class.forName("com.mparticle.kits.KitManagerImpl")
                val constructor: Constructor<KitFrameworkWrapper> = clazz.getConstructor(
                    Context::class.java,
                    ReportingManager::class.java,
                    CoreCallbacks::class.java,
                    MParticleOptions::class.java
                ) as Constructor<KitFrameworkWrapper>
                val kitManager: KitManager =
                    constructor.newInstance(mContext, mReportingManager, mCoreCallbacks, mOptions)
                val configuration = mCoreCallbacks.latestKitConfiguration
                Logger.debug("Kit Framework loaded.")
                if (!MPUtility.isEmpty(configuration)) {
                    Logger.debug("Restoring previous Kit configuration.")
                    kitManager
                        .updateKits(configuration)
                        .onKitsLoaded(
                            object : OnKitManagerLoaded {
                                override fun onKitManagerLoaded() {
                                    mKitManager = kitManager
                                    kitsLoaded = true
                                }
                            }
                        )
                } else {
                    mKitManager = kitManager
                }
                updateDataplan(mCoreCallbacks.dataplanOptions)
            } catch (e: Exception) {
                Logger.debug("No Kit Framework detected.")
                kitsLoaded = true
            }
        }
    }

    fun setKitManager(manager: KitManager?) {
        mKitManager = manager
    }

    var kitsLoaded: Boolean = false
        get() = Companion.kitsLoaded
        set(kitsLoaded) {
            field = kitsLoaded
            if (kitsLoaded) {
                replayAndDisableQueue()
            } else {
                disableQueuing()
            }
            val kitsLoadedListenersCopy: List<KitsLoadedListener> = ArrayList(kitsLoadedListeners)
            for (kitsLoadedListener in kitsLoadedListenersCopy) {
                kitsLoadedListener?.onKitsLoaded()
            }
            kitsLoadedListeners.clear()
        }

    fun addKitsLoadedListener(listener: KitsLoadedListener?) {
        if (listener != null) {
            if (Companion.kitsLoaded) {
                listener.onKitsLoaded()
            } else {
                kitsLoadedListeners.add(listener)
            }
        }
    }

    @Synchronized
    fun disableQueuing() {
        if (eventQueue != null) {
            eventQueue!!.clear()
            eventQueue = null
            Logger.debug("Kit initialization complete. Disabling event queueing.")
        }
        if (attributeQueue != null) {
            attributeQueue!!.clear()
            attributeQueue = null
        }
    }

    fun replayEvents() {
        if (mKitManager == null) {
            return
        }
        mKitManager!!.onSessionStart()
        if (registerForPush) {
            val instanceId = mCoreCallbacks.pushInstanceId
            val senderId = mCoreCallbacks.pushSenderId
            if (!MPUtility.isEmpty(instanceId)) {
                mKitManager!!.onPushRegistration(instanceId, senderId)
            }
        }
        if (eventQueue != null && eventQueue!!.size > 0) {
            Logger.debug("Replaying events after receiving first kit configuration.")
            for (event in eventQueue!!) {
                if (event is MPEvent) {
                    val mpEvent = event
                    if (mpEvent.isScreenEvent) {
                        mKitManager!!.logScreen(mpEvent)
                    } else {
                        mKitManager!!.logEvent(mpEvent)
                    }
                } else if (event is BaseEvent) {
                    mKitManager!!.logEvent(event)
                }
            }
        }
        if (attributeQueue != null && attributeQueue!!.size > 0) {
            Logger.debug("Replaying user attributes after receiving first kit configuration.")
            for (attributeChange in attributeQueue!!) {
                when (attributeChange.type) {
                    AttributeChange.SET_ATTRIBUTE -> if (attributeChange.value == null) {
                        mKitManager!!.setUserAttribute(
                            attributeChange.key!!,
                            null,
                            attributeChange.mpid
                        )
                    } else if (attributeChange.value is String) {
                        mKitManager!!.setUserAttribute(
                            attributeChange.key!!,
                            attributeChange.value as String?,
                            attributeChange.mpid
                        )
                    } else if (attributeChange.value is List<*>) {
                        mKitManager!!.setUserAttributeList(
                            attributeChange.key!!,
                            attributeChange.value as List<String?>?,
                            attributeChange.mpid
                        )
                    }
                    AttributeChange.REMOVE_ATTRIBUTE -> mKitManager!!.removeUserAttribute(
                        attributeChange.key!!, attributeChange.mpid
                    )
                    AttributeChange.INCREMENT_ATTRIBUTE -> if (attributeChange.value is String) {
                        mKitManager!!.incrementUserAttribute(
                            attributeChange.key!!,
                            attributeChange.incrementedBy!!,
                            attributeChange.value as String?,
                            attributeChange.mpid
                        )
                    }
                    AttributeChange.TAG -> mKitManager!!.setUserTag(
                        attributeChange.key!!,
                        attributeChange.mpid
                    )
                }
            }
        }
    }

    @Synchronized
    fun replayAndDisableQueue() {
        replayEvents()
        disableQueuing()
    }

    @Synchronized
    fun queueEvent(event: Any?): Boolean {
        if (kitsLoaded) {
            return false
        }
        if (eventQueue == null) {
            eventQueue = ConcurrentLinkedQueue<Any>()
        }
        //It's an edge case to even need this, so 10
        //should be enough.
        if (eventQueue!!.size < 10) {
            Logger.debug("Queuing Kit event while waiting for initial configuration.")
            eventQueue!!.add(event)
        }
        return true
    }

    fun queueAttributeRemove(key: String?, mpid: Long): Boolean {
        return queueAttribute(AttributeChange(key, mpid))
    }

    fun queueAttributeSet(key: String?, value: Any?, mpid: Long): Boolean {
        return queueAttribute(AttributeChange(key, value, mpid, AttributeChange.SET_ATTRIBUTE))
    }

    fun queueAttributeTag(key: String?, mpid: Long): Boolean {
        return queueAttribute(AttributeChange(key, mpid, AttributeChange.TAG))
    }

    fun queueAttributeIncrement(
        key: String?,
        incrementedBy: Number?,
        newValue: String?,
        mpid: Long
    ): Boolean {
        return queueAttribute(AttributeChange(key, incrementedBy, newValue, mpid))
    }

    @Synchronized
    fun queueAttribute(change: AttributeChange): Boolean {
        if (kitsLoaded) {
            return false
        }
        if (attributeQueue == null) {
            attributeQueue = ConcurrentLinkedQueue()
        }
        attributeQueue!!.add(change)
        return true
    }

    override fun getSurveyUrl(
        kitId: Int,
        userAttributes: Map<String, String?>?,
        userAttributeLists: Map<String, List<String?>?>?
    ): Uri? {
        return if (mKitManager != null) {
            mKitManager!!.getSurveyUrl(kitId, userAttributes, userAttributeLists)
        } else null
    }

    class AttributeChange {
        val key: String?
        val value: Any?
        val mpid: Long
        val type: Int
        var incrementedBy: Number? = null

        constructor(key: String?, mpid: Long) {
            this.key = key
            value = null
            this.mpid = mpid
            type = REMOVE_ATTRIBUTE
        }

        constructor(key: String?, value: Any?, mpid: Long, type: Int) {
            this.key = key
            this.value = value
            this.mpid = mpid
            this.type = type
        }

        constructor(key: String?, mpid: Long, type: Int) {
            this.key = key
            value = null
            this.mpid = mpid
            this.type = type
        }

        constructor(key: String?, incrementedBy: Number?, newValue: String?, mpid: Long) {
            this.key = key
            value = newValue
            this.incrementedBy = incrementedBy
            this.mpid = mpid
            type = INCREMENT_ATTRIBUTE
        }

        companion object {
            const val REMOVE_ATTRIBUTE = 1
            const val SET_ATTRIBUTE = 2
            const val INCREMENT_ATTRIBUTE = 3
            const val TAG = 4
        }
    }

    override val currentActivity: WeakReference<Activity>
        get() = mCoreCallbacks.currentActivity

    override fun logEvent(event: BaseEvent) {
        if (!queueEvent(event) && mKitManager != null) {
            mKitManager!!.logEvent(event)
        }
    }

    override fun logScreen(screenEvent: MPEvent) {
        if (!queueEvent(screenEvent) && mKitManager != null) {
            mKitManager!!.logScreen(screenEvent)
        }
    }

    override fun logBatch(jsonObject: String) {
        if (mKitManager != null) {
            mKitManager!!.logBatch(jsonObject)
        }
    }

    override fun leaveBreadcrumb(breadcrumb: String) {
        if (mKitManager != null) {
            mKitManager!!.leaveBreadcrumb(breadcrumb)
        }
    }

    override fun logError(message: String, eventData: Map<String, String?>?) {
        if (mKitManager != null) {
            mKitManager!!.logError(message, eventData)
        }
    }

    override fun logNetworkPerformance(
        url: String,
        startTime: Long,
        method: String,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
        responseCode: Int
    ) {
        if (mKitManager != null) {
            mKitManager!!.logNetworkPerformance(
                url,
                startTime,
                method,
                length,
                bytesSent,
                bytesReceived,
                requestString,
                responseCode
            )
        }
    }

    override fun logException(
        exception: Exception,
        eventData: Map<String, String?>?,
        message: String?
    ) {
        if (mKitManager != null) {
            mKitManager!!.logException(exception, eventData, message)
        }
    }

    override fun setLocation(location: Location?) {
        if (mKitManager != null) {
            mKitManager!!.setLocation(location)
        }
    }

    override fun logout() {
        if (mKitManager != null) {
            mKitManager!!.logout()
        }
    }

    override fun setUserAttribute(key: String, value: String?, mpid: Long) {
        if (!queueAttributeSet(key, value, mpid) && mKitManager != null) {
            mKitManager!!.setUserAttribute(key, value, mpid)
        }
    }

    override fun setUserAttributeList(key: String, value: List<String?>?, mpid: Long) {
        if (!queueAttributeSet(key, value, mpid) && mKitManager != null) {
            mKitManager!!.setUserAttributeList(key, value, mpid)
        }
    }

    override fun removeUserAttribute(key: String, mpid: Long) {
        if (!queueAttributeRemove(key, mpid) && mKitManager != null) {
            mKitManager!!.removeUserAttribute(key, mpid)
        }
    }

    override fun setUserTag(tag: String, mpid: Long) {
        if (!queueAttributeTag(tag, mpid) && mKitManager != null) {
            mKitManager!!.setUserTag(tag, mpid)
        }
    }

    override fun incrementUserAttribute(
        key: String,
        incrementValue: Number,
        newValue: String?,
        mpid: Long
    ) {
        if (!queueAttributeIncrement(key, incrementValue, newValue, mpid) && mKitManager != null) {
            mKitManager!!.incrementUserAttribute(key, incrementValue, newValue, mpid)
        }
    }

    override fun setUserIdentity(id: String, identityType: MParticle.IdentityType) {
        if (mKitManager != null) {
            mKitManager!!.setUserIdentity(id, identityType)
        }
    }

    override fun removeUserIdentity(id: MParticle.IdentityType) {
        if (mKitManager != null) {
            mKitManager!!.removeUserIdentity(id)
        }
    }

    override fun setOptOut(optOutStatus: Boolean) {
        if (mKitManager != null) {
            mKitManager!!.setOptOut(optOutStatus)
        }
    }

    override fun onMessageReceived(context: Context, intent: Intent): Boolean {
        return if (mKitManager != null) {
            mKitManager!!.onMessageReceived(context, intent)
        } else false
    }

    override fun onPushRegistration(instanceId: String?, senderId: String?): Boolean {
        if (kitsLoaded && mKitManager != null) {
            mKitManager!!.onPushRegistration(instanceId, senderId)
        } else {
            registerForPush = true
        }
        return false
    }

    override fun isKitActive(kitId: Int): Boolean {
        return if (mKitManager != null) {
            mKitManager!!.isKitActive(kitId)
        } else false
    }

    override fun getKitInstance(kitId: Int): Any? {
        return if (mKitManager != null) {
            mKitManager!!.getKitInstance(kitId)
        } else null
    }

    override val supportedKits: Set<Int>
        get() = if (mKitManager != null) {
            mKitManager!!.supportedKits
        } else mutableSetOf()

    override fun updateKits(kitConfiguration: JSONArray?): KitsLoadedCallback {
        val kitsLoadedCallback = KitsLoadedCallback()
        if (mKitManager != null) {
            // we may have initialized the KitManagerImpl but didn't have a cached config to initialize
            // any kits with. In this case, we will wait until this next config update to replay + disable queueing
            if (!Companion.kitsLoaded) {
                mKitManager!!
                    .updateKits(kitConfiguration)
                    .onKitsLoaded(
                        object : OnKitManagerLoaded {
                            override fun onKitManagerLoaded() {
                                kitsLoaded = true
                                kitsLoadedCallback.setKitsLoaded()
                            }
                        }
                    )
            } else {
                return mKitManager!!.updateKits(kitConfiguration)
            }
        }
        return kitsLoadedCallback
    }

    override fun updateDataplan(dataplanOptions: DataplanOptions?) {
        if (mKitManager != null) {
            mKitManager!!.updateDataplan(dataplanOptions)
        }
    }

    override val kitStatus: Map<Int, KitStatus>
        get() = if (mKitManager != null) {
            mKitManager!!.kitStatus
        } else {
            HashMap()
        }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (mKitManager != null) {
            mKitManager!!.onActivityCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (mKitManager != null) {
            mKitManager!!.onActivityStarted(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (mKitManager != null) {
            mKitManager!!.onActivityResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (mKitManager != null) {
            mKitManager!!.onActivityPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (mKitManager != null) {
            mKitManager!!.onActivityStopped(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
        if (mKitManager != null) {
            mKitManager!!.onActivitySaveInstanceState(activity, outState)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (mKitManager != null) {
            mKitManager!!.onActivityDestroyed(activity)
        }
    }

    override fun onSessionEnd() {
        if (mKitManager != null) {
            mKitManager!!.onSessionEnd()
        }
    }

    override fun onSessionStart() {
        if (mKitManager != null) {
            mKitManager!!.onSessionStart()
        }
    }

    override fun installReferrerUpdated() {
        if (mKitManager != null) {
            mKitManager!!.installReferrerUpdated()
        }
    }

    override fun onApplicationForeground() {
        if (mKitManager != null) {
            mKitManager!!.onApplicationForeground()
        }
    }

    override fun onApplicationBackground() {
        if (mKitManager != null) {
            mKitManager!!.onApplicationBackground()
        }
    }

    override val attributionResults: Map<Int, AttributionResult>
        get() = if (mKitManager != null) {
            mKitManager!!.attributionResults
        } else TreeMap()

    override fun onIdentifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
        if (mKitManager != null) {
            mKitManager!!.onIdentifyCompleted(user, request)
        }
    }

    override fun onLoginCompleted(user: MParticleUser, request: IdentityApiRequest) {
        if (mKitManager != null) {
            mKitManager!!.onLoginCompleted(user, request)
        }
    }

    override fun onLogoutCompleted(user: MParticleUser, request: IdentityApiRequest) {
        if (mKitManager != null) {
            mKitManager!!.onLogoutCompleted(user, request)
        }
    }

    override fun onModifyCompleted(user: MParticleUser?, request: IdentityApiRequest) {
        if (mKitManager != null) {
            mKitManager!!.onModifyCompleted(user, request)
        }
    }

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        mpid: Long
    ) {
        if (mKitManager != null) {
            mKitManager!!.onConsentStateUpdated(oldState, newState, mpid)
        }
    }

    override fun reset() {
        if (mKitManager != null) {
            mKitManager!!.reset()
        }
    }

    internal class CoreCallbacksImpl(
        var mKitFrameworkWrapper: KitFrameworkWrapper,
        var mConfigManager: ConfigManager,
        var mAppStateManager: AppStateManager
    ) : CoreCallbacks {
        override fun isBackgrounded(): Boolean {
            return mAppStateManager.isBackgrounded
        }

        override fun getUserBucket(): Int {
            return mConfigManager.userBucket
        }

        override fun isEnabled(): Boolean {
            return mConfigManager.isEnabled
        }

        override fun setIntegrationAttributes(
            kitId: Int,
            integrationAttributes: Map<String, String>
        ) {
            mConfigManager.setIntegrationAttributes(kitId, integrationAttributes)
        }

        override fun getIntegrationAttributes(kitId: Int): Map<String, String> {
            return mConfigManager.getIntegrationAttributes(kitId)
        }

        override fun getCurrentActivity(): WeakReference<Activity> {
            return mAppStateManager.currentActivity
        }

        override fun getLatestKitConfiguration(): JSONArray {
            return mConfigManager.latestKitConfiguration!!
        }

        override fun getDataplanOptions(): DataplanOptions {
            return mConfigManager.dataplanOptions
        }

        override fun isPushEnabled(): Boolean {
            return mConfigManager.isPushEnabled
        }

        override fun getPushSenderId(): String {
            return mConfigManager.pushSenderId
        }

        override fun getPushInstanceId(): String {
            return mConfigManager.pushInstanceId!!
        }

        override fun getLaunchUri(): Uri {
            return mAppStateManager.launchUri
        }

        override fun getLaunchAction(): String {
            return mAppStateManager.launchAction
        }

        override fun getKitListener(): KitListener {
            return kitListener
        }

        private val kitListener: KitListener = object : KitListener {
            override fun kitFound(kitId: Int) {
                InternalListenerManager.getListener().onKitDetected(kitId)
            }

            override fun kitConfigReceived(kitId: Int, configuration: String) {
                InternalListenerManager.getListener().onKitConfigReceived(kitId, configuration)
            }

            override fun kitExcluded(kitId: Int, reason: String) {
                InternalListenerManager.getListener().onKitExcluded(kitId, reason)
            }

            override fun kitStarted(kitId: Int) {
                InternalListenerManager.getListener().onKitStarted(kitId)
            }

            override fun onKitApiCalled(kitId: Int, used: Boolean, vararg objects: Any) {
                InternalListenerManager.getListener().onKitApiCalled(kitId, used, *objects)
            }

            override fun onKitApiCalled(
                methodName: String,
                kitId: Int,
                used: Boolean,
                vararg objects: Any
            ) {
                InternalListenerManager.getListener()
                    .onKitApiCalled(methodName, kitId, used, *objects)
            }
        }
    }

    companion object {
        @Volatile
        private var kitsLoaded = false
        private val kitsLoadedListeners: MutableList<KitsLoadedListener> = ArrayList()
    }
}