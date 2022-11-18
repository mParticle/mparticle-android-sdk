package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.MainThread
import com.mparticle.AttributionError
import com.mparticle.AttributionListener
import com.mparticle.AttributionResult
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.UserAttributeListener
import com.mparticle.commerce.CommerceEvent
import com.mparticle.consent.ConsentState
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityStateListener
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Constants
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.KitManager
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.internal.KitsLoadedCallback
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.ReportingManager
import com.mparticle.kits.KitIntegration.ActivityListener
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.BatchListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.KitIntegration.SessionListener
import com.mparticle.kits.ReportingMessage.ProjectionReport
import com.mparticle.kits.mappings.CustomMapping
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.LinkedList
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

open class KitManagerImpl( // ================================================================================
    // KitIntegration.EventListener forwarding
    // ================================================================================
    val context: Context,
    private val reportingManager: ReportingManager,
    val mCoreCallbacks: CoreCallbacks,
    options: MParticleOptions?
) : KitManager, AttributionListener, UserAttributeListener, IdentityStateListener {
    private var mKitHandler: Handler? = null
    var mKitIntegrationFactory: KitIntegrationFactory?
    private var mDataplanFilter = DataplanFilterImpl.EMPTY
    private var mKitOptions: KitOptions? = null

    @Volatile
    private var kitConfigurations: List<KitConfiguration> = ArrayList()
    private val mAttributionResultsMap: MutableMap<Int, AttributionResult> = TreeMap()
    private val kitsLoadedListeners = ArrayList<KitsLoadedListener>()
    var providers = ConcurrentHashMap<Int, KitIntegration?>()

    init {
        mKitIntegrationFactory = KitIntegrationFactory()
        val instance = MParticle.getInstance()
        instance?.Identity()?.addIdentityStateListener(this)
        if (options != null) {
            for (configuration in options.getConfigurationsForTarget(this.javaClass)) {
                configuration.apply(this)
            }
        }
        initializeKitIntegrationFactory()
    }

    fun setKitOptions(kitOptions: KitOptions?) {
        mKitOptions = kitOptions
    }

    /**
     * Need this method so that we can override it during unit tests.
     */
    @Throws(JSONException::class)
    protected open fun createKitConfiguration(configuration: JSONObject?): KitConfiguration {
        return KitConfiguration.createKitConfiguration(configuration)
    }

    fun setKitFactory(kitIntegrationFactory: KitIntegrationFactory?) {
        mKitIntegrationFactory = kitIntegrationFactory
    }

    val isBackgrounded: Boolean
        get() = mCoreCallbacks.isBackgrounded
    open val userBucket: Int
        get() = mCoreCallbacks.userBucket
    val isOptedOut: Boolean
        get() = !mCoreCallbacks.isEnabled
    val launchUri: Uri
        get() = mCoreCallbacks.launchUri
    val launchAction: String
        get() = mCoreCallbacks.launchAction

    fun setIntegrationAttributes(
        kitIntegration: KitIntegration,
        integrationAttributes: Map<String?, String?>?
    ) {
        mCoreCallbacks.setIntegrationAttributes(
            kitIntegration.configuration.kitId,
            integrationAttributes
        )
    }

    fun getIntegrationAttributes(kitIntegration: KitIntegration): Map<String, String> {
        return mCoreCallbacks.getIntegrationAttributes(kitIntegration.configuration.kitId)
    }

    fun clearIntegrationAttributes(kitIntegration: KitIntegration) {
        setIntegrationAttributes(kitIntegration, null)
    }

    override fun updateKits(kitConfigs: JSONArray?): KitsLoadedCallback? {
        val callback = KitsLoadedCallback()
        runOnKitThread {
            kitConfigurations = parseKitConfigurations(kitConfigs)
            runOnMainThread {
                configureKits(kitConfigurations)
                callback.setKitsLoaded()
            }
        }
        return callback
    }

    @MainThread
    fun reloadKits() {
        configureKits(kitConfigurations)
    }

    override fun updateDataplan(dataplanOptions: DataplanOptions?) {
        if (dataplanOptions != null) {
            mDataplanFilter = try {
                Logger.info("Updating Data Plan")
                Logger.debug(dataplanOptions.toString())
                DataplanFilterImpl(dataplanOptions)
            } catch (ex: Exception) {
                Logger.warning(
                    ex,
                    "Failed to parse DataplanOptions, Dataplan filtering for Kits will not be applied"
                )
                DataplanFilterImpl.EMPTY
            }
        } else {
            mDataplanFilter = DataplanFilterImpl.EMPTY
            Logger.info("Clearing Data Plan")
        }
    }

    /**
     * Update the current list of active kits based on server (or cached) configuration.
     *
     *
     * Note: This method is meant to always be run on the main thread.
     */
    @Synchronized
    protected fun configureKits(kitConfigurations: List<KitConfiguration>) {
        var kitConfigurations = kitConfigurations
        if (kitConfigurations == null) {
            kitConfigurations = ArrayList()
        }
        val instance = MParticle.getInstance()
            ?: // if MParticle has been dereferenced, abandon ship. This will run again when it is restarted
            return
        val user = instance.Identity().currentUser
        val activeIds = HashSet<Int>()
        val previousKits = HashMap(providers)
        if (kitConfigurations != null) {
            for (configuration in kitConfigurations) {
                try {
                    val currentModuleID = configuration.kitId
                    if (configuration.shouldExcludeUser(user)) {
                        mCoreCallbacks.kitListener.kitExcluded(
                            currentModuleID,
                            "User was required to be known, but was not."
                        )
                        continue
                    }
                    if (!mKitIntegrationFactory!!.isSupported(configuration.kitId)) {
                        Logger.debug("Kit id configured but is not bundled: $currentModuleID")
                        continue
                    }
                    var activeKit = providers[currentModuleID]
                    if (activeKit == null) {
                        activeKit = mKitIntegrationFactory!!.createInstance(
                            this@KitManagerImpl,
                            configuration
                        )
                        if (activeKit.isDisabled ||
                            !configuration.shouldIncludeFromConsentRules(user)
                        ) {
                            Logger.debug("Kit id configured but is filtered or disabled: $currentModuleID")
                            continue
                        }
                        activeIds.add(currentModuleID)
                        initializeKit(activeKit)
                        providers[currentModuleID] = activeKit
                        mCoreCallbacks.kitListener.kitStarted(currentModuleID)
                    } else {
                        activeKit.configuration = configuration
                        if (activeKit.isDisabled ||
                            !configuration.shouldIncludeFromConsentRules(user)
                        ) {
                            continue
                        }
                        activeIds.add(currentModuleID)
                        activeKit.onSettingsUpdated(configuration.settings)
                    }
                } catch (e: Exception) {
                    mCoreCallbacks.kitListener.kitExcluded(
                        configuration.kitId,
                        "exception while starting. Exception: " + e.message
                    )
                    Logger.error("Exception while starting kit " + configuration.kitId + ": " + e.message)
                }
            }
        }
        val ids = providers.keys.iterator()
        while (ids.hasNext()) {
            val id = ids.next()
            if (!activeIds.contains(id)) {
                val integration = providers[id]
                if (integration != null) {
                    Logger.debug("De-initializing kit: " + integration.name)
                    clearIntegrationAttributes(integration)
                    integration.onKitDestroy()
                    integration.onKitCleanup()
                }
                ids.remove()
                val intent = Intent(MParticle.ServiceProviders.BROADCAST_DISABLED + id)
                context.sendBroadcast(intent)
            }
        }
        onKitsLoaded(HashMap(providers), previousKits, ArrayList(kitConfigurations))
    }

    private fun initializeKit(activeKit: KitIntegration?) {
        Logger.debug("Initializing kit: " + activeKit!!.name)
        activeKit.onKitCreate(activeKit.configuration.settings, context)
        if (activeKit is ActivityListener) {
            val activityWeakReference = currentActivity
            if (activityWeakReference != null) {
                val activity = activityWeakReference.get()
                if (activity != null) {
                    val listener = activeKit as ActivityListener
                    reportingManager.logAll(
                        listener.onActivityCreated(activity, null)
                    )
                    reportingManager.logAll(
                        listener.onActivityStarted(activity)
                    )
                    reportingManager.logAll(
                        listener.onActivityResumed(activity)
                    )
                }
            }
        }
        if (activeKit is AttributeListener) {
            syncUserIdentities(activeKit as AttributeListener, activeKit.configuration)
        }
        val instance = MParticle.getInstance()
        if (instance != null) {
            instance.installReferrer?.let {
                getMockInstallReferrerIntent(it)?.let { activeKit.setInstallReferrer(it) }
            }
        }
        if (activeKit is PushListener) {
            val senderId = mCoreCallbacks.pushSenderId
            val instanceId = mCoreCallbacks.pushInstanceId
            if (!MPUtility.isEmpty(instanceId)) {
                if ((activeKit as PushListener).onPushRegistration(instanceId, senderId)) {
                    val message = ReportingMessage.fromPushRegistrationMessage(activeKit)
                    reportingManager.log(message)
                }
            }
        }
        val intent =
            Intent(MParticle.ServiceProviders.BROADCAST_ACTIVE + activeKit.configuration.kitId)
        context.sendBroadcast(intent)
    }

    override val kitStatus: Map<Int, KitStatus>
        get() {
            val kitStatusMap: MutableMap<Int, KitStatus> = HashMap()
            for (kitId in mKitIntegrationFactory!!.getSupportedKits()) {
                kitStatusMap[kitId] = KitStatus.NOT_CONFIGURED
            }
            for (kitConfiguration in kitConfigurations) {
                kitStatusMap[kitConfiguration.kitId] = KitStatus.STOPPED
            }
            for ((key, value) in providers) {
                if (!value!!.isDisabled) {
                    kitStatusMap[key] = KitStatus.ACTIVE
                }
            }
            return kitStatusMap
        }

    override fun isKitActive(serviceProviderId: Int): Boolean {
        val provider = providers[serviceProviderId]
        return provider != null && !provider.isDisabled
    }

    override fun getKitInstance(kitId: Int): Any? {
        val kit = providers[kitId]
        return kit?.instance
    }

    // ================================================================================
    // General KitIntegration forwarding
    // ================================================================================
    override fun setLocation(location: Location?) {
        for (provider in providers.values) {
            try {
                if (!provider!!.isDisabled) {
                    provider.setLocation(location)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        true,
                        location
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call setLocation for kit: " + provider!!.name + ": " + e.message)
            }
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
        for (provider in providers.values) {
            try {
                if (!provider!!.isDisabled) {
                    val report = provider.logNetworkPerformance(
                        url,
                        startTime,
                        method,
                        length,
                        bytesSent,
                        bytesReceived,
                        requestString,
                        responseCode
                    )
                    reportingManager.logAll(report)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        !MPUtility.isEmpty(report),
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
            } catch (e: Exception) {
                Logger.warning("Failed to call logNetworkPerformance for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun setOptOut(optOutStatus: Boolean) {
        for (provider in providers.values) {
            try {
                if (!provider!!.isDisabled(true)) {
                    val messages = provider.setOptOut(optOutStatus)
                    reportingManager.logAll(messages)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        !MPUtility.isEmpty(messages),
                        optOutStatus
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call setOptOut for kit: " + provider!!.name + ": " + e.message)
            }
        }
        reloadKits()
    }

    override val supportedKits: Set<Int>
        get() = mKitIntegrationFactory!!.getSupportedKits()

    override fun logEvent(event: BaseEvent) {
        var event: BaseEvent? = event
        if (mDataplanFilter != null) {
            event = mDataplanFilter.transformEventForEvent(event)
            if (event == null) {
                return
            }
        }
        for (provider in providers.values) {
            try {
                val messages = provider!!.logBaseEvent(
                    event!!
                )
                mCoreCallbacks.kitListener.onKitApiCalled(
                    provider.configuration.kitId,
                    !MPUtility.isEmpty(messages),
                    event
                )
                reportingManager.logAll(messages)
            } catch (e: Exception) {
                Logger.warning("Failed to call logMPEvent for kit: " + provider!!.name + ": " + e.message)
                mCoreCallbacks.kitListener.onKitApiCalled(
                    provider.configuration.kitId,
                    false,
                    event,
                    e
                )
            }
        }
        if (event is MPEvent) {
            logMPEvent(event)
        } else if (event is CommerceEvent) {
            logCommerceEvent(event as CommerceEvent?)
        }
    }

    // ================================================================================
    // KitIntegration.CommerceListener forwarding
    // ================================================================================
    protected open fun logCommerceEvent(event: CommerceEvent?) {
        for (provider in providers.values) {
            try {
                if (!provider!!.isDisabled) {
                    val filteredEvent = provider.configuration.filterCommerceEvent(event)
                    if (filteredEvent != null) {
                        if (provider is CommerceListener) {
                            val projectedEvents = CustomMapping.projectEvents(
                                filteredEvent,
                                provider.configuration.customMappingList,
                                provider.configuration.defaultCommerceCustomMapping
                            )
                            if (projectedEvents != null && projectedEvents.size > 0) {
                                val masterMessage =
                                    ReportingMessage.fromEvent(provider, filteredEvent)
                                var forwarded = false
                                for (i in projectedEvents.indices) {
                                    val result = projectedEvents[i]
                                    var report: List<ReportingMessage?>? = null
                                    var messageType: String? = null
                                    if (result.mpEvent != null) {
                                        val projectedEvent = projectedEvents[i].mpEvent
                                        report =
                                            (provider as KitIntegration.EventListener).logEvent(
                                                projectedEvent
                                            )
                                        mCoreCallbacks.kitListener.onKitApiCalled(
                                            "logMPEvent()",
                                            provider.configuration.kitId,
                                            !MPUtility.isEmpty(report),
                                            projectedEvent
                                        )
                                        messageType = ReportingMessage.MessageType.EVENT
                                    } else {
                                        val projectedEvent = projectedEvents[i].commerceEvent
                                        report =
                                            (provider as CommerceListener).logEvent(projectedEvent)
                                        mCoreCallbacks.kitListener.onKitApiCalled(
                                            "logMPEvent()",
                                            provider.configuration.kitId,
                                            !MPUtility.isEmpty(report),
                                            projectedEvent
                                        )
                                        messageType = ReportingMessage.MessageType.COMMERCE_EVENT
                                    }
                                    if (report != null && report.size > 0) {
                                        forwarded = true
                                        for (message in report) {
                                            masterMessage.addProjectionReport(
                                                ProjectionReport(
                                                    projectedEvents[i].projectionId,
                                                    messageType,
                                                    message!!.eventName,
                                                    message.eventTypeString
                                                )
                                            )
                                        }
                                    }
                                }
                                if (forwarded) {
                                    reportingManager.log(masterMessage)
                                }
                            } else {
                                val reporting =
                                    (provider as CommerceListener).logEvent(filteredEvent)
                                mCoreCallbacks.kitListener.onKitApiCalled(
                                    "logMPEvent()",
                                    provider.configuration.kitId,
                                    !MPUtility.isEmpty(reporting),
                                    filteredEvent
                                )
                                if (reporting != null && reporting.size > 0) {
                                    reportingManager.log(
                                        ReportingMessage.fromEvent(provider, filteredEvent)
                                    )
                                }
                            }
                        } else if (provider is KitIntegration.EventListener) {
                            val events = CommerceEventUtils.expand(filteredEvent)
                            var forwarded = false
                            if (events != null) {
                                for (expandedEvent in events) {
                                    val reporting =
                                        (provider as KitIntegration.EventListener).logEvent(
                                            expandedEvent
                                        )
                                    mCoreCallbacks.kitListener.onKitApiCalled(
                                        "logMPEvent()",
                                        provider.configuration.kitId,
                                        !MPUtility.isEmpty(reporting),
                                        expandedEvent
                                    )
                                    forwarded = forwarded || reporting != null && reporting.size > 0
                                }
                            }
                            if (forwarded) {
                                reportingManager.log(
                                    ReportingMessage.fromEvent(provider, filteredEvent)
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logCommerceEvent for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    // ================================================================================
    // KitIntegration.PushListener forwarding
    // ================================================================================
    override fun onMessageReceived(context: Context?, intent: Intent?): Boolean {
        for (provider in providers.values) {
            if (provider is PushListener) {
                try {
                    if (!provider.isDisabled) {
                        val willHandlePush =
                            (provider as PushListener).willHandlePushMessage(intent)
                        mCoreCallbacks.kitListener.onKitApiCalled(
                            "willHandlePushMessage()",
                            provider.configuration.kitId,
                            willHandlePush,
                            intent
                        )
                        if (willHandlePush) {
                            (provider as PushListener).onPushMessageReceived(context, intent)
                            mCoreCallbacks.kitListener.onKitApiCalled(
                                "onPushMessageReceived()",
                                provider.configuration.kitId,
                                null,
                                context,
                                intent
                            )
                            val message = ReportingMessage.fromPushMessage(provider, intent)
                            reportingManager.log(message)
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Logger.warning("Failed to call onPushMessageReceived for kit: " + provider.name + ": " + e.message)
                }
            }
        }
        return false
    }

    override fun onPushRegistration(token: String?, senderId: String?): Boolean {
        for (provider in providers.values) {
            if (provider is PushListener) {
                try {
                    if (!provider.isDisabled) {
                        val onPushRegistration =
                            (provider as PushListener).onPushRegistration(token, senderId)
                        mCoreCallbacks.kitListener.onKitApiCalled(
                            provider.configuration.kitId,
                            onPushRegistration,
                            token,
                            senderId
                        )
                        if (onPushRegistration) {
                            val message = ReportingMessage.fromPushRegistrationMessage(provider)
                            reportingManager.log(message)
                        }
                        return true
                    }
                } catch (e: Exception) {
                    Logger.warning("Failed to call onPushRegistration for kit: " + provider.name + ": " + e.message)
                }
            }
        }
        return false
    }

    // ================================================================================
    // KitIntegration.AttributeListener forwarding
    // ================================================================================
    override fun onUserAttributesReceived(
        userAttributes: Map<String, String>?,
        userAttributeLists: Map<String, List<String>>?,
        mpid: Long?
    ) {
        var userAttributes = userAttributes
        var userAttributeLists = userAttributeLists
        userAttributes = mDataplanFilter.transformUserAttributes(userAttributes)
        userAttributeLists = mDataplanFilter.transformUserAttributes(userAttributeLists)
        for (provider in providers.values) {
            try {
                if ((provider is AttributeListener || provider is KitIntegration.UserAttributeListener) &&
                    !provider.isDisabled
                ) {
                    val filteredAttributeSingles = KitConfiguration.filterAttributes(
                        provider.configuration.userAttributeFilters,
                        userAttributes
                    ) as Map<String, String?>
                    val filteredAttributeLists = KitConfiguration.filterAttributes(
                        provider.configuration.userAttributeFilters,
                        userAttributeLists
                    ) as Map<String, List<String>?>
                    if (provider is AttributeListener) {
                        if ((provider as AttributeListener).supportsAttributeLists()) {
                            (provider as AttributeListener).setAllUserAttributes(
                                filteredAttributeSingles,
                                filteredAttributeLists
                            )
                        } else {
                            val singlesCopy: MutableMap<String, String?> =
                                HashMap(filteredAttributeSingles)
                            for ((key, value) in filteredAttributeLists) {
                                singlesCopy[key] = KitUtils.join(value)
                            }
                            (provider as AttributeListener).setAllUserAttributes(
                                singlesCopy,
                                HashMap()
                            )
                        }
                    }
                    if (provider is KitIntegration.UserAttributeListener) {
                        if ((provider as KitIntegration.UserAttributeListener).supportsAttributeLists()) {
                            (provider as KitIntegration.UserAttributeListener).onSetAllUserAttributes(
                                filteredAttributeSingles,
                                filteredAttributeLists,
                                FilteredMParticleUser.getInstance(
                                    mpid!!, provider
                                )
                            )
                        } else {
                            val singlesCopy: MutableMap<String, String?> =
                                HashMap(filteredAttributeSingles)
                            for ((key, value) in filteredAttributeLists) {
                                singlesCopy[key] = KitUtils.join(value)
                            }
                            (provider as KitIntegration.UserAttributeListener).onSetAllUserAttributes(
                                singlesCopy, HashMap(),
                                FilteredMParticleUser.getInstance(
                                    mpid!!, provider
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call setUserAttributes for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    private fun syncUserIdentities(
        attributeListener: AttributeListener,
        configuration: KitConfiguration
    ) {
        val instance = MParticle.getInstance()
        if (instance != null) {
            val user = instance.Identity().currentUser
            if (user != null) {
                val identities = user.userIdentities
                if (identities != null) {
                    for ((key, value) in identities) {
                        if (configuration.shouldSetIdentity(key)) {
                            attributeListener.setUserIdentity(key, value)
                        }
                    }
                }
            }
        }
    }

    override fun setUserAttribute(attributeKey: String?, attributeValue: String?, mpid: Long) {
        if (mDataplanFilter.isUserAttributeBlocked(attributeKey)) {
            return
        }
        for (provider in providers.values) {
            try {
                setUserAttribute(provider, attributeKey, attributeValue, mpid)
            } catch (e: Exception) {
                Logger.warning("Failed to call setUserAttributes/onSetUserAttribute for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun setUserAttributeList(
        attributeKey: String?,
        valuesList: List<String?>?,
        mpid: Long
    ) {
        if (mDataplanFilter.isUserAttributeBlocked(attributeKey)) {
            return
        }
        for (provider in providers.values) {
            try {
                setUserAttribute(provider, attributeKey, valuesList, mpid)
            } catch (e: Exception) {
                Logger.warning("Failed to call setUserAttributes/onSetUserAttribute for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    private fun setUserAttribute(
        provider: KitIntegration?,
        attributeKey: String?,
        valueList: List<String?>?,
        mpid: Long
    ) {
        if ((provider is AttributeListener || provider is KitIntegration.UserAttributeListener) &&
            !provider.isDisabled &&
            KitConfiguration.shouldForwardAttribute(
                    provider.configuration.userAttributeFilters,
                    attributeKey
                )
        ) {
            if (provider is AttributeListener) {
                if ((provider as AttributeListener).supportsAttributeLists()) {
                    (provider as AttributeListener).setUserAttributeList(attributeKey, valueList)
                } else {
                    (provider as AttributeListener).setUserAttribute(
                        attributeKey,
                        KitUtils.join(valueList)
                    )
                }
            }
            if (provider is KitIntegration.UserAttributeListener) {
                if ((provider as KitIntegration.UserAttributeListener).supportsAttributeLists()) {
                    (provider as KitIntegration.UserAttributeListener).onSetUserAttributeList(
                        attributeKey,
                        valueList,
                        FilteredMParticleUser.getInstance(mpid, provider)
                    )
                } else {
                    (provider as KitIntegration.UserAttributeListener).onSetUserAttribute(
                        attributeKey,
                        KitUtils.join(valueList),
                        FilteredMParticleUser.getInstance(mpid, provider)
                    )
                }
            }
        }
    }

    private fun setUserAttribute(
        provider: KitIntegration?,
        attributeKey: String?,
        attributeValue: String?,
        mpid: Long
    ) {
        if ((provider is AttributeListener || provider is KitIntegration.UserAttributeListener) &&
            !provider.isDisabled &&
            KitConfiguration.shouldForwardAttribute(
                    provider.configuration.userAttributeFilters,
                    attributeKey
                )
        ) {
            if (provider is AttributeListener) {
                (provider as AttributeListener).setUserAttribute(attributeKey, attributeValue)
            }
            if (provider is KitIntegration.UserAttributeListener) {
                (provider as KitIntegration.UserAttributeListener).onSetUserAttribute(
                    attributeKey,
                    attributeValue,
                    FilteredMParticleUser.getInstance(mpid, provider)
                )
            }
        }
    }

    override fun removeUserAttribute(key: String?, mpid: Long) {
        if (mDataplanFilter.isUserAttributeBlocked(key)) {
            return
        }
        for (provider in providers.values) {
            try {
                if ((provider is AttributeListener || provider is KitIntegration.UserAttributeListener) &&
                    !provider.isDisabled &&
                    KitConfiguration.shouldForwardAttribute(
                            provider.configuration.userAttributeFilters,
                            key
                        )
                ) {
                    if (provider is AttributeListener) {
                        (provider as AttributeListener).removeUserAttribute(key)
                    }
                    if (provider is KitIntegration.UserAttributeListener) {
                        (provider as KitIntegration.UserAttributeListener).onRemoveUserAttribute(
                            key,
                            FilteredMParticleUser.getInstance(mpid, provider)
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call removeUserAttribute/onRemoveUserAttribute for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun incrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        newValue: String?,
        mpid: Long
    ) {
        if (mDataplanFilter.isUserAttributeBlocked(key)) {
            return
        }
        for (provider in providers.values) {
            try {
                if (!provider!!.isDisabled && KitConfiguration.shouldForwardAttribute(
                        provider.configuration.userAttributeFilters,
                        key
                    )
                ) if (provider is KitIntegration.UserAttributeListener) {
                    (provider as KitIntegration.UserAttributeListener).onIncrementUserAttribute(
                        key,
                        incrementedBy,
                        newValue,
                        FilteredMParticleUser.getInstance(mpid, provider)
                    )
                }
                if (provider is AttributeListener) {
                    (provider as AttributeListener).setUserAttribute(key, newValue)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onIncrementUserAttribute for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun setUserTag(tag: String?, mpid: Long) {
        if (mDataplanFilter.isUserAttributeBlocked(tag)) {
            return
        }
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.UserAttributeListener && !provider.isDisabled &&
                    KitConfiguration.shouldForwardAttribute(
                            provider.configuration.userAttributeFilters,
                            tag
                        )
                ) {
                    (provider as KitIntegration.UserAttributeListener).onSetUserTag(
                        tag,
                        FilteredMParticleUser.getInstance(mpid, provider)
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onSetUserTag for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun setUserIdentity(id: String?, identityType: IdentityType?) {
        if (mDataplanFilter.isUserIdentityBlocked(identityType)) {
            return
        }
        for (provider in providers.values) {
            try {
                if (provider is AttributeListener && !provider.isDisabled && provider.configuration.shouldSetIdentity(
                        identityType
                    )
                ) {
                    (provider as AttributeListener).setUserIdentity(identityType, id)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call setUserIdentity for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun removeUserIdentity(identityType: IdentityType?) {
        if (mDataplanFilter.isUserIdentityBlocked(identityType)) {
            return
        }
        for (provider in providers.values) {
            try {
                if (provider is AttributeListener && !provider.isDisabled) {
                    (provider as AttributeListener).removeUserIdentity(identityType)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call removeUserIdentity for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun logout() {
        for (provider in providers.values) {
            try {
                if (provider is AttributeListener && !provider.isDisabled) {
                    val report = (provider as AttributeListener).logout()
                    reportingManager.logAll(report)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logout for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override val currentActivity: WeakReference<Activity>
        get() = mCoreCallbacks.currentActivity

    protected open fun logMPEvent(event: MPEvent) {
        if (event.isScreenEvent) {
            logScreen(event)
            return
        }
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.EventListener && !provider.isDisabled && provider.configuration.shouldLogEvent(
                        event
                    )
                ) {
                    val eventCopy = MPEvent(event)
                    eventCopy.customAttributes =
                        provider.configuration.filterEventAttributes(eventCopy)
                    val projectedEvents = CustomMapping.projectEvents(
                        eventCopy,
                        provider.configuration.customMappingList,
                        provider.configuration.defaultEventProjection
                    )
                    val reportingMessages: MutableList<ReportingMessage?> = LinkedList()
                    if (projectedEvents == null) {
                        var messages: List<ReportingMessage?>? = null
                        if (eventCopy.customAttributeStrings != null && eventCopy.customAttributeStrings!!.containsKey(
                                METHOD_NAME
                            ) &&
                            eventCopy.customAttributeStrings!![METHOD_NAME] == LOG_LTV
                        ) {
                            messages = (provider as CommerceListener).logLtvIncrease(
                                BigDecimal(eventCopy.customAttributeStrings!![RESERVED_KEY_LTV]),
                                BigDecimal(eventCopy.customAttributeStrings!![RESERVED_KEY_LTV]),
                                eventCopy.eventName,
                                eventCopy.customAttributeStrings
                            )
                        } else {
                            messages =
                                (provider as KitIntegration.EventListener).logEvent(eventCopy)
                            mCoreCallbacks.kitListener.onKitApiCalled(
                                provider.configuration.kitId,
                                !MPUtility.isEmpty(messages),
                                eventCopy
                            )
                        }
                        if (messages != null && messages.size > 0) {
                            reportingMessages.addAll(messages)
                        }
                    } else {
                        val masterMessage = ReportingMessage.fromEvent(provider, eventCopy)
                        var forwarded = false
                        for (i in projectedEvents.indices) {
                            val projectedEvent = projectedEvents[i].mpEvent
                            val messages =
                                (provider as KitIntegration.EventListener).logEvent(projectedEvent)
                            mCoreCallbacks.kitListener.onKitApiCalled(
                                provider.configuration.kitId,
                                !MPUtility.isEmpty(messages),
                                projectedEvent
                            )
                            if (messages != null && messages.size > 0) {
                                forwarded = true
                                for (message in messages) {
                                    val report = ProjectionReport(
                                        projectedEvents[i].projectionId,
                                        ReportingMessage.MessageType.EVENT,
                                        message!!.eventName,
                                        message.eventTypeString
                                    )
                                    masterMessage.addProjectionReport(report)
                                }
                            }
                        }
                        if (forwarded) {
                            reportingMessages.add(masterMessage)
                        }
                    }
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logMPEvent for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun logBatch(batch: String?) {
        for (provider in providers.values) {
            try {
                if (provider is BatchListener) {
                    val jsonObject = JSONObject(batch)
                    val reportingMessages = (provider as BatchListener).logBatch(jsonObject)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (jse: JSONException) {
                Logger.error(
                    jse,
                    "Failed to call logBatch (unable to deserialize Batch) for kit" + provider!!.name + ": " + jse.message
                )
            } catch (e: Exception) {
                Logger.warning("Failed to call logBatch for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun leaveBreadcrumb(breadcrumb: String) {
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.EventListener && !provider.isDisabled) {
                    val report =
                        (provider as KitIntegration.EventListener).leaveBreadcrumb(breadcrumb)
                    reportingManager.logAll(report)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        !MPUtility.isEmpty(report),
                        breadcrumb
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call leaveBreadcrumb for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun logError(message: String, eventData: Map<String, String>?) {
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.EventListener && !provider.isDisabled) {
                    val report =
                        (provider as KitIntegration.EventListener).logError(message, eventData)
                    reportingManager.logAll(report)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        !MPUtility.isEmpty(report),
                        message,
                        eventData
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logError for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun logException(
        exception: Exception,
        eventData: Map<String, String>?,
        message: String?
    ) {
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.EventListener && !provider.isDisabled) {
                    val report = (provider as KitIntegration.EventListener).logException(
                        exception,
                        eventData,
                        message
                    )
                    reportingManager.logAll(report)
                    mCoreCallbacks.kitListener.onKitApiCalled(
                        provider.configuration.kitId,
                        !MPUtility.isEmpty(report),
                        exception,
                        message,
                        eventData
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logException for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun logScreen(screenEvent: MPEvent?) {
        var screenEvent: MPEvent? = screenEvent
        if (mDataplanFilter != null) {
            screenEvent = mDataplanFilter.transformEventForEvent(screenEvent)
            if (screenEvent == null) {
                return
            }
        }
        for (provider in providers.values) {
            try {
                if (provider is KitIntegration.EventListener && !provider.isDisabled && provider.configuration.shouldLogScreen(
                        screenEvent!!.eventName
                    )
                ) {
                    val filteredEvent = MPEvent.Builder(screenEvent)
                        .customAttributes(
                            provider.configuration.filterScreenAttributes(
                                null,
                                screenEvent.eventName,
                                screenEvent.customAttributes
                            )
                        )
                        .build()
                    val projectedEvents = CustomMapping.projectEvents(
                        filteredEvent,
                        true,
                        provider.configuration.customMappingList,
                        provider.configuration.defaultEventProjection,
                        provider.configuration.defaultScreenCustomMapping
                    )
                    if (projectedEvents == null) {
                        val eventName = filteredEvent.eventName
                        val eventInfo = filteredEvent.customAttributeStrings
                        val report = (provider as KitIntegration.EventListener).logScreen(
                            eventName,
                            eventInfo
                        )
                        mCoreCallbacks.kitListener.onKitApiCalled(
                            provider.configuration.kitId,
                            !MPUtility.isEmpty(report),
                            eventName,
                            eventInfo
                        )
                        if (report != null && report.size > 0) {
                            for (message in report) {
                                message!!.setMessageType(ReportingMessage.MessageType.SCREEN_VIEW)
                                message.setScreenName(filteredEvent.eventName)
                            }
                        }
                        reportingManager.logAll(report)
                    } else {
                        val masterMessage = ReportingMessage(
                            provider,
                            ReportingMessage.MessageType.SCREEN_VIEW,
                            System.currentTimeMillis(),
                            filteredEvent.customAttributeStrings
                        )
                        var forwarded = false
                        for (projectedEvent in projectedEvents) {
                            val report =
                                (provider as KitIntegration.EventListener).logEvent(projectedEvent.mpEvent)
                            mCoreCallbacks.kitListener.onKitApiCalled(
                                "logMPEvent()",
                                provider.configuration.kitId,
                                !MPUtility.isEmpty(report),
                                projectedEvent
                            )
                            if (report != null && report.size > 0) {
                                forwarded = true
                                for (message in report) {
                                    val projectionReport = ProjectionReport(
                                        projectedEvent.projectionId,
                                        ReportingMessage.MessageType.EVENT,
                                        message!!.eventName,
                                        message.eventTypeString
                                    )
                                    masterMessage.setMessageType(ReportingMessage.MessageType.SCREEN_VIEW)
                                    masterMessage.setScreenName(message.eventName)
                                    masterMessage.addProjectionReport(projectionReport)
                                }
                            }
                        }
                        if (forwarded) {
                            reportingManager.log(masterMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call logScreen for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    // ================================================================================
    // KitIntegration.ActivityListener forwarding
    // ================================================================================
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages = (provider as ActivityListener).onActivityCreated(
                        activity,
                        savedInstanceState
                    )
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivityCreated for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivityStarted(activity)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivityStarted for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivityResumed(activity: Activity?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivityResumed(activity)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivityResumed for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivityPaused(activity)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onResume for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivityStopped(activity: Activity?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivityStopped(activity)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivityStopped for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivitySaveInstanceState(
                            activity,
                            outState
                        )
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivitySaveInstanceState for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity?) {
        for (provider in providers.values) {
            try {
                if (provider is ActivityListener && !provider.isDisabled) {
                    val reportingMessages =
                        (provider as ActivityListener).onActivityDestroyed(activity)
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onActivityDestroyed for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onSessionEnd() {
        for (provider in providers.values) {
            try {
                if (provider is SessionListener && !provider.isDisabled) {
                    val reportingMessages = (provider as SessionListener).onSessionEnd()
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onSessionEnd for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onSessionStart() {
        for (provider in providers.values) {
            try {
                if (provider is SessionListener && !provider.isDisabled) {
                    val reportingMessages = (provider as SessionListener).onSessionStart()
                    reportingManager.logAll(reportingMessages)
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onSessionStart for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onApplicationForeground() {
        for (provider in providers.values) {
            try {
                if (provider is ApplicationStateListener) {
                    (provider as ApplicationStateListener).onApplicationForeground()
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onApplicationForeground for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onApplicationBackground() {
        for (provider in providers.values) {
            try {
                if (provider is ApplicationStateListener) {
                    (provider as ApplicationStateListener).onApplicationBackground()
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onApplicationBackground for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override val attributionResults: Map<Int, AttributionResult>
        get() = mAttributionResultsMap

    // ================================================================================
    // AttributionListener forwarding
    // ================================================================================
    override fun onResult(result: AttributionResult) {
        mAttributionResultsMap[result.serviceProviderId] = result
        val instance = MParticle.getInstance()
        if (instance != null) {
            val listener = instance.attributionListener
            if (listener != null && result != null) {
                Logger.debug("Attribution result returned: \n$result")
                listener.onResult(result)
            }
        }
    }

    override fun onError(error: AttributionError) {
        val instance = MParticle.getInstance()
        if (instance != null) {
            val listener = instance.attributionListener
            if (listener != null && error != null) {
                Logger.debug("Attribution error returned: \n$error")
                listener.onError(error)
            }
        }
    }

    override fun installReferrerUpdated() {
        val instance = MParticle.getInstance()
        if (instance != null) {
            val mockIntent = getMockInstallReferrerIntent(instance.installReferrer!!)
            for (provider in providers.values) {
                try {
                    if (!provider!!.isDisabled) {
                        provider.setInstallReferrer(mockIntent)
                    }
                } catch (e: Exception) {
                    Logger.warning("Failed to update Install Referrer for kit: " + provider!!.name + ": " + e.message)
                }
            }
        }
    }

    // ================================================================================
    // IdentityListener forwarding
    // ================================================================================
    override fun onUserIdentified(mParticleUser: MParticleUser, previousUser: MParticleUser?) {
        // due to consent forwarding rules we need to re-verify kits whenever the user changes
        reloadKits()
        for (provider in providers.values) {
            try {
                if (provider is IdentityListener && !provider.isDisabled) {
                    (provider as IdentityListener).onUserIdentified(
                        FilteredMParticleUser.getInstance(
                            mParticleUser,
                            provider
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onUserIdentified for kit: " + provider!!.name + ": " + e.message)
            }
        }
        mParticleUser.getUserAttributes(this)
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        identityApiRequest: IdentityApiRequest
    ) {
        for (provider in providers.values) {
            try {
                if (provider is IdentityListener && !provider.isDisabled) {
                    (provider as IdentityListener).onIdentifyCompleted(
                        FilteredMParticleUser.getInstance(
                            mParticleUser,
                            provider
                        ),
                        FilteredIdentityApiRequest(identityApiRequest, provider)
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onIdentifyCompleted for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        identityApiRequest: IdentityApiRequest
    ) {
        for (provider in providers.values) {
            try {
                if (provider is IdentityListener && !provider.isDisabled) {
                    (provider as IdentityListener).onLoginCompleted(
                        FilteredMParticleUser.getInstance(
                            mParticleUser,
                            provider
                        ),
                        FilteredIdentityApiRequest(identityApiRequest, provider)
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onLoginCompleted for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        identityApiRequest: IdentityApiRequest
    ) {
        for (provider in providers.values) {
            try {
                if (provider is IdentityListener && !provider.isDisabled) {
                    (provider as IdentityListener).onLogoutCompleted(
                        FilteredMParticleUser.getInstance(
                            mParticleUser,
                            provider
                        ),
                        FilteredIdentityApiRequest(identityApiRequest, provider)
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onLogoutCompleted for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        identityApiRequest: IdentityApiRequest
    ) {
        for (provider in providers.values) {
            try {
                if (provider is IdentityListener && !provider.isDisabled) {
                    (provider as IdentityListener).onModifyCompleted(
                        FilteredMParticleUser.getInstance(
                            mParticleUser,
                            provider
                        ),
                        FilteredIdentityApiRequest(identityApiRequest, provider)
                    )
                }
            } catch (e: Exception) {
                Logger.warning("Failed to call onModifyCompleted for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        mpid: Long
    ) {
        // Due to consent forwarding rules we need to re-initialize kits whenever the user changes.
        reloadKits()
        for (provider in providers.values) {
            if (provider is KitIntegration.UserAttributeListener && !provider.isDisabled) {
                try {
                    (provider as KitIntegration.UserAttributeListener).onConsentStateUpdated(
                        oldState,
                        newState,
                        FilteredMParticleUser.getInstance(mpid, provider)
                    )
                } catch (e: Exception) {
                    Logger.warning("Failed to call onConsentStateUpdated for kit: " + provider.name + ": " + e.message)
                }
            }
        }
    }

    override fun reset() {
        for (provider in providers.values) {
            try {
                provider!!.reset()
            } catch (e: Exception) {
                Logger.warning("Failed to call reset for kit: " + provider!!.name + ": " + e.message)
            }
        }
    }

    open fun runOnKitThread(runnable: Runnable?) {
        if (mKitHandler == null) {
            mKitHandler = Handler(kitHandlerThread!!.looper)
        }
        mKitHandler!!.post(runnable!!)
    }

    open fun runOnMainThread(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }

    val isPushEnabled: Boolean
        get() = mCoreCallbacks.isPushEnabled
    val pushSenderId: String
        get() = mCoreCallbacks.pushSenderId
    val pushInstanceId: String
        get() = mCoreCallbacks.pushInstanceId
    var dataplanFilter: DataplanFilter?
        get() = if (mDataplanFilter == null) {
            Logger.warning("DataplanFilter could not be found")
            DataplanFilterImpl.EMPTY
        } else {
            mDataplanFilter
        }
        set(dataplanFilter) {
            mDataplanFilter = dataplanFilter ?: DataplanFilterImpl.EMPTY
        }

    private fun initializeKitIntegrationFactory() {
        if (mKitIntegrationFactory != null) {
            if (mKitOptions != null) {
                for ((key, value) in mKitOptions!!.kits) {
                    Logger.info("Kit registered: " + value.simpleName + "(" + key + ")")
                    mKitIntegrationFactory!!.addSupportedKit(key, value)
                }
            }
            if (mKitIntegrationFactory!!.supportedKits != null) {
                for (kitId in mKitIntegrationFactory!!.supportedKits.keys) {
                    mCoreCallbacks.kitListener.kitFound(kitId)
                }
            }
        }
    }

    private fun parseKitConfigurations(kitConfigs: JSONArray?): List<KitConfiguration> {
        var kitConfigs = kitConfigs
        val configurations: MutableList<KitConfiguration> = ArrayList()
        if (kitConfigs == null) {
            kitConfigs = JSONArray()
        }
        for (i in 0 until kitConfigs.length()) {
            var kitConfig: JSONObject? = null
            try {
                kitConfig = kitConfigs.getJSONObject(i)
            } catch (e: JSONException) {
                Logger.error(e, "Malformed Kit configuration")
            }
            if (kitConfig != null) {
                try {
                    configurations.add(createKitConfiguration(kitConfig))
                } catch (e: JSONException) {
                    val kitId = kitConfig.optInt("id", -1)
                    mCoreCallbacks.kitListener.kitExcluded(
                        kitId,
                        "exception while starting. Exception: " + e.message
                    )
                    Logger.error("Exception while starting kit: " + kitId + ": " + e.message)
                }
            }
        }
        return configurations
    }

    fun addKitsLoadedListener(kitsLoadedListener: KitsLoadedListener) {
        kitsLoadedListeners.add(kitsLoadedListener)
    }

    private fun onKitsLoaded(
        kits: Map<Int, KitIntegration?>,
        previousKits: Map<Int, KitIntegration?>,
        kitConfigs: List<KitConfiguration>
    ) {
        for (listener in kitsLoadedListeners) {
            listener.onKitsLoaded(kits, previousKits, kitConfigs)
        }
    }

    override fun getSurveyUrl(
        serviceId: Int,
        userAttributes: Map<String, String>?,
        userAttributeLists: Map<String, List<String>>?
    ): Uri? {
        var userAttributes = userAttributes
        var userAttributeLists = userAttributeLists
        userAttributes = mDataplanFilter.transformUserAttributes(userAttributes)
        userAttributeLists = mDataplanFilter.transformUserAttributes(userAttributeLists)
        val provider = providers[serviceId]
        return provider?.getSurveyUrl(
            KitConfiguration.filterAttributes(
                provider.configuration.userAttributeFilters,
                userAttributes
            ) as Map<String?, String?>,
            KitConfiguration.filterAttributes(
                provider.configuration.userAttributeFilters,
                userAttributeLists
            ) as Map<String?, List<String?>?>
        )
    }

    interface KitsLoadedListener {
        fun onKitsLoaded(
            kits: Map<Int, KitIntegration?>,
            previousKits: Map<Int, KitIntegration?>,
            kitConfigs: List<KitConfiguration>
        )
    }

    companion object {
        private var kitHandlerThread: HandlerThread? = null

        init {
            kitHandlerThread = HandlerThread("mParticle_kit_thread")
            kitHandlerThread!!.start()
        }

        private const val RESERVED_KEY_LTV = "\$Amount"
        private const val METHOD_NAME = "\$MethodName"
        private const val LOG_LTV = "LogLTVIncrease"
        private fun getMockInstallReferrerIntent(referrer: String): Intent? {
            return if (!MPUtility.isEmpty(referrer)) {
                val fakeReferralIntent = Intent("com.android.vending.INSTALL_REFERRER")
                fakeReferralIntent.putExtra(Constants.REFERRER, referrer)
                fakeReferralIntent
            } else {
                null
            }
        }
    }
}
