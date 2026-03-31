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
import com.mparticle.commerce.Promotion
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

class GoogleAnalyticsFirebaseGA4Kit :
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
    ): List<ReportingMessage>? {
        Logger.info(
            "$name Kit relies on a functioning instance of Firebase Analytics. If your Firebase Analytics instance is not configured properly, this Kit will not work",
        )
        val userConsentState = currentUser?.consentState
        userConsentState?.let {
            setConsent(currentUser.consentState)
        }
        updateInstanceIDIntegration()
        return null
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

    override fun logEvent(mpEvent: MPEvent): List<ReportingMessage>? {
        if (forwardRequestsServerSide()) {
            return null
        }

        val bundle = toBundle(mpEvent.customAttributeStrings)
        FirebaseAnalytics
            .getInstance(context)
            .logEvent(getFirebaseEventName(mpEvent)!!, bundle)
        return listOf(ReportingMessage.fromEvent(this, mpEvent))
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String>?,
    ): List<ReportingMessage> {
        if (forwardRequestsServerSide()) {
            return emptyList()
        }
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

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage>? {
        if (forwardRequestsServerSide()) {
            return null
        }
        val instance = FirebaseAnalytics.getInstance(context)

        if (commerceEvent.promotionAction != null) {
            promotionActionNotNull(commerceEvent, instance)
        } else if (commerceEvent.impressions != null) {
            impressionsNotNull(commerceEvent, instance)
        } else if (commerceEvent.productAction != null) {
            productActionNotNull(commerceEvent, instance)
        } else {
            return null
        }
        return listOf(ReportingMessage.fromEvent(this, commerceEvent))
    }

    private fun promotionActionNotNull(
        commerceEvent: CommerceEvent,
        instance: FirebaseAnalytics,
    ) {
        var bundle: Bundle
        val eventName: String =
            if (commerceEvent.promotionAction === Promotion.CLICK) {
                FirebaseAnalytics.Event.SELECT_PROMOTION
            } else {
                FirebaseAnalytics.Event.VIEW_PROMOTION
            }
        commerceEvent.promotions?.let {
            for (promotion in it) {
                bundle =
                    getPromotionCommerceEventBundle(
                        promotion,
                        commerceEvent.customAttributeStrings,
                    ).bundle
                instance.logEvent(eventName, bundle)
            }
        }
    }

    private fun impressionsNotNull(
        commerceEvent: CommerceEvent,
        instance: FirebaseAnalytics,
    ) {
        var bundle: Bundle
        val eventName: String = FirebaseAnalytics.Event.VIEW_ITEM_LIST
        commerceEvent.impressions?.let {
            for (impression in it) {
                bundle =
                    getImpressionCommerceEventBundle(
                        impression.listName,
                        impression.products,
                        commerceEvent.customAttributeStrings,
                    ).bundle
                instance.logEvent(eventName, bundle)
            }
        }
    }

    private fun productActionNotNull(
        commerceEvent: CommerceEvent,
        instance: FirebaseAnalytics,
    ) {
        val bundle: Bundle = getCommerceEventBundle(commerceEvent).bundle
        val eventName =
            when (commerceEvent.productAction) {
                Product.ADD_TO_CART -> FirebaseAnalytics.Event.ADD_TO_CART
                Product.ADD_TO_WISHLIST -> FirebaseAnalytics.Event.ADD_TO_WISHLIST
                Product.CHECKOUT -> FirebaseAnalytics.Event.BEGIN_CHECKOUT
                Product.PURCHASE -> FirebaseAnalytics.Event.PURCHASE
                Product.REFUND -> FirebaseAnalytics.Event.REFUND
                Product.REMOVE_FROM_CART -> FirebaseAnalytics.Event.REMOVE_FROM_CART
                Product.CLICK -> FirebaseAnalytics.Event.SELECT_ITEM
                Product.CHECKOUT_OPTION -> {
                    val warningMessage = WARNING_MESSAGE
                    val customFlags = commerceEvent.customFlags
                    if ((customFlags != null) && customFlags.containsKey(CF_GA4COMMERCE_EVENT_TYPE)) {
                        val commerceEventTypes =
                            customFlags[CF_GA4COMMERCE_EVENT_TYPE]
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
                                    return
                                }
                            }
                        } else {
                            Logger.warning(warningMessage)
                            return
                        }
                    } else {
                        Logger.warning(warningMessage)
                        return
                    }
                }
                Product.DETAIL -> FirebaseAnalytics.Event.VIEW_ITEM
                else -> return
            }

        instance.logEvent(eventName, bundle)
    }

    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        if (!forwardRequestsServerSide()) {
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
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        if (!forwardRequestsServerSide()) {
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
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser)
        }
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest,
    ) {
        if (!forwardRequestsServerSide()) {
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
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {
        if (!forwardRequestsServerSide()) {
            setUserId(mParticleUser)
        }
    }

    private fun forwardRequestsServerSide(): Boolean =
        true.toString().equals(
            settings[FORWARD_REQUESTS_SERVER_SIDE],
            true,
        )

    /**
     * Sets the GA4 Instance ID as an mParticle integration attribute.
     */
    private fun updateInstanceIDIntegration() {
        val instance = FirebaseAnalytics.getInstance(context)
        val task = instance.appInstanceId
        task.addOnSuccessListener { appInstanceID ->
            if (!KitUtils.isEmpty(appInstanceID)) {
                val integrationAttributes = HashMap<String, String>(1)
                integrationAttributes[INSTANCE_ID_INTEGRATION_KEY] = appInstanceID!!
                setIntegrationAttributes(integrationAttributes)
            }
        }
    }

    private fun setUserId(user: MParticleUser?) {
        var userId: String? = null
        val setting = settings[EXTERNAL_USER_IDENTITY_TYPE]

        if (user != null) {
            if (EXTERNAL_USER_IDENTITY_CUSTOMER_ID.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.CustomerId]
            } else if (EXTERNAL_USER_IDENTITY_MPID.equals(setting, true)) {
                userId = user.id.toString()
            } else if (EXTERNAL_USER_IDENTITY_OTHER.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other]
            } else if (EXTERNAL_USER_IDENTITY_OTHER2.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other2]
            } else if (EXTERNAL_USER_IDENTITY_OTHER3.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other3]
            } else if (EXTERNAL_USER_IDENTITY_OTHER4.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other4]
            } else if (EXTERNAL_USER_IDENTITY_OTHER5.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other5]
            } else if (EXTERNAL_USER_IDENTITY_OTHER6.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other6]
            } else if (EXTERNAL_USER_IDENTITY_OTHER7.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other7]
            } else if (EXTERNAL_USER_IDENTITY_OTHER8.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other8]
            } else if (EXTERNAL_USER_IDENTITY_OTHER9.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other9]
            } else if (EXTERNAL_USER_IDENTITY_OTHER10.equals(setting, true)) {
                userId = user.userIdentities[MParticle.IdentityType.Other10]
            }

            if (!KitUtils.isEmpty(userId)) {
                if (true.toString().equals(settings[SHOULD_HASH_USER_ID], true)) {
                    if (userId != null) {
                        userId = KitUtils.hashFnv1a(userId.toByteArray()).toString()
                    }
                }
                FirebaseAnalytics.getInstance(context).setUserId(userId)
            }
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

    private fun getPromotionCommerceEventBundle(
        promotion: Promotion?,
        customAttributesStrings: Map<String, String>?,
    ): PickyBundle {
        var pickyBundle = PickyBundle()
        if (promotion != null) {
            pickyBundle
                .putString(FirebaseAnalytics.Param.PROMOTION_ID, promotion.id)
                .putString(
                    FirebaseAnalytics.Param.CREATIVE_NAME,
                    promotion.creative,
                ).putString(
                    FirebaseAnalytics.Param.PROMOTION_NAME,
                    promotion.name,
                ).putString(
                    FirebaseAnalytics.Param.CREATIVE_SLOT,
                    promotion.position,
                )

            customAttributesStrings?.let { customAttributes ->
                var map = standardizeAttributes(customAttributes, true)

                if (map != null) {
                    for (attributes in map) {
                        pickyBundle.putString(attributes.key, attributes.value.toString())
                    }
                }
            }
        }
        return pickyBundle
    }

    private fun getImpressionCommerceEventBundle(
        impressionKey: String?,
        products: List<Product>?,
        customAttributesStrings: Map<String, String>?,
    ): PickyBundle {
        var pickyBundle =
            PickyBundle()
                .putString(FirebaseAnalytics.Param.ITEM_LIST_ID, impressionKey)
                .putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, impressionKey)
                .putBundleList(FirebaseAnalytics.Param.ITEMS, getProductBundles(products))

        customAttributesStrings?.let { customAttributes ->
            var map = standardizeAttributes(customAttributes, true)

            if (map != null) {
                for (attributes in map) {
                    pickyBundle.putString(attributes.key, attributes.value.toString())
                }
            }
        }

        return pickyBundle
    }

    private fun getCommerceEventBundle(commerceEvent: CommerceEvent): PickyBundle {
        val pickyBundle = getTransactionAttributesBundle(commerceEvent)
        var currency = commerceEvent.currency
        if (currency == null) {
            Logger.info(CURRENCY_FIELD_NOT_SET)
            currency = USD
        }

        // Google Analytics 4 introduces 2 new event types - add_shipping_info and add_payment_info
        // each of these has an extra parameter that is optional
        val customFlags = commerceEvent.customFlags
        if (customFlags != null && customFlags.containsKey(CF_GA4COMMERCE_EVENT_TYPE)) {
            val commerceEventTypeList = customFlags[CF_GA4COMMERCE_EVENT_TYPE]
            if (!commerceEventTypeList.isNullOrEmpty()) {
                val commerceEventType = commerceEventTypeList[0]
                if (commerceEventType == FirebaseAnalytics.Event.ADD_SHIPPING_INFO) {
                    val shippingTier = customFlags[CF_GA4_SHIPPING_TIER]
                    if (!shippingTier.isNullOrEmpty()) {
                        pickyBundle.putString(
                            FirebaseAnalytics.Param.SHIPPING_TIER,
                            shippingTier[0],
                        )
                    }
                } else if (commerceEventType == FirebaseAnalytics.Event.ADD_PAYMENT_INFO) {
                    val paymentType = customFlags[CF_GA4_PAYMENT_TYPE]
                    if (!paymentType.isNullOrEmpty()) {
                        pickyBundle.putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentType[0])
                    }
                }
            }
        }

        commerceEvent.customAttributeStrings?.let { customAttributes ->
            var map = standardizeAttributes(customAttributes, true)

            if (map != null) {
                for (attributes in map) {
                    pickyBundle.putString(attributes.key, attributes.value.toString())
                }
            }
        }

        pickyBundle
            .putString(FirebaseAnalytics.Param.CURRENCY, currency)
            .putBundleList(FirebaseAnalytics.Param.ITEMS, getProductBundles(commerceEvent))

        return pickyBundle
    }

    private fun getProductBundles(commerceEvent: CommerceEvent): Array<Bundle> {
        val products = commerceEvent.products
        return getProductBundles(products)
    }

    private fun getProductBundles(products: List<Product>?): Array<Bundle> {
        products?.let {
            val bundles = arrayOfNulls<Bundle>(products.size)
            for ((i, product) in products.withIndex()) {
                val bundle = getHashmap(product)
                val bundleSize = bundle.size
                if (bundleSize > ITEM_MAX_PARAMETER) {
                    val additionalParameterSize = bundleSize - ITEM_MAX_PARAMETER
                    val keysToRemove =
                        bundle
                            .keys
                            .sortedWith(String.CASE_INSENSITIVE_ORDER)
                            .takeLast(additionalParameterSize)
                    keysToRemove.forEach { key ->
                        bundle.remove(key)
                    }
                }
                bundles[i] = hashMapToBundle(bundle).bundle
            }
            return products.map { hashMapToBundle(getHashmap(it)).bundle }.toTypedArray()
        }
        return arrayOf()
    }

    private fun getTransactionAttributesBundle(commerceEvent: CommerceEvent): PickyBundle {
        val pickyBundle = PickyBundle()
        val transactionAttributes = commerceEvent.transactionAttributes

        return if (transactionAttributes != null) {
            pickyBundle
                .putString(FirebaseAnalytics.Param.TRANSACTION_ID, transactionAttributes.id)
                .putDouble(FirebaseAnalytics.Param.VALUE, transactionAttributes.revenue)
                .putDouble(FirebaseAnalytics.Param.TAX, transactionAttributes.tax)
                .putDouble(FirebaseAnalytics.Param.SHIPPING, transactionAttributes.shipping)
                .putString(FirebaseAnalytics.Param.COUPON, transactionAttributes.couponCode)
        } else {
            return pickyBundle
        }
    }

    private fun hashMapToBundle(hashMap: HashMap<String, Any>): PickyBundle {
        val bundle = PickyBundle()
        for ((key, value) in hashMap) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Double -> bundle.putDouble(key, value)
                is Long -> bundle.putLong(key, value)
            }
        }
        return bundle
    }

    private fun getHashmap(product: Product): HashMap<String, Any> =
        hashMapOf<String, Any>().apply {
            put(FirebaseAnalytics.Param.QUANTITY, product.quantity.toLong())
            put(FirebaseAnalytics.Param.ITEM_ID, product.sku)
            put(FirebaseAnalytics.Param.ITEM_NAME, product.name)
            put(FirebaseAnalytics.Param.PRICE, product.unitPrice)
            product.category?.let { put(FirebaseAnalytics.Param.ITEM_CATEGORY, it) }
            product.brand?.let { put(FirebaseAnalytics.Param.ITEM_BRAND, it) }
            product.position?.let { put(FirebaseAnalytics.Param.INDEX, it) }
            product.customAttributes?.let {
                for ((key, value) in it) {
                    put(key, value)
                }
            }
        }

    private fun getValue(commerceEvent: CommerceEvent): Double? {
        var value = 0.0
        val products = commerceEvent.products ?: return null
        for (product in products) {
            value += product.quantity * product.unitPrice
        }
        return value
    }

    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?,
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
        filteredMParticleUser: FilteredMParticleUser,
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
        s: String,
        list: List<String>,
        filteredMParticleUser: FilteredMParticleUser,
    ) {
    }

    override fun onSetAllUserAttributes(
        userAttributesIn: Map<String, String>,
        userAttributeLists: Map<String, List<String>>?,
        filteredMParticleUser: FilteredMParticleUser?,
    ) {
        var userAttributes: Map<String, String>? = userAttributesIn
        userAttributes = standardizeAttributes(userAttributes, false)
        if (userAttributes != null) {
            for ((key, value) in userAttributes) {
                FirebaseAnalytics.getInstance(context).setUserProperty(key, value)
            }
        }
    }

    override fun supportsAttributeLists(): Boolean = false

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
                "Google Analytics 4 kit was unable to parse the user's ConsentState, consent may not be set correctly on the Google Analytics SDK",
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
                "The Google Analytics 4 kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
            )
        }
        return null
    }

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
            Logger.error(
                jse,
                "The Google Analytics 4 kit threw an exception while searching for the configured consent purpose mapping in the current user's consent status.",
            )
            emptyMap()
        }
    }

    private fun standardizeAttributes(
        attributes: Map<String, String>?,
        event: Boolean,
    ): Map<String, String>? {
        if (attributes == null) {
            return null
        }
        val attributeCopy = HashMap<String, String>()
        for ((key, value) in attributes) {
            standardizeName(key, event)?.let {
                attributeCopy[it] = standardizeValue(value, event)
            }
        }
        limitAttributes(attributeCopy, EVENT_MAX_PARAMETER_PROPERTY)
        return attributeCopy
    }

    private fun limitAttributes(
        attributeCopy: HashMap<String, String>,
        maxCount: Int,
    ) {
        if (!attributeCopy.isNullOrEmpty() && attributeCopy.size > maxCount) {
            val dictionaryKeys = ArrayList(attributeCopy.keys)
            dictionaryKeys.sortWith(String.CASE_INSENSITIVE_ORDER)
            for (i in maxCount until dictionaryKeys.size) {
                attributeCopy.remove(dictionaryKeys[i])
            }
        }
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
        var name = nameIn ?: return INVALID_GA4_KEY
        clientStandardizationCallback?.let { name = it.nameStandardization(name) }

        if (event) {
            name = name.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            for (forbiddenPrefix in forbiddenPrefixes) {
                if (name.startsWith(forbiddenPrefix)) {
                    name = name.replaceFirst(forbiddenPrefix.toRegex(), "")
                }
            }
            while (name.isNotEmpty() && !Character.isLetter(name.toCharArray()[0])) {
                name = name.substring(1)
            }
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
        if (name.isNotEmpty()) {
            return name
        } else {
            return INVALID_GA4_KEY
        }
    }

    class PickyBundle {
        val bundle = Bundle()

        fun putString(
            key: String,
            value: String?,
        ): PickyBundle {
            if (value != null) {
                bundle.putString(key, value)
            }
            return this
        }

        fun putDouble(
            key: String,
            value: Double?,
        ): PickyBundle {
            if (value != null) {
                bundle.putDouble(key, value)
            }
            return this
        }

        fun putLong(
            key: String,
            value: Long?,
        ): PickyBundle {
            if (value != null) {
                bundle.putLong(key, value)
            }
            return this
        }

        fun putInt(
            key: String,
            value: Int?,
        ): PickyBundle {
            if (value != null) {
                bundle.putInt(key, value)
            }
            return this
        }

        fun putBundleList(
            key: String,
            value: Array<Bundle>?,
        ): PickyBundle {
            value?.let {
                if (it.isNotEmpty()) {
                    bundle.putParcelableArray(key, value)
                }
            }
            return this
        }
    }

    companion object {
        private var clientStandardizationCallback: MPClientStandardization? = null

        @JvmStatic
        fun setClientStandardizationCallback(standardizationCallback: MPClientStandardization?) {
            clientStandardizationCallback = standardizationCallback
        }

        const val SHOULD_HASH_USER_ID = "hashUserId"
        const val FORWARD_REQUESTS_SERVER_SIDE = "forwardWebRequestsServerSide"
        const val EXTERNAL_USER_IDENTITY_TYPE = "externalUserIdentityType"
        const val EXTERNAL_USER_IDENTITY_CUSTOMER_ID = "CustomerId"
        const val EXTERNAL_USER_IDENTITY_MPID = "mpid"
        const val EXTERNAL_USER_IDENTITY_OTHER = "Other"
        const val EXTERNAL_USER_IDENTITY_OTHER2 = "Other2"
        const val EXTERNAL_USER_IDENTITY_OTHER3 = "Other3"
        const val EXTERNAL_USER_IDENTITY_OTHER4 = "Other4"
        const val EXTERNAL_USER_IDENTITY_OTHER5 = "Other5"
        const val EXTERNAL_USER_IDENTITY_OTHER6 = "Other6"
        const val EXTERNAL_USER_IDENTITY_OTHER7 = "Other7"
        const val EXTERNAL_USER_IDENTITY_OTHER8 = "Other8"
        const val EXTERNAL_USER_IDENTITY_OTHER9 = "Other9"
        const val EXTERNAL_USER_IDENTITY_OTHER10 = "Other10"
        const val CF_GA4COMMERCE_EVENT_TYPE = "GA4.CommerceEventType"
        const val CF_GA4_PAYMENT_TYPE = "GA4.PaymentType"
        const val CF_GA4_SHIPPING_TIER = "GA4.ShippingTier"
        const val WARNING_MESSAGE =
            "GA4 no longer supports CHECKOUT_OPTION. To specify a different eventName, add CF_GA4COMMERCE_EVENT_TYPE to your customFlags with a valid value"
        private const val INSTANCE_ID_INTEGRATION_KEY = "app_instance_id"
        private val forbiddenPrefixes = arrayOf("google_", "firebase_", "ga_")
        private const val EVENT_MAX_LENGTH = 40
        private const val USER_ATTRIBUTE_MAX_LENGTH = 24

        // Following limits are based off Google Analytics 360 limits, docs here "https://support.google.com/analytics/answer/11202874?sjid=14644072134282618832-NA#limits"
        private const val EVENT_VAL_MAX_LENGTH = 500
        private const val USER_ATTRIBUTE_VAL_MAX_LENGTH = 36
        private const val EVENT_MAX_PARAMETER_PROPERTY = 100
        private const val ITEM_MAX_PARAMETER = 25
        private const val INVALID_GA4_KEY = "invalid_ga4_key"
        private const val KIT_NAME = "GA4 for Firebase"
        private const val CURRENCY_FIELD_NOT_SET =
            "Currency field required by Firebase was not set, defaulting to 'USD'"
        private const val USD = "USD"

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

    public interface MPClientStandardization {
        fun nameStandardization(name: String): String
    }
}
