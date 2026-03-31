package com.mparticle.kits

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.EventType
import com.mparticle.TypedUserAttributeListener
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.EnumMap

class GoogleAnalyticsFirebaseKit :
    KitIntegration(),
    KitIntegration.EventListener,
    IdentityListener,
    CommerceListener,
    KitIntegration.UserAttributeListener {
    override fun getName(): String = KIT_NAME

    @Throws(IllegalArgumentException::class)
    public override fun onKitCreate(
        map: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        Logger.info(
            "$name Kit relies on a functioning instance of Firebase Analytics. If your Firebase Analytics instance is not configured properly, this Kit will not work",
        )
        val userConsentState = currentUser?.consentState
        userConsentState?.let {
            setConsent(currentUser.consentState)
        }
        return emptyList()
    }

    override fun setOptOut(b: Boolean): List<ReportingMessage> = emptyList()

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
        getFirebaseEventName(mpEvent)?.let {
            FirebaseAnalytics
                .getInstance(context)
                .logEvent(it, toBundle(mpEvent.customAttributeStrings))
        }
        return listOf(ReportingMessage.fromEvent(this, mpEvent))
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>?,
    ): List<ReportingMessage> {
        val bundle = toBundle(screenAttributes)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, standardizeName(screenName, true))
        val activity = currentActivity.get()
        if (activity != null) {
            FirebaseAnalytics
                .getInstance(context)
                .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            return listOf(
                ReportingMessage(
                    this,
                    ReportingMessage.MessageType.SCREEN_VIEW,
                    System.currentTimeMillis(),
                    null,
                ),
            )
        }
        return emptyList()
    }

    override fun logLtvIncrease(
        bigDecimal: BigDecimal,
        bigDecimal1: BigDecimal,
        s: String,
        map: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        val instance = FirebaseAnalytics.getInstance(context)
        if (commerceEvent.productAction == null) {
            return emptyList()
        }
        val bundle =
            getCommerceEventBundle(commerceEvent)
                .bundle
        val eventName: String =
            when (commerceEvent.productAction) {
                Product.ADD_TO_CART -> FirebaseAnalytics.Event.ADD_TO_CART
                Product.ADD_TO_WISHLIST -> FirebaseAnalytics.Event.ADD_TO_WISHLIST
                Product.CHECKOUT -> FirebaseAnalytics.Event.BEGIN_CHECKOUT
                Product.PURCHASE -> FirebaseAnalytics.Event.PURCHASE
                Product.REFUND -> FirebaseAnalytics.Event.REFUND
                Product.REMOVE_FROM_CART -> FirebaseAnalytics.Event.REMOVE_FROM_CART
                Product.CLICK -> FirebaseAnalytics.Event.SELECT_CONTENT
                Product.CHECKOUT_OPTION -> {
                    val warningMessage = WARNING_MESSAGE
                    val customFlags = commerceEvent.customFlags
                    if ((customFlags != null) && customFlags.containsKey(CF_COMMERCE_EVENT_TYPE)) {
                        val commerceEventTypes =
                            customFlags[CF_COMMERCE_EVENT_TYPE]
                        if (!commerceEventTypes.isNullOrEmpty()) {
                            when (commerceEventTypes[0]) {
                                FirebaseAnalytics.Event.ADD_SHIPPING_INFO -> {
                                    FirebaseAnalytics.Event.ADD_SHIPPING_INFO
                                }
                                FirebaseAnalytics.Event.ADD_PAYMENT_INFO -> {
                                    FirebaseAnalytics.Event.ADD_PAYMENT_INFO
                                }
                                else -> {
                                    Logger.warning(warningMessage)
                                    return emptyList()
                                }
                            }
                        } else {
                            Logger.warning(warningMessage)
                            return emptyList()
                        }
                    } else {
                        Logger.warning(warningMessage)
                        return emptyList()
                    }
                }
                Product.DETAIL -> FirebaseAnalytics.Event.VIEW_ITEM
                else -> return emptyList()
            }
        instance.logEvent(eventName, bundle)
        return listOf(ReportingMessage.fromEvent(this, commerceEvent))
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserId(mParticleUser)
        try {
            mParticleUser.getUserAttributes(
                object : TypedUserAttributeListener {
                    override fun onUserAttributesReceived(
                        userAttributes: Map<String, Any?>,
                        userAttributeLists: Map<String, List<String?>?>,
                        mpid: Long,
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val stringAttributes = userAttributes.mapValues { it.value?.toString() }.filterValues { it != null } as Map<String, String>
                        onSetAllUserAttributes(stringAttributes, null, null)
                    }
                },
            )
        } catch (e: Exception) {
            Logger.warning(e, "Unable to fetch User Attributes")
        }
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserId(mParticleUser)
        try {
            mParticleUser.getUserAttributes(
                object : TypedUserAttributeListener {
                    override fun onUserAttributesReceived(
                        userAttributes: Map<String, Any?>,
                        userAttributeLists: Map<String, List<String?>?>,
                        mpid: Long,
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val stringAttributes = userAttributes.mapValues { it.value?.toString() }.filterValues { it != null } as Map<String, String>
                        onSetAllUserAttributes(stringAttributes, null, null)
                    }
                },
            )
        } catch (e: Exception) {
            Logger.warning(e, "Unable to fetch User Attributes")
        }
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserId(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        setUserId(mParticleUser)
        try {
            mParticleUser.getUserAttributes(
                object : TypedUserAttributeListener {
                    override fun onUserAttributesReceived(
                        userAttributes: Map<String, Any?>,
                        userAttributeLists: Map<String, List<String?>?>,
                        mpid: Long,
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val stringAttributes = userAttributes.mapValues { it.value?.toString() }.filterValues { it != null } as Map<String, String>
                        onSetAllUserAttributes(stringAttributes, null, null)
                    }
                },
            )
        } catch (e: Exception) {
            Logger.warning(e, "Unable to fetch User Attributes")
        }
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    private fun setUserId(user: MParticleUser?) {
        var userId: String? = null
        if (user != null) {
            if (USER_ID_CUSTOMER_ID_VALUE.equals(settings[USER_ID_FIELD_KEY], true)) {
                userId = user.userIdentities[MParticle.IdentityType.CustomerId]
            } else if (USER_ID_EMAIL_VALUE.equals(settings[USER_ID_FIELD_KEY], true)) {
                userId = user.userIdentities[MParticle.IdentityType.Email]
            } else if (USER_ID_MPID_VALUE.equals(settings[USER_ID_FIELD_KEY], true)) {
                userId = user.id.toString()
            }
        }
        if (!KitUtils.isEmpty(userId)) {
            FirebaseAnalytics.getInstance(context).setUserId(userId)
        }
    }

    private fun getFirebaseEventName(event: MPEvent): String? {
        if (event.eventType == EventType.Search) {
            return FirebaseAnalytics.Event.SEARCH
        }
        return if (event.isScreenEvent) {
            FirebaseAnalytics.Event.VIEW_ITEM
        } else {
            standardizeName(event.eventName, true)
        }
    }

    private fun toBundle(mapIn: Map<String, String>?): Bundle {
        var map = mapIn
        val bundle = Bundle()
        map = standardizeAttributes(map, true)
        if (map != null) {
            for ((key, value) in map) {
                bundle.putString(key, value)
            }
        }
        return bundle
    }

    private fun getCommerceEventBundle(commerceEvent: CommerceEvent): PickyBundle {
        val pickyBundle = getTransactionAttributesBundle(commerceEvent)
        var currency = commerceEvent.currency
        if (currency == null) {
            Logger.info(CURRENCY_FIELD_NOT_SET)
            currency = USD
        }
        commerceEvent.customAttributes?.let { customAttributes ->
            for (attributes in customAttributes) {
                pickyBundle.putString(attributes.key, attributes.value.toString())
            }
        }
        val customFlags = commerceEvent.customFlags
        if (customFlags != null && customFlags.containsKey(CF_COMMERCE_EVENT_TYPE)) {
            val commerceEventTypeList = customFlags[CF_COMMERCE_EVENT_TYPE]
            if (!commerceEventTypeList.isNullOrEmpty()) {
                val commerceEventType = commerceEventTypeList[0]
                if (commerceEventType == FirebaseAnalytics.Event.ADD_SHIPPING_INFO) {
                    val shippingTier = customFlags[CF_SHIPPING_TIER]
                    if (!shippingTier.isNullOrEmpty()) {
                        pickyBundle.putString(
                            FirebaseAnalytics.Param.SHIPPING_TIER,
                            shippingTier[0],
                        )
                    }
                } else if (commerceEventType == FirebaseAnalytics.Event.ADD_PAYMENT_INFO) {
                    val paymentType = customFlags[CF_PAYMENT_TYPE]
                    if (!paymentType.isNullOrEmpty()) {
                        pickyBundle.putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentType[0])
                    }
                }
            }
        }

        pickyBundle
            .putString(FirebaseAnalytics.Param.CURRENCY, currency)
            .putBundleList(FirebaseAnalytics.Param.ITEMS, getProductBundles(commerceEvent))

        return pickyBundle
    }

    private fun getProductBundles(commerceEvent: CommerceEvent): Array<Bundle?> {
        val products = commerceEvent.products
        if (products != null) {
            val bundles = arrayOfNulls<Bundle>(products.size)
            var i = 0
            for (product in products) {
                val bundle =
                    getBundle(product)
                        .putString(FirebaseAnalytics.Param.CURRENCY, commerceEvent.currency)
                bundles[i] = bundle.bundle
                i++
            }
            return bundles
        }
        return arrayOfNulls(0)
    }

    private fun getTransactionAttributesBundle(commerceEvent: CommerceEvent): PickyBundle {
        val pickyBundle = PickyBundle()
        val transactionAttributes = commerceEvent.transactionAttributes
        return if (commerceEvent.transactionAttributes == null) {
            pickyBundle
        } else {
            pickyBundle
                .putString(
                    FirebaseAnalytics.Param.TRANSACTION_ID,
                    transactionAttributes?.id,
                ).putDouble(
                    FirebaseAnalytics.Param.VALUE,
                    transactionAttributes?.revenue,
                ).putDouble(
                    FirebaseAnalytics.Param.TAX,
                    transactionAttributes?.tax,
                ).putDouble(
                    FirebaseAnalytics.Param.SHIPPING,
                    transactionAttributes?.shipping,
                ).putString(
                    FirebaseAnalytics.Param.COUPON,
                    transactionAttributes?.couponCode,
                )
        }
    }

    private fun getBundle(product: Product): PickyBundle =
        PickyBundle()
            .putLong(FirebaseAnalytics.Param.QUANTITY, product.quantity.toLong())
            .putString(FirebaseAnalytics.Param.ITEM_ID, product.sku)
            .putString(FirebaseAnalytics.Param.ITEM_NAME, product.name)
            .putString(FirebaseAnalytics.Param.ITEM_CATEGORY, product.category)
            .putDouble(FirebaseAnalytics.Param.PRICE, product.unitPrice)

    private fun getValue(commerceEvent: CommerceEvent): Double? {
        var value = 0.0
        val products = commerceEvent.products ?: return null
        for (product in products) {
            value += product.quantity * product.unitPrice
        }
        return value
    }

    override fun onIncrementUserAttribute(
        key: String,
        incrementedBy: Number,
        value: String,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
        standardizeName(key, false)?.let {
            FirebaseAnalytics.getInstance(context).setUserProperty(
                it,
                value,
            )
        }
    }

    override fun onRemoveUserAttribute(
        key: String,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
        standardizeName(key, false)?.let {
            FirebaseAnalytics.getInstance(context).setUserProperty(
                it,
                null,
            )
        }
    }

    /**
     * We are going to ignore Lists here, since Firebase only supports String "user property" values
     */
    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null) {
            return
        }
        if (value is String) {
            standardizeName(key, false)?.let {
                FirebaseAnalytics.getInstance(context).setUserProperty(
                    it,
                    standardizeValue(value, false),
                )
            }
        }
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
        // not supported
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, List<String>>?,
        filteredMParticleUser: FilteredMParticleUser?,
    ) {
        var userAttributes: Map<String, String>? = userAttributes
        userAttributes = standardizeAttributes(userAttributes, false)
        if (userAttributes != null) {
            for ((key, value) in userAttributes) {
                FirebaseAnalytics.getInstance(context).setUserProperty(key, value)
            }
        }
    }

    override fun supportsAttributeLists(): Boolean = false

    override fun onConsentStateUpdated(
        consentState: ConsentState,
        consentState1: ConsentState,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
        setConsent(consentState1)
    }

    private fun setConsent(consentState: ConsentState) {
        val consentMap: MutableMap<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> =
            EnumMap(
                FirebaseAnalytics.ConsentType::class.java,
            )
        googleConsentMapSettings.forEach { it ->
            val mpConsentSetting = settings[it.value]
            if (!mpConsentSetting.isNullOrEmpty()) {
                if (mpConsentSetting == GoogleConsentValues.GRANTED.consentValue) {
                    consentMap[it.key] = FirebaseAnalytics.ConsentStatus.GRANTED
                } else if (mpConsentSetting == GoogleConsentValues.DENIED.consentValue) {
                    consentMap[it.key] = FirebaseAnalytics.ConsentStatus.DENIED
                }
            }
        }

        val clientConsentSettings = parseToNestedMap(consentState.toString())

        parseConsentMapping(settings[CONSENT_MAPPING_SDK]).iterator().forEach { currentConsent ->

            val isConsentAvailable =
                searchKeyInNestedMap(clientConsentSettings, key = currentConsent.key)

            if (isConsentAvailable != null) {
                val isConsentGranted: Boolean =
                    JSONObject(isConsentAvailable.toString()).opt("consented") as Boolean
                val consentStatus =
                    if (isConsentGranted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED

                when (currentConsent.value) {
                    "ad_storage" ->
                        consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] =
                            consentStatus

                    "ad_user_data" ->
                        consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] =
                            consentStatus

                    "ad_personalization" ->
                        consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] =
                            consentStatus

                    "analytics_storage" ->
                        consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] =
                            consentStatus
                }
            }
        }
        if (consentMap.isNotEmpty()) {
            FirebaseAnalytics.getInstance(context).setConsent(consentMap)
        }
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
                "The Google Firebase kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
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
            Logger.error(
                e,
                "The Google Firebase kit was unable to parse the user's ConsentState, consent may not be set correctly on the Google Analytics SDK",
            )
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
                "The Google Firebase kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
            )
        }
        return null
    }

    fun standardizeAttributes(
        attributes: Map<String, String>?,
        event: Boolean,
    ): Map<String, String>? {
        if (attributes == null) {
            return null
        }
        val attributeCopy = HashMap<String, String>()
        for ((key, value) in attributes) {
            attributeCopy[standardizeName(key, event)!!] = standardizeValue(value, event)
        }
        return attributeCopy
    }

    fun standardizeValue(
        valueIn: String?,
        event: Boolean,
    ): String {
        var value = valueIn ?: return ""
        if (event) {
            if (value.length > EVENT_VAL_MAX_LENGTH) {
                value = value.substring(0, EVENT_VAL_MAX_LENGTH)
            }
        } else {
            if (value.length > USER_ATTRIBUTE_VAL_MAX_LENGTH) {
                value = value.substring(0, USER_ATTRIBUTE_VAL_MAX_LENGTH)
            }
        }
        return value
    }

    fun standardizeName(
        nameIn: String?,
        event: Boolean,
    ): String? {
        var name = nameIn ?: return null
        name = name.replace("[^a-zA-Z0-9_\\s]".toRegex(), " ")
        name = name.replace("[\\s]+".toRegex(), "_")
        for (forbiddenPrefix in forbiddenPrefixes) {
            if (name.startsWith(forbiddenPrefix)) {
                name = name.replaceFirst(forbiddenPrefix.toRegex(), "")
            }
        }
        while (name.isNotEmpty() && !Character.isLetter(name.toCharArray()[0])) {
            name = name.substring(1)
        }
        if (event) {
            if (name.length > EVENT_MAX_LENGTH) {
                name = name.substring(0, EVENT_MAX_LENGTH)
            }
        } else {
            if (name.length > USER_ATTRIBUTE_MAX_LENGTH) {
                name = name.substring(0, USER_ATTRIBUTE_MAX_LENGTH)
            }
        }
        return name
    }

    class PickyBundle {
        val bundle = Bundle()

        fun putString(
            key: String?,
            value: String?,
        ): PickyBundle {
            if (value != null) {
                bundle.putString(key, value)
            }
            return this
        }

        fun putDouble(
            key: String?,
            value: Double?,
        ): PickyBundle {
            if (value != null) {
                bundle.putDouble(key, value)
            }
            return this
        }

        fun putLong(
            key: String?,
            value: Long?,
        ): PickyBundle {
            if (value != null) {
                bundle.putLong(key, value)
            }
            return this
        }

        fun putInt(
            key: String?,
            value: Int?,
        ): PickyBundle {
            if (value != null) {
                bundle.putInt(key, value)
            }
            return this
        }

        fun putBundleList(
            key: String?,
            value: Array<Bundle?>?,
        ): PickyBundle {
            if (value != null) {
                bundle.putParcelableArray(key, value)
            }
            return this
        }
    }

    companion object {
        const val USER_ID_FIELD_KEY = "userIdField"
        const val USER_ID_CUSTOMER_ID_VALUE = "customerId"
        const val USER_ID_EMAIL_VALUE = "email"
        const val USER_ID_MPID_VALUE = "mpid"
        private val forbiddenPrefixes = arrayOf("google_", "firebase_", "ga_")
        private const val CURRENCY_FIELD_NOT_SET = "Currency field required by Firebase was not set, defaulting to 'USD'"
        const val CF_COMMERCE_EVENT_TYPE = "Firebase.CommerceEventType"
        const val CF_PAYMENT_TYPE = "Firebase.PaymentType"
        const val CF_SHIPPING_TIER = "Firebase.ShippingTier"
        const val WARNING_MESSAGE =
            "Firebase no longer supports CHECKOUT_OPTION. To specify a different eventName, add CF_COMMERCE_EVENT_TYPE to your customFlags with a valid value"
        private const val USD = "USD"
        private const val EVENT_MAX_LENGTH = 40
        private const val USER_ATTRIBUTE_MAX_LENGTH = 24
        private const val EVENT_VAL_MAX_LENGTH = 100
        private const val USER_ATTRIBUTE_VAL_MAX_LENGTH = 36
        private const val KIT_NAME = "Google Analytics for Firebase"

        // Constants for Read Consent
        private const val CONSENT_MAPPING_SDK = "consentMappingSDK"

        enum class GoogleConsentValues(
            val consentValue: String,
        ) {
            GRANTED("Granted"),
            DENIED("Denied"),
        }

        val googleConsentMapSettings =
            mapOf(
                FirebaseAnalytics.ConsentType.AD_STORAGE to "defaultAdStorageConsentSDK",
                FirebaseAnalytics.ConsentType.AD_USER_DATA to "defaultAdUserDataConsentSDK",
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to "defaultAdPersonalizationConsentSDK",
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to "defaultAnalyticsStorageConsentSDK",
            )
    }
}
