package com.mparticle.kits.mappings

import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.KitUtils.hashForFiltering
import java.util.AbstractMap
import java.util.HashMap

/**
 * Decorator classes for MPEvent and CommerceEvent. Used to extend functionality and to cache values for Projection processing.
 */
abstract class EventWrapper {
    abstract fun getAttributeHashes(): Map<Int, String>
    protected var _attributeHashes: MutableMap<Int, String>? = null
    abstract val eventTypeOrdinal: Int
    abstract val event: Any?
    abstract val messageType: Int
    abstract val eventHash: Int
    abstract fun findAttribute(
        propertyType: String?,
        hash: Int,
        product: Product?,
        promotion: Promotion?
    ): Map.Entry<String, String?>?

    abstract fun findAttribute(
        propertyType: String?,
        keyName: String,
        product: Product?,
        promotion: Promotion?
    ): Map.Entry<String, String?>?

    class CommerceEventWrapper(private var mCommerceEvent: CommerceEvent) :
        EventWrapper() {
        private var eventFieldHashes: Map<Int, String>? = null
        private var eventFieldAttributes: HashMap<String, String?>? = null
        override fun getAttributeHashes(): Map<Int, String> {
            if (_attributeHashes == null) {
                _attributeHashes = HashMap()
                if (mCommerceEvent.customAttributeStrings != null) {
                    for ((key) in mCommerceEvent.customAttributeStrings!!) {
                        val hash = hashForFiltering(eventTypeOrdinal.toString() + key)
                        _attributeHashes!![hash] = key
                    }
                }
            }
            return _attributeHashes!!
        }

        override val eventTypeOrdinal: Int
            get() = CommerceEventUtils.getEventType(mCommerceEvent)
        override val event: CommerceEvent
            get() = mCommerceEvent

        fun setEvent(event: CommerceEvent) {
            mCommerceEvent = event
        }

        override val messageType: Int
            get() = 16
        override val eventHash: Int
            get() = hashForFiltering("" + eventTypeOrdinal)

        override fun findAttribute(
            propertyType: String?,
            hash: Int,
            product: Product?,
            promotion: Promotion?
        ): Map.Entry<String, String?>? {
            if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (mCommerceEvent.customAttributeStrings == null || mCommerceEvent.customAttributeStrings!!.size == 0) {
                    return null
                }
                val key = getAttributeHashes()[hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(
                        key,
                        mCommerceEvent.customAttributeStrings!![key]
                    )
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (eventFieldHashes == null) {
                    if (eventFieldAttributes == null) {
                        eventFieldAttributes = HashMap()
                        CommerceEventUtils.extractActionAttributes(
                            mCommerceEvent,
                            eventFieldAttributes!!
                        )
                        CommerceEventUtils.extractTransactionAttributes(
                            mCommerceEvent,
                            eventFieldAttributes!!
                        )
                    }
                    eventFieldHashes =
                        getHashes(eventTypeOrdinal.toString() + "", eventFieldAttributes!!)
                }
                val key = eventFieldHashes!![hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(key, eventFieldAttributes!![key])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PRODUCT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (product == null || product.customAttributes == null || product.customAttributes!!.size == 0) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractProductAttributes(product, attributes)
                val hashes = getHashes(
                    eventTypeOrdinal.toString() + "", attributes
                )
                val key = hashes[hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(key, attributes[key])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PRODUCT_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (product == null) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractProductFields(product, attributes)
                val hashes = getHashes(
                    eventTypeOrdinal.toString() + "", attributes
                )
                val key = hashes[hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(key, attributes[key])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PROMOTION_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (promotion == null) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractPromotionAttributes(promotion, attributes)
                val hashes = getHashes(
                    eventTypeOrdinal.toString() + "", attributes
                )
                val key = hashes[hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(key, attributes[key])
                }
            }
            return null
        }

        override fun findAttribute(
            propertyType: String?,
            keyName: String,
            product: Product?,
            promotion: Promotion?
        ): Map.Entry<String, String?>? {
            if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (mCommerceEvent.customAttributeStrings == null || mCommerceEvent.customAttributeStrings!!.size == 0) {
                    return null
                }
                if (mCommerceEvent.customAttributeStrings!!.containsKey(keyName)) {
                    return AbstractMap.SimpleEntry(
                        keyName,
                        mCommerceEvent.customAttributeStrings!![keyName]
                    )
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (eventFieldAttributes == null) {
                    eventFieldAttributes = HashMap()
                    CommerceEventUtils.extractActionAttributes(
                        mCommerceEvent,
                        eventFieldAttributes!!
                    )
                    CommerceEventUtils.extractTransactionAttributes(
                        mCommerceEvent,
                        eventFieldAttributes!!
                    )
                }
                if (eventFieldAttributes!!.containsKey(keyName)) {
                    return AbstractMap.SimpleEntry(keyName, eventFieldAttributes!![keyName])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PRODUCT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (product == null || product.customAttributes == null) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractProductAttributes(product, attributes)
                if (attributes.containsKey(keyName)) {
                    return AbstractMap.SimpleEntry(keyName, attributes[keyName])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PRODUCT_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (product == null) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractProductFields(product, attributes)
                if (attributes.containsKey(keyName)) {
                    return AbstractMap.SimpleEntry(keyName, attributes[keyName])
                }
            } else if (CustomMapping.Companion.PROPERTY_LOCATION_PROMOTION_FIELD.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (promotion == null) {
                    return null
                }
                val attributes: MutableMap<String, String?> = HashMap()
                CommerceEventUtils.extractPromotionAttributes(promotion, attributes)
                if (attributes.containsKey(keyName)) {
                    return AbstractMap.SimpleEntry(keyName, attributes[keyName])
                }
            }
            return null
        }
    }

    class MPEventWrapper @JvmOverloads constructor(
        private val mEvent: MPEvent,
        private val mScreenEvent: Boolean = false
    ) : EventWrapper() {
        override fun getAttributeHashes(): Map<Int, String> {
            if (_attributeHashes == null) {
                _attributeHashes = HashMap()
                if (mEvent.customAttributeStrings != null) {
                    for ((key) in mEvent.customAttributeStrings!!) {
                        val hash =
                            hashForFiltering(eventTypeOrdinal.toString() + mEvent.eventName + key)
                        _attributeHashes!![hash] = key
                    }
                }
            }
            return _attributeHashes!!
        }

        override val event: MPEvent
            get() = mEvent
        override val eventTypeOrdinal: Int
            get() = if (mScreenEvent) {
                0
            } else {
                mEvent.eventType.ordinal
            }
        override val eventHash: Int
            get() = if (mScreenEvent) {
                hashForFiltering(eventTypeOrdinal.toString() + mEvent.eventName)
            } else {
                mEvent.eventHash
            }
        override val messageType: Int
            get() = if (mScreenEvent) {
                3
            } else {
                4
            }

        override fun findAttribute(
            propertyType: String?,
            keyName: String,
            product: Product?,
            promotion: Promotion?
        ): Map.Entry<String, String?>? {
            if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                if (mEvent.customAttributeStrings == null) {
                    return null
                }
                val value = mEvent.customAttributeStrings!![keyName]
                if (value != null) {
                    return AbstractMap.SimpleEntry(keyName, value)
                }
            }
            return null
        }

        override fun findAttribute(
            propertyType: String?,
            hash: Int,
            product: Product?,
            promotion: Promotion?
        ): Map.Entry<String, String?>? {
            if (CustomMapping.Companion.PROPERTY_LOCATION_EVENT_ATTRIBUTE.equals(
                    propertyType,
                    ignoreCase = true
                )
            ) {
                val key = getAttributeHashes()[hash]
                if (key != null) {
                    return AbstractMap.SimpleEntry(key, mEvent.customAttributeStrings!![key])
                }
            }
            return null
        }
    }

    companion object {
        protected fun getHashes(hashPrefix: String, map: Map<String, String?>): Map<Int, String> {
            val hashedMap: MutableMap<Int, String> = HashMap()
            for ((key) in map) {
                val hash = hashForFiltering(hashPrefix + key)
                hashedMap[hash] = key
            }
            return hashedMap
        }
    }
}
