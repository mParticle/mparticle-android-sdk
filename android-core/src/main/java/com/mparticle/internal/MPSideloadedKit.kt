package com.mparticle.internal

import android.util.SparseBooleanArray
import com.mparticle.MParticle.EventType
import org.json.JSONObject

class MPSideloadedKit private constructor(builder: Builder) : SideloadedKit {

    private val filters: MutableMap<String, String> = builder.filters
    val kit: SideloadedKit = builder.kit

    fun getFilterType(type: String): JSONObject? {
        //TODO()
        return null
    }

     class Builder(internal val kit: SideloadedKit) {
        internal var filters: MutableMap<String, String> = mutableMapOf()
            private set

        fun filters(filters: MPSideloadedFilterBuilder): Builder {
            this.filters = filters.filters
            return this
        }

        private fun createKitJSONConfiguration(): JSONObject {
            return JSONObject()
            //
        }

        fun build(): MPSideloadedKit {
            kit.setJSONConfiguration(createKitJSONConfiguration())
            return MPSideloadedKit(this)
        }

    }

    override fun setJSONConfiguration(configuration: JSONObject) {
        kit.setJSONConfiguration(configuration)
    }

}

class MPSideloadedFilterBuilder() {

    var filters: MutableMap<String, String> = mutableMapOf()
        private set

    // Each of these methods hashes these values using the correct algorithm as described
    // in the previous section and adds them to a private dictionary
    fun addEventTypeFilter(eventType: EventType): MPSideloadedFilterBuilder {
        return this
    }

    fun addEventNameFilter(eventType: EventType, eventName: String): MPSideloadedFilterBuilder {
        return this
    }

    fun addScreenNameFilter(screenName: String): MPSideloadedFilterBuilder {
        return this
    }

    fun addEventAttributeFilter(
        eventType: EventType,
        eventName: String,
        customAttributeKey: String
    ): MPSideloadedFilterBuilder {
        return this
    }

    fun addScreenAttributeFilter(
        screenName: String,
        customAttributeKey: String
    ): MPSideloadedFilterBuilder {
        return this
    }

    fun addUserIdentityFilter(userIdentity: SparseBooleanArray): MPSideloadedFilterBuilder {
        return this
    }

    fun addUserAttributeFilter(userAttributeKey: String): MPSideloadedFilterBuilder {
        return this
    }

    fun addCommerceEventAttributeFilter(
        eventType: EventType,
        eventAttributeKey: String
    ): MPSideloadedFilterBuilder {
        return this
    }

    fun addCommerceEventEntityTypeFilter(commerceEventKind: SparseBooleanArray): MPSideloadedFilterBuilder {
        return this
    }

    fun addCommerceEventAppFamilyAttributeFilter(attributeKey: String): MPSideloadedFilterBuilder {
        return this
    }

    // Special filter case that can only have 1 at a time unlike the others
    // If `forward` is true, ONLY matching events are forwarded, if false, any matching events are blocked
    // NOTE: This is iOS/Android only, web has a different signature
    fun setEventAttributeConditionalForwarding(
        attributeName: String,
        attributeValue: String,
        onlyForward: Boolean
    ): MPSideloadedFilterBuilder {
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
    ): MPSideloadedFilterBuilder {
        return this
    }

    // iOS only, this accepts a string because the constants are strings, but in the inline docs comments and main docs, we will refer people to the constants
    fun addMessageTypeFilter(messageTypeConstant: String): MPSideloadedFilterBuilder {
        return this
    }
}
