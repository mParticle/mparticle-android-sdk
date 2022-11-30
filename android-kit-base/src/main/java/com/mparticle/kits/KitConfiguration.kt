package com.mparticle.kits

import android.util.SparseBooleanArray
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.KitUtils.hashForFiltering
import com.mparticle.kits.mappings.CustomMapping
import org.json.JSONException
import org.json.JSONObject
import java.util.*

open class KitConfiguration {
    var isAttributeValueFilteringActive = false
        private set
    var isAvfShouldIncludeMatches = false
        private set
    var consentForwardingIncludeMatches = false
    var avfHashedAttribute = 0
        private set
    var avfHashedValue = 0
        private set
    private val settings = HashMap<String, String>(0)
    var eventTypeFilters: SparseBooleanArray? = SparseBooleanArray(0)
        protected set
    var eventNameFilters = SparseBooleanArray(0)
        protected set
    var eventAttributeFilters = SparseBooleanArray(0)
        protected set
    var screenNameFilters = SparseBooleanArray(0)
        protected set
    var screenAttributeFilters = SparseBooleanArray(0)
        protected set
    var userIdentityFilters = SparseBooleanArray(0)
        protected set
    var userAttributeFilters = SparseBooleanArray(0)
        protected set
    var commerceAttributeFilters: SparseBooleanArray? = SparseBooleanArray(0)
        protected set
    var commerceEntityFilters: SparseBooleanArray? = SparseBooleanArray(0)
        protected set
    protected var mAttributeAddToUser: MutableMap<Int, String> = HashMap()
    protected var mAttributeRemoveFromUser: MutableMap<Int, String> = HashMap()
    protected var mAttributeSingleItemUser: MutableMap<Int, String> = HashMap()
    private val mCommerceEntityAttributeFilters: MutableMap<Int, SparseBooleanArray>? = HashMap(0)
    var mConsentForwardingRules: MutableMap<Int, Boolean> = HashMap()
    var lowBracket = 0
        private set
    var highBracket = 101
        private set
    private var customMappingList: LinkedList<CustomMapping>? = null
    var defaultEventProjection: CustomMapping? = null
        private set
    var defaultScreenCustomMapping: CustomMapping? = null
        private set
    var defaultCommerceCustomMapping: CustomMapping? = null
        private set
    var kitId = 0
        private set
    var mExcludeAnonymousUsers = false

    @Throws(JSONException::class)
    open fun parseConfiguration(json: JSONObject): KitConfiguration? {
        kitId = json.getInt(KEY_ID)
        if (json.has(KEY_ATTRIBUTE_VALUE_FILTERING)) {
            isAttributeValueFilteringActive = true
            try {
                val avfJson = json.getJSONObject(KEY_ATTRIBUTE_VALUE_FILTERING)
                isAvfShouldIncludeMatches = avfJson.getBoolean(
                    KEY_ATTRIBUTE_VALUE_FILTERING_SHOULD_INCLUDE_MATCHES
                )
                avfHashedAttribute = avfJson.getInt(KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE)
                avfHashedValue = avfJson.getInt(KEY_ATTRIBUTE_VALUE_FILTERING_VALUE)
            } catch (jse: JSONException) {
                Logger.error("Issue when parsing attribute value filtering configuration: " + jse.message)
                isAttributeValueFilteringActive = false
            }
        }
        if (json.has(KEY_PROPERTIES)) {
            val propJson = json.getJSONObject(KEY_PROPERTIES)
            val iterator = propJson.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (!propJson.isNull(key)) {
                    settings[key] = propJson.optString(key)
                }
            }
        }
        if (json.has(KEY_FILTERS)) {
            val filterJson = json.getJSONObject(KEY_FILTERS)
            if (filterJson.has(KEY_EVENT_TYPES_FILTER)) {
                eventTypeFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_EVENT_TYPES_FILTER
                    )
                )
            } else {
                eventTypeFilters!!.clear()
            }
            if (filterJson.has(KEY_EVENT_NAMES_FILTER)) {
                eventNameFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_EVENT_NAMES_FILTER
                    )
                )
            } else {
                eventNameFilters.clear()
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTES_FILTER)) {
                eventAttributeFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_EVENT_ATTRIBUTES_FILTER
                    )
                )
            } else {
                eventAttributeFilters.clear()
            }
            if (filterJson.has(KEY_SCREEN_NAME_FILTER)) {
                screenNameFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_SCREEN_NAME_FILTER
                    )
                )
            } else {
                screenNameFilters.clear()
            }
            if (filterJson.has(KEY_SCREEN_ATTRIBUTES_FILTER)) {
                screenAttributeFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_SCREEN_ATTRIBUTES_FILTER
                    )
                )
            } else {
                screenAttributeFilters.clear()
            }
            if (filterJson.has(KEY_USER_IDENTITY_FILTER)) {
                userIdentityFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_USER_IDENTITY_FILTER
                    )
                )
            } else {
                userIdentityFilters.clear()
            }
            if (filterJson.has(KEY_USER_ATTRIBUTE_FILTER)) {
                userAttributeFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_USER_ATTRIBUTE_FILTER
                    )
                )
            } else {
                userAttributeFilters.clear()
            }
            if (filterJson.has(KEY_COMMERCE_ATTRIBUTE_FILTER)) {
                commerceAttributeFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_COMMERCE_ATTRIBUTE_FILTER
                    )
                )
            } else {
                commerceAttributeFilters!!.clear()
            }
            if (filterJson.has(KEY_COMMERCE_ENTITY_FILTERS)) {
                commerceEntityFilters = convertToSparseArray(
                    filterJson.getJSONObject(
                        KEY_COMMERCE_ENTITY_FILTERS
                    )
                )
            } else {
                commerceEntityFilters!!.clear()
            }
            if (filterJson.has(KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS)) {
                val entityAttributeFilters = filterJson.getJSONObject(
                    KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS
                )
                mCommerceEntityAttributeFilters!!.clear()
                val keys = entityAttributeFilters.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    mCommerceEntityAttributeFilters[key.toInt()] =
                        convertToSparseArray(entityAttributeFilters.getJSONObject(key))
                }
            } else {
                mCommerceEntityAttributeFilters!!.clear()
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_ADD_USER)) {
                mAttributeAddToUser = convertToSparseMap(
                    filterJson.getJSONObject(
                        KEY_EVENT_ATTRIBUTE_ADD_USER
                    )
                )
            } else {
                mAttributeAddToUser.clear()
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_REMOVE_USER)) {
                mAttributeRemoveFromUser = convertToSparseMap(
                    filterJson.getJSONObject(
                        KEY_EVENT_ATTRIBUTE_REMOVE_USER
                    )
                )
            } else {
                mAttributeRemoveFromUser.clear()
            }
            if (filterJson.has(KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER)) {
                mAttributeSingleItemUser = convertToSparseMap(
                    filterJson.getJSONObject(
                        KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER
                    )
                )
            } else {
                mAttributeSingleItemUser.clear()
            }
        }
        if (json.has(KEY_BRACKETING)) {
            val bracketing = json.getJSONObject(KEY_BRACKETING)
            lowBracket = bracketing.optInt(KEY_BRACKETING_LOW, 0)
            highBracket = bracketing.optInt(KEY_BRACKETING_HIGH, 101)
        } else {
            lowBracket = 0
            highBracket = 101
        }
        customMappingList = LinkedList()
        defaultEventProjection = null
        if (json.has(KEY_PROJECTIONS)) {
            val projections = json.getJSONArray(KEY_PROJECTIONS)
            for (i in 0 until projections.length()) {
                val customMapping = CustomMapping(projections.getJSONObject(i))
                if (customMapping.isDefault) {
                    if (customMapping.messageType == 4) {
                        defaultEventProjection = customMapping
                    } else if (customMapping.messageType == 3) {
                        defaultScreenCustomMapping = customMapping
                    } else {
                        defaultCommerceCustomMapping = customMapping
                    }
                } else {
                    customMappingList!!.add(customMapping)
                }
            }
        }
        mConsentForwardingRules.clear()
        if (json.has(KEY_CONSENT_FORWARDING_RULES)) {
            val consentForwardingRule = json.getJSONObject(KEY_CONSENT_FORWARDING_RULES)
            consentForwardingIncludeMatches = consentForwardingRule.optBoolean(
                KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES
            )
            val rules = consentForwardingRule.getJSONArray(KEY_CONSENT_FORWARDING_RULES_ARRAY)
            for (i in 0 until rules.length()) {
                mConsentForwardingRules[
                    rules.getJSONObject(i).getInt(
                        KEY_CONSENT_FORWARDING_RULES_VALUE_HASH
                    )
                ] = rules.getJSONObject(i).getBoolean(KEY_CONSENT_FORWARDING_RULES_VALUE_CONSENTED)
            }
        }
        mExcludeAnonymousUsers = json.optBoolean(KEY_EXCLUDE_ANONYMOUS_USERS, false)
        return this
    }

    fun shouldIncludeFromAttributeValueFiltering(attributes: Map<String?, String?>?): Boolean {
        var shouldInclude = true
        if (isAttributeValueFilteringActive) {
            var isMatch = false
            if (attributes != null) {
                val attIterator = attributes.entries.iterator()
                while (attIterator.hasNext()) {
                    val (key, value) = attIterator.next()
                    val keyHash = hashForFiltering(key)
                    if (keyHash == avfHashedAttribute) {
                        val valueHash = hashForFiltering(value)
                        if (valueHash == avfHashedValue) {
                            isMatch = true
                        }
                        break
                    }
                }
            }
            shouldInclude = if (isAvfShouldIncludeMatches) isMatch else !isMatch
        }
        return shouldInclude
    }

    fun shouldIncludeFromConsentRules(user: MParticleUser?): Boolean {
        if (mConsentForwardingRules.size == 0) {
            return true
        }
        // if we don't have a user, but there are rules, be safe and exclude
        if (user == null) {
            return false
        }
        val isMatch = isConsentStateFilterMatch(user.consentState)
        return consentForwardingIncludeMatches == isMatch
    }

    /**
     * This method indicates if the given consent state matches any of the Consent
     * forwarding rules.
     *
     * @param consentState
     * @return
     */
    fun isConsentStateFilterMatch(consentState: ConsentState?): Boolean {
        if (mConsentForwardingRules.size == 0 || consentState == null || consentState.gdprConsentState.size == 0 && consentState.ccpaConsentState == null) {
            return false
        }
        val gdprConsentState = consentState.gdprConsentState
        for ((key, value) in gdprConsentState) {
            val consentPurposeHash = hashForFiltering("1$key")
            val consented = mConsentForwardingRules[consentPurposeHash]
            if (consented != null && consented == value.isConsented) {
                return true
            }
        }
        val ccpaConsent = consentState.ccpaConsentState
        if (ccpaConsent != null) {
            val consentPurposeHash = hashForFiltering("2" + Constants.MessageKey.CCPA_CONSENT_KEY)
            val consented = mConsentForwardingRules[consentPurposeHash]
            if (consented != null && consented == ccpaConsent.isConsented) {
                return true
            }
        }
        return false
    }

    fun filterCommerceEvent(event: CommerceEvent): CommerceEvent? {
        if (!shouldIncludeFromAttributeValueFiltering(event.customAttributeStrings)) {
            return null
        }
        if (eventTypeFilters != null &&
            !eventTypeFilters!![
                hashForFiltering(
                        CommerceEventUtils.getEventType(event).toString() + ""
                    ), true
            ]
        ) {
            return null
        }
        var filteredEvent = CommerceEvent.Builder(event).build()
        filteredEvent = filterCommerceEntities(filteredEvent)
        filteredEvent = filterCommerceEntityAttributes(filteredEvent)
        filteredEvent = filterCommerceEventAttributes(filteredEvent)
        return filteredEvent
    }

    private fun filterCommerceEntityAttributes(filteredEvent: CommerceEvent): CommerceEvent {
        if (mCommerceEntityAttributeFilters == null || mCommerceEntityAttributeFilters.size == 0) {
            return filteredEvent
        }
        val builder = CommerceEvent.Builder(filteredEvent)
        for ((entity, filters) in mCommerceEntityAttributeFilters) {
            when (entity) {
                ENTITY_PRODUCT -> if (filteredEvent.products != null && filteredEvent.products!!.size > 0) {
                    val filteredProducts: MutableList<Product> = LinkedList()
                    for (product in filteredEvent.products!!) {
                        val productBuilder = Product.Builder(product)
                        if (product.customAttributes != null && product.customAttributes!!.size > 0) {
                            val filteredCustomAttributes = HashMap<String, String>(
                                product.customAttributes!!.size
                            )
                            for ((key, value) in product.customAttributes!!) {
                                if (filters[hashForFiltering(key), true]) {
                                    filteredCustomAttributes[key] = value
                                }
                            }
                            productBuilder.customAttributes(filteredCustomAttributes)
                        }
                        if (!MPUtility.isEmpty(product.couponCode) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PRODUCT_COUPON_CODE
                                ), true
                        ]
                        ) {
                            productBuilder.couponCode(product.couponCode)
                        } else {
                            productBuilder.couponCode(null)
                        }
                        if (product.position != null && filters[hashForFiltering(CommerceEventUtils.Constants.Companion.ATT_PRODUCT_POSITION), true]) {
                            productBuilder.position(product.position)
                        } else {
                            productBuilder.position(null)
                        }
                        if (!MPUtility.isEmpty(product.variant) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PRODUCT_VARIANT
                                ), true
                        ]
                        ) {
                            productBuilder.variant(product.variant)
                        } else {
                            productBuilder.variant(null)
                        }
                        if (!MPUtility.isEmpty(product.category) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PRODUCT_CATEGORY
                                ), true
                        ]
                        ) {
                            productBuilder.category(product.category)
                        } else {
                            productBuilder.category(null)
                        }
                        if (!MPUtility.isEmpty(product.brand) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PRODUCT_BRAND
                                ), true
                        ]
                        ) {
                            productBuilder.brand(product.brand)
                        } else {
                            productBuilder.brand(null)
                        }
                        filteredProducts.add(productBuilder.build())
                    }
                    builder.products(filteredProducts)
                }
                ENTITY_PROMOTION -> if (filteredEvent.promotions != null && filteredEvent.promotions!!.size > 0) {
                    val filteredPromotions: MutableList<Promotion> = LinkedList()
                    for (promotion in filteredEvent.promotions!!) {
                        val filteredPromotion = Promotion()
                        if (!MPUtility.isEmpty(promotion.id) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PROMOTION_ID
                                ), true
                        ]
                        ) {
                            filteredPromotion.id = promotion.id
                        }
                        if (!MPUtility.isEmpty(promotion.creative) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PROMOTION_CREATIVE
                                ), true
                        ]
                        ) {
                            filteredPromotion.creative = promotion.creative
                        }
                        if (!MPUtility.isEmpty(promotion.name) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PROMOTION_NAME
                                ), true
                        ]
                        ) {
                            filteredPromotion.name = promotion.name
                        }
                        if (!MPUtility.isEmpty(promotion.position) && filters[
                            hashForFiltering(
                                    CommerceEventUtils.Constants.Companion.ATT_PROMOTION_POSITION
                                ), true
                        ]
                        ) {
                            filteredPromotion.position = promotion.position
                        }
                        filteredPromotions.add(filteredPromotion)
                    }
                    builder.promotions(filteredPromotions)
                }
            }
        }
        return builder.build()
    }

    fun filterEventAttributes(event: MPEvent): Map<String, Any>? {
        return filterEventAttributes(
            event.eventType,
            event.eventName,
            eventAttributeFilters,
            event.customAttributes
        )
    }

    fun filterScreenAttributes(
        eventType: MParticle.EventType?,
        eventName: String,
        eventAttributes: Map<String, Any>?
    ): Map<String, Any>? {
        return filterEventAttributes(eventType, eventName, screenNameFilters, eventAttributes)
    }

    fun filterEventAttributes(
        eventType: MParticle.EventType?,
        eventName: String,
        filter: SparseBooleanArray?,
        eventAttributes: Map<String, Any>?
    ): Map<String, Any>? {
        return if (eventAttributes != null && eventAttributes.size > 0 && filter != null && filter.size() > 0) {
            var eventTypeStr = "0"
            if (eventType != null) {
                eventTypeStr = eventType.ordinal.toString() + ""
            }
            val attIterator = eventAttributes.entries.iterator()
            val newAttributes: MutableMap<String, Any> =
                HashMap()
            while (attIterator.hasNext()) {
                val (key, value) = attIterator.next()
                val hash = hashForFiltering(eventTypeStr + eventName + key)
                if (filter[hash, true]) {
                    newAttributes[key] = value
                }
            }
            newAttributes
        } else {
            eventAttributes
        }
    }

    private fun filterCommerceEntities(filteredEvent: CommerceEvent): CommerceEvent {
        if (commerceEntityFilters == null || commerceEntityFilters!!.size() == 0) {
            return filteredEvent
        }
        val builder = CommerceEvent.Builder(filteredEvent)
        val removeProducts = !commerceEntityFilters!![ENTITY_PRODUCT, true]
        val removePromotions = !commerceEntityFilters!![ENTITY_PROMOTION, true]
        if (removeProducts) {
            builder.products(LinkedList())
            val impressionList = filteredEvent.impressions
            if (impressionList != null) {
                for (impression in impressionList) {
                    builder.addImpression(Impression(impression.listName))
                }
            }
        }
        if (removePromotions) {
            builder.promotions(LinkedList())
        }
        return builder.build()
    }

    private fun filterCommerceEventAttributes(filteredEvent: CommerceEvent): CommerceEvent {
        val eventType = Integer.toString(CommerceEventUtils.getEventType(filteredEvent))
        if (commerceAttributeFilters == null || commerceAttributeFilters!!.size() == 0) {
            return filteredEvent
        }
        val builder = CommerceEvent.Builder(filteredEvent)
        val customAttributes = filteredEvent.customAttributeStrings
        if (customAttributes != null) {
            val filteredCustomAttributes: MutableMap<String, String> =
                HashMap(customAttributes.size)
            for ((key, value) in customAttributes) {
                if (commerceAttributeFilters!![hashForFiltering(eventType + key), true]) {
                    filteredCustomAttributes[key] = value
                }
            }
            builder.customAttributes(filteredCustomAttributes)
        }
        if (filteredEvent.checkoutStep != null &&
            !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_ACTION_CHECKOUT_STEP), true]
        ) {
            builder.checkoutStep(null)
        }
        if (filteredEvent.checkoutOptions != null &&
            !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_ACTION_CHECKOUT_OPTIONS), true]
        ) {
            builder.checkoutOptions(null)
        }
        val attributes = filteredEvent.transactionAttributes
        if (attributes != null) {
            if (attributes.couponCode != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_TRANSACTION_COUPON_CODE), true]
            ) {
                attributes.couponCode = null
            }
            if (attributes.shipping != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_SHIPPING), true]
            ) {
                attributes.shipping = null
            }
            if (attributes.tax != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_TAX), true]
            ) {
                attributes.tax = null
            }
            if (attributes.revenue != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_TOTAL), true]
            ) {
                attributes.revenue = 0.0
            }
            if (attributes.id != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_TRANSACTION_ID), true]
            ) {
                attributes.setId(null)
            }
            if (attributes.affiliation != null &&
                !commerceAttributeFilters!![hashForFiltering(eventType + CommerceEventUtils.Constants.Companion.ATT_AFFILIATION), true]
            ) {
                attributes.affiliation = null
            }
            builder.transactionAttributes(attributes)
        }
        return builder.build()
    }

    fun shouldLogScreen(screenName: String): Boolean {
        val nameHash = hashForFiltering("0$screenName")
        return if (screenNameFilters.size() > 0 && !screenNameFilters[nameHash, true]) {
            false
        } else true
    }

    fun shouldLogEvent(event: MPEvent): Boolean {
        if (!shouldIncludeFromAttributeValueFiltering(event.customAttributeStrings)) {
            return false
        }
        val typeHash = hashForFiltering(event.eventType.ordinal.toString() + "")
        return eventTypeFilters!![typeHash, true] && eventNameFilters[event.eventHash, true]
    }

    fun passesBracketing(userBucket: Int): Boolean {
        return userBucket >= lowBracket && userBucket < highBracket
    }

    protected open fun convertToSparseArray(json: JSONObject): SparseBooleanArray {
        val map = SparseBooleanArray()
        val iterator = json.keys()
        while (iterator.hasNext()) {
            try {
                val key = iterator.next()
                map.put(key.toInt(), json.getInt(key) == 1)
            } catch (jse: JSONException) {
                Logger.error("Issue while parsing kit configuration: " + jse.message)
            }
        }
        return map
    }

    protected fun convertToSparseMap(json: JSONObject): MutableMap<Int, String> {
        val map: MutableMap<Int, String> = HashMap()
        val iterator = json.keys()
        while (iterator.hasNext()) {
            try {
                val key = iterator.next()
                map[key.toInt()] = json.getString(key)
            } catch (jse: JSONException) {
                Logger.error("Issue while parsing kit configuration: " + jse.message)
            }
        }
        return map
    }

    fun getCustomMappingList(): List<CustomMapping>? {
        return customMappingList
    }

    fun getSettings(): Map<String, String> {
        return settings
    }

    fun shouldHonorOptOut(): Boolean {
        if (settings.containsKey(HONOR_OPT_OUT)) {
            val optOut = settings[HONOR_OPT_OUT]
            return java.lang.Boolean.parseBoolean(optOut)
        }
        return true
    }

    fun shouldSetIdentity(identityType: MParticle.IdentityType): Boolean {
        val userIdentityFilters: SparseBooleanArray? = userIdentityFilters
        return userIdentityFilters == null || userIdentityFilters.size() == 0 ||
            userIdentityFilters[identityType.value, true]
    }

    val commerceEntityAttributeFilters: Map<Int, SparseBooleanArray>?
        get() = mCommerceEntityAttributeFilters
    val eventAttributesAddToUser: Map<Int, String>
        get() = mAttributeAddToUser
    val eventAttributesRemoveFromUser: Map<Int, String>
        get() = mAttributeRemoveFromUser
    val eventAttributesSingleItemUser: Map<Int, String>
        get() = mAttributeSingleItemUser

    fun excludeAnonymousUsers(): Boolean {
        return mExcludeAnonymousUsers
    }

    fun shouldExcludeUser(user: MParticleUser?): Boolean {
        return mExcludeAnonymousUsers && (user == null || !user.isLoggedIn)
    }

    companion object {
        private const val ENTITY_PRODUCT = 1
        private const val ENTITY_PROMOTION = 2
        const val KEY_ID = "id"
        private const val KEY_ATTRIBUTE_VALUE_FILTERING = "avf"
        private const val KEY_PROPERTIES = "as"
        private const val KEY_FILTERS = "hs"
        private const val KEY_BRACKETING = "bk"
        private const val KEY_ATTRIBUTE_VALUE_FILTERING_SHOULD_INCLUDE_MATCHES = "i"
        private const val KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE = "a"
        private const val KEY_ATTRIBUTE_VALUE_FILTERING_VALUE = "v"
        private const val KEY_EVENT_TYPES_FILTER = "et"
        private const val KEY_EVENT_NAMES_FILTER = "ec"
        private const val KEY_EVENT_ATTRIBUTES_FILTER = "ea"
        private const val KEY_SCREEN_NAME_FILTER = "svec"
        private const val KEY_SCREEN_ATTRIBUTES_FILTER = "svea"
        private const val KEY_USER_IDENTITY_FILTER = "uid"
        private const val KEY_USER_ATTRIBUTE_FILTER = "ua"
        private const val KEY_EVENT_ATTRIBUTE_ADD_USER = "eaa"
        private const val KEY_EVENT_ATTRIBUTE_REMOVE_USER = "ear"
        private const val KEY_EVENT_ATTRIBUTE_SINGLE_ITEM_USER = "eas"
        private const val KEY_BRACKETING_LOW = "lo"
        private const val KEY_BRACKETING_HIGH = "hi"
        private const val KEY_COMMERCE_ATTRIBUTE_FILTER = "cea"
        private const val KEY_COMMERCE_ENTITY_FILTERS = "ent"
        private const val KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS = "afa"
        private const val KEY_CONSENT_FORWARDING_RULES = "crvf"
        private const val KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES = "i"
        private const val KEY_CONSENT_FORWARDING_RULES_ARRAY = "v"
        private const val KEY_CONSENT_FORWARDING_RULES_VALUE_CONSENTED = "c"
        private const val KEY_CONSENT_FORWARDING_RULES_VALUE_HASH = "h"
        private const val KEY_EXCLUDE_ANONYMOUS_USERS = "eau"

        // If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting.
        private const val HONOR_OPT_OUT = "honorOptOut"
        private const val KEY_PROJECTIONS = "pr"

        @Throws(JSONException::class)
        fun createKitConfiguration(json: JSONObject): KitConfiguration {
            return KitConfiguration().parseConfiguration(json)!!
        }

        fun filterAttributes(
            attributeFilters: SparseBooleanArray?,
            attributes: Map<String, Any?>?
        ): Map<String, Any?>? {
            return if (attributes != null && attributeFilters != null && attributeFilters.size() > 0 && attributes.size > 0) {
                val newAttributes = mutableMapOf<String, Any?>()
                for ((key, value) in attributes) {
                    if (shouldForwardAttribute(attributeFilters, key)) {
                        newAttributes[key] = value
                    }
                }
                newAttributes
            } else {
                attributes
            }
        }

        fun shouldForwardAttribute(attributeFilters: SparseBooleanArray, key: String?): Boolean {
            val hash = hashForFiltering(key)
            return attributeFilters[hash, true]
        }
    }
}
