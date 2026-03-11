package com.mparticle.kits

import android.content.Context
import android.content.Intent
import com.mparticle.MPEvent
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.kits.KitIntegration.CommerceListener
import com.urbanairship.Airship
import com.urbanairship.Autopilot
import com.urbanairship.PrivacyManager
import com.urbanairship.analytics.CustomEvent
import com.urbanairship.analytics.InstallReceiver
import com.urbanairship.analytics.customEvent
import com.urbanairship.analytics.templates.RetailEventTemplate
import com.urbanairship.json.JsonValue
import com.urbanairship.push.PushMessage
import com.urbanairship.push.PushProviderBridge
import java.math.BigDecimal
import java.util.LinkedList

/**
 * mParticle-Urban Airship Kit integration
 */
class UrbanAirshipKit :
    KitIntegration(),
    KitIntegration.PushListener,
    KitIntegration.EventListener,
    CommerceListener,
    KitIntegration.AttributeListener {
    private var channelIdListener: ChannelIdListener? = null
    private var configuration: UrbanAirshipConfiguration? = null

    interface ChannelIdListener {
        fun channelIdUpdated()
    }

    override fun getName(): String = KIT_NAME

    override fun getInstance(): ChannelIdListener? = channelIdListener

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        setUrbanConfiguration(UrbanAirshipConfiguration(settings))
        channelIdListener =
            object : ChannelIdListener {
                override fun channelIdUpdated() {
                    updateChannelIntegration()
                }
            }
        configuration?.let { MParticleAutopilot.updateConfig(context, it) }
        Autopilot.automaticTakeOff(context)
        updateChannelIntegration()
        return emptyList()
    }

    fun setUrbanConfiguration(configuration: UrbanAirshipConfiguration?) {
        this.configuration = configuration
    }

    override fun onSettingsUpdated(settings: Map<String, String>) {
        setUrbanConfiguration(UrbanAirshipConfiguration(settings))
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        Airship.privacyManager.setEnabledFeatures(
            if (optedOut) PrivacyManager.Feature.NONE else PrivacyManager.Feature.ALL,
        )
        val message =
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            )
        return listOf(message)
    }

    override fun setInstallReferrer(intent: Intent) {
        InstallReceiver().onReceive(Airship.application.applicationContext, intent)
    }

    override fun willHandlePushMessage(intent: Intent?): Boolean {
        return intent?.let { intent ->
            intent.extras?.let { extras ->
                PushMessage(extras).containsAirshipKeys()
            }
        } ?: return false
    }

    override fun onPushMessageReceived(
        context: Context,
        intent: Intent?,
    ) {
        intent?.extras?.let {
            val pushMessage = PushMessage(it)
            PushProviderBridge
                .processPush(MParticlePushProvider::class.java, pushMessage)
                .executeSync(context)
        } ?: return
    }

    override fun onPushRegistration(
        instanceId: String,
        senderId: String,
    ): Boolean {
        MParticlePushProvider.instance.setRegistrationToken(instanceId)
        PushProviderBridge.requestRegistrationUpdate(
            context,
            MParticlePushProvider.instance::class.java,
            instanceId,
        )
        return true
    }

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

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        val tagSet = extractTags(event)
        if (tagSet.isNotEmpty()) {
            Airship.channel
                .editTags()
                .addTags(tagSet)
                .apply()
        }
        logUrbanAirshipEvent(event)
        return listOf(ReportingMessage.fromEvent(this, event))
    }

    override fun logScreen(
        screenName: String,
        attributes: Map<String, String>,
    ): List<ReportingMessage> {
        val tagSet = extractScreenTags(screenName, attributes)
        if (tagSet.isNotEmpty()) {
            Airship.channel
                .editTags()
                .addTags(tagSet)
                .apply()
        }
        Airship.analytics.trackScreen(screenName)
        val message =
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                attributes,
            )
        return listOf(message)
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        totalValue: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> {
        val customEvent =
            CustomEvent
                .Builder(eventName)
                .setEventValue(valueIncreased)
                .build()
        Airship.analytics.recordCustomEvent(customEvent)
        val message =
            ReportingMessage(
                this,
                ReportingMessage.MessageType.EVENT,
                System.currentTimeMillis(),
                contextInfo,
            )
        return listOf(message)
    }

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        val tagSet = extractCommerceTags(commerceEvent)
        if (tagSet.isNotEmpty()) {
            Airship.channel
                .editTags()
                .addTags(tagSet)
                .apply()
        }
        val messages: MutableList<ReportingMessage> = LinkedList()
        if (logAirshipRetailEvents(commerceEvent)) {
            messages.add(ReportingMessage.fromEvent(this, commerceEvent))
        } else {
            for (event in CommerceEventUtils.expand(commerceEvent)) {
                logUrbanAirshipEvent(event)
                messages.add(ReportingMessage.fromEvent(this, event))
            }
        }
        return messages
    }

    override fun setUserIdentity(
        identityType: IdentityType,
        identity: String,
    ) {
        val airshipId = getAirshipIdentifier(identityType)
        if (airshipId != null) {
            Airship.analytics
                .editAssociatedIdentifiers()
                .addIdentifier(airshipId, identity)
                .apply()
        }
        if (identityType == configuration?.userIdField) {
            Airship.contact.identify(identity) // Previously setting namedUser but now is immutable
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        val airshipId = getAirshipIdentifier(identityType)
        if (airshipId != null) {
            Airship.analytics
                .editAssociatedIdentifiers()
                .removeIdentifier(airshipId)
                .apply()
        }
        if (identityType == configuration?.userIdField && Airship.contact.namedUserId != null) {
            Airship.contact.reset() // Previously setting namedUser to null but now is immutable
        }
    }

    override fun setUserAttribute(
        key: String,
        value: String,
    ) {
        if (configuration?.enableTags == true) {
            if (KitUtils.isEmpty(value)) {
                Airship.channel
                    .editTags()
                    .addTag(KitUtils.sanitizeAttributeKey(key))
                    .apply()
            } else if (configuration?.includeUserAttributes == true) {
                Airship.channel
                    .editTags()
                    .addTag(KitUtils.sanitizeAttributeKey(key) + "-" + value)
                    .apply()
            }
        }
    }

    override fun setUserAttributeList(
        s: String,
        list: List<String>,
    ) {
        // not supported
    }

    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        stringAttributes: Map<String, String>,
        listAttributes: Map<String, List<String>>,
    ) {
        if (configuration?.enableTags == true) {
            val editor =
                Airship.channel
                    .editTags()
            for ((key, value) in stringAttributes) {
                if (KitUtils.isEmpty(value)) {
                    editor.addTag(KitUtils.sanitizeAttributeKey(key))
                } else if (configuration?.includeUserAttributes == true) {
                    editor.addTag(KitUtils.sanitizeAttributeKey(key) + "-" + value)
                }
            }
            editor.apply()
        }
    }

    override fun removeUserAttribute(attribute: String) {
        Airship.channel
            .editTags()
            .removeTag(attribute)
            .apply()
    }

    // not supported
    override fun logout(): List<ReportingMessage> = emptyList()

    /**
     * Logs Urban Airship RetailEvents from a CommerceEvent.
     *
     * @param event The commerce event.
     * @return `true` if retail events were able to be generated from the CommerceEvent,
     * otherwise `false`.
     */
    private fun logAirshipRetailEvents(event: CommerceEvent): Boolean {
        if (event.productAction == null || event.products?.isEmpty() == true) {
            return false
        }
        event.products?.let { eventProducts ->
            for (product in eventProducts) {
                val templateType =
                    when (event.productAction) {
                        Product.PURCHASE -> RetailEventTemplate.Type.Purchased
                        Product.ADD_TO_CART -> RetailEventTemplate.Type.AddedToCart
                        Product.CLICK -> RetailEventTemplate.Type.Browsed
                        Product.ADD_TO_WISHLIST -> RetailEventTemplate.Type.Starred
                        else -> return false
                    }
                customEvent(
                    templateType,
                    populateRetailEventTemplate(product),
                ) {
                    setEventValue(product.totalAmount)
                    setTransactionId(event.transactionAttributes?.id)
                }.track()
            }
        }
        return true
    }

    /**
     * Populates an Urban Airship RetailEventTemplate from a product.
     *
     * @param template The retail event template.
     * @param product The product.
     * @return The populated retail event template.
     */
    private fun populateRetailEventTemplate(product: Product): RetailEventTemplate.Properties =
        RetailEventTemplate.Properties(
            id = product.sku,
            category = product.category,
            eventDescription = product.name,
            brand = product.brand,
        )

    /**
     * Logs an Urban Airship CustomEvent from an MPEvent.
     *
     * @param event The MPEvent.
     */
    private fun logUrbanAirshipEvent(event: MPEvent) {
        val eventBuilder = CustomEvent.Builder(event.eventName)
        if (event.customAttributeStrings != null) {
            eventBuilder.setProperties(JsonValue.wrapOpt(event.customAttributeStrings).optMap())
        }
        Airship.analytics.recordCustomEvent(eventBuilder.build())
    }

    fun extractTags(event: MPEvent): Set<String> {
        val tags: MutableSet<String> = HashSet()
        configuration?.let { configuration ->
            configuration.eventClass
            if (configuration.eventClass.containsKey(event.eventHash)) {
                configuration.eventClass[event.eventHash]?.let { eventHashIt ->
                    tags.addAll(
                        eventHashIt,
                    )
                }
            }

            event.customAttributeStrings?.let {
                for ((key, value) in it) {
                    val hash =
                        KitUtils.hashForFiltering(
                            event.eventType.ordinal.toString() +
                                event.eventName +
                                key,
                        )
                    val tagValues: ArrayList<String>? = configuration.eventAttributeClass[hash]
                    if (tagValues != null) {
                        tags.addAll(tagValues)
                        if (!KitUtils.isEmpty(value)) {
                            for (tagValue in tagValues) {
                                tags.add("$tagValue-$value")
                            }
                        }
                    }
                }
            }
        }
        return tags
    }

    fun extractCommerceTags(commerceEvent: CommerceEvent?): Set<String> {
        val tags: MutableSet<String> = HashSet()
        val commerceEventHash =
            KitUtils.hashForFiltering(
                CommerceEventUtils.getEventType(commerceEvent).toString() + "",
            )
        configuration?.let { configuration ->
            if (configuration.eventClassDetails.containsKey(
                    commerceEventHash,
                )
            ) {
                configuration.eventClassDetails[commerceEventHash]?.let { tags.addAll(it) }
            }
            val expandedEvents = CommerceEventUtils.expand(commerceEvent)
            for (event in expandedEvents) {
                event.customAttributeStrings?.let {
                    for ((key, value) in it) {
                        val hash =
                            KitUtils.hashForFiltering(
                                CommerceEventUtils.getEventType(commerceEvent).toString() +
                                    key,
                            )
                        val tagValues: List<String>? =
                            configuration.eventAttributeClassDetails[hash]
                        if (tagValues != null) {
                            tags.addAll(tagValues)
                            if (!KitUtils.isEmpty(value)) {
                                for (tagValue in tagValues) {
                                    tags.add("$tagValue-$value")
                                }
                            }
                        }
                    }
                }
            }
        }
        return tags
    }

    fun extractScreenTags(
        screenName: String,
        attributes: Map<String, String>?,
    ): Set<String> {
        val tags: MutableSet<String> = HashSet()
        val screenEventHash = KitUtils.hashForFiltering("0$screenName")
        configuration?.let { configuration ->
            if (configuration.eventClassDetails.containsKey(
                    screenEventHash,
                )
            ) {
                configuration.eventClassDetails[screenEventHash]?.let { tags.addAll(it) }
            }
            if (attributes != null) {
                for ((key, value) in attributes) {
                    val hash =
                        KitUtils.hashForFiltering(
                            "0" +
                                screenName +
                                key,
                        )
                    val tagValues = configuration.eventAttributeClassDetails[hash]

                    if (tagValues != null) {
                        tags.addAll(tagValues)
                        if (!KitUtils.isEmpty(value)) {
                            for (tagValue in tagValues) {
                                tags.add("$tagValue-$value")
                            }
                        }
                    }
                }
            }
        }
        return tags
    }

    /**
     * Maps MParticle.IdentityType to an Urban Airship device identifier.
     *
     * @param identityType The mParticle identity type.
     * @return The Urban Airship identifier, or `null` if one does not exist.
     */
    private fun getAirshipIdentifier(identityType: IdentityType): String? =
        when (identityType) {
            IdentityType.CustomerId -> IDENTITY_CUSTOMER_ID
            IdentityType.Facebook -> IDENTITY_FACEBOOK
            IdentityType.Twitter -> IDENTITY_TWITTER
            IdentityType.Google -> IDENTITY_GOOGLE
            IdentityType.Microsoft -> IDENTITY_MICROSOFT
            IdentityType.Yahoo -> IDENTITY_YAHOO
            IdentityType.Email -> IDENTITY_EMAIL
            IdentityType.FacebookCustomAudienceId -> IDENTITY_FACEBOOK_CUSTOM_AUDIENCE_ID
            else -> {
                null
            }
        }

    /**
     * Sets the Urban Airship Channel ID as an mParticle integration attribute.
     */
    private fun updateChannelIntegration() {
        val channelId = Airship.channel.id
        if (!KitUtils.isEmpty(channelId)) {
            val integrationAttributes = HashMap<String, String?>(1)
            integrationAttributes[CHANNEL_ID_INTEGRATION_KEY] = channelId
            setIntegrationAttributes(integrationAttributes)
        }
    }

    companion object {
        // Identities
        private const val IDENTITY_EMAIL = "email"
        private const val IDENTITY_FACEBOOK = "facebook_id"
        private const val IDENTITY_TWITTER = "twitter_id"
        private const val IDENTITY_GOOGLE = "google_id"
        private const val IDENTITY_MICROSOFT = "microsoft_id"
        private const val IDENTITY_YAHOO = "yahoo_id"
        private const val IDENTITY_FACEBOOK_CUSTOM_AUDIENCE_ID = "facebook_custom_audience_id"
        private const val IDENTITY_CUSTOMER_ID = "customer_id"
        private const val KIT_NAME = "Urban Airship"
        const val CHANNEL_ID_INTEGRATION_KEY = "com.urbanairship.channel_id"
    }
}
