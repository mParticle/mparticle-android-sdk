package com.mparticle.internal

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.internal.messages.BaseMPMessage

object HashingUtility {

    fun hashEvent(event: MPEvent): Int =
        MPUtility.mpHash("${event.eventType.ordinal}${event.eventName}")

    fun hashMpMessageTypeName(mpMessage: BaseMPMessage): Int =
        MPUtility.mpHash("${mpMessage.messageType}${mpMessage.name}")

    fun hashFilterAvKey(key: String): Int = MPUtility.mpHash(key)

    fun hashFilterAvValue(value: String): Int = MPUtility.mpHash(value)

    fun hashGDPRContentPurposeKey(consentKey: String) = MPUtility.mpHash("1$consentKey")

    fun hashCCPAContentPurposeKey() = MPUtility.mpHash("2${Constants.MessageKey.CCPA_CONSENT_KEY}")

    fun hashFilterTypeCommerceEvent(eventType: Int) = MPUtility.mpHash("$eventType")

    fun hashFilterCommerceEntityAttributeKey(key: String) = MPUtility.mpHash(key)

    fun hashFilterEventAttributes(
        eventType: MParticle.EventType?,
        eventName: String,
        key: String
    ): Int {
        var eventTypeStr = eventType?.let { it.ordinal.toString() } ?: run { "0" }
        return MPUtility.mpHash(eventTypeStr + eventName + key)
    }

    fun hashFilterCommerceEventAttribute(eventType: Int, customAttributeKey: String) =
        MPUtility.mpHash("$eventType$customAttributeKey")

    fun hashFilterScreenName(screenName: String) = MPUtility.mpHash("0$screenName")

    fun hashEventType(mpEvent: MPEvent) = MPUtility.mpHash("${mpEvent.eventType.ordinal}")

    fun hashPromotionField(eventType: Int, promotionKey: String) =
        MPUtility.mpHash("$eventType$promotionKey")

    fun hashProductField(eventType: Int, productKey: String) =
        MPUtility.mpHash("$eventType$productKey")


}