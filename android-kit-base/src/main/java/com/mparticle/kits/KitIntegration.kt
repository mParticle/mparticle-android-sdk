package com.mparticle.kits

import android.app.Activity
import android.content.Context
import com.mparticle.kits.DataplanFilter.transformIdentities
import com.mparticle.kits.DataplanFilter.transformUserAttributes
import com.mparticle.kits.KitManagerImpl
import com.mparticle.kits.KitConfiguration
import com.mparticle.kits.KitIntegration
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticle
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitUtils
import com.mparticle.kits.FilteredMParticleUser
import android.content.SharedPreferences
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.kits.FilteredIdentityApiRequest
import com.mparticle.consent.ConsentState
import org.jetbrains.annotations.Nullable
import org.json.JSONObject
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.HashMap

/**
 * Base Kit implementation - all Kits must subclass this.
 */
abstract class KitIntegration {
    // why is returning the concrete class instead of abstracion? - should it be nullable?
    var kitManager //nullable or lateinit? - what to do if null  should reference kitmanagerimpl or kitManager?
            : KitManagerImpl? = null
        private set

    /**
     * Get the configuration of the Kit. The KitConfiguration object contains
     * all of the settings and filters of a Kit. Generally there's no need to use
     * this API, and you can instead just call [.getSettings].
     *
     * @return
     */ //public with private setter...but.. should be accesible?
    var configuration //nullable or lateinit? - what to do if null
            : KitConfiguration? = null
        private set

    /**
     * Determine if the application is currently in the background. This is derived
     * by the mParticle SDK based on whether there is currently an Activity visible.
     *
     * @return true if the application is background
     */ //nullable or default?
    protected val isBackgrounded: Boolean
        protected get() = kitManager!!.isBackgrounded

    /**
     * Get an application Context object.
     *
     * @return an application Context, this will never be null.
     */ //nullable?
    val context: Context
        get() = kitManager!!.context// should be accesible?

    /**
     * Get a WeakReference to an Activity. The mParticle SDK maintains a WeakReference
     * to the currently visible Activity. This will sometimes return null (if the app is in the background), and will
     * sometimes return a WeakReference with no attached Activity - so be sure to code safely.
     *
     * @return a WeakReference to an Activity, or null.
     */
    val currentActivity: WeakReference<Activity>
        get() =// should be accesible?
            kitManager.getCurrentActivity()

    fun setConfiguration(configuration: KitConfiguration?): KitIntegration {
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
     */ // maybe we can get the casted value - nullable?
    open val instance: Any?
        get() = null
    open val isDisabled: Boolean
        get() = isDisabled(false)

    fun isDisabled(isOptOutEvent: Boolean): Boolean { //what happens if kitmanager is null or not initialized
        return !configuration!!.passesBracketing(kitManager!!.userBucket) ||
                configuration!!.shouldHonorOptOut() && kitManager!!.isOptedOut && !isOptOutEvent
    }//in which scenario the instance can be null?

    //which is the new function?
    @get:Deprecated("")
    val userIdentities: Map<IdentityType, String?>?
        get() {
            val instance = MParticle.getInstance() //in which scenario the instance can be null?
            if (instance != null) {
                val user = instance.Identity().currentUser
                if (user != null) {
                    var identities: Map<IdentityType, String?>? = user.userIdentities
                    identities = kitManager!!.dataplanFilter.transformIdentities(identities)
                    val filteredIdentities: MutableMap<IdentityType, String?> = HashMap(
                        identities!!.size
                    )
                    for ((key, value) in identities) {
                        if (configuration!!.shouldSetIdentity(key)) {
                            filteredIdentities[key] = value
                        }
                    }
                    return identities
                }
            }
            return HashMap()
        }//in which scenario the instance can be null?//map value can be null?

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
    @get:Deprecated("")
    val allUserAttributes: Map<String, Any?>
        get() { //map value can be null?
            val instance = MParticle.getInstance() //in which scenario the instance can be null?
            if (instance != null) {
                val user = instance.Identity().currentUser
                if (user != null) {
                    var userAttributes: Map<String?, Any?>? = user.userAttributes
                    if (kitManager != null) {
                        userAttributes =
                            kitManager!!.dataplanFilter.transformUserAttributes<Any?>(userAttributes)
                    }
                    val attributes = KitConfiguration.filterAttributes(
                        configuration!!.userAttributeFilters,
                        userAttributes
                    ) as MutableMap<String, Any?>
                    return if (this is AttributeListener && (this as AttributeListener).supportsAttributeLists()) {
                        attributes
                    } else {
                        for ((key, value) in attributes) {
                            if (value is List<*>) {
                                attributes[key] = KitUtils.join(value as List<*>?)
                            }
                        }
                        attributes
                    }
                }
            }
            return HashMap()
        }//in which scenario the instance can be null?

    //should be nullable?
    val currentUser: MParticleUser
        get() { //should be nullable?
            val instance = MParticle.getInstance() //in which scenario the instance can be null?
            if (instance != null) {
                val user = instance.Identity().currentUser
                return FilteredMParticleUser.getInstance(user, this)
            }
            return FilteredMParticleUser.getInstance(null, this)
        }

    fun getUser(mpid: Long?): MParticleUser { //should be nullable?
        val instance = MParticle.getInstance() //in which scenario the instance can be null?
        if (instance != null) {
            val user = instance.Identity().getUser(mpid!!)
            return FilteredMParticleUser.getInstance(user, this)
        }
        return FilteredMParticleUser.getInstance(null, this)
    }

    /**
     * Kits must override this an provide a human-readable name of their service. This name will
     * show up in ADB logs.
     *
     * @return the name of your company/sdk
     */
    abstract val name: String?

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
        settings: Map<String?, String?>?,
        context: Context?
    ): List<ReportingMessage?>? //nullable context, nullable string and key,value?

    /**
     * This method will be called when an integration has been disabled. Ideally, any unnecessary memory should
     * be cleaned up when this method is called. After this method is called, mParticle will no longer delegate events
     * to the kit, and on subsequent application startups, the Kit will not be initialized at all.
     */
    fun onKitDestroy() {}

    /**
     * Called by the KitManager when this kit is to be removed.
     */
    fun onKitCleanup() { //why try/catch?
        try {
            val allValues: Map<*, *>? = kitPreferences.all
            if (allValues != null && allValues.size > 0) {
                kitPreferences.edit().clear().apply()
            }
        } catch (npe: NullPointerException) {
        }
    }//what to do if configuration is null?

    /**
     * Get the SharedPreferences file for this Kit. Kits should use this rather than their own file, as the mParticle SDK
     * will automatically remove this file if the kit is removed from the app.
     *
     * @return
     */
    protected val kitPreferences: SharedPreferences
        protected get() =//what to do if configuration is null?
            context.getSharedPreferences(
                KIT_PREFERENCES_FILE + configuration!!.kitId,
                Context.MODE_PRIVATE
            )

    /**
     * The mParticle SDK is able to track a user's location based on provider and accuracy settings, and additionally allows
     * developers to manually set a location. In either case, this method will be called on a Kit whenever a new location object
     * is received.
     *
     * @param location
     */
    fun setLocation(location: Location?) {}

    /**
     * Retrieve the settings that are configured for this Kit, such as API key, etc
     *
     * @return a Map of settings
     */ // nullable map and key, value = what if configuration is null?
    val settings: Map<String, String>
        get() = configuration!!.settings

    fun logNetworkPerformance(
        url: String?,
        startTime: Long,
        method: String?,
        length: Long,
        bytesSent: Long,
        bytesReceived: Long,
        requestString: String?,
        responseCode: Int
    ): List<ReportingMessage>? {
        return null
    }

    fun getSurveyUrl(
        userAttributes: Map<String?, String?>?,
        userAttributeLists: Map<String?, List<String?>?>?
    ): Uri? {
        return null
    }

    /**
     * @param optedOut
     * @return
     */
    abstract fun setOptOut(optedOut: Boolean): List<ReportingMessage?>? // return type nullable and reportingMessage nullable?
    fun setKitManager(kitManager: KitManagerImpl?): KitIntegration { //nullable??
        this.kitManager = kitManager
        return this
    }

    fun logBaseEvent(baseEvent: BaseEvent): List<ReportingMessage>? { //if nullable why returns empty list?
        return emptyList()
    }

    open fun logEvent(baseEvent: MPEvent): List<ReportingMessage?>? { // if null why return empty list
        return emptyList<ReportingMessage>()
    }

    /**
     * Set integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @param integrationAttributes
     */
    protected fun setIntegrationAttributes(integrationAttributes: Map<String?, String?>?) { // param nullable, key, value pair nullable? - what to do if kitManager is null
        kitManager!!.setIntegrationAttributes(this, integrationAttributes)
    }// nullable? key,value pair nullable? - what to do if kitmanager is null?

    /**
     * Get integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @return
     */
    protected val integrationAttributes: Map<String, String>
        protected get() =// nullable? key,value pair nullable? - what to do if kitmanager is null?
            kitManager!!.getIntegrationAttributes(this)

    /**
     * Remove all integration attributes set for this integration.
     */
    protected fun clearIntegrationAttributes() { // what to do if kitmanager null?
        kitManager!!.clearIntegrationAttributes(this)
    }

    /**
     * Implement this method to receive com.android.vending.INSTALL_REFERRER Intents that
     * have been captured by the mParticle SDK. Developers/users of the SDK may also call setInstallReferrer
     * at any time after an install has occurred.
     *
     * @param intent an intent with the INSTALL_REFERRER action
     */
    fun setInstallReferrer(intent: Intent?) { // intent can be nullable?
    }

    /**
     * Implement this method to listen for when settings are updated while the kit is already active.
     *
     * @param settings
     */
    fun onSettingsUpdated(settings: Map<String?, String?>?) { //onsettings updated nullable? key,value pair nullable?
    }

    /**
     * Queues and groups network requests on the MParticle Core network handler
     */
    fun executeNetworkRequest(runnable: Runnable?) { // nullable runnable? - what if kitmanager is null?
        kitManager!!.runOnKitThread(runnable)
    }

    /**
     * Indicates that the user wishes to remove personal data and shutdown a Kit and/or underlying 3rd
     * party SDK
     */
    fun reset() {}

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
            @Nullable savedInstanceState: Bundle?
        ): List<ReportingMessage?>?

        fun onActivityStarted(activity: Activity): List<ReportingMessage?>? //returns nullable?
        fun onActivityResumed(activity: Activity): List<ReportingMessage?>? //returns nullable?
        fun onActivityPaused(activity: Activity): List<ReportingMessage?>? //returns nullable?
        fun onActivityStopped(activity: Activity): List<ReportingMessage?>? //returns nullable?
        fun onActivitySaveInstanceState(
            activity: Activity,
            @Nullable outState: Bundle?
        ): List<ReportingMessage?>?

        fun onActivityDestroyed(activity: Activity): List<ReportingMessage?>?
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
        fun onSessionStart(): List<ReportingMessage?>? //nullable, accept nulls within list?

        /**
         * mParticle will end a session when the app is sent to the background, after the session timeout (defaulted to 60s)
         *
         * @return
         */
        fun onSessionEnd(): List<ReportingMessage?>? //nullable, accept nulls within list?
    }

    /**
     * Kits should implement this interface when their underlying service has the notion
     * of a user with attributes.
     */
    @Deprecated("") //which instead? - can we remove or divide the implementation and compose in the child class?
    interface AttributeListener {
        fun setUserAttribute(
            attributeKey: String?,
            attributeValue: String?
        ) //value nullable, aceepts nulls? - what about key?

        fun setUserAttributeList(
            attributeKey: String?,
            attributeValueList: List<String?>?
        ) //list nullable, aceepts nulls? - what about key?

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
            userAttributes: Map<String?, String?>?,
            userAttributeLists: Map<String?, List<String?>?>?
        ) //define nullables

        fun removeUserAttribute(key: String?) //accept key null? -should do this..
        fun setUserIdentity(identityType: IdentityType?, identity: String?) //define nullables
        fun removeUserIdentity(identityType: IdentityType?) //accept identityType null? -should do this..

        /**
         * The mParticle SDK exposes a logout API, allowing developers to track an event
         * when a user logs out of their app/platform. Use this opportunity to perform the appropriate logic
         * as per your platforms logout paradigm, such as clearing user attributes.
         *
         * @return Kits should return a List of ReportingMessages indicating that the logout was processed one or more times, or null if it was not processed
         */
        fun logout(): List<ReportingMessage?>? //nullable return
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
            valueIncreased: BigDecimal?,
            valueTotal: BigDecimal?,
            eventName: String?,
            contextInfo: Map<String?, String?>?
        ): List<ReportingMessage?>? //define nullables

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
        fun logEvent(event: CommerceEvent?): List<ReportingMessage?>? //define nullables
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
        fun leaveBreadcrumb(breadcrumb: String?): List<ReportingMessage?>? //define nullables

        /**
         * The mParticle SDK exposes an error API, allowing developers to keep track of handled errors.
         *
         * @param message         a description of the error
         * @param errorAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the error was processed one or more times, or null if it was not processed
         */
        fun logError(
            message: String?,
            errorAttributes: Map<String?, String?>?
        ): List<ReportingMessage?>? //define nullables

        /**
         * The mParticle SDK exposes an exception API, allowing developers to keep track of handled exceptions.
         *
         * @param exception           the exception that was caught
         * @param message             a description of the error
         * @param exceptionAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the exception was processed one or more times, or null if it was not processed
         */
        fun logException(
            exception: Exception?,
            exceptionAttributes: Map<String?, String?>?,
            message: String?
        ): List<ReportingMessage?>? //define nullables

        /**
         * The mParticle SDK exposes a general event API, allowing developers to keep track of any activity with their app.
         *
         * @param event the MPEvent that was logged.
         * @return Kits should return a List of ReportingMessages indicating that the MPEvent was processed one or more times, or null if it was not processed
         */
        fun logEvent(event: MPEvent?): List<ReportingMessage?>? //define nullables

        /**
         * The mParticle SDK exposes a screen-view API, allowing developers to keep track of screens that are viewed by the user. Some mParticle integrations
         * use screen views for funnel analysis. Implementing Kits may optionally forward screen views to their SDK as regular events if there is no direct API analogue
         *
         * @param screenName       the name of the screen
         * @param screenAttributes optional Map of attributes associated with this event
         * @return Kits should return a List of ReportingMessages indicating that the screen-view was processed one or more times, or null if it was not processed
         */
        fun logScreen(
            screenName: String?,
            screenAttributes: Map<String?, String?>?
        ): List<ReportingMessage?>? //define nullables
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
        fun willHandlePushMessage(intent: Intent?): Boolean //define nullables

        /**
         * If a Kit returns true from [.willHandlePushMessage], this method will be called immediately after.
         * Kits should implement this method to show or otherwise react to a received push message.
         *
         * @param context
         * @param pushIntent
         */
        fun onPushMessageReceived(context: Context?, pushIntent: Intent?) //define nullables

        /**
         * This method will be called when the mParticle SDK successfully registers to receive
         * push messages. This method will be call on app startup and repetitively to aggressively
         * sync tokens as they're updated.
         *
         * @param instanceId the device instance ID registered with the FCM scope
         * @param senderId the senderid with permissions for this instanceId
         * @return true if the push registration was processed.
         */
        fun onPushRegistration(instanceId: String?, senderId: String?): Boolean //define nullables
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
            identityApiRequest: FilteredIdentityApiRequest?
        ) //define nullables

        fun onLoginCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest?
        ) //define nullables

        fun onLogoutCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest?
        ) //define nullables

        fun onModifyCompleted(
            mParticleUser: MParticleUser?,
            identityApiRequest: FilteredIdentityApiRequest?
        ) //define nullables

        fun onUserIdentified(mParticleUser: MParticleUser?) //define nullables
    }

    interface UserAttributeListener {
        fun onIncrementUserAttribute(
            key: String?,
            incrementedBy: Number?,
            value: String?,
            user: FilteredMParticleUser?
        ) //define nullables

        fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) //define nullables
        fun onSetUserAttribute(
            key: String?,
            value: Any?,
            user: FilteredMParticleUser?
        ) //define nullables

        fun onSetUserTag(key: String?, user: FilteredMParticleUser?) //define nullables
        fun onSetUserAttributeList(
            attributeKey: String?,
            attributeValueList: List<String?>?,
            user: FilteredMParticleUser?
        ) //define nullables

        fun onSetAllUserAttributes(
            userAttributes: Map<String?, String?>?,
            userAttributeLists: Map<String?, List<String?>?>?,
            user: FilteredMParticleUser?
        ) //define nullables

        fun supportsAttributeLists(): Boolean //define nullables
        fun onConsentStateUpdated(
            oldState: ConsentState?,
            newState: ConsentState?,
            user: FilteredMParticleUser?
        ) //define nullables
    }

    interface BatchListener {
        fun logBatch(jsonObject: JSONObject?): List<ReportingMessage?>? //define nullables
    }

    companion object {
        private const val KIT_PREFERENCES_FILE = "mp::kit::"
    }
}