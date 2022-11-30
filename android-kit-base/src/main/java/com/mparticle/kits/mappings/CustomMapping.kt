package com.mparticle.kits.mappings

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.internal.MPUtility
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.mappings.EventWrapper.CommerceEventWrapper
import com.mparticle.kits.mappings.EventWrapper.MPEventWrapper
import org.json.JSONObject
import java.lang.NumberFormatException
import java.util.*
import java.util.AbstractMap

class CustomMapping(projectionJson: JSONObject) {
    val mID: Int
    val mMappingId: Int
    val mModuleMappingId: Int
    var mMaxCustomParams = 0
    var mAppendUnmappedAsIs = false
    var isDefault = false
    var mProjectedEventName: String? = null
    var mStaticAttributeMapList: MutableList<AttributeMap>? = null
    var mRequiredAttributeMapList: MutableList<AttributeMap>? = null
    private var isSelectorLast = false
    var mOutboundMessageType = 0
    private var matchList: MutableList<CustomMappingMatch>? = null
    fun getMatchList(): List<CustomMappingMatch>? {
        return matchList
    }

    init {
        mID = projectionJson.getInt("id")
        mMappingId = projectionJson.optInt("pmid")
        mModuleMappingId = projectionJson.optInt("pmmid")
        if (projectionJson.has("matches")) {
            val matchJson = projectionJson.getJSONArray("matches")
            matchList = ArrayList(matchJson.length())
            for (i in 0 until matchJson.length()) {
                val match = CustomMappingMatch(matchJson.getJSONObject(i))
                matchList!!.add(match)
            }
        } else {
            matchList = ArrayList(1)
            val match = CustomMappingMatch(null)
            matchList!!.add(match)
        }
        if (projectionJson.has("behavior")) {
            val behaviors = projectionJson.getJSONObject("behavior")
            mMaxCustomParams = behaviors.optInt("max_custom_params", Int.MAX_VALUE)
            mAppendUnmappedAsIs = behaviors.optBoolean("append_unmapped_as_is")
            isDefault = behaviors.optBoolean("is_default")
            isSelectorLast =
                behaviors.optString("selector", "foreach").equals("last", ignoreCase = true)
        } else {
            mMaxCustomParams = Int.MAX_VALUE
            mAppendUnmappedAsIs = false
            isDefault = false
            isSelectorLast = false
        }
        if (projectionJson.has("action")) {
            val action = projectionJson.getJSONObject("action")
            mOutboundMessageType = action.optInt("outbound_message_type", 4)
            mProjectedEventName = action.optString("projected_event_name")
            if (action.has("attribute_maps")) {
                mRequiredAttributeMapList = LinkedList()
                mStaticAttributeMapList = LinkedList()
                val attributeMapList = action.getJSONArray("attribute_maps")
                for (i in 0 until attributeMapList.length()) {
                    val attProjection = AttributeMap(attributeMapList.getJSONObject(i))
                    if (attProjection.mMatchType.startsWith(MATCH_TYPE_STATIC)) {
                        mStaticAttributeMapList!!.add(attProjection)
                    } else {
                        mRequiredAttributeMapList!!.add(attProjection)
                    }
                }
                Collections.sort(mRequiredAttributeMapList) { lhs, rhs ->
                    if (lhs.mIsRequired == rhs.mIsRequired) {
                        0
                    } else if (lhs.mIsRequired && !rhs.mIsRequired) {
                        -1
                    } else {
                        1
                    }
                }
            } else {
                mRequiredAttributeMapList = null
                mStaticAttributeMapList = null
            }
        } else {
            mRequiredAttributeMapList = null
            mStaticAttributeMapList = null
            mProjectedEventName = null
            mOutboundMessageType = 4
        }
    }

    private fun projectMPEvent(event: MPEvent): ProjectionResult? {
        val eventWrapper = MPEventWrapper(event)
        val eventName =
            if (MPUtility.isEmpty(mProjectedEventName)) event!!.eventName else mProjectedEventName!!
        val builder = MPEvent.Builder(event!!)
        builder.eventName(eventName)
        builder.customAttributes(null)
        val newAttributes: MutableMap<String?, String?> = HashMap()
        val usedAttributes: MutableSet<String?> = HashSet()
        if (!mapAttributes(
                mRequiredAttributeMapList,
                eventWrapper,
                newAttributes,
                usedAttributes,
                null,
                null
            )
        ) {
            return null
        }
        if (mStaticAttributeMapList != null) {
            for (i in mStaticAttributeMapList!!.indices) {
                val attProjection = mStaticAttributeMapList!!.get(i)
                newAttributes[attProjection.mProjectedAttributeName] = attProjection.mValue
                usedAttributes.add(attProjection.mValue)
            }
        }
        if (mAppendUnmappedAsIs && mMaxCustomParams > 0 && newAttributes.size < mMaxCustomParams) {
            val originalAttributes: Map<String?, String>
            originalAttributes = if (event.customAttributeStrings != null) {
                HashMap(event.customAttributeStrings)
            } else {
                HashMap()
            }
            val sortedKeys = originalAttributes.keys.toMutableList()
            sortedKeys.sortBy { it }
            var i = 0
            while (i < sortedKeys.size && newAttributes.size < mMaxCustomParams) {
                val key = sortedKeys[i]
                if (!usedAttributes.contains(key) && !newAttributes.containsKey(key)) {
                    newAttributes[key] = originalAttributes[key]
                }
                i++
            }
        }
        builder.customAttributes(newAttributes)
        return ProjectionResult(builder.build(), mID)
    }

    fun project(commerceEventWrapper: CommerceEventWrapper): List<ProjectionResult>? {
        val projectionResults: MutableList<ProjectionResult> = LinkedList()
        val commerceEvent = commerceEventWrapper.event
        val eventType = CommerceEventUtils.getEventType(commerceEvent)
        // TODO Impression projections are not supported for now
        if (eventType == CommerceEventUtils.Constants.Companion.EVENT_TYPE_IMPRESSION) {
            return null
        } else if (eventType == CommerceEventUtils.Constants.Companion.EVENT_TYPE_PROMOTION_CLICK || eventType == CommerceEventUtils.Constants.Companion.EVENT_TYPE_PROMOTION_VIEW) {
            val promotions = commerceEvent!!.promotions
            if (promotions == null || promotions.size == 0) {
                val projectionResult = projectCommerceEvent(commerceEventWrapper, null, null)
                if (projectionResult != null) {
                    projectionResults.add(projectionResult)
                }
            } else {
                if (isSelectorLast) {
                    val promotion = promotions[promotions.size - 1]
                    val projectionResult =
                        projectCommerceEvent(commerceEventWrapper, null, promotion)
                    if (projectionResult != null) {
                        projectionResults.add(projectionResult)
                    }
                } else {
                    for (i in promotions.indices) {
                        val projectionResult =
                            projectCommerceEvent(commerceEventWrapper, null, promotions[i])
                        if (projectionResult != null) {
                            if (projectionResult.commerceEvent != null) {
                                val foreachCommerceEvent = CommerceEvent.Builder(
                                    projectionResult.commerceEvent!!
                                )
                                    .addPromotion(promotions[i])
                                    .build()
                                projectionResult.commerceEvent = foreachCommerceEvent
                            }
                            projectionResults.add(projectionResult)
                        }
                    }
                }
            }
        } else {
            val products = commerceEvent!!.products
            if (isSelectorLast) {
                var product: Product? = null
                if (products != null && products.size > 0) {
                    product = products[products.size - 1]
                }
                val projectionResult = projectCommerceEvent(commerceEventWrapper, product, null)
                if (projectionResult != null) {
                    projectionResults.add(projectionResult)
                }
            } else {
                if (products != null) {
                    for (i in products.indices) {
                        val projectionResult =
                            projectCommerceEvent(commerceEventWrapper, products[i], null)
                        if (projectionResult != null) {
                            if (projectionResult.commerceEvent != null) {
                                val foreachCommerceEvent = CommerceEvent.Builder(
                                    projectionResult.commerceEvent!!
                                )
                                    .addProduct(products[i])
                                    .build()
                                projectionResult.commerceEvent = foreachCommerceEvent
                            }
                            projectionResults.add(projectionResult)
                        }
                    }
                }
            }
        }
        return if (projectionResults.size > 0) {
            projectionResults
        } else {
            null
        }
    }

    fun project(event: MPEventWrapper): List<ProjectionResult>? {
        val projectionResults: MutableList<ProjectionResult> = LinkedList()
        val projectionResult = projectMPEvent(event.event)
        if (projectionResult != null) {
            projectionResults.add(projectionResult)
        }
        return if (projectionResults.size > 0) {
            projectionResults
        } else {
            null
        }
    }

    private fun mapAttributes(
        projectionList: List<AttributeMap>?,
        eventWrapper: EventWrapper,
        mappedAttributes: MutableMap<String?, String?>,
        usedAttributes: MutableSet<String?>,
        product: Product?,
        promotion: Promotion?
    ): Boolean {
        if (projectionList != null) {
            for (i in projectionList.indices) {
                val attProjection = projectionList[i]
                var entry: Map.Entry<String?, String?>? = null
                if (attProjection.mMatchType.startsWith(MATCH_TYPE_STRING)) {
                    entry = eventWrapper.findAttribute(
                        attProjection.mLocation,
                        attProjection.mValue,
                        product,
                        promotion
                    )
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_HASH)) {
                    entry = eventWrapper.findAttribute(
                        attProjection.mLocation,
                        attProjection.mValue.toInt(),
                        product,
                        promotion
                    )
                } else if (attProjection.mMatchType.startsWith(MATCH_TYPE_FIELD) && eventWrapper.event is MPEvent) {
                    // match_type field is a special case for mapping the event name to an attribute, only supported by MPEvent
                    entry = AbstractMap.SimpleEntry(
                        attProjection.mProjectedAttributeName,
                        (eventWrapper.event as MPEvent).eventName
                    )
                }
                if (entry == null || !attProjection.matchesDataType(entry.value.orEmpty())) {
                    return if (attProjection.mIsRequired) {
                        false
                    } else {
                        continue
                    }
                }
                var key = entry.key
                if (!MPUtility.isEmpty(attProjection.mProjectedAttributeName)) {
                    key = attProjection.mProjectedAttributeName
                }
                mappedAttributes[key] = entry.value
                usedAttributes.add(entry.key)
            }
        }
        return true
    }

    private fun projectCommerceEvent(
        eventWrapper: CommerceEventWrapper,
        product: Product?,
        promotion: Promotion?
    ): ProjectionResult? {
        val mappedAttributes: MutableMap<String?, String?> = HashMap()
        val usedAttributes: MutableSet<String?> = HashSet()
        if (!mapAttributes(
                mRequiredAttributeMapList,
                eventWrapper,
                mappedAttributes,
                usedAttributes,
                product,
                promotion
            )
        ) {
            return null
        }
        mStaticAttributeMapList?.let {
            it.forEach {
                mappedAttributes[it.mProjectedAttributeName] = it.mValue
                usedAttributes.add(it.mValue)
            }
        }
        if (mAppendUnmappedAsIs && mMaxCustomParams > 0 && mappedAttributes.size < mMaxCustomParams) {
            val event = eventWrapper.event
            val originalAttributes: Map<String?, String>
            originalAttributes = if (event!!.customAttributeStrings != null) {
                HashMap(event.customAttributeStrings)
            } else {
                HashMap()
            }
            val sortedKeys = originalAttributes.keys.toMutableList()
            sortedKeys.sortBy { it }
            var i = 0
            while (i < sortedKeys.size && mappedAttributes.size < mMaxCustomParams) {
                val key = sortedKeys[i]
                if (!usedAttributes.contains(key) && !mappedAttributes.containsKey(key)) {
                    mappedAttributes[key] = originalAttributes[key]
                }
                i++
            }
        }
        return if (mOutboundMessageType == 16) {
            ProjectionResult(
                CommerceEvent.Builder(eventWrapper.event!!)
                    .internalEventName(mProjectedEventName)
                    .customAttributes(mappedAttributes)
                    .build(),
                mID
            )
        } else {
            ProjectionResult(
                MPEvent.Builder(
                    mProjectedEventName!!,
                    MParticle.EventType.Transaction
                )
                    .customAttributes(mappedAttributes)
                    .build(),
                mID
            )
        }
    }

    /**
     * All CustomMappingMatches for a given CustomMapping must have the same message type,
     * and a CustomMapping is guaranteed to have at least 1 match.
     * Due to this - just return the message type of the first CustomMappingMatch.
     */
    val messageType: Int
        get() = matchList!![0].mMessageType

    class AttributeMap(attributeMapJson: JSONObject) {
        val mProjectedAttributeName: String
        val mValue: String
        val mDataType: Int
        val mMatchType: String
        val mIsRequired: Boolean
        val mLocation: String

        init {
            mProjectedAttributeName = attributeMapJson.optString("projected_attribute_name")
            mMatchType = attributeMapJson.optString("match_type", "String")
            mValue = attributeMapJson.optString("value")
            mDataType = attributeMapJson.optInt("data_type", 1)
            mIsRequired = attributeMapJson.optBoolean("is_required")
            mLocation = attributeMapJson.optString("property", PROPERTY_LOCATION_EVENT_ATTRIBUTE)
        }

        override fun equals(o: Any?): Boolean {
            return super.equals(o) || this.toString() == o.toString()
        }

        override fun toString(): String {
            return """
                projected_attribute_name: $mProjectedAttributeName
                match_type: $mMatchType
                value: $mValue
                data_type: $mDataType
                is_required: $mIsRequired
            """.trimIndent()
        }

        fun matchesDataType(value: String): Boolean {
            return when (mDataType) {
                1 -> true
                2 -> {
                    return try {
                        value.toInt()
                        true
                    } catch (nfe: NumberFormatException) {
                        false
                    }
                    "true".equals(value, ignoreCase = true) || "false".equals(
                        value,
                        ignoreCase = true
                    )
                }
                3 -> "true".equals(value, ignoreCase = true) || "false".equals(
                    value,
                    ignoreCase = true
                )
                4 -> {
                    return try {
                        value.toDouble()
                        true
                    } catch (nfe: NumberFormatException) {
                        false
                    }
                    false
                }
                else -> false
            }
        }
    }

    fun isMatch(wrapper: EventWrapper): Boolean {
        if (isDefault) {
            return true
        }
        for (match in matchList!!) {
            if (!match.isMatch(wrapper)) {
                return false
            }
        }
        return true
    }

    class ProjectionResult {
        val mPEvent: MPEvent?
        val projectionId: Int
        var commerceEvent: CommerceEvent?

        constructor(event: MPEvent?, projectionId: Int) {
            mPEvent = event
            commerceEvent = null
            this.projectionId = projectionId
        }

        constructor(commerceEvent: CommerceEvent?, projectionId: Int) {
            this.commerceEvent = commerceEvent
            mPEvent = null
            this.projectionId = projectionId
        }
    }

    companion object {
        const val MATCH_TYPE_STRING = "S"
        const val MATCH_TYPE_HASH = "H"
        const val MATCH_TYPE_FIELD = "F"
        const val MATCH_TYPE_STATIC = "Sta"
        const val PROPERTY_LOCATION_EVENT_FIELD = "EventField"
        const val PROPERTY_LOCATION_EVENT_ATTRIBUTE = "EventAttribute"
        const val PROPERTY_LOCATION_PRODUCT_FIELD = "ProductField"
        const val PROPERTY_LOCATION_PRODUCT_ATTRIBUTE = "ProductAttribute"
        const val PROPERTY_LOCATION_PROMOTION_FIELD = "PromotionField"
        fun projectEvents(
            event: MPEvent,
            customMappingList: List<CustomMapping>?,
            defaultCustomMapping: CustomMapping?
        ): List<ProjectionResult>? {
            return projectEvents(event, false, customMappingList, defaultCustomMapping, null)
        }

        fun projectEvents(
            event: CommerceEvent,
            customMappingList: List<CustomMapping>?,
            defaultCommerceCustomMapping: CustomMapping?
        ): List<ProjectionResult>? {
            if (CommerceEventUtils.getEventType(event) == CommerceEventUtils.Constants.Companion.EVENT_TYPE_IMPRESSION) {
                return null
            }
            val events: MutableList<ProjectionResult> = LinkedList()
            val wrapper = CommerceEventWrapper(event)
            customMappingList?.let {
                for (i in it.indices) {
                    val customMapping = customMappingList[i]
                    if (customMapping.isMatch(wrapper)) {
                        val results = customMapping.project(wrapper)
                        if (results != null) {
                            events.addAll(results)
                        }
                    }
                }
            }
            if (events.isEmpty()) {
                if (defaultCommerceCustomMapping != null) {
                    events.addAll(defaultCommerceCustomMapping.project(wrapper)!!)
                } else {
                    return null
                }
            }
            return events
        }

        fun projectEvents(
            event: MPEvent,
            isScreenEvent: Boolean,
            customMappingList: List<CustomMapping>?,
            defaultCustomMapping: CustomMapping?,
            defaultScreenCustomMapping: CustomMapping?
        ): List<ProjectionResult>? {
            val events: MutableList<ProjectionResult> = LinkedList()
            val wrapper = MPEventWrapper(event, isScreenEvent)
            customMappingList?.let {
                for (i in it.indices) {
                    val customMapping = it[i]
                    if (customMapping.isMatch(wrapper)) {
                        val newEvents = customMapping.project(wrapper)
                        if (newEvents != null) {
                            events.addAll(newEvents)
                        }
                    }
                }
            }
            if (events.isEmpty()) {
                if (isScreenEvent) {
                    if (defaultScreenCustomMapping != null) {
                        events.addAll(defaultScreenCustomMapping.project(wrapper)!!)
                    } else {
                        return null
                    }
                } else {
                    if (defaultCustomMapping != null) {
                        events.addAll(defaultCustomMapping.project(wrapper)!!)
                    } else {
                        return null
                    }
                }
            }
            return events
        }
    }
}
