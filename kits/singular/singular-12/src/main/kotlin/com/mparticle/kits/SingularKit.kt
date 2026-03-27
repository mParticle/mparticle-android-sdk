package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.mparticle.AttributionResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.KitIntegration.ActivityListener
import com.mparticle.kits.KitIntegration.ApplicationStateListener
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.EventListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import com.singular.sdk.SDIDAccessorHandler
import com.singular.sdk.Singular
import com.singular.sdk.SingularConfig
import com.singular.sdk.SingularDeviceAttributionHandler
import com.singular.sdk.internal.SingularLog
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal

open class SingularKit :
    KitIntegration(),
    ActivityListener,
    EventListener,
    PushListener,
    CommerceListener,
    ApplicationStateListener,
    UserAttributeListener,
    AttributeListener,
    KitIntegration.LogoutListener {
    interface DeviceAttributionCallback : SingularDeviceAttributionHandler

    interface SdidAccessorHandler : SDIDAccessorHandler

    private val logger = SingularLog.getLogger(Singular::class.java.simpleName)
    private var isInitialized = false
    private var deviceToken: String? = null

    //endregion
    //region Kit Integration Implementation
    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        // Returning the reporting message to state that the method was successful and
        // Preventing from the mParticle Kit to retry to activate to method.
        val messages: MutableList<ReportingMessage> = ArrayList()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messages
    }

    fun buildSingularConfig(settings: Map<String, String>?): SingularConfig? =
        try {
            val singularKey = settings?.get(API_KEY)
            val singularSecret = settings?.get(API_SECRET)

            // Getting the DDL timeout from the settings. If does not exist, use 60(S) as default.
            val ddlTimeout = settings?.get(DDL_TIMEOUT)
            var ddlHandlerTimeoutSec = 60L
            if (!KitUtils.isEmpty(ddlTimeout)) {
                try {
                    if (ddlTimeout != null) {
                        ddlHandlerTimeoutSec = ddlTimeout.toLong()
                    }
                } catch (unableToGetDDLTimeout: Exception) {
                }
            }
            val config = SingularConfig(singularKey, singularSecret)
            config.withDDLTimeoutInSec(ddlHandlerTimeoutSec)
            val activity = currentActivity.get()
            if (activity != null) {
                val intent = activity.intent
                config.withSingularLink(intent) { singularLinkParams ->
                    val attributionResult = AttributionResult()
                    attributionResult.serviceProviderId = MParticle.ServiceProviders.SINGULAR
                    attributionResult.link = singularLinkParams.deeplink
                    try {
                        val linkParams = JSONObject()
                        linkParams.put(PASSTHROUGH, singularLinkParams.passthrough)
                        linkParams.put(IS_DEFERRED, singularLinkParams.isDeferred)
                        if (singularLinkParams.urlParameters != null) {
                            linkParams.put(
                                QUERY_PARAMS,
                                (singularLinkParams.urlParameters as Map<*, *>?)?.let {
                                    JSONObject(
                                        it,
                                    )
                                },
                            )
                        }
                        attributionResult.parameters = linkParams
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    kitManager.onResult(attributionResult)
                }
            }

            // If the environment is in development mode, enable logging.
            if (MPUtility.isDevEnv()) {
                config.withLoggingEnabled()
                config.withLogLevel(Log.DEBUG)
            }

            config.deviceAttributionHandler = deviceAttributionCallback

            config.withCustomSdid(customSdid, sdidAccessorHandler)

            Singular.setWrapperNameAndVersion(MPARTICLE_WRAPPER_NAME, MPARTICLE_WRAPPER_VERSION)
            config
        } catch (ex: Exception) {
            logger.error(CANT_BUILD_SINGULAR_CONFIG_MESSAGE, ex)
            null
        }

    override fun setOptOut(b: Boolean): List<ReportingMessage> = emptyList()

    override fun getName(): String = KIT_NAME

    override fun setInstallReferrer(intent: Intent) {}

    //endregion
    //region Activity Listener Implementation
    override fun onActivityResumed(activity: Activity): List<ReportingMessage> {
        Singular.onActivityResumed()
        return emptyList()
    }

    override fun onActivityPaused(activity: Activity): List<ReportingMessage> {
        Singular.onActivityPaused()
        return emptyList()
    }

    //region Unimplemented (Empty Methods)
    override fun onActivityCreated(
        activity: Activity,
        bundle: Bundle?,
    ): List<ReportingMessage> {
        initializeSingular()
        return emptyList()
    }

    override fun onActivityStarted(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityStopped(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivitySaveInstanceState(
        activity: Activity,
        bundle: Bundle?,
    ): List<ReportingMessage> = emptyList()

    override fun onActivityDestroyed(activity: Activity): List<ReportingMessage> = emptyList()

    //endregion
    //endregion
    //region Event Listener Implementation
    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage>? {
        val messages: MutableList<ReportingMessage> = ArrayList()
        executeIfSingularInitialized({
            val eventName = mpEvent.eventName
            val eventInfo = mpEvent.customAttributes

            // Logging the event with the Singular API
            val eventStatus: Boolean =
                if (!eventInfo.isNullOrEmpty()) {
                    Singular.eventJSON(eventName, JSONObject(eventInfo))
                } else {
                    Singular.event(eventName)
                }

            // If the Singular event logging was successful, return the message to the mParticle Kit
            // So it won't retry the event
            if (eventStatus) {
                messages.add(ReportingMessage.fromEvent(this, mpEvent))
            }
        }, forceInitSingular = true, "logEvent")
        return messages
    }

    //region Unimplemented (Empty Methods)
    override fun leaveBreadcrumb(s: String): List<ReportingMessage> = emptyList()

    override fun logError(
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        e: Exception,
        map: Map<String, String>,
        s: String,
    ): List<ReportingMessage> = emptyList()

    override fun logScreen(
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    //endregion
    //endregion
    //region Push Listener Implementation
    override fun onPushRegistration(
        deviceToken: String,
        senderId: String,
    ): Boolean {
        // Saving the registration token to determine when the user uninstalls the app.
        this.deviceToken = deviceToken
        executeIfSingularInitialized({
            if (MPUtility.isFirebaseAvailable()) {
                Singular.setFCMDeviceToken(deviceToken)
            }
        }, forceInitSingular = false, "onPushRegistration")
        return true
    }

    private fun executeIfSingularInitialized(
        operation: () -> Unit,
        forceInitSingular: Boolean = false,
        operationName: String,
    ) {
        if (isInitialized) {
            operation.invoke()
            Logger.debug("$operationName executed")
        } else {
            if (forceInitSingular) {
                initializeSingular()
                executeIfSingularInitialized(operation, false, operationName)
            } else {
                Logger.debug("$operationName can't be executed, Singular not initialized")
            }
        }
    }

    private fun initializeSingular() {
        if (!isInitialized) {
            if (Singular.init(context, buildSingularConfig(settings))) {
                currentUser?.id?.toString()?.let { Singular.setCustomUserId(it) }
                isInitialized = true
                singularSettings = settings
                deviceToken?.let { deviceToken ->
                    if (MPUtility.isFirebaseAvailable()) {
                        Singular.setFCMDeviceToken(deviceToken)
                    }
                }
            }
        }
    }

    //region Unimplemented (Empty Methods)
    override fun willHandlePushMessage(intent: Intent): Boolean = false

    override fun onPushMessageReceived(
        context: Context,
        intent: Intent,
    ) {}

    //endregion
    //endregion
    //region Commerce Listener Implementation
    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        var list = emptyList<ReportingMessage>()
        executeIfSingularInitialized(operation = {
            if (commerceEvent.productAction == Product.PURCHASE) {
                list = handlePurchaseEvents(commerceEvent)
            } else {
                list = handleNonPurchaseEvents(commerceEvent)
            }
        }, forceInitSingular = true, "logEvent")
        return list
    }

    private fun handlePurchaseEvents(commerceEvent: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = ArrayList()
        commerceEvent.products?.let {
            for (product in it) {
                Singular.revenue(
                    commerceEvent.currency,
                    product.totalAmount,
                    product.sku,
                    product.name,
                    product.category,
                    product.quantity.toInt(),
                    product.unitPrice,
                )
            }
        }
        messages.add(ReportingMessage.fromEvent(this, commerceEvent))
        return messages
    }

    private fun handleNonPurchaseEvents(commerceEvent: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = ArrayList()

        // Getting the mParticle events from the commerce event
        val eventList = CommerceEventUtils.expand(commerceEvent)
        if (eventList != null) {
            for (event in eventList) {
                try {
                    logEvent(event)?.let {
                        for (message in it) {
                            messages.add(message)
                        }
                    }
                } catch (e: Exception) {
                    Logger.warning("Failed to call logCustomEvent to Singular kit: $e")
                }
            }
        }
        return messages
    }

    //region Unimplemented (Empty Methods)
    override fun logLtvIncrease(
        bigDecimal: BigDecimal,
        bigDecimal1: BigDecimal,
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    //endregion
    //endregion
    //region Deprecated Attribute Listener
    override fun setUserAttribute(
        key: String,
        value: String,
    ) {
        // TODO: Debug these lines to understand the code
        val map = HashMap<String?, String?>()
        if (MParticle.UserAttributes.AGE == key) {
            map[USER_AGE_KEY] = value
        } else if (MParticle.UserAttributes.GENDER == key) {
            if (value.contains("fe")) {
                map[USER_GENDER_KEY] = "f"
            } else {
                map[USER_GENDER_KEY] = "m"
            }
        }
        if (map.isNotEmpty()) {
            executeIfSingularInitialized(
                {
                    Singular.eventJSON("UserAttribute", (map as Map<*, *>?)?.let { JSONObject(it) })
                },
                forceInitSingular = false,
                "setUserAttribute",
            )
        }
    }

    override fun setUserAttributeList(
        s: String,
        list: List<String>,
    ) {}

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?,
    ) {
    }

    override fun onRemoveUserAttribute(
        s: String,
        filteredMParticleUser: FilteredMParticleUser,
    ) {}

    override fun onSetUserAttribute(
        s: String,
        o: Any,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
    }

    override fun onSetUserTag(
        s: String,
        filteredMParticleUser: FilteredMParticleUser,
    ) {}

    override fun onSetUserAttributeList(
        s: String,
        list: List<String>,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
    }

    override fun onSetAllUserAttributes(
        map: Map<String, String>,
        map1: Map<String, List<String>>,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
    }

    override fun supportsAttributeLists(): Boolean = false

    override fun onConsentStateUpdated(
        consentState: ConsentState,
        consentState1: ConsentState,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
        executeIfSingularInitialized({
            consentState.ccpaConsentState?.let { Singular.limitDataSharing(it.isConsented) }
        }, forceInitSingular = false, "onConsentStateUpdated")
    }

    override fun setAllUserAttributes(
        map: Map<String, String>,
        map1: Map<String, List<String>>,
    ) {}

    override fun removeUserAttribute(s: String) {}

    override fun setUserIdentity(
        identityType: IdentityType,
        s: String,
    ) {
        if (identityType == IdentityType.CustomerId) {
            executeIfSingularInitialized({
                Singular.setCustomUserId(s)
            }, forceInitSingular = false, "setUserIdentity")
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        if (identityType == IdentityType.CustomerId) {
            executeIfSingularInitialized({
                Singular.unsetCustomUserId()
                isInitialized = false
            }, forceInitSingular = false, "removeUserIdentity")
        }
    }

    override fun logout(): List<ReportingMessage> {
        val messageList: MutableList<ReportingMessage> = ArrayList()
        executeIfSingularInitialized({
            Singular.unsetCustomUserId()
            isInitialized = false
            messageList.add(ReportingMessage.logoutMessage(this))
        }, forceInitSingular = false, "logout")
        return messageList
    }

    override fun onApplicationForeground() {
        // Handling deeplinks when the application resumes from background
        initializeSingular()
    }

    override fun onApplicationBackground() {} //endregion

    companion object {
        //region Members
        // Config Consts
        private const val API_KEY = "apiKey"
        private const val API_SECRET = "secret"
        private const val DDL_TIMEOUT = "ddlTimeout"
        private const val KIT_NAME = "Singular"

        // User Attribute Consts
        private const val USER_AGE_KEY = "age"
        private const val USER_GENDER_KEY = "gender"

        // Singular Link Consts
        private const val PASSTHROUGH = "passthrough"
        private const val IS_DEFERRED = "is_deferred"
        private const val QUERY_PARAMS = "query_params"

        // Wrapper Consts
        private const val MPARTICLE_WRAPPER_NAME = "mParticle"
        private const val MPARTICLE_WRAPPER_VERSION = "1.0.1"
        private const val CANT_BUILD_SINGULAR_CONFIG_MESSAGE =
            "Can't build Singular Config in the mParticle Kit"
        private var singularSettings: Map<String, String>? = null

        private var deviceAttributionCallback: DeviceAttributionCallback? = null
        private var customSdid: String? = null
        private var sdidAccessorHandler: SdidAccessorHandler? = null

        @JvmStatic fun setDeviceAttributionCallback(deviceAttributionCallback: DeviceAttributionCallback?) {
            this.deviceAttributionCallback = deviceAttributionCallback
        }

        @JvmStatic fun setCustomSDID(
            customSDID: String?,
            sdidAccessorHandler: SdidAccessorHandler?,
        ) {
            this.customSdid = customSDID
            this.sdidAccessorHandler = sdidAccessorHandler
        }
    }
}
