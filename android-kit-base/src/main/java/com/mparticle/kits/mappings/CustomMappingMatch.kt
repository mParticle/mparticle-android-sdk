package com.mparticle.kits.mappings

import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.KitUtils.hashForFiltering
import com.mparticle.kits.mappings.EventWrapper.CommerceEventWrapper
import com.mparticle.kits.mappings.EventWrapper.MPEventWrapper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CustomMappingMatch(match: JSONObject?) {
    var mMessageType = -1
    var mMatchType = "String"
    var commerceMatchProperty: String? = null
    var commerceMatchPropertyName: String? = null
    var commerceMatchPropertyValues: MutableSet<String>? = null
    var mEventHash = 0
    var mEventName: String? = null
    var mAttributeKey: String? = null
    var mAttributeValues: MutableSet<String>? = null

    init {
        if (match == null) {
            mEventHash = 0
            mMessageType = -1
            mMatchType = "String"
            mEventName = null
            mAttributeKey = null
            mAttributeValues = null
        } else {
            mMessageType = match.optInt("message_type")
            mMatchType = match.optString("event_match_type", "String")
            commerceMatchProperty = match.optString("property", PROPERTY_LOCATION_EVENT_ATTRIBUTE)
            commerceMatchPropertyName = match.optString("property_name", null)
            if (match.has("property_values")) {
                try {
                    val propertyValues = match.getJSONArray("property_values")
                    commerceMatchPropertyValues = HashSet(propertyValues.length())
                    for (i in 0 until propertyValues.length()) {
                        commerceMatchPropertyValues!!.add(propertyValues.optString(i).lowercase())
                    }
                } catch (jse: JSONException) {
                }
            }
            if (mMatchType.startsWith(MATCH_TYPE_HASH)) {
                mEventHash = match.optString("event").toInt()
                mAttributeKey = null
                mAttributeValues = null
                mEventName = null
            } else {
                mEventHash = 0
                mEventName = match.optString("event")
                mAttributeKey = match.optString("attribute_key")
                try {
                    if (match.has("attribute_values") && match["attribute_values"] is JSONArray) {
                        val values = match.getJSONArray("attribute_values")
                        mAttributeValues = HashSet(values.length())
                        for (i in 0 until values.length()) {
                            mAttributeValues!!.add(values.optString(i).lowercase())
                        }
                    } else if (match.has("attribute_values")) {
                        mAttributeValues = HashSet(1)
                        mAttributeValues!!.add(match.optString("attribute_values").lowercase())
                    } else {
                        mAttributeValues = HashSet(1)
                        mAttributeValues!!.add(match.optString("attribute_value").lowercase())
                    }
                } catch (jse: JSONException) {
                }
            }
        }
    }

    /**
     * This is an optimization - check the basic stuff to see if we have a match before actually trying to do the projection.
     */
    fun isMatch(eventWrapper: EventWrapper): Boolean {
        if (eventWrapper.messageType != mMessageType) {
            return false
        }
        if (eventWrapper is MPEventWrapper) {
            if (matchAppEvent(eventWrapper)) {
                return true
            }
        } else {
            val commerceEvent = matchCommerceEvent(eventWrapper as CommerceEventWrapper)
            if (commerceEvent != null) {
                eventWrapper.setEvent(commerceEvent)
                return true
            }
        }
        return false
    }

    private fun matchAppEvent(eventWrapper: MPEventWrapper): Boolean {
        val event = eventWrapper.event ?: return false
        return if (mMatchType.startsWith(MATCH_TYPE_HASH) && eventWrapper.eventHash == mEventHash) {
            true
        } else if (mMatchType.startsWith(MATCH_TYPE_STRING) &&
            event.eventName.equals(
                    mEventName,
                    ignoreCase = true
                ) && event.customAttributeStrings != null &&
            event.customAttributeStrings!!.containsKey(mAttributeKey) &&
            attributeValues!!.contains(
                    event.customAttributeStrings!![mAttributeKey]!!.lowercase()
                )
        ) {
            true
        } else {
            false
        }
    }

    private fun matchCommerceEvent(eventWrapper: CommerceEventWrapper): CommerceEvent? {
        val commerceEvent = eventWrapper.event ?: return null
        if (commerceMatchProperty != null && commerceMatchPropertyName != null) {
            if (commerceMatchProperty.equals(PROPERTY_LOCATION_EVENT_FIELD, ignoreCase = true)) {
                return if (matchCommerceFields(commerceEvent)) {
                    commerceEvent
                } else {
                    null
                }
            } else if (commerceMatchProperty.equals(
                    PROPERTY_LOCATION_EVENT_ATTRIBUTE,
                    ignoreCase = true
                )
            ) {
                return if (matchCommerceAttributes(commerceEvent)) {
                    commerceEvent
                } else {
                    null
                }
            } else if (commerceMatchProperty.equals(
                    PROPERTY_LOCATION_PRODUCT_FIELD,
                    ignoreCase = true
                )
            ) {
                return matchProductFields(commerceEvent)
            } else if (commerceMatchProperty.equals(
                    PROPERTY_LOCATION_PRODUCT_ATTRIBUTE,
                    ignoreCase = true
                )
            ) {
                return matchProductAttributes(commerceEvent)
            } else if (commerceMatchProperty.equals(
                    PROPERTY_LOCATION_PROMOTION_FIELD,
                    ignoreCase = true
                )
            ) {
                return matchPromotionFields(commerceEvent)
            }
        }
        return if (mMatchType.startsWith(MATCH_TYPE_HASH) && eventWrapper.eventHash == mEventHash) {
            commerceEvent
        } else null
    }

    private fun matchPromotionFields(event: CommerceEvent): CommerceEvent? {
        val hash = commerceMatchPropertyName!!.toInt()
        val promotionList = event.promotions
        if (promotionList == null || promotionList.size == 0) {
            return null
        }
        val matchedPromotions: MutableList<Promotion> = LinkedList()
        val promotionFields: MutableMap<String, String?> = HashMap()
        for (promotion in promotionList) {
            promotionFields.clear()
            CommerceEventUtils.extractPromotionAttributes(promotion, promotionFields)
            if (promotionFields != null) {
                for ((key, value) in promotionFields) {
                    val attributeHash =
                        hashForFiltering(CommerceEventUtils.getEventType(event).toString() + key)
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues!!.contains(value!!.lowercase())) {
                            matchedPromotions.add(promotion)
                        }
                    }
                }
            }
        }
        return if (matchedPromotions.size == 0) {
            null
        } else if (matchedPromotions.size != promotionList.size) {
            CommerceEvent.Builder(event).promotions(matchedPromotions).build()
        } else {
            event
        }
    }

    private fun matchProductFields(event: CommerceEvent): CommerceEvent? {
        val hash = commerceMatchPropertyName!!.toInt()
        val type = CommerceEventUtils.getEventType(event)
        val productList = event.products
        if (productList == null || productList.size == 0) {
            return null
        }
        val matchedProducts: MutableList<Product> = LinkedList()
        val productFields: MutableMap<String, String?> = HashMap()
        for (product in productList) {
            productFields.clear()
            CommerceEventUtils.extractProductFields(product, productFields)
            if (productFields != null) {
                for ((key, value) in productFields) {
                    val attributeHash = hashForFiltering(type.toString() + key)
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues!!.contains(value!!.lowercase())) {
                            matchedProducts.add(product)
                        }
                    }
                }
            }
        }
        return if (matchedProducts.size == 0) {
            null
        } else if (matchedProducts.size != productList.size) {
            CommerceEvent.Builder(event).products(matchedProducts).build()
        } else {
            event
        }
    }

    private fun matchProductAttributes(event: CommerceEvent): CommerceEvent? {
        val hash = commerceMatchPropertyName!!.toInt()
        val productList = event.products
        if (productList == null || productList.size == 0) {
            return null
        }
        val matchedProducts: MutableList<Product> = LinkedList()
        for (product in productList) {
            val attributes = product.customAttributes
            if (attributes != null) {
                for ((key, value) in attributes) {
                    val attributeHash =
                        hashForFiltering(CommerceEventUtils.getEventType(event).toString() + key)
                    if (attributeHash == hash) {
                        if (commerceMatchPropertyValues!!.contains(value.lowercase())) {
                            matchedProducts.add(product)
                        }
                    }
                }
            }
        }
        return if (matchedProducts.size == 0) {
            null
        } else if (matchedProducts.size != productList.size) {
            CommerceEvent.Builder(event).products(matchedProducts).build()
        } else {
            event
        }
    }

    private fun matchCommerceAttributes(event: CommerceEvent): Boolean {
        val attributes = event.customAttributeStrings
        if (attributes == null || attributes.size < 1) {
            return false
        }
        val hash = commerceMatchPropertyName!!.toInt()
        for ((key, value) in attributes) {
            val attributeHash =
                hashForFiltering(CommerceEventUtils.getEventType(event).toString() + key)
            if (attributeHash == hash) {
                return commerceMatchPropertyValues!!.contains(value.lowercase())
            }
        }
        return false
    }

    private fun matchCommerceFields(event: CommerceEvent): Boolean {
        val hash = commerceMatchPropertyName!!.toInt()
        val fields: MutableMap<String, String?> = HashMap()
        CommerceEventUtils.extractActionAttributes(event, fields)
        for ((key, value) in fields) {
            val fieldHash =
                hashForFiltering(CommerceEventUtils.getEventType(event).toString() + key)
            if (fieldHash == hash) {
                return commerceMatchPropertyValues!!.contains(value!!.lowercase())
            }
        }
        return false
    }

    val attributeValues: Set<String>?
        get() = mAttributeValues

    companion object {
        const val MATCH_TYPE_STRING = "S"
        const val MATCH_TYPE_HASH = "H"
        const val PROPERTY_LOCATION_EVENT_FIELD = "EventField"
        const val PROPERTY_LOCATION_EVENT_ATTRIBUTE = "EventAttribute"
        const val PROPERTY_LOCATION_PRODUCT_FIELD = "ProductField"
        const val PROPERTY_LOCATION_PRODUCT_ATTRIBUTE = "ProductAttribute"
        const val PROPERTY_LOCATION_PROMOTION_FIELD = "PromotionField"
    }
}
