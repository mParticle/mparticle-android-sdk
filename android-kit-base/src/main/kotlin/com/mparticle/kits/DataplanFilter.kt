package com.mparticle.kits

import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleIdentityClientImpl
import com.mparticle.internal.Logger
import org.json.JSONArray
import org.json.JSONObject

internal interface DataplanFilter {
    fun <T : BaseEvent> transformEventForEvent(event: T?): T?
    fun transformIdentities(identities: Map<MParticle.IdentityType, String?>?): Map<MParticle.IdentityType, String?>?
    fun <T> transformUserAttributes(attributes: Map<String, T>?): Map<String, T>?
    fun isUserAttributeBlocked(key: String?): Boolean
    fun isUserIdentityBlocked(key: MParticle.IdentityType?): Boolean
}

internal class DataplanFilterImpl constructor(
    val dataPoints: Map<String, HashSet<String>?>,
    private val blockEvents: Boolean,
    private val blockEventAttributes: Boolean,
    private val blockUserAttributes: Boolean,
    private val blockUserIdentities: Boolean
) : DataplanFilter {

    constructor(dataplanOptions: MParticleOptions.DataplanOptions) :
        this(
            extractDataPoints(dataplanOptions.dataplan),
            dataplanOptions.isBlockEvents,
            dataplanOptions.isBlockEventAttributes,
            dataplanOptions.isBlockUserAttributes,
            dataplanOptions.isBlockUserIdentities
        )

    init {
        Logger.debug(
            """

Data Plan parsed for Kit Filtering: 
    blockEvents=$blockEvents
    blockEventAttributes=$blockEventAttributes
    blockUserAttributes=$blockUserAttributes
    blockUserIdentities=$blockUserIdentities
        ${
            dataPoints.entries.joinToString("\n") { (key, value) ->
                "$key\n\t${value?.joinToString("\n\t") { it }}"
            }
            }
        """
        )
    }

    /**
     * filters out events and their attributes if
     * 1) they are not defined within the dataplan
     * 2) the corresponding flag for the filtering area is true
     *
     * note: this will NOT return a copy of the event, it will return either the original event filtered or null
     */
    override fun <T : BaseEvent> transformEventForEvent(event: T?): T? {
        if (event == null) {
            return null
        }
        if (blockEvents || blockEventAttributes) {
            val dataPointKey = when {
                event is MPEvent ->
                    when {
                        event.isScreenEvent -> DataPoint(SCREEN_EVENT_KEY, event.eventName)
                        else -> DataPoint(CUSTOM_EVENT_KEY, event.eventName, event.eventType.getEventsApiName())
                    }
                event is CommerceEvent ->
                    when {
                        !event.productAction.isNullOrBlank() -> DataPoint(PRODUCT_ACTION_KEY, event.productAction)
                        !event.promotionAction.isNullOrBlank() -> DataPoint(PROMOTION_ACTION_KEY, event.promotionAction)
                        !event.impressions.isNullOrEmpty() -> DataPoint(PRODUCT_IMPRESSION_KEY)
                        else -> null
                    }
                else -> null
            }
            // shouldn't happen but we have to handle it
            dataPointKey ?: return event
            // if there is no valid datapoint then it is an unplanned event
            if (blockEvents && !dataPoints.containsKey(dataPointKey.toString())) {
                Logger.verbose("Blocking unplanned event: $dataPointKey")
                return null
            }
            val dataPoint = dataPoints[dataPointKey.toString()]
            if (blockEventAttributes) {
                event.customAttributes = event.customAttributeStrings?.filterKeys {
                    // null dataPoint means there are no constraints for custom attributes
                    if (dataPoint == null || dataPoint.contains(it)) {
                        true
                    } else {
                        Logger.verbose("Blocking unplanned attribute: $it")
                        false
                    }
                }
                if (event is CommerceEvent) {
                    val productActionDatapoint = dataPoints["$dataPointKey.$PRODUCT_ACTION_PRODUCTS"]
                    event.products?.forEach { product ->
                        product?.customAttributes?.apply {
                            val filteredAttributes = filterKeys {
                                productActionDatapoint?.contains(it) ?: true
                            }
                            clear()
                            putAll(filteredAttributes)
                        }
                    }
                    val productImpressionDatapoint = dataPoints["$dataPointKey.$PRODUCT_IMPRESSION_PRODUCTS"]
                    if (event.impressions?.size ?: 0 > 0) {
                        event.impressions?.forEach {
                            it.products.forEach { product ->
                                product?.customAttributes?.apply {
                                    val filteredAttributes = filterKeys {
                                        productImpressionDatapoint?.contains(it) ?: true
                                    }
                                    clear()
                                    putAll(filteredAttributes)
                                }
                            }
                        }
                    }
                }
            }
            return event
        }
        return event
    }

    override fun transformIdentities(identities: Map<MParticle.IdentityType, String?>?): Map<MParticle.IdentityType, String?>? {
        if (identities == null) {
            return null
        }
        if (blockUserIdentities) {
            val datapoint = dataPoints[USER_IDENTITIES_KEY]
            if (datapoint != null) {
                return identities.filterKeys { identity ->
                    datapoint.contains(MParticleIdentityClientImpl.getStringValue(identity)).also {
                        if (!it) {
                            Logger.verbose("Blocking unplanned UserIdentity: $it")
                        }
                    }
                }
            }
        }
        return identities
    }

    override fun <T> transformUserAttributes(attributes: Map<String, T>?): Map<String, T>? {
        if (attributes == null) {
            return null
        }
        if (blockUserAttributes) {
            val datapoint = dataPoints[USER_ATTRIBUTES_KEY]
            if (datapoint != null) {
                return attributes.filterKeys { attribute ->
                    datapoint.contains(attribute).also {
                        if (!it) {
                            Logger.verbose("Blocking unplanned UserAttribute: $it")
                        }
                    }
                }
            }
        }
        return attributes
    }

    override fun isUserAttributeBlocked(key: String?): Boolean {
        if (blockUserAttributes && key != null) {
            val datapoint = dataPoints[USER_ATTRIBUTES_KEY]
            if (datapoint != null) {
                return !datapoint.contains(key).also {
                    if (!it) {
                        Logger.verbose("Blocking unplanned UserAttribute: $key")
                    }
                }
            }
        }
        return false
    }

    override fun isUserIdentityBlocked(key: MParticle.IdentityType?): Boolean {
        if (blockUserIdentities && key != null) {
            val datapoint = dataPoints[USER_IDENTITIES_KEY]
            if (datapoint != null) {
                return !datapoint.contains(MParticleIdentityClientImpl.getStringValue(key)).also {
                    if (!it) {
                        Logger.verbose("Blocking unplanned UserIdentity: $key")
                    }
                }
            }
        }
        return false
    }

    internal companion object {
        const val SCREEN_EVENT_KEY = "screen_view"
        const val CUSTOM_EVENT_KEY = "custom_event"
        const val PRODUCT_ACTION_KEY = "product_action"
        const val PROMOTION_ACTION_KEY = "promotion_action"
        const val PRODUCT_IMPRESSION_KEY = "product_impression"
        const val USER_IDENTITIES_KEY = "user_identities"
        const val USER_ATTRIBUTES_KEY = "user_attributes"

        const val PRODUCT_ACTION_PRODUCTS = "product_action_product"
        const val PRODUCT_IMPRESSION_PRODUCTS = "product_impression_product"

        private val emptyJSONObject = JSONObject()

        @JvmField
        val EMPTY: DataplanFilter = EmptyDataplanFilter()

        /**
         * parse dataplan into memory. data structure consists of a key composed of the
         * event type concatenated to the event name (if applicable) concatenated to the custom
         * event type (if applicable), with periods, pointing to a set of "legal" attribute keys
         **/
        fun extractDataPoints(dataplan: JSONObject): Map<String, HashSet<String>?> {
            val points = mutableMapOf<String, HashSet<String>?>()
            dataplan
                .optJSONObject("version_document")
                ?.optJSONArray("data_points")
                ?.toList()
                ?.filterIsInstance<JSONObject>()
                ?.forEach {
                    val match = it.getJSONObject("match")
                    val key = generateDatapointKey(match)
                    if (key != null) {
                        val properties = getAllowedKeys(key, it)
                        points.put(key.toString(), properties)
                        val productKeys = key.getProductDataPoints()
                        productKeys?.forEach { productKey ->
                            points.put(productKey.toString(), getAllowedKeys(productKey, it))
                        }
                    }
                }
            return points
        }

        fun generateDatapointKey(match: JSONObject): DataPoint? {
            val criteria = match.optJSONObject("criteria")
            val matchType = match.optString("type")
            when (matchType) {
                CUSTOM_EVENT_KEY -> {
                    val eventName = criteria?.optString("event_name")
                    val eventType = criteria?.optString("custom_event_type")
                    if (!eventName.isNullOrBlank() && !eventType.isNullOrBlank()) {
                        return DataPoint(matchType, eventName, eventType)
                    }
                }
                PRODUCT_ACTION_KEY, PROMOTION_ACTION_KEY -> {
                    var commerceEventType = criteria?.optString("action")
                    if (commerceEventType == "remove_from_wish_list") {
                        commerceEventType = "remove_from_wishlist"
                    }
                    if (!commerceEventType.isNullOrBlank()) {
                        return DataPoint(matchType, commerceEventType)
                    }
                }
                PRODUCT_IMPRESSION_KEY, USER_ATTRIBUTES_KEY, USER_IDENTITIES_KEY -> {
                    return DataPoint(matchType)
                }
                SCREEN_EVENT_KEY -> {
                    val screenName = criteria?.optString("screen_name")
                    if (!screenName.isNullOrBlank()) {
                        return DataPoint(matchType, screenName)
                    }
                }
            }
            return null
        }

        // returns a set of "allowed" keys, or `null` if all keys are allowed
        private fun getAllowedKeys(datapoint: DataPoint, jsonObject: JSONObject): HashSet<String>? {
            val definition = jsonObject.optJSONObject("validator")
                ?.optJSONObject("definition")
            when (datapoint.type) {
                CUSTOM_EVENT_KEY, SCREEN_EVENT_KEY, PRODUCT_IMPRESSION_KEY, PROMOTION_ACTION_KEY, PRODUCT_ACTION_KEY -> {
                    val data = definition
                        ?.optJSONObject("properties")
                        ?.optJSONObject("data") ?: return null
                    if (datapoint.productAttributeType == null) {
                        data.let {
                            val customAttributes = it.getConstrainedPropertiesJSONObject("custom_attributes")
                            if (customAttributes == null) {
                                return null
                            } else {
                                return customAttributes.getConstrainedPropertiesKeySet()
                            }
                            // if nothing can be gathered, allow all attributes
                        }
                    } else {
                        data.let {
                            val products = when (datapoint.productAttributeType) {
                                PRODUCT_ACTION_PRODUCTS -> {
                                    val productBlock = it.getConstrainedPropertiesJSONObject("product_action")
                                        ?.getConstrainedPropertiesJSONObject("products")
                                    if (productBlock == emptyJSONObject) {
                                        return hashSetOf()
                                    }
                                    productBlock
                                }
                                PRODUCT_IMPRESSION_PRODUCTS -> {
                                    var productBlock = it.getConstrainedPropertiesJSONObject("product_impressions")
                                    if (productBlock == emptyJSONObject) {
                                        return hashSetOf()
                                    }
                                    productBlock = productBlock?.optJSONObject("items")
                                        ?.getConstrainedPropertiesJSONObject("products")
                                    if (productBlock == emptyJSONObject) {
                                        return hashSetOf()
                                    }
                                    productBlock
                                }
                                else -> {
                                    return null
                                }
                            }
                            return products?.optJSONObject("items")
                                ?.getConstrainedPropertiesJSONObject("custom_attributes")
                                ?.getConstrainedPropertiesKeySet()
                        }
                    }
                }
                else -> {
                    return definition?.getConstrainedPropertiesKeySet()
                }
            }
        }

        fun JSONArray.toList(): List<Any> {
            val list = ArrayList<Any>()
            for (i in 0 until this.length()) {
                list.add(this[i])
            }
            return list
        }

        /**
         *  accepts an object like this:
         *  "data": {
         *     "additionalProperties": false
         *     "properties": {
         *          constraintField: {
         *
         *          }
         *          "some other field" {
         *
         *          }
         *     }
         *  }
         *
         *  and returns either:
         *  1) null, if the constraintField is not present but additionalProperties are allowed
         *  2) empty JSONObject if the constrainField is not present and additionalProperties are **not** allowed
         *  3) the constarintField JSONObject if the field is present
         */
        fun JSONObject.getConstrainedPropertiesJSONObject(constraintField: String): JSONObject? {
            if (this == emptyJSONObject) {
                return this
            }
            val constraintBlock = optJSONObject("properties")?.optJSONObject(constraintField)
            if (constraintBlock == null) {
                if (optBoolean("additionalProperties", true)) {
                    return null
                } else {
                    return emptyJSONObject
                }
            } else {
                return constraintBlock
            }
        }

        /**
         * similar to the above, but instead of returning the JSONObject targeted by `constraintField`,
         * this will return the set of keys in the "properties" object
         */
        fun JSONObject.getConstrainedPropertiesKeySet(): HashSet<String>? {
            if (this == emptyJSONObject) {
                return hashSetOf()
            }
            if (optBoolean("additionalProperties", true)) {
                return null
            } else {
                return optJSONObject("properties")?.keys()?.toHashSet() ?: hashSetOf()
            }
        }

        fun MParticle.IdentityType.getEventsApiName(): String {
            return MParticleIdentityClientImpl.getStringValue(this)
        }

        fun MParticle.EventType.getEventsApiName(): String {
            return when (this) {
                MParticle.EventType.Location -> "location"
                MParticle.EventType.Media -> "media"
                MParticle.EventType.Navigation -> "navigation"
                MParticle.EventType.Other -> "other"
                MParticle.EventType.Search -> "search"
                MParticle.EventType.Social -> "social"
                MParticle.EventType.Transaction -> "transaction"
                MParticle.EventType.UserContent -> "user_content"
                MParticle.EventType.UserPreference -> "user_preference"
                else -> "unknown"
            }
        }

        fun <T> Iterator<T>.toHashSet(): HashSet<T> {
            val set = HashSet<T>()
            this.forEach { set.add(it) }
            return set
        }
    }

    class DataPoint(val type: String, val name: String? = null, val eventType: String? = null) {
        constructor(datapoint: DataPoint) : this(datapoint.type, datapoint.name, datapoint.eventType)

        var productAttributeType: String? = null
            private set

        fun getProductDataPoints() =
            when (type) {
                PRODUCT_ACTION_KEY, PRODUCT_IMPRESSION_KEY -> listOf(
                    DataPoint(this).apply { productAttributeType = PRODUCT_ACTION_PRODUCTS },
                    DataPoint(this).apply { productAttributeType = PRODUCT_IMPRESSION_PRODUCTS }
                )
                else -> null
            }

        override fun toString() = "$type${if (name != null) ".$name" else ""}${if (eventType != null) ".$eventType" else ""}${productAttributeType?.let { ".$it" } ?: ""}"
    }

    class EmptyDataplanFilter : DataplanFilter {
        override fun <T : BaseEvent> transformEventForEvent(event: T?) = event
        override fun transformIdentities(identities: Map<MParticle.IdentityType, String?>?) = identities
        override fun <T> transformUserAttributes(attributes: Map<String, T>?) = attributes
        override fun isUserAttributeBlocked(key: String?) = false
        override fun isUserIdentityBlocked(key: MParticle.IdentityType?) = false
    }
}
