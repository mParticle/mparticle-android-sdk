package com.mparticle.kits

import android.util.SparseBooleanArray
import com.mparticle.MParticle

class MPSideloadedFilters {

    var filters: MutableMap<String, String> = mutableMapOf()
        private set

    // Each of these methods hashes these values using the correct algorithm as described
    // in the previous section and adds them to a private dictionary
    fun addEventTypeFilter(eventType: MParticle.EventType): MPSideloadedFilters {
        return this
    }

    fun addEventNameFilter(eventType: MParticle.EventType, eventName: String): MPSideloadedFilters {
        return this
    }

    fun addScreenNameFilter(screenName: String): MPSideloadedFilters {
        return this
    }

    fun addEventAttributeFilter(
        eventType: MParticle.EventType,
        eventName: String,
        customAttributeKey: String
    ): MPSideloadedFilters {
        return this
    }

    fun addScreenAttributeFilter(
        screenName: String,
        customAttributeKey: String
    ): MPSideloadedFilters {
        return this
    }

    fun addUserIdentityFilter(userIdentity: SparseBooleanArray): MPSideloadedFilters {
        return this
    }

    fun addUserAttributeFilter(userAttributeKey: String): MPSideloadedFilters {
        return this
    }

    fun addCommerceEventAttributeFilter(
        eventType: MParticle.EventType,
        eventAttributeKey: String
    ): MPSideloadedFilters {
        return this
    }

    fun addCommerceEventEntityTypeFilter(commerceEventKind: SparseBooleanArray): MPSideloadedFilters {
        return this
    }

    fun addCommerceEventAppFamilyAttributeFilter(attributeKey: String): MPSideloadedFilters {
        return this
    }

    // Special filter case that can only have 1 at a time unlike the others
    // If `forward` is true, ONLY matching events are forwarded, if false, any matching events are blocked
    // NOTE: This is iOS/Android only, web has a different signature
    fun setEventAttributeConditionalForwarding(
        attributeName: String,
        attributeValue: String,
        onlyForward: Boolean
    ): MPSideloadedFilters {
        return this
    }

    // NOTE: This is Web only
    // If isEvent is true, this is an event attribute conditional filter, if false, it's a user attribute conditional filter,
    // there can only be one or the other and that latter is not supported at all on iOS/Android.
    fun setAttributeConditionalForwarding(
        isEvent: Boolean,
        attributeName: String,
        attributeValue: String,
        onlyForward: Boolean
    ): MPSideloadedFilters {
        return this
    }

    // iOS only, this accepts a string because the constants are strings, but in the inline docs comments and main docs, we will refer people to the constants
    fun addMessageTypeFilter(messageTypeConstant: String): MPSideloadedFilters {
        return this
    }

}