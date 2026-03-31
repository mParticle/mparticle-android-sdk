package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Handler
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.UserAttributes
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.FilteredIdentityApiRequest
import com.mparticle.kits.FilteredMParticleUser
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import com.mparticle.kits.KitUtils
import com.mparticle.kits.ReportingMessage
import java.math.BigDecimal
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList

class CleverTapKit :
    KitIntegration(),
    UserAttributeListener,
    CommerceListener,
    KitIntegration.EventListener,
    PushListener,
    IdentityListener {
    private var cleverTapInstance: CleverTapAPI? = null

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val accountID = settings[ACCOUNT_ID_KEY]
        require(!KitUtils.isEmpty(accountID)) { "CleverTap AccountID is empty." }
        val accountToken = settings[ACCOUNT_TOKEN_KEY]
        require(!KitUtils.isEmpty(accountToken)) { "CleverTap AccountToken is empty." }
        val region = settings[ACCOUNT_REGION_KEY]
        CleverTapAPI.changeCredentials(accountID, accountToken, region)
        ActivityLifecycleCallback.register(context.applicationContext as Application)
        cleverTapInstance = CleverTapAPI.getDefaultInstance(getContext())
        updateIntegrationAttributes()
        return emptyList()
    }

    /**
     * Sets the CleverTap Device ID as an mParticle integration attribute.
     * Need to poll for it as its set asynchronously within the SDK (on initial launch)
     */
    private fun updateIntegrationAttributes() {
        cleverTapInstance?.let {
            it.getCleverTapID { id ->
                if (!KitUtils.isEmpty(id)) {
                    val integrationAttributes = HashMap<String, String>(1)
                    integrationAttributes[CLEVERTAPID_INTEGRATION_KEY] = id
                } else {
                    if (handler == null) {
                        handler = Handler()
                    }
                    handler?.postDelayed({ updateIntegrationAttributes() }, 500)
                }
            }
        }
    }

    override fun getName(): String = KIT_NAME

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()
        cleverTapInstance?.let {
            it.setOptOut(optedOut)
            messages.add(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.OPT_OUT,
                    System.currentTimeMillis(),
                    null,
                ),
            )
        }
        return messages
    }

    override fun setLocation(location: Location) {
        cleverTapInstance?.location = location
    }

    override fun setInstallReferrer(intent: Intent) {
        cleverTapInstance?.pushInstallReferrer(intent.dataString)
    }

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        val info = event.customAttributeStrings
        val props = info?.let { HashMap<String, Any>(it) }
        cleverTapInstance?.pushEvent(event.eventName, props)
        val messages: MutableList<ReportingMessage> = LinkedList()
        messages.add(ReportingMessage.fromEvent(this, event))
        return messages
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        cleverTapInstance?.recordScreen(screenName)
        val messages: MutableList<ReportingMessage> = LinkedList()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                screenAttributes,
            ),
        )
        return messages
    }

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()

        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(
                Product.PURCHASE,
                true,
            ) &&
            event.products?.isNotEmpty() == true
        ) {
            val details = HashMap<String, Any>()
            val items = ArrayList<HashMap<String, Any>>()
            val eventAttributes = HashMap<String, String>()
            CommerceEventUtils.extractActionAttributes(event, eventAttributes)
            for ((key, value) in eventAttributes) {
                details[key] = value
            }
            val transactionId =
                if (
                    event.transactionAttributes != null &&
                    !MPUtility.isEmpty(
                        event.transactionAttributes?.id,
                    )
                ) {
                    event.transactionAttributes?.id
                } else {
                    null
                }
            if (transactionId != null) {
                details["Charged ID"] = transactionId
            }
            val products = event.products

            products?.let {
                for (i in products.indices) {
                    try {
                        val product = products[i]
                        val attrs = HashMap<String, String>()
                        CommerceEventUtils.extractProductFields(product, attrs)
                        val item = HashMap<String, Any>(attrs)
                        items.add(item)
                    } catch (t: Throwable) {
                        cleverTapInstance?.pushError("Error handling Commerce Event product: " + t.message, 512)
                    }
                }
            }
            cleverTapInstance?.pushChargedEvent(details, items)
            messages.add(ReportingMessage.fromEvent(this, event))
            return messages
        }
        val eventList = CommerceEventUtils.expand(event)
        for (i in eventList.indices) {
            try {
                logEvent(eventList[i])
                messages.add(ReportingMessage.fromEvent(this, event))
            } catch (e: Exception) {
                Logger.warning("Failed to call logCustomEvent to CleverTap kit: $e")
            }
        }
        return messages
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
        user: FilteredMParticleUser?,
    ) {
        if (attributeKey == null || attributeValueList == null) {
            return
        }
        cleverTapInstance!!.setMultiValuesForKey(attributeKey, ArrayList(attributeValueList))
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?,
    ) {
        // not supported
    }

    override fun onRemoveUserAttribute(
        keyIn: String,
        user: FilteredMParticleUser,
    ) {
        var key = keyIn
        if (UserAttributes.MOBILE_NUMBER == key) {
            key = PHONE
        } else {
            if (key.startsWith("$")) {
                key = key.substring(1)
            }
        }
        cleverTapInstance?.removeValueForKey(key)
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null || value == null) {
            return
        }
        var mappedKey = key
        var mappedValue: Any = value
        val profile =
            HashMap<String, Any>()
        when {
            BIRTHDAY == mappedKey -> {
                mappedKey = DOB
            }
            "name" == mappedKey -> {
                mappedKey = NAME
            }
            UserAttributes.GENDER == mappedKey -> {
                val genderValue = mappedValue as String
                mappedValue =
                    if (genderValue.contains("fe")) {
                        FEMALE
                    } else {
                        MALE
                    }
            }
            UserAttributes.MOBILE_NUMBER == mappedKey -> {
                mappedKey = PHONE
            }
            else -> {
                if (mappedKey.startsWith("$")) {
                    mappedKey = mappedKey.substring(1)
                }
            }
        }
        profile[mappedKey] = mappedValue
        cleverTapInstance?.pushProfile(profile)
    }

    override fun onSetUserTag(
        key: String,
        user: FilteredMParticleUser,
    ) {
        // not supported
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, List<String>>,
        user: FilteredMParticleUser,
    ) {
        if (!kitPreferences.getBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, false)) {
            for ((key, value) in userAttributes) {
                onSetUserAttribute(key, value, user)
            }
            for ((attributeKey, attributeValueList) in userAttributeLists) {
                onSetUserAttributeList(attributeKey, attributeValueList, user)
            }
            kitPreferences.edit().putBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, true).apply()
        }
    }

    override fun onConsentStateUpdated(
        oldState: ConsentState,
        newState: ConsentState,
        user: FilteredMParticleUser,
    ) {
        // not supported
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun willHandlePushMessage(intent: Intent): Boolean {
        if (intent.extras == null) {
            return false
        }
        val info = CleverTapAPI.getNotificationInfo(intent.extras)
        return info.fromCleverTap
    }

    override fun onPushMessageReceived(
        context: Context,
        pushIntent: Intent,
    ) {
        if (pushIntent.extras == null) {
            return
        }
        val extras = pushIntent.extras
        CleverTapAPI.createNotification(getContext(), extras)
    }

    override fun onPushRegistration(
        instanceId: String,
        senderId: String,
    ): Boolean {
        cleverTapInstance?.pushFcmRegistrationId(instanceId, true)
        return true
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        // not used
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser, false)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser, true)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        // not used
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        updateUser(mParticleUser, false)
    }

    private fun updateUser(
        mParticleUser: MParticleUser,
        isLogin: Boolean,
    ) {
        val profile = HashMap<String, Any>()
        val customerId = mParticleUser.userIdentities[MParticle.IdentityType.CustomerId]
        val email = mParticleUser.userIdentities[MParticle.IdentityType.Email]
        val fbid = mParticleUser.userIdentities[MParticle.IdentityType.Facebook]
        val gpid = mParticleUser.userIdentities[MParticle.IdentityType.Google]
        if (customerId != null) {
            profile[IDENTITY_IDENTITY] = customerId
        }
        if (email != null) {
            profile[IDENTITY_EMAIL] = email
        }
        if (fbid != null) {
            profile[IDENTITY_FACEBOOK] = fbid
        }
        if (gpid != null) {
            profile[IDENTITY_GOOGLE] = gpid
        }
        if (profile.isEmpty()) {
            return
        }
        if (isLogin) {
            cleverTapInstance?.onUserLogin(profile)
        } else {
            cleverTapInstance?.pushProfile(profile)
        }
    }

    companion object {
        private const val CLEVERTAP_KEY = "CleverTap"
        private const val ACCOUNT_ID_KEY = "AccountID"
        private const val ACCOUNT_TOKEN_KEY = "AccountToken"
        private const val ACCOUNT_REGION_KEY = "Region"
        private const val PREF_KEY_HAS_SYNCED_ATTRIBUTES = "clevertap::has_synced_attributes"
        private const val CLEVERTAPID_INTEGRATION_KEY = "clevertap_id_integration_setting"
        private const val IDENTITY_EMAIL = "Email"
        private const val IDENTITY_FACEBOOK = "FBID"
        private const val IDENTITY_GOOGLE = "GPID"
        private const val IDENTITY_IDENTITY = "Identity"
        private const val PHONE = "Phone"
        private const val NAME = "Name"
        private const val BIRTHDAY = "birthday"
        private const val DOB = "DOB"
        private const val MALE = "M"
        private const val FEMALE = "F"
        private const val KIT_NAME = CLEVERTAP_KEY
        private var handler: Handler? = null
    }
}
