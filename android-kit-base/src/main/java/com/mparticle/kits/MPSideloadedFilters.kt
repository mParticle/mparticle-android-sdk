package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.MParticle.EventType
import com.mparticle.internal.HashingUtility
import com.mparticle.kits.KitConfiguration.KEY_ATTRIBUTE_VALUE_FILTERING
import com.mparticle.kits.KitConfiguration.KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE
import com.mparticle.kits.KitConfiguration.KEY_COMMERCE_ATTRIBUTE_FILTER
import com.mparticle.kits.KitConfiguration.KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS
import com.mparticle.kits.KitConfiguration.KEY_COMMERCE_ENTITY_FILTERS
import com.mparticle.kits.KitConfiguration.KEY_CONSENT_FORWARDING_RULES_ARRAY
import com.mparticle.kits.KitConfiguration.KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES
import com.mparticle.kits.KitConfiguration.KEY_EVENT_ATTRIBUTES_FILTER
import com.mparticle.kits.KitConfiguration.KEY_EVENT_NAMES_FILTER
import com.mparticle.kits.KitConfiguration.KEY_EVENT_TYPES_FILTER
import com.mparticle.kits.KitConfiguration.KEY_FILTERS
import com.mparticle.kits.KitConfiguration.KEY_SCREEN_ATTRIBUTES_FILTER
import com.mparticle.kits.KitConfiguration.KEY_SCREEN_NAME_FILTER
import com.mparticle.kits.KitConfiguration.KEY_USER_ATTRIBUTE_FILTER
import com.mparticle.kits.KitConfiguration.KEY_USER_IDENTITY_FILTER
import org.json.JSONObject

class MPSideloadedFilters {

    companion object {
        private const val EXCLUDING_FILTER_VALUE = 0
    }

    var filters: MutableMap<String, JSONObject> = mutableMapOf()
        private set

    /**
     * Add event type filter
     *
     * @param eventType
     * @return MPSideloadedFilters instance
     */
    //Internally use et as the key
    fun addEventTypeFilter(eventType: MParticle.EventType): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_EVENT_TYPES_FILTER, Pair(
                HashingUtility.hashFilterTypeCommerceEvent(eventType).toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add event name filter
     *
     * @param eventType
     * @param eventName
     * @return MPSideloadedFilters instance
     */
    //Internally use ec as the key
    fun addEventNameFilter(eventType: MParticle.EventType, eventName: String): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_EVENT_NAMES_FILTER, Pair(
                HashingUtility.hashEvent(eventType, eventName).toString(), EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add screen name filter
     *
     * @param screenName
     * @return MPSideloadedFilters instance
     */
    fun addScreenNameFilter(screenName: String): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_SCREEN_NAME_FILTER, Pair(
                HashingUtility.hashFilterScreenName(screenName).toString(), EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add event attribute filter
     *
     * @param eventType
     * @param eventName
     * @param customAttributeKey
     * @return MPSideloadedFilters instance
     */
    //Internally use ea as the key
    fun addEventAttributeFilter(
        eventType: MParticle.EventType,
        eventName: String,
        customAttributeKey: String
    ): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_EVENT_ATTRIBUTES_FILTER, Pair(
                HashingUtility.hashFilterEventAttributes(eventType, eventName, customAttributeKey)
                    .toString(), EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add Screen attribute filter
     *
     * @param screenName
     * @param customAttributeKey
     * @return MPSideloadedFilters instance
     */
    //Internally use svea as the key
    fun addScreenAttributeFilter(
        screenName: String,
        customAttributeKey: String
    ): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_SCREEN_ATTRIBUTES_FILTER, Pair(
                HashingUtility.hashFilterScreenName("$screenName$customAttributeKey").toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add user identity filter
     *
     * @param userIdentityType
     * @return MPSideloadedFilters instance
     */
    //Internally use uid as the key
    fun addUserIdentityFilter(userIdentityType: MParticle.IdentityType): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_USER_IDENTITY_FILTER, Pair(
                userIdentityType.value.toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add user attribute filter
     *
     * @param userAttributeKey
     * @return MPSideloadedFilters instance
     */
    //Internally use ua as the key
    fun addUserAttributeFilter(userAttributeKey: String): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_USER_ATTRIBUTE_FILTER, Pair(
                HashingUtility.hashUserAttributes(userAttributeKey).toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add commerce event attribute filter
     *
     * @param eventType
     * @param eventAttributeKey
     * @return MPSideloadedFilters
     */
    //Internally use cea as the key
    fun addCommerceEventAttributeFilter(
        eventType: EventType,
        eventAttributeKey: String
    ): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_COMMERCE_ATTRIBUTE_FILTER, Pair(
                HashingUtility.hashFilterCommerceEventAttribute(
                    eventType.ordinal,
                    eventAttributeKey
                ).toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    /**
     * Add commerce event entity type filter
     *
     * @param commerceEventType mapped to a product action ordinal for [CommerceEventKind.PRODUCT],
     * [CommerceEventKind.PROMOTION] or [CommerceEventKind.IMPRESSION]
     * Adding type [CommerceEventKind.UNKNOWN] would prevent adding the filter
     * @return MPSideloadedFilters instance
     */
    //Internally use ent as the key
    fun addCommerceEventEntityTypeFilter(commerceEventType: CommerceEventKind): MPSideloadedFilters {
        if (commerceEventType != CommerceEventKind.UNKNOWN) {
            applyToFiltersNode(
                KEY_COMMERCE_ENTITY_FILTERS, Pair(
                    HashingUtility.hashFilterCommerceEntityAttributeKey(commerceEventType.eventType.toString())
                        .toString(),
                    EXCLUDING_FILTER_VALUE
                )
            )
        }
        return this
    }

    /**
     * Add commerce event app famility attribute filter
     *
     * @param attributeKey
     * @return MPSideloadedFilters instance
     */
    //Internally use afa as the key
    fun addCommerceEventAppFamilyAttributeFilter(attributeKey: String): MPSideloadedFilters {
        applyToFiltersNode(
            KEY_COMMERCE_ENTITY_ATTRIBUTE_FILTERS, Pair(
                HashingUtility.hashFilterCommerceEntityAttributeKey(attributeKey).toString(),
                EXCLUDING_FILTER_VALUE
            )
        )
        return this
    }

    // Special filter case that can only have 1 at a time unlike the others
// If `forward` is true, ONLY matching events are forwarded, if false, any matching events are blocked
// NOTE: This is iOS/Android only, web has a different signature
    /**
     * Use this function to add event attribute conditional forwarding.
     * @param attributeName consent attribute name
     * @param attributeValue consent attribute value
     * @param onlyForward
     *
     * @return MPSideloadedFilters instance
     */
    //Internally use avf as the key
    fun addEventAttributeConditionalForwardingFilter(
        attributeName: String,
        attributeValue: String,
        onlyForward: Boolean
    ): MPSideloadedFilters {
        val obj = JSONObject().apply {
            this.put(KEY_CONSENT_FORWARDING_RULES_SHOULD_INCLUDE_MATCHES, onlyForward)
            this.put(
                KEY_ATTRIBUTE_VALUE_FILTERING_ATTRIBUTE,
                HashingUtility.hashGDPRContentPurposeKey(attributeName)
            )
            this.put(
                KEY_CONSENT_FORWARDING_RULES_ARRAY,
                HashingUtility.hashGDPRContentPurposeKey(attributeValue)
            )
        }
        filters.put(KEY_ATTRIBUTE_VALUE_FILTERING, obj)
        return this
    }

    private fun applyToFiltersNode(key: String, vararg pairs: Pair<String, Any>) {
        var node = filters.get(key)
        if (node == null) {
            node = JSONObject()
        }
        pairs.forEach { pair ->
            node.put(pair.first, pair.second)
        }
        filters.put(key, node)
    }

    internal fun toJSONObject(): JSONObject {
        val result = JSONObject()

        val filtersJSONObject = JSONObject()
        filters.forEach {
            if (it.key != KEY_ATTRIBUTE_VALUE_FILTERING) {
                filtersJSONObject.put(it.key, it.value)
            }
        }
        result.put(KEY_FILTERS, filtersJSONObject)

        filters.get(KEY_ATTRIBUTE_VALUE_FILTERING)
            ?.let { result.put(KEY_ATTRIBUTE_VALUE_FILTERING, it) }
        return result
    }

    enum class CommerceEventKind(internal val eventType: Int) {
        PRODUCT(1), PROMOTION(2), IMPRESSION(3), UNKNOWN(0);
    }

}
