package com.mparticle.kits

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.braze.Braze
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.BrazeUser
import com.braze.configuration.BrazeConfig
import com.braze.enums.BrazeSdkMetadata
import com.braze.enums.Gender
import com.braze.enums.Month
import com.braze.enums.NotificationSubscriptionType
import com.braze.enums.SdkFlavor
import com.braze.events.IValueCallback
import com.braze.models.outgoing.BrazeProperties
import com.braze.push.BrazeFirebaseMessagingService
import com.braze.push.BrazeNotificationUtils.isBrazePushMessage
import com.google.firebase.messaging.RemoteMessage
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticle.UserAttributes
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.CommerceEventUtils.OnAttributeExtracted
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.EventListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.LogoutListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.EnumSet
import java.util.LinkedList
import kotlin.collections.HashMap

/**
 * mParticle client-side Appboy integration
 */
open class AppboyKit :
    KitIntegration(),
    AttributeListener,
    LogoutListener,
    CommerceListener,
    KitIntegration.EventListener,
    PushListener,
    IdentityListener,
    KitIntegration.UserAttributeListener {
    var enableTypeDetection = false
    var bundleCommerceEvents = false
    var isMpidIdentityType = false
    var identityType: IdentityType? = null
    var subscriptionGroupIds: MutableMap<String, String>? = mutableMapOf()
    private val dataFlushHandler = Handler()
    private var dataFlushRunnable: Runnable? = null
    private var forwardScreenViews = false
    private lateinit var updatedInstanceId: String

    override fun getName() = NAME

    public override fun onKitCreate(
        settings: Map<String, String?>,
        context: Context,
    ): List<ReportingMessage>? {
        val key = settings[APPBOY_KEY]
        require(!KitUtils.isEmpty(key)) { "Braze key is empty." }

        // try to get endpoint from the host setting
        val authority = settings[HOST]
        if (!KitUtils.isEmpty(authority)) {
            setAuthority(authority)
        }
        val enableDetectionType = settings[ENABLE_TYPE_DETECTION]
        if (!KitUtils.isEmpty(enableDetectionType)) {
            try {
                enableTypeDetection = enableDetectionType.toBoolean()
            } catch (e: Exception) {
                Logger.warning("Braze, unable to parse \"enableDetectionType\"")
            }
        }
        val bundleCommerce = settings[BUNDLE_COMMERCE_EVENTS]
        if (!KitUtils.isEmpty(bundleCommerce)) {
            try {
                bundleCommerceEvents = bundleCommerce.toBoolean()
            } catch (e: Exception) {
                bundleCommerceEvents = false
            }
        }
        forwardScreenViews = settings[FORWARD_SCREEN_VIEWS].toBoolean()
        subscriptionGroupIds =
            settings[SUBSCRIPTION_GROUP_MAPPING]
                ?.let {
                    getSubscriptionGroupIds(it)
                }
        if (key != null) {
            val config =
                BrazeConfig
                    .Builder()
                    .setApiKey(key)
                    .setSdkFlavor(SdkFlavor.MPARTICLE)
                    .setSdkMetadata(EnumSet.of(BrazeSdkMetadata.MPARTICLE))
                    .build()
            Braze.configure(context, config)
        }
        dataFlushRunnable =
            Runnable {
                if (kitManager.isBackgrounded) {
                    Braze.getInstance(getContext()).requestImmediateDataFlush()
                }
            }
        queueDataFlush()
        if (setDefaultAppboyLifecycleCallbackListener) {
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(
                BrazeActivityLifecycleCallbackListener() as ActivityLifecycleCallbacks,
            )
        }
        setIdentityType(settings)

        val user = MParticle.getInstance()?.Identity()?.currentUser
        if (user != null) {
            updateUser(user)
        }
        val userConsentState = currentUser?.consentState
        userConsentState?.let {
            setConsent(currentUser.consentState)
        }
        return null
    }

    fun setIdentityType(settings: Map<String, String?>) {
        val userIdentificationType = settings[USER_IDENTIFICATION_TYPE]
        if (!KitUtils.isEmpty(userIdentificationType)) {
            if (userIdentificationType == "MPID") {
                isMpidIdentityType = true
            } else {
                identityType = userIdentificationType?.let { IdentityType.valueOf(it) }
            }
        }
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

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
        val newAttributes: MutableMap<String, Any?> = HashMap()
        if (event.customAttributes == null) {
            Braze.getInstance(context).logCustomEvent(event.eventName)
        } else {
            val properties = BrazeProperties()
            val brazePropertiesSetter = BrazePropertiesSetter(properties, enableTypeDetection)
            event.customAttributeStrings?.let { it ->
                for ((key, value) in it) {
                    newAttributes[key] = brazePropertiesSetter.parseValue(key, value)
                }
            }
            Braze.getInstance(context).logCustomEvent(event.eventName, properties)
            Braze
                .getInstance(context)
                .getCurrentUser(
                    object : IValueCallback<BrazeUser> {
                        override fun onSuccess(value: BrazeUser) {
                            val userAttributeSetter = UserAttributeSetter(value, enableTypeDetection)
                            event.customAttributeStrings?.let { it ->
                                for ((key, attributeValue) in it) {
                                    val hashedKey =
                                        KitUtils.hashForFiltering(event.eventType.value.toString() + event.eventName + key)

                                    configuration.eventAttributesAddToUser?.get(hashedKey)?.let {
                                        value.addToCustomAttributeArray(it, attributeValue)
                                    }
                                    configuration.eventAttributesRemoveFromUser?.get(hashedKey)?.let {
                                        value.removeFromCustomAttributeArray(it, attributeValue)
                                    }
                                    configuration.eventAttributesSingleItemUser?.get(hashedKey)?.let {
                                        userAttributeSetter.parseValue(it, attributeValue)
                                    }
                                }
                            }
                        }

                        override fun onError() {
                            Logger.warning("unable to acquire user to add or remove custom user attributes from events")
                        }
                    },
                )
        }
        queueDataFlush()
        return listOf(ReportingMessage.fromEvent(this, event).setAttributes(newAttributes))
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>?,
    ): List<ReportingMessage> =
        if (forwardScreenViews) {
            if (screenAttributes == null) {
                Braze.getInstance(context).logCustomEvent(screenName)
            } else {
                val properties = BrazeProperties()
                val propertyParser = BrazePropertiesSetter(properties, enableTypeDetection)
                for ((key, value) in screenAttributes) {
                    propertyParser.parseValue(key, value)
                }
                Braze.getInstance(context).logCustomEvent(screenName, properties)
            }
            queueDataFlush()
            val messages: MutableList<ReportingMessage> = LinkedList()
            messages.add(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.SCREEN_VIEW,
                    System.currentTimeMillis(),
                    screenAttributes,
                ),
            )
            messages
        } else {
            emptyList()
        }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()
        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(
                Product.PURCHASE,
                true,
            ) &&
            !event.products.isNullOrEmpty()
        ) {
            if (bundleCommerceEvents) {
                logOrderLevelTransaction(event)
                messages.add(ReportingMessage.fromEvent(this, event))
            } else {
                val productList = event.products
                productList?.let {
                    for (product in productList) {
                        logTransaction(event, product)
                    }
                }
            }
            messages.add(ReportingMessage.fromEvent(this, event))
        } else {
            if (bundleCommerceEvents) {
                logOrderLevelTransaction(event)
                messages.add(ReportingMessage.fromEvent(this, event))
            } else {
                val eventList = CommerceEventUtils.expand(event)
                if (eventList != null) {
                    for (i in eventList.indices) {
                        try {
                            val e = eventList[i]
                            val map = mutableMapOf<String, String>()
                            event.customAttributeStrings?.let { map.putAll(it) }
                            for (pair in map) {
                                e.customAttributes?.put(pair.key, pair.value)
                            }
                            logEvent(e)
                            messages.add(ReportingMessage.fromEvent(this, event))
                        } catch (e: Exception) {
                            Logger.warning("Failed to call logCustomEvent to Appboy kit: $e")
                        }
                    }
                }
            }
        }
        queueDataFlush()
        return messages
    }

    private fun applyScalarUserAttribute(
        keyIn: String,
        attributeValue: String,
    ) {
        Braze
            .getInstance(context)
            .getCurrentUser(
                object : IValueCallback<BrazeUser> {
                    override fun onSuccess(value: BrazeUser) {
                        val userAttributeSetter = UserAttributeSetter(value, enableTypeDetection)
                        applyScalarUserAttributeOnUser(value, keyIn, attributeValue, userAttributeSetter)
                        queueDataFlush()
                    }

                    override fun onError() {
                        Logger.warning("unable to set key: " + keyIn + " with value: " + attributeValue)
                    }
                },
            )
    }

    private fun applyScalarUserAttributeOnUser(
        user: BrazeUser,
        key: String,
        attributeValue: String,
        userAttributeSetter: UserAttributeSetter,
    ) {
        when (key) {
            UserAttributes.CITY -> user.setHomeCity(attributeValue)
            UserAttributes.COUNTRY -> user.setCountry(attributeValue)
            UserAttributes.FIRSTNAME -> user.setFirstName(attributeValue)
            UserAttributes.LASTNAME -> user.setLastName(attributeValue)
            UserAttributes.MOBILE_NUMBER -> user.setPhoneNumber(attributeValue)
            UserAttributes.ZIPCODE -> user.setCustomUserAttribute("Zip", attributeValue)
            UserAttributes.AGE -> setAgeAsDateOfBirthFromUserAttribute(user, attributeValue)
            EMAIL_SUBSCRIBE -> setEmailSubscriptionFromUserAttribute(user, attributeValue)
            PUSH_SUBSCRIBE -> setPushSubscriptionFromUserAttribute(user, attributeValue)
            DOB -> useDobString(attributeValue, user)
            UserAttributes.GENDER -> setGenderFromUserAttribute(user, attributeValue)
            else -> applyUnmappedScalarUserAttribute(user, key, attributeValue, userAttributeSetter)
        }
    }

    private fun setAgeAsDateOfBirthFromUserAttribute(
        user: BrazeUser,
        attributeValue: String,
    ) {
        val calendar = getCalendarMinusYears(attributeValue)
        if (calendar != null) {
            user.setDateOfBirth(calendar[Calendar.YEAR], Month.JANUARY, 1)
        } else {
            Logger.warning("unable to set DateOfBirth for " + UserAttributes.AGE + " = " + attributeValue)
        }
    }

    private fun setEmailSubscriptionFromUserAttribute(
        user: BrazeUser,
        attributeValue: String,
    ) {
        when (attributeValue) {
            OPTED_IN -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
            UNSUBSCRIBED -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
            SUBSCRIBED -> user.setEmailNotificationSubscriptionType(NotificationSubscriptionType.SUBSCRIBED)
            else -> Logger.warning("unable to set email_subscribe with invalid value: $attributeValue")
        }
    }

    private fun setPushSubscriptionFromUserAttribute(
        user: BrazeUser,
        attributeValue: String,
    ) {
        when (attributeValue) {
            OPTED_IN -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.OPTED_IN)
            UNSUBSCRIBED -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.UNSUBSCRIBED)
            SUBSCRIBED -> user.setPushNotificationSubscriptionType(NotificationSubscriptionType.SUBSCRIBED)
            else -> Logger.warning("unable to set push_subscribe with invalid value: $attributeValue")
        }
    }

    private fun setGenderFromUserAttribute(
        user: BrazeUser,
        attributeValue: String,
    ) {
        if (attributeValue.contains("fe")) {
            user.setGender(Gender.FEMALE)
        } else {
            user.setGender(Gender.MALE)
        }
    }

    private fun applySubscriptionGroupAttribute(
        user: BrazeUser,
        key: String,
        attributeValue: String,
    ) {
        val groupId = subscriptionGroupIds?.get(key)
        when (attributeValue.lowercase()) {
            "true" -> groupId?.let { user.addToSubscriptionGroup(it) }
            "false" -> groupId?.let { user.removeFromSubscriptionGroup(it) }
            else ->
                Logger.warning(
                    "Unable to set Subscription Group ID for user attribute: $key due to invalid value data type. Expected Boolean.",
                )
        }
    }

    private fun applyUnmappedScalarUserAttribute(
        user: BrazeUser,
        key: String,
        attributeValue: String,
        userAttributeSetter: UserAttributeSetter,
    ) {
        if (subscriptionGroupIds?.containsKey(key) == true) {
            applySubscriptionGroupAttribute(user, key, attributeValue)
        } else {
            val customKey = if (key.startsWith("$")) key.substring(1) else key
            userAttributeSetter.parseValue(customKey, attributeValue)
        }
    }

    // Expected Date Format @"yyyy'-'MM'-'dd"
    private fun useDobString(
        value: String,
        user: BrazeUser,
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        try {
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(value) as Date
            val year = calendar[Calendar.YEAR]
            val monthNum = calendar[Calendar.MONTH]
            val month = Month.values()[monthNum] //
            val day = calendar[Calendar.DAY_OF_MONTH]
            user.setDateOfBirth(year, month, day)
        } catch (e: Exception) {
            Logger.warning("unable to set DateOfBirth for \"dob\" = " + value + ". Exception: " + e.message)
        }
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?,
    ) {
    }

    override fun onRemoveUserAttribute(
        key: String?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null) {
            return
        }
        var keyMut = key
        Braze
            .getInstance(context)
            .getCurrentUser(
                object : IValueCallback<BrazeUser> {
                    override fun onSuccess(value: BrazeUser) {
                        when (keyMut) {
                            UserAttributes.CITY -> value.setHomeCity(null)
                            UserAttributes.COUNTRY -> value.setCountry(null)
                            UserAttributes.FIRSTNAME -> value.setFirstName(null)
                            UserAttributes.LASTNAME -> value.setLastName(null)
                            UserAttributes.MOBILE_NUMBER -> value.setPhoneNumber(null)
                            else -> {
                                var customKey = keyMut
                                if (customKey.startsWith("$")) {
                                    customKey = customKey.substring(1)
                                }
                                value.unsetCustomUserAttribute(customKey)
                            }
                        }
                        queueDataFlush()
                    }

                    override fun onError() {
                        Logger.warning("unable to remove User Attribute with key: " + key)
                    }
                },
            )
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null || value == null || value !is String) {
            return
        }
        applyScalarUserAttribute(key, value)
    }

    override fun onSetUserTag(
        key: String?,
        user: FilteredMParticleUser?,
    ) {
        // No-op: this kit does not implement this feature.
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: MutableList<String>?,
        user: FilteredMParticleUser?,
    ) {
        if (attributeKey == null || attributeValueList == null) {
            return
        }
        Braze
            .getInstance(context)
            .getCurrentUser(
                object : IValueCallback<BrazeUser> {
                    override fun onSuccess(value: BrazeUser) {
                        val array = attributeValueList.toTypedArray<String?>()
                        value.setCustomAttributeArray(attributeKey, array)
                        queueDataFlush()
                    }

                    override fun onError() {
                        Logger.warning(
                            "unable to set key: " + attributeKey + " with User Attribute List: " + attributeValueList,
                        )
                    }
                },
            )
    }

    override fun onSetAllUserAttributes(
        userAttributes: MutableMap<String, String>?,
        userAttributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?,
    ) {
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun onConsentStateUpdated(
        oldState: ConsentState,
        newState: ConsentState,
        user: FilteredMParticleUser,
    ) {
        setConsent(newState)
    }

    private fun setConsent(consentState: ConsentState) {
        val clientConsentSettings = parseToNestedMap(consentState.toString())

        parseConsentMapping(settings[CONSENT_MAPPING_SDK]).iterator().forEach { currentConsent ->
            val isConsentAvailable =
                searchKeyInNestedMap(clientConsentSettings, key = currentConsent.key)

            if (isConsentAvailable != null) {
                val isConsentGranted: Boolean =
                    JSONObject(isConsentAvailable.toString()).opt("consented") as Boolean

                when (currentConsent.value) {
                    "google_ad_user_data" ->
                        setConsentValueToBraze(
                            KEY_GOOGLE_AD_USER_DATA,
                            isConsentGranted,
                        )

                    "google_ad_personalization" ->
                        setConsentValueToBraze(
                            KEY_GOOGLE_AD_PERSONALIZATION,
                            isConsentGranted,
                        )
                }
            }
        }
    }

    private fun setConsentValueToBraze(
        key: String,
        value: Boolean,
    ) {
        Braze
            .getInstance(context)
            .getCurrentUser(
                object : IValueCallback<BrazeUser> {
                    override fun onSuccess(brazeUser: BrazeUser) {
                        brazeUser.setCustomUserAttribute(key, value)
                    }

                    override fun onError() {
                        super.onError()
                    }
                },
            )
    }

    private fun parseConsentMapping(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) {
            return emptyMap()
        }
        val jsonWithFormat = json.replace("\\", "")

        return try {
            JSONArray(jsonWithFormat)
                .let { jsonArray ->
                    (0 until jsonArray.length())
                        .associate {
                            val jsonObject = jsonArray.getJSONObject(it)
                            val map = jsonObject.getString("map")
                            val value = jsonObject.getString("value")
                            map to value
                        }
                }
        } catch (jse: JSONException) {
            Logger.warning(
                jse,
                "The Braze kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
            )
            emptyMap()
        }
    }

    private fun parseToNestedMap(jsonString: String): Map<String, Any> {
        val topLevelMap = mutableMapOf<String, Any>()
        try {
            val jsonObject = JSONObject(jsonString)

            for (key in jsonObject.keys()) {
                val value = jsonObject.get(key)
                if (value is JSONObject) {
                    topLevelMap[key] = parseToNestedMap(value.toString())
                } else {
                    topLevelMap[key] = value
                }
            }
        } catch (e: Exception) {
            Logger.error(e, "The Braze kit was unable to parse the user's ConsentState, consent may not be set correctly on the Braze SDK")
        }
        return topLevelMap
    }

    private fun searchKeyInNestedMap(
        map: Map<*, *>,
        key: Any,
    ): Any? {
        if (map.isNullOrEmpty()) {
            return null
        }
        try {
            for ((mapKey, mapValue) in map) {
                if (mapKey.toString().equals(key.toString(), ignoreCase = true)) {
                    return mapValue
                }
                if (mapValue is Map<*, *>) {
                    val foundValue = searchKeyInNestedMap(mapValue, key)
                    if (foundValue != null) {
                        return foundValue
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(
                e,
                "The Braze kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
            )
        }
        return null
    }

    protected open fun queueDataFlush() {
        dataFlushRunnable?.let { dataFlushHandler.removeCallbacks(it) }
        dataFlushRunnable?.let { dataFlushHandler.postDelayed(it, FLUSH_DELAY.toLong()) }
    }

    /**
     * This is called when the Kit is added to the mParticle SDK, typically on app-startup.
     */
    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
    ) {
        if (!kitPreferences.getBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, false)) {
            for ((key, value) in attributes) {
                applyScalarUserAttribute(key, value)
            }
            for ((key, value) in attributeLists) {
                Braze
                    .getInstance(context)
                    .getCurrentUser(
                        object : IValueCallback<BrazeUser> {
                            override fun onSuccess(brazeUser: BrazeUser) {
                                val array = value.toTypedArray<String?>()
                                brazeUser.setCustomAttributeArray(key, array)
                                queueDataFlush()
                            }

                            override fun onError() {
                                Logger.warning("unable to set key: " + key + " with User Attribute List: " + value)
                            }
                        },
                    )
            }
            kitPreferences.edit().putBoolean(PREF_KEY_HAS_SYNCED_ATTRIBUTES, true).apply()
        }
    }

    override fun setUserIdentity(
        identityType: IdentityType,
        identity: String,
    ) {}

    override fun removeUserIdentity(identityType: IdentityType) {
        // No-op: this kit does not implement this feature.
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    fun logTransaction(
        event: CommerceEvent?,
        product: Product,
    ) {
        val purchaseProperties = BrazeProperties()
        val currency = arrayOfNulls<String>(1)
        val commerceTypeParser: StringTypeParser =
            BrazePropertiesSetter(
                purchaseProperties,
                enableTypeDetection,
            )
        val onAttributeExtracted: OnAttributeExtracted =
            object : OnAttributeExtracted {
                override fun onAttributeExtracted(
                    key: String,
                    value: String,
                ) {
                    if (!checkCurrency(key, value)) {
                        commerceTypeParser.parseValue(key, value)
                    }
                }

                override fun onAttributeExtracted(
                    key: String,
                    value: Double,
                ) {
                    if (!checkCurrency(key, value)) {
                        purchaseProperties.addProperty(key, value)
                    }
                }

                override fun onAttributeExtracted(
                    key: String,
                    value: Int,
                ) {
                    purchaseProperties.addProperty(key, value)
                }

                override fun onAttributeExtracted(attributes: Map<String, String>) {
                    for ((key, value) in attributes) {
                        if (!checkCurrency(key, value)) {
                            commerceTypeParser.parseValue(key, value)
                        }
                    }
                }

                private fun checkCurrency(
                    key: String,
                    value: Any?,
                ): Boolean =
                    if (CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE ==
                        key
                    ) {
                        currency[0] = value?.toString()
                        true
                    } else {
                        false
                    }
            }
        CommerceEventUtils.extractActionAttributes(event, onAttributeExtracted)
        var currencyValue = currency[0]
        if (KitUtils.isEmpty(currencyValue)) {
            currencyValue = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE
        }

        event?.customAttributes?.let {
            for ((key, value) in it) {
                purchaseProperties.addProperty(key, value)
            }
        }

        product.couponCode?.let {
            purchaseProperties.addProperty(
                CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE,
                it,
            )
        }
        product.brand?.let {
            purchaseProperties.addProperty(CommerceEventUtils.Constants.ATT_PRODUCT_BRAND, it)
        }
        product.category?.let {
            purchaseProperties.addProperty(CommerceEventUtils.Constants.ATT_PRODUCT_CATEGORY, it)
        }
        product.name.let {
            purchaseProperties.addProperty(CommerceEventUtils.Constants.ATT_PRODUCT_NAME, it)
        }
        product.variant?.let {
            purchaseProperties.addProperty(CommerceEventUtils.Constants.ATT_PRODUCT_VARIANT, it)
        }
        product.position?.let {
            purchaseProperties.addProperty(CommerceEventUtils.Constants.ATT_PRODUCT_POSITION, it)
        }
        product.customAttributes?.let {
            for ((key, value) in it) {
                purchaseProperties.addProperty(key, value)
            }
        }

        var sanitizedProductName: String = product.sku
        try {
            if (settings[REPLACE_SKU_AS_PRODUCT_NAME] == "True") {
                sanitizedProductName = product.name
            }
        } catch (e: Exception) {
            Logger.error(e, "The Braze kit threw an exception while searching for forward sku as product name flag.")
        }

        Braze.Companion.getInstance(context).logPurchase(
            sanitizedProductName,
            currencyValue,
            BigDecimal(product.unitPrice),
            product.quantity.toInt(),
            purchaseProperties,
        )
    }

    fun logOrderLevelTransaction(event: CommerceEvent?) {
        val properties = BrazeProperties()
        val currency = arrayOfNulls<String>(1)
        val commerceTypeParser: StringTypeParser =
            BrazePropertiesSetter(
                properties,
                enableTypeDetection,
            )
        val onAttributeExtracted: OnAttributeExtracted =
            object : OnAttributeExtracted {
                override fun onAttributeExtracted(
                    key: String,
                    value: String,
                ) {
                    if (!checkCurrency(key, value)) {
                        commerceTypeParser.parseValue(key, value)
                    }
                }

                override fun onAttributeExtracted(
                    key: String,
                    value: Double,
                ) {
                    if (!checkCurrency(key, value)) {
                        properties.addProperty(key, value)
                    }
                }

                override fun onAttributeExtracted(
                    key: String,
                    value: Int,
                ) {
                    properties.addProperty(key, value)
                }

                override fun onAttributeExtracted(attributes: Map<String, String>) {
                    for ((key, value) in attributes) {
                        if (!checkCurrency(key, value)) {
                            commerceTypeParser.parseValue(key, value)
                        }
                    }
                }

                private fun checkCurrency(
                    key: String,
                    value: Any?,
                ): Boolean =
                    if (CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE ==
                        key
                    ) {
                        currency[0] = value?.toString()
                        true
                    } else {
                        false
                    }
            }
        CommerceEventUtils.extractActionAttributes(event, onAttributeExtracted)
        var currencyValue = currency[0]
        if (KitUtils.isEmpty(currencyValue)) {
            currencyValue = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE
        }

        event?.customAttributes?.let {
            properties.addProperty(CUSTOM_ATTRIBUTES_KEY, it)
        }

        val productList = event?.products
        productList?.let {
            val productArray = getProductListParameters(it)
            properties.addProperty(PRODUCT_KEY, productArray)
        }

        val promotionList = event?.promotions
        promotionList?.let {
            val promotionArray = getPromotionListParameters(it)
            properties.addProperty(PROMOTION_KEY, promotionArray)
        }

        val impressionList = event?.impressions
        impressionList?.let {
            val impressionArray = getImpressionListParameters(it)
            properties.addProperty(IMPRESSION_KEY, impressionArray)
        }

        val eventName = "eCommerce - %s"
        if (!KitUtils.isEmpty(event?.productAction) &&
            event?.productAction.equals(Product.PURCHASE, true)
        ) {
            Braze.Companion.getInstance(context).logPurchase(
                String.format(eventName, event?.productAction),
                currencyValue,
                event?.transactionAttributes?.revenue?.let { BigDecimal(it) } ?: BigDecimal(0),
                1,
                properties,
            )
        } else {
            if (!KitUtils.isEmpty(event?.productAction)) {
                Braze
                    .getInstance(context)
                    .logCustomEvent(
                        String.format(eventName, event?.productAction),
                        properties,
                    )
            } else if (!KitUtils.isEmpty(event?.promotionAction)) {
                Braze
                    .getInstance(context)
                    .logCustomEvent(
                        String.format(eventName, event?.promotionAction),
                        properties,
                    )
            } else {
                Braze
                    .getInstance(context)
                    .logCustomEvent(
                        String.format(eventName, "Impression"),
                        properties,
                    )
            }
        }
    }

    override fun willHandlePushMessage(intent: Intent): Boolean =
        if (!(settings[PUSH_ENABLED].toBoolean())) {
            false
        } else {
            intent.isBrazePushMessage()
        }

    override fun onPushMessageReceived(
        context: Context,
        pushIntent: Intent,
    ) {
        if (settings[PUSH_ENABLED].toBoolean()) {
            BrazeFirebaseMessagingService.handleBrazeRemoteMessage(
                context,
                RemoteMessage(pushIntent.extras),
            )
        }
    }

    override fun onPushRegistration(
        instanceId: String,
        senderId: String,
    ): Boolean =
        if (settings[PUSH_ENABLED].toBoolean()) {
            updatedInstanceId = instanceId
            Braze.getInstance(context).registeredPushToken = instanceId
            queueDataFlush()
            true
        } else {
            false
        }

    protected open fun setAuthority(authority: String?) {
        Braze.setEndpointProvider { appboyEndpoint ->
            appboyEndpoint
                .buildUpon()
                .authority(authority)
                .build()
        }
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?,
    ) {
        updateUser(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?,
    ) {
        updateUser(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?,
    ) {
        updateUser(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest?,
    ) {
        updateUser(mParticleUser)
    }

    private fun updateUser(mParticleUser: MParticleUser) {
        val identity = getIdentity(isMpidIdentityType, identityType, mParticleUser)
        val email = mParticleUser.userIdentities[IdentityType.Email]
        identity?.let { setId(it) }
        email?.let { setEmail(it) }
    }

    fun getIdentity(
        isMpidIdentityType: Boolean,
        identityType: IdentityType?,
        mParticleUser: MParticleUser?,
    ): String? {
        var identity: String? = null
        if (isMpidIdentityType && mParticleUser != null) {
            identity = mParticleUser.id.toString()
        } else if (identityType != null && mParticleUser != null) {
            identity = mParticleUser.userIdentities[identityType]
        }
        return identity
    }

    protected open fun setId(customerId: String) {
        Braze
            .getInstance(context)
            .getCurrentUser(
                object : IValueCallback<BrazeUser> {
                    override fun onSuccess(value: BrazeUser) {
                        if (value.userId != customerId) {
                            Braze.getInstance(context).changeUser(customerId)
                            queueDataFlush()
                        }
                    }

                    override fun onError() {
                        Logger.warning("unable to change user to customer ID: " + customerId)
                    }
                },
            )
    }

    protected open fun setEmail(email: String) {
        if (email != kitPreferences.getString(PREF_KEY_CURRENT_EMAIL, null)) {
            Braze
                .getInstance(context)
                .getCurrentUser(
                    object : IValueCallback<BrazeUser> {
                        override fun onSuccess(value: BrazeUser) {
                            value.setEmail(email)
                            queueDataFlush()
                            kitPreferences
                                .edit()
                                .putString(PREF_KEY_CURRENT_EMAIL, email)
                                .apply()
                        }

                        override fun onError() {
                            Logger.warning("unable to set email with value: " + email)
                        }
                    },
                )
        }
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        if (updatedInstanceId.isNotEmpty()) {
            Braze.getInstance(context).registeredPushToken = updatedInstanceId
        }
    }

    fun addToProperties(
        properties: BrazeProperties,
        key: String,
        value: String,
    ) {
        try {
            if ("true".equals(value, true) ||
                "false".equals(
                    value,
                    true,
                )
            ) {
                properties.addProperty(key, (value).toBoolean())
            } else {
                val doubleValue = value.toDouble()
                if (doubleValue % 1 == 0.0) {
                    properties.addProperty(key, value.toInt())
                } else {
                    properties.addProperty(key, doubleValue)
                }
            }
        } catch (e: Exception) {
            properties.addProperty(key, value)
        }
    }

    fun getCalendarMinusYears(yearsString: String): Calendar? {
        try {
            val years = yearsString.toInt()
            return getCalendarMinusYears(years)
        } catch (ignored: NumberFormatException) {
            try {
                val years = yearsString.toDouble()
                return getCalendarMinusYears(years.toInt())
            } catch (ignoredToo: NumberFormatException) {
            }
        }
        return null
    }

    fun getCalendarMinusYears(years: Int): Calendar? =
        if (years >= 0) {
            val calendar = Calendar.getInstance()
            calendar[Calendar.YEAR] = calendar[Calendar.YEAR] - years
            calendar
        } else {
            null
        }

    fun getProductListParameters(productList: List<Product>): JSONArray {
        val productArray = JSONArray()
        for ((i, product) in productList.withIndex()) {
            val productProperties = JSONObject()

            product.customAttributes?.let {
                productProperties.put(CUSTOM_ATTRIBUTES_KEY, it)
            }
            product.couponCode?.let {
                productProperties.put(
                    CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE,
                    it,
                )
            }
            product.brand?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_BRAND, it)
            }
            product.category?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_CATEGORY, it)
            }
            product.name?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_NAME, it)
            }
            product.sku?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_ID, it)
            }
            product.variant?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_VARIANT, it)
            }
            product.position?.let {
                productProperties.put(CommerceEventUtils.Constants.ATT_PRODUCT_POSITION, it)
            }
            productProperties.put(
                CommerceEventUtils.Constants.ATT_PRODUCT_PRICE,
                product.unitPrice,
            )
            productProperties.put(
                CommerceEventUtils.Constants.ATT_PRODUCT_QUANTITY,
                product.quantity,
            )
            productProperties.put(
                CommerceEventUtils.Constants.ATT_PRODUCT_TOTAL_AMOUNT,
                product.totalAmount,
            )

            productArray.put(productProperties)
        }
        return productArray
    }

    fun getPromotionListParameters(promotionList: List<Promotion>): JSONArray {
        val promotionArray = JSONArray()
        for ((i, promotion) in promotionList.withIndex()) {
            val promotionProperties = JSONObject()
            promotion.creative?.let {
                promotionProperties.put(
                    CommerceEventUtils.Constants.ATT_PROMOTION_CREATIVE,
                    it,
                )
            }
            promotion.id?.let {
                promotionProperties.put(CommerceEventUtils.Constants.ATT_PROMOTION_ID, it)
            }
            promotion.name?.let {
                promotionProperties.put(CommerceEventUtils.Constants.ATT_PROMOTION_NAME, it)
            }
            promotion.position?.let {
                promotionProperties.put(
                    CommerceEventUtils.Constants.ATT_PROMOTION_POSITION,
                    it,
                )
            }
            promotionArray.put(promotionProperties)
        }
        return promotionArray
    }

    private fun getSubscriptionGroupIds(subscriptionGroupMap: String): MutableMap<String, String> {
        val subscriptionGroupIds = mutableMapOf<String, String>()

        if (subscriptionGroupMap.isEmpty()) {
            return subscriptionGroupIds
        }

        val subscriptionGroupsArray = JSONArray(subscriptionGroupMap)

        return try {
            for (i in 0 until subscriptionGroupsArray.length()) {
                val subscriptionGroup = subscriptionGroupsArray.getJSONObject(i)
                val key = subscriptionGroup.getString("map")
                val value = subscriptionGroup.getString("value")
                subscriptionGroupIds[key] = value
            }
            subscriptionGroupIds
        } catch (e: JSONException) {
            Logger.warning("Braze, unable to parse \"subscriptionGroup\"")
            mutableMapOf()
        }
    }

    fun getImpressionListParameters(impressionList: List<Impression>): JSONArray {
        val impressionArray = JSONArray()
        for ((i, impression) in impressionList.withIndex()) {
            val impressionProperties = JSONObject()
            impression.listName?.let {
                impressionProperties.put("Product Impression List", it)
            }
            impression.products?.let {
                val productArray = getProductListParameters(it)
                impressionProperties.put(PRODUCT_KEY, productArray)
            }
            impressionArray.put(impressionProperties)
        }
        return impressionArray
    }

    internal abstract class StringTypeParser(
        var enableTypeDetection: Boolean,
    ) {
        fun parseValue(
            key: String,
            value: String,
        ): Any {
            if (!enableTypeDetection) {
                toString(key, value)
                return value
            }
            return if (
                true.toString().equals(value, true) ||
                false.toString().equals(
                    value,
                    true,
                )
            ) {
                val newBool = (value).toBoolean()
                toBoolean(key, newBool)
                newBool
            } else {
                try {
                    if (value.contains(".")) {
                        val doubleValue = value.toDouble()
                        toDouble(key, doubleValue)
                        doubleValue
                    } else {
                        val newLong = value.toLong()
                        if (newLong <= Int.MAX_VALUE && newLong >= Int.MIN_VALUE) {
                            val newInt = newLong.toInt()
                            toInt(key, newInt)
                            newInt
                        } else {
                            toLong(key, newLong)
                            newLong
                        }
                    }
                } catch (nfe: NumberFormatException) {
                    toString(key, value)
                    value
                }
            }
        }

        abstract fun toInt(
            key: String,
            value: Int,
        )

        abstract fun toLong(
            key: String,
            value: Long,
        )

        abstract fun toDouble(
            key: String,
            value: Double,
        )

        abstract fun toBoolean(
            key: String,
            value: Boolean,
        )

        abstract fun toString(
            key: String,
            value: String,
        )
    }

    internal inner class BrazePropertiesSetter(
        private var properties: BrazeProperties,
        enableTypeDetection: Boolean,
    ) : StringTypeParser(enableTypeDetection) {
        override fun toInt(
            key: String,
            value: Int,
        ) {
            properties.addProperty(key, value)
        }

        override fun toLong(
            key: String,
            value: Long,
        ) {
            properties.addProperty(key, value)
        }

        override fun toDouble(
            key: String,
            value: Double,
        ) {
            properties.addProperty(key, value)
        }

        override fun toBoolean(
            key: String,
            value: Boolean,
        ) {
            properties.addProperty(key, value)
        }

        override fun toString(
            key: String,
            value: String,
        ) {
            properties.addProperty(key, value)
        }
    }

    internal inner class UserAttributeSetter(
        private var brazeUser: BrazeUser,
        enableTypeDetection: Boolean,
    ) : StringTypeParser(enableTypeDetection) {
        override fun toInt(
            key: String,
            value: Int,
        ) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toLong(
            key: String,
            value: Long,
        ) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toDouble(
            key: String,
            value: Double,
        ) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toBoolean(
            key: String,
            value: Boolean,
        ) {
            brazeUser.setCustomUserAttribute(key, value)
        }

        override fun toString(
            key: String,
            value: String,
        ) {
            brazeUser.setCustomUserAttribute(key, value)
        }
    }

    companion object {
        const val APPBOY_KEY = "apiKey"
        const val FORWARD_SCREEN_VIEWS = "forwardScreenViews"
        const val USER_IDENTIFICATION_TYPE = "userIdentificationType"
        const val ENABLE_TYPE_DETECTION = "enableTypeDetection"
        const val BUNDLE_COMMERCE_EVENTS = "bundleCommerceEventData"
        const val SUBSCRIPTION_GROUP_MAPPING = "subscriptionGroupMapping"
        const val HOST = "host"
        const val PUSH_ENABLED = "push_enabled"
        const val NAME = "Appboy"

        // if this flag is true, kit will send Product name as sku
        const val REPLACE_SKU_AS_PRODUCT_NAME = "replaceSkuWithProductName"
        private const val PREF_KEY_HAS_SYNCED_ATTRIBUTES = "appboy::has_synced_attributes"
        private const val PREF_KEY_CURRENT_EMAIL = "appboy::current_email"
        private const val FLUSH_DELAY = 5000
        var setDefaultAppboyLifecycleCallbackListener = true

        private const val EMAIL_SUBSCRIBE = "email_subscribe"
        private const val PUSH_SUBSCRIBE = "push_subscribe"
        private const val DOB = "dob"

        private const val OPTED_IN = "opted_in"
        private const val UNSUBSCRIBED = "unsubscribed"
        private const val SUBSCRIBED = "subscribed"

        // Constants for Read Consent
        private const val CONSENT_MAPPING_SDK = "consentMappingSDK"
        private const val KEY_GOOGLE_AD_USER_DATA = "\$google_ad_user_data"
        private const val KEY_GOOGLE_AD_PERSONALIZATION = "\$google_ad_personalization"

        const val CUSTOM_ATTRIBUTES_KEY = "Attributes"
        const val PRODUCT_KEY = "products"
        const val PROMOTION_KEY = "promotions"
        const val IMPRESSION_KEY = "impressions"
    }
}
