package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.os.Bundle
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.math.BigDecimal
import kotlin.Exception

interface LegacyKitIntegration {
    fun getUserIdentities(): Map<IdentityType, String?>
    fun getAllUserAttributes(): Map<String, Any?>
}

/**
 * Base Kit implementation - all Kits must subclass this.
 */
abstract class KitIntegration : LegacyKitIntegration {

    var kitManager: KitManagerImpl? = null
        private set

    /**
     * Get the configuration of the Kit. The KitConfiguration object contains
     * all of the settings and filters of a Kit. Generally there's no need to use
     * this API, and you can instead just call [.getSettings].
     *
     * @return
     */
    var configuration: KitConfiguration? = null
        private set

    /**
     * Determine if the application is currently in the background. This is derived
     * by the mParticle SDK based on whether there is currently an Activity visible.
     *
     * @return true if the application is background
     */
    protected val isBackgrounded: Boolean = kitManager?.isBackgrounded ?: false

    /**
     * Get an application Context object.
     *
     * @return an application Context, this will never be null.
     */
    val context: Context? = kitManager?.context

    /**
     * Get a WeakReference to an Activity. The mParticle SDK maintains a WeakReference
     * to the currently visible Activity. This will sometimes return null (if the app is in the background), and will
     * sometimes return a WeakReference with no attached Activity - so be sure to code safely.
     *
     * @return a WeakReference to an Activity, or null.
     */
    fun getCurrentActivity(): WeakReference<Activity>? = kitManager?.currentActivity

    fun setConfiguration(configuration: KitConfiguration): KitIntegration {
        this.configuration = configuration
        return this
    }

    /**
     * Kits should override this method in order to provide implementing developers
     * direct access to their APIs. Though generally we don't want developers to need direct
     * access - it can be unavoidable if there are APIs in the underlying Kit SDK for which
     * the mParticle SDK has no analogue or abstraction.
     *
     * @return
     */
    open fun getInstance(): Any? = null

    open fun isDisabled(): Boolean = isDisabled(false)

    @Throws(RuntimeException::class)
    fun isDisabled(isOptOutEvent: Boolean): Boolean {
        if (kitManager == null || configuration == null) return true
        return try {
            !configuration?.passesBracketing(kitManager?.userBucket!!)!! || configuration?.shouldHonorOptOut()!! && kitManager?.isOptedOut!! && !isOptOutEvent
        } catch (e: Exception) {
            false
        }
    }

    @Deprecated("Use the async version")
    override fun getUserIdentities(): Map<IdentityType, String?> {
        var returnValue: Map<IdentityType, String?>? = mutableMapOf()
        MParticle.getInstance()?.Identity()?.currentUser?.let {
            val transformedIdentities =
                kitManager?.dataplanFilter?.transformIdentities(it.userIdentities)

            configuration?.let { config ->
                returnValue = transformedIdentities?.filter {
                    config.shouldSetIdentity(it.key)
                }
            }
        }
        return returnValue ?: mutableMapOf()
    }

    /**
     * Retrieve filtered user attributes. Use this method to retrieve user attributes at any time.
     * To ensure that filtering is respected, kits must use this method rather than the public API.
     *
     * If the KitIntegration implements the [AttributeListener] interface and returns true
     * for [AttributeListener.supportsAttributeLists], this method will pass back all attributes
     * as they are (as String values or as List&lt;String&gt; values). Otherwise, this method will comma-separate
     * the List values and return back all String values.
     *
     * @return a Map of attributes according to the logic above.
     */
    @Deprecated("")
    override fun getAllUserAttributes(): Map<String, Any?> {
        val transformedUserAttributes = kitManager?.dataplanFilter?.transformUserAttributes(
            MParticle.getInstance()?.Identity()?.currentUser?.userAttributes
        )

        var attributes: MutableMap<String, Any?> = mutableMapOf()
        configuration?.let { config ->
            KitConfiguration.filterAttributes(
                config.userAttributeFilters, transformedUserAttributes
            )?.toMutableMap()?.let { attributes = it }
        }

        return if (this is AttributeListener && (this as AttributeListener).supportsAttributeLists()) {
            attributes
        } else {
            attributes.forEach {
                if (it.value is List<*>) {
                    val key = it.key
                    attributes[key] = KitUtils.join((it.value as List<*>).map { it.toString() })
                }
            }
            attributes
        }
    }

    val currentUser: MParticleUser?
        get() = getUser()

    fun getUser(mpid: Long? = null): MParticleUser? {
        val identity = MParticle.getInstance()?.Identity()
        val user = mpid?.let { identity?.getUser(it) } ?: identity?.currentUser
        return user?.let { FilteredMParticleUser.getInstance(it, this) }
    }

    /**
     * Kits must override this an provide a human-readable name of their service. This name will
     * show up in ADB logs.
     *
     * @return the name of your company/sdk
     */
    abstract fun getName(): String

    /**
     * Kits must override this method and should use it for initialization. This method will only be called
     * once. The first time that a given user installs and opens an app, this may occur anywhere from milliseconds
     * after launch to minutes, depending on the user's data connection.
     *
     * @param settings the settings that have been configured in mParticle UI. Use this to extract your API key, etc
     * @param context  an Application Context object
     *
     * @throws IllegalArgumentException if the kit is unable to start based on the provided settings.
     */
    @Throws(IllegalArgumentException::class)
    abstract fun onKitCreate(
        settings: Map<String, String>,
        context: Context
    ): List<ReportingMessage>

    /**
     * This method will be called when an integration has been disabled. Ideally, any unnecessary memory should
     * be cleaned up when this method is called. After this method is called, mParticle will no longer delegate events
     * to the kit, and on subsequent application startups, the Kit will not be initialized at all.
     */
    open fun onKitDestroy() {}

    /**
     * Called by the KitManager when this kit is to be removed.
     */
    fun onKitCleanup() {
        kitPreferences?.edit()?.clear()?.apply()
    }

    /**
     * Get the SharedPreferences file for this Kit. Kits should use this rather than their own file, as the mParticle SDK
     * will automatically remove this file if the kit is removed from the app.
     *
     * @return
     */
    protected val kitPreferences: SharedPreferences?
        get() {
            return if (kitManager != null && configuration != null) kitManager!!.context.getSharedPreferences(
                "$KIT_PREFERENCES_FILE${configuration!!.kitId}", Context.MODE_PRIVATE
            ) else null
        }

    /**
     * The mParticle SDK is able to track a user's location based on provider and accuracy settings, and additionally allows
     * developers to manually set a location. In either case, this method will be called on a Kit whenever a new location object
     * is received.
     *
     * @param location
     */
    open fun setLocation(location: Location?) {}

    /**
     * Retrieve the settings that are configured for this Kit, such as API key, etc
     *
     * @return a Map of settings
     */
    val settings: Map<String, String>
        get() = configuration?.settings ?: mutableMapOf()

    open fun logNetworkPerformance(
        url: String?,
        startTime: Long,
        method: String?,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
        responseCode: Int
    ): List<ReportingMessage> = emptyList()

    open fun getSurveyUrl(
        userAttributes: Map<String, String?>?,
        userAttributeLists: Map<String, List<String?>?>?
    ): Uri? = null

    /**
     * @param optedOut
     * @return
     */
    abstract fun setOptOut(optedOut: Boolean): List<ReportingMessage>

    fun setKitManager(kitManager: KitManagerImpl): KitIntegration {
        this.kitManager = kitManager
        return this
    }

    open fun logBaseEvent(baseEvent: BaseEvent): List<ReportingMessage> = emptyList()

    open fun logEvent(baseEvent: MPEvent): List<ReportingMessage> = emptyList()

    /**
     * Set integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @param integrationAttributes
     */
    protected fun setIntegrationAttributes(integrationAttributes: Map<String, String?>?) {
        kitManager?.setIntegrationAttributes(this, integrationAttributes)
    }

    /**
     * Get integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @return
     */
    protected fun getIntegrationAttributes(): Map<String, String> =
        kitManager?.getIntegrationAttributes(this) ?: mapOf()

    /**
     * Remove all integration attributes set for this integration.
     */
    protected fun clearIntegrationAttributes() {
        kitManager?.clearIntegrationAttributes(this)
    }

    /**
     * Implement this method to receive com.android.vending.INSTALL_REFERRER Intents that
     * have been captured by the mParticle SDK. Developers/users of the SDK may also call setInstallReferrer
     * at any time after an install has occurred.
     *
     * @param intent an intent with the INSTALL_REFERRER action
     */
    open fun setInstallReferrer(intent: Intent?) {}

    /**
     * Implement this method to listen for when settings are updated while the kit is already active.
     *
     * @param settings
     */
    fun onSettingsUpdated(settings: Map<String, String>) {}

    /**
     * Queues and groups network requests on the MParticle Core network handler
     */
    fun executeNetworkRequest(runnable: Runnable?) {
        kitManager?.runOnKitThread(runnable)
    }

    /**
     * Indicates that the user wishes to remove personal data and shutdown a Kit and/or underlying 3rd
     * party SDK
     */
    open fun reset() {}

    /**
     * Kits should implement this interface when they require Activity callbacks for any reason.
     *
     *
     * The mParticle SDK will automatically call every method of this interface in API > 14 devices. Otherwise only
     * [.onActivityStarted] and [.onActivityStopped] will be called.
     */
    interface ActivityListener {
        fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?
        ): List<ReportingMessage>

        fun onActivityStarted(activity: Activity): List<ReportingMessage>
        fun onActivityResumed(activity: Activity): List<ReportingMessage>
        fun onActivityPaused(activity: Activity): List<ReportingMessage>
        fun onActivityStopped(activity: Activity): List<ReportingMessage>
        fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle?
        ): List<ReportingMessage>

        fun onActivityDestroyed(activity: Activity): List<ReportingMessage>
    }

    /**
     * Kit should implement this interface to listen for mParticle session-start and session-end messages.
     *
     */
    interface SessionListener {
        /**
         * mParticle will start a new session when:
         * 1. The app is brought to the foreground, and there isn't already an active session.
         * 2. Any event (ie custom events, screen events, user attributes, etc) is logged by the hosting app
         *
         * @return
         */
        fun onSessionStart(): List<ReportingMessage>

        /**
         * mParticle will end a session when the app is sent to the background, after the session timeout (defaulted to 60s)
         *
         * @return
         */
        fun onSessionEnd(): List<ReportingMessage>
    }

    /**
     * Kits should implement this interface when their underlying service has the notion
     * of a user with attributes.
     */
    @Deprecated("")
    interface AttributeListener {
        fun setUserAttribute(attributeKey: String, attributeValue: String?)

        fun setUserAttributeList(attributeKey: String, attributeValueList: List<String?>?)

        /**
         * Indicate to the mParticle Kit framework if this AttributeListener supports attribute-values as lists.
         *
         * If an AttributeListener returns false, the setUserAttributeList method will never be called. Instead, setUserAttribute
         * will be called with the attribute-value lists combined as a csv.
         *
         * @return true if this AttributeListener supports attribute values as lists.
         */
        fun supportsAttributeLists(): Boolean
        fun setAllUserAttributes(
            userAttributes: Map<String, String?>?,
            userAttributeLists: Map<String, List<String?>?>?
        )

        fun removeUserAttribute(key: String)
        fun setUserIdentity(identityType: IdentityType, identity: String)
        fun removeUserIdentity(identityType: IdentityType)

        /**
         * The mParticle SDK exposes a logout API, allowing developers to track an event
         * when a user logs out of their app/platform. Use this opportunity to perform the appropriate logic
         * as per your platforms logout paradigm, such as clearing user attributes.
         *
         * @return Kits should return a List of ReportingMessages indicating that the logout was processed one or more times, or null if it was not processed
         */
        fun logout(): List<ReportingMessage>
    }

    /**
     * Kits should implement this interface in order to listen for eCommerce events
     */
    interface CommerceListener {
        /**
         * The mParticle SDK exposes a basic lifetime-value-increase API. This allows developers to increase the value associated with a user
         * in the case where no particular event is appropriate.
         *
         * @param valueIncreased the amount that the LTV should be increased by.
         * @param valueTotal     the total value that mParticle now associates with the user after this increase
         * @param eventName      an optional name to associate with the LTV increase
         * @param contextInfo    an optional Map of attributes to associate with the LTV increase
         * @return Kits should return a List of ReportingMessages indicating that the LTV increase was processed one or more times, or null if it was not processed
         */
        fun logLtvIncrease(
            valueIncreased: BigDecimal,
            valueTotal: BigDecimal,
            eventName: String,
            contextInfo: Map<String, String?>?
        ): List<ReportingMessage>

        /**
         * The mParticle SDK exposes a rich eCommerce API, allowing developers to keep track of purchases and many other
         * product-related interactions.
         *
         *
         * CommerceEvents may contain several Products, Impressions, and/or Promotions. Depending on the nature of your SDK's API, you should iterate over
         * the items within a CommerceEvent and forward several discrete events, returning a ReportingMessage for each API call.
         *
         * @param event the CommerceEvent that was logged
         * @return Kits should return a List of ReportingMessages indicating that the CommerceEvent was processed one or more times, or null if it was not processed
         */
        fun logEvent(event: CommerceEvent): List<ReportingMessage>
    }

    /**
     * Kits should implement this listener to ensure they receive events as they are
     * sent into the mParticle SDK. The methods in this listener generally have a 1-1 mapping
     * with the mParticle SDKs public API - it is the reasonability of Kit-writers to faithfully
     * map the appropriate mParticle APIs onto the proper APIs in their service.
     */
    interface EventListener {
        /**
         * The mParticle SDK exposes a breadcrumb API, allowing developers to track transactions as they occur.
         * Implementing Kits may optionally forward breadcrumbs to their SDK as regular events if there is no direct API analogue.
         *
         * @param breadcrumb a human-readable, typically short label of a step in a transaction, funnel, etc
         * @return Kits should return a List of ReportingMessages indicating that the breadcrumb was processed one or more times, or null if it was not processed
         */
        fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage>

        /**
         * The mParticle SDK exposes an error API, allowing developers to keep track of handled errors.
         *
         * @param message         a description of the error
         * @param errorAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the error was processed one or more times, or null if it was not processed
         */
        fun logError(
            message: String,
            errorAttributes: Map<String, String?>?
        ): List<ReportingMessage>

        /**
         * The mParticle SDK exposes an exception API, allowing developers to keep track of handled exceptions.
         *
         * @param exception           the exception that was caught
         * @param message             a description of the error
         * @param exceptionAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the exception was processed one or more times, or null if it was not processed
         */
        fun logException(
            exception: Exception,
            exceptionAttributes: Map<String, String?>?,
            message: String?
        ): List<ReportingMessage>

        /**
         * The mParticle SDK exposes a general event API, allowing developers to keep track of any activity with their app.
         *
         * @param event the MPEvent that was logged.
         * @return Kits should return a List of ReportingMessages indicating that the MPEvent was processed one or more times, or null if it was not processed
         */
        fun logEvent(event: MPEvent): List<ReportingMessage>

        /**
         * The mParticle SDK exposes a screen-view API, allowing developers to keep track of screens that are viewed by the user. Some mParticle integrations
         * use screen views for funnel analysis. Implementing Kits may optionally forward screen views to their SDK as regular events if there is no direct API analogue
         *
         * @param screenName       the name of the screen
         * @param screenAttributes optional Map of attributes associated with this event
         * @return Kits should return a List of ReportingMessages indicating that the screen-view was processed one or more times, or null if it was not processed
         */
        fun logScreen(
            screenName: String,
            screenAttributes: Map<String, String?>?
        ): List<ReportingMessage>
    }

    /**
     * Kits should implement this interface when they have Google Cloud Messaging/push features.
     */
    interface PushListener {
        /**
         * Kits must implement this method to inspect a push message to determine if it's intended for their
         * SDK. Typically, a given push service will include a predefined key within the payload of a
         * push message that uniquely identifies the push by company.
         *
         * @param intent the Intent object from the push-received broadcast
         * @return true if this push should be handled by the given Kit
         */
        fun willHandlePushMessage(intent: Intent?): Boolean

        /**
         * If a Kit returns true from [.willHandlePushMessage], this method will be called immediately after.
         * Kits should implement this method to show or otherwise react to a received push message.
         *
         * @param context
         * @param pushIntent
         */
        fun onPushMessageReceived(context: Context, pushIntent: Intent?)

        /**
         * This method will be called when the mParticle SDK successfully registers to receive
         * push messages. This method will be call on app startup and repetitively to aggressively
         * sync tokens as they're updated.
         *
         * @param instanceId the device instance ID registered with the FCM scope
         * @param senderId the senderid with permissions for this instanceId
         * @return true if the push registration was processed.
         */
        fun onPushRegistration(instanceId: String?, senderId: String?): Boolean
    }

    interface ApplicationStateListener {
        /**
         * Application has entered the foreground
         */
        fun onApplicationForeground()

        /**
         * Application has been sent to the background or terminated
         */
        fun onApplicationBackground()
    }

    interface IdentityListener {
        fun onIdentifyCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest
        )

        fun onLoginCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest
        )

        fun onLogoutCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest
        )

        fun onModifyCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest
        )

        fun onUserIdentified(mParticleUser: MParticleUser?)
    }

    interface UserAttributeListener {
        fun onIncrementUserAttribute(
            key: String,
            incrementedBy: Number,
            value: String?,
            user: FilteredMParticleUser?
        )

        fun onRemoveUserAttribute(key: String, user: FilteredMParticleUser?)

        fun onSetUserAttribute(key: String, value: Any?, user: FilteredMParticleUser?)

        fun onSetUserTag(key: String, user: FilteredMParticleUser?)

        fun onSetUserAttributeList(
            attributeKey: String,
            attributeValueList: List<String?>?,
            user: FilteredMParticleUser?
        )

        fun onSetAllUserAttributes(
            userAttributes: Map<String, String?>?,
            userAttributeLists: Map<String, List<String?>?>?,
            user: FilteredMParticleUser?
        )

        fun supportsAttributeLists(): Boolean

        fun onConsentStateUpdated(
            oldState: ConsentState?,
            newState: ConsentState?,
            user: FilteredMParticleUser?
        )
    }

    interface BatchListener {
        fun logBatch(jsonObject: JSONObject): List<ReportingMessage>
    }

    companion object {
        private const val KIT_PREFERENCES_FILE = "mp::kit::"
    }
}
