package com.mparticle.kits

import android.R.attr
import android.app.Application
import android.content.Context
import android.content.Intent
import com.leanplum.Leanplum
import com.leanplum.LeanplumActivityHelper
import com.leanplum.LeanplumDeviceIdMode
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.TypedUserAttributeListener
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.FilteredIdentityApiRequest
import com.mparticle.kits.FilteredMParticleUser
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.EventListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import com.mparticle.kits.ReportingMessage
import java.math.BigDecimal
import java.util.HashMap
import java.util.LinkedList

class LeanplumKit :
    KitIntegration(),
    UserAttributeListener,
    KitIntegration.EventListener,
    CommerceListener,
    IdentityListener,
    PushListener {
    public override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val deviceIdType = settings[DEVICE_ID_TYPE]

        val userId = generateLeanplumId()

        if (deviceIdType != null) {
            setDeviceIdType(deviceIdType)
        }
        if (MParticle.getInstance()?.environment == MParticle.Environment.Development) {
            Leanplum.setLogLevel(3)
            Leanplum.setAppIdForDevelopmentMode(settings[APP_ID_KEY], settings[CLIENT_KEY_KEY])
        } else {
            Leanplum.setAppIdForProductionMode(settings[APP_ID_KEY], settings[CLIENT_KEY_KEY])
        }

        // Starting Leanplum with empty map to avoid db query, setting it after calling async fun
        Leanplum.start(context, userId)
        LeanplumActivityHelper.enableLifecycleCallbacks(context.applicationContext as Application)
        MParticle
            .getInstance()
            ?.Identity()
            ?.currentUser
            ?.getUserAttributes()

        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null,
            ),
        )
    }

    private fun generateLeanplumId(): String? =
        MParticle.getInstance()?.Identity()?.currentUser?.let {
            generateLeanplumUserId(
                it,
                settings,
                it.userIdentities ?: emptyMap(),
            )?.ifEmpty { null }
        }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        // do nothing
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        // do nothing
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        // do nothing
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        // do nothing
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        val userIdentities = mParticleUser.userIdentities
        val userId = generateLeanplumUserId(mParticleUser, settings, userIdentities)
        // first set userId to effectively switch users
        if (!KitUtils.isEmpty(userId)) {
            Leanplum.setUserId(userId)
        }
        // then set the attributes of the new user
        try {
            mParticleUser.getUserAttributes(
                object : TypedUserAttributeListener {
                    override fun onUserAttributesReceived(
                        userAttributes: Map<String, Any?>,
                        userAttributeLists: Map<String, List<String?>?>,
                        mpid: Long,
                    ) {
                        val attributes: MutableMap<String, Any?> = HashMap()
                        attributes.putAll(userAttributes)
                        attributes.putAll(userAttributeLists)
                        setLeanplumUserAttributes(userIdentities, attributes)
                    }
                },
            )
        } catch (e: Exception) {
            Logger.warning(e, "Unable to fetch User Attributes")
        }
    }

    private fun setLeanplumUserAttributes(
        userIdentities: Map<IdentityType, String>,
        userAttributes: MutableMap<String, Any?>,
    ) {
        if (!userAttributes.containsKey(LEANPLUM_EMAIL_USER_ATTRIBUTE) &&
            configuration.shouldSetIdentity(
                IdentityType.Email,
            )
        ) {
            if (userIdentities.containsKey(IdentityType.Email)) {
                userAttributes[LEANPLUM_EMAIL_USER_ATTRIBUTE] =
                    userIdentities[IdentityType.Email]
            } else {
                userAttributes[LEANPLUM_EMAIL_USER_ATTRIBUTE] = null
            }
        }
        setAttributesAndCheckId(userAttributes)
    }

    private fun setAttributesAndCheckId(userAttributes: MutableMap<String, Any?>) {
        // If by the time onKitCreated was called, userAttributes were not available to create a LeanplumId, creating and setting one
        if (Leanplum.getUserId().isNullOrEmpty()) {
            generateLeanplumId()?.let { id ->
                Leanplum.setUserAttributes(id, userAttributes)
            } ?: run {
                Leanplum.setUserAttributes(userAttributes)
            }
        } else {
            Leanplum.setUserAttributes(userAttributes)
        }
        // per Leanplum - it's a good idea to make sure the SDK refreshes itself
        Leanplum.forceContentUpdate()
    }

    fun generateLeanplumUserId(
        user: MParticleUser?,
        settings: Map<String, String>,
        userIdentities: Map<IdentityType, String>,
    ): String? {
        var userId: String? = null

        if (USER_ID_CUSTOMER_ID_VALUE.equals(
                settings[USER_ID_FIELD_KEY],
                ignoreCase = true,
            ) &&
            configuration.shouldSetIdentity(
                IdentityType.CustomerId,
            )
        ) {
            userId = userIdentities[IdentityType.CustomerId]
        } else if (USER_ID_EMAIL_VALUE.equals(
                settings[USER_ID_FIELD_KEY],
                ignoreCase = true,
            ) &&
            configuration.shouldSetIdentity(
                IdentityType.Email,
            )
        ) {
            userId = userIdentities[IdentityType.Email]
        } else if (USER_ID_MPID_VALUE.equals(
                settings[USER_ID_FIELD_KEY],
                ignoreCase = true,
            )
        ) {
            if (user != null) {
                userId = user.id.toString()
            }
        }
        return userId
    }

    override fun getName(): String = NAME

    // Leanplum doesn't have the notion of opt-out.
    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null) {
            return
        }
        val attributes = mutableMapOf<String, Any?>()
        attributes[key] = value
        setAttributesAndCheckId(attributes)
    }

    override fun onSetUserTag(
        s: String,
        filteredMParticleUser: FilteredMParticleUser,
    ) {}

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
        user: FilteredMParticleUser?,
    ) {
        if (attributeKey == null || attributeValueList == null) {
            return
        }
        val attributes = mutableMapOf<String, Any?>()
        attributes[attributeKey] = attributeValueList
        setAttributesAndCheckId(attributes)
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        newValue: String,
        user: FilteredMParticleUser?,
    ) {
        val attributes = mutableMapOf<String, Any?>()
        attributes[attr.key.toString()] = newValue
        setAttributesAndCheckId(attributes)
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onConsentStateUpdated(
        consentState: ConsentState,
        consentState1: ConsentState,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
    }

    override fun onSetAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
        user: FilteredMParticleUser,
    ) {
        val map = mutableMapOf<String, Any?>()
        map.putAll(attributes)
        map.putAll(attributeLists)
        setAttributesAndCheckId(map)
        // we set user attributes on start so there's no point in doing it here as well.
    }

    override fun onRemoveUserAttribute(
        key: String,
        user: FilteredMParticleUser,
    ) {
        val attributes = mutableMapOf<String, Any?>()
        attributes[key] = null
        setAttributesAndCheckId(attributes)
    }

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

    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage> {
        Leanplum.track(mpEvent.eventName, mpEvent.customAttributes)
        return listOf(ReportingMessage.fromEvent(this, mpEvent))
    }

    override fun logScreen(
        screenName: String,
        attributes: Map<String, String>,
    ): List<ReportingMessage> {
        Leanplum.advanceTo(screenName, attributes)
        return listOf(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                attributes,
            ),
        )
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        total: BigDecimal,
        eventName: String,
        attributes: Map<String, String>,
    ): List<ReportingMessage> {
        Leanplum.track(eventName, valueIncreased.toDouble(), attributes)
        return listOf(
            ReportingMessage.fromEvent(
                this,
                MPEvent
                    .Builder(eventName, MParticle.EventType.Transaction)
                    .customAttributes(attributes)
                    .build(),
            ),
        )
    }

    private fun logTransaction(
        event: CommerceEvent,
        product: Product,
    ) {
        val eventAttributes = HashMap<String, String>()
        CommerceEventUtils.extractActionAttributes(event, eventAttributes)
        Leanplum.track(
            Leanplum.PURCHASE_EVENT_NAME,
            product.totalAmount,
            product.name,
            eventAttributes,
        )
    }

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages = LinkedList<ReportingMessage>()
        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(
                Product.PURCHASE,
                ignoreCase = true,
            ) &&
            !event.products.isNullOrEmpty()
        ) {
            val productList = event.products
            if (productList != null) {
                for (product in productList) {
                    logTransaction(event, product)
                }
            }
            messages.add(ReportingMessage.fromEvent(this, event))
            return messages
        }
        val eventList = CommerceEventUtils.expand(event)
        if (eventList != null) {
            for (i in eventList.indices) {
                try {
                    logEvent(eventList[i])
                    messages.add(ReportingMessage.fromEvent(this, event))
                } catch (e: Exception) {
                    Logger.warning("Failed to call track to Leanplum kit: $e")
                }
            }
        }
        return messages
    }

    fun setDeviceIdType(deviceIdType: String?) {
        if (DEVICE_ID_TYPE_ANDROID_ID == deviceIdType && MParticle.isAndroidIdEnabled()) {
            Leanplum.setDeviceIdMode(LeanplumDeviceIdMode.ANDROID_ID)
        } else if (DEVICE_ID_TYPE_GOOGLE_AD_ID == deviceIdType) {
            Leanplum.setDeviceIdMode(LeanplumDeviceIdMode.ADVERTISING_ID)
        } else if (DEVICE_ID_TYPE_DAS == deviceIdType) {
            Leanplum.setDeviceId(MParticle.getInstance()?.Identity()?.deviceApplicationStamp)
        }
    }

    override fun willHandlePushMessage(intent: Intent): Boolean = intent.extras?.containsKey("lp_version") ?: false

    override fun onPushMessageReceived(
        context: Context,
        intent: Intent,
    ) {
        // Firebase only
    }

    override fun onPushRegistration(
        s: String,
        s1: String,
    ): Boolean {
        // Firebase only
        return false
    }

    companion object {
        private const val APP_ID_KEY = "appId"
        private const val CLIENT_KEY_KEY = "clientKey"
        const val USER_ID_FIELD_KEY = "userIdField"
        const val USER_ID_CUSTOMER_ID_VALUE = "customerId"
        const val USER_ID_EMAIL_VALUE = "email"
        const val USER_ID_MPID_VALUE = "mpid"
        const val LEANPLUM_EMAIL_USER_ATTRIBUTE = "email"
        const val DEVICE_ID_TYPE = "androidDeviceId"
        const val DEVICE_ID_TYPE_GOOGLE_AD_ID = "gaid"
        const val DEVICE_ID_TYPE_ANDROID_ID = "androidId"
        const val DEVICE_ID_TYPE_DAS = "das"
        const val NAME = "Leanplum"
    }
}
