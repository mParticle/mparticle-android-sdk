package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import com.localytics.androidx.CallToActionListener
import com.localytics.androidx.Campaign
import com.localytics.androidx.FirebaseService
import com.localytics.androidx.Localytics
import com.localytics.androidx.ReferralReceiver
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.EventListener
import com.mparticle.kits.KitIntegration.LogoutListener
import com.mparticle.kits.KitIntegration.PushListener
import com.mparticle.kits.ReportingMessage
import org.json.JSONArray
import java.math.BigDecimal
import java.util.HashMap
import java.util.LinkedList

class LocalyticsKit :
    KitIntegration(),
    KitIntegration.EventListener,
    CommerceListener,
    AttributeListener,
    LogoutListener,
    PushListener,
    CallToActionListener {
    private var customDimensionJson: JSONArray? = null
    private var trackAsRawLtv = false

    override fun getName(): String = KIT_NAME

    public override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        try {
            customDimensionJson = JSONArray(settings[CUSTOM_DIMENSIONS])
        } catch (jse: Exception) {
        }
        trackAsRawLtv = java.lang.Boolean.parseBoolean(settings[RAW_LTV])
        Localytics.setOption("ll_app_key", settings[API_KEY])
        Localytics.autoIntegrate((context.applicationContext as Application))

        // after a reset() call, we need to set Provivacy OptedOut back to false, so the kit can run normally
        Localytics.setCallToActionListener(this)
        Localytics.setLoggingEnabled(MParticle.getInstance()!!.environment == MParticle.Environment.Development)
        return emptyList()
    }

    override fun setLocation(location: Location) {
        Localytics.setLocation(location)
    }

    override fun setInstallReferrer(intent: Intent) {
        ReferralReceiver().onReceive(context, intent)
    }

    override fun setUserIdentity(
        identityType: IdentityType,
        id: String,
    ) {
        when (identityType) {
            IdentityType.Email -> {
                Localytics.setCustomerEmail(id)
            }
            IdentityType.CustomerId -> {
                Localytics.setCustomerId(id)
            }
            else -> {
                Localytics.setIdentifier(identityType.name, id)
            }
        }
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        when (identityType) {
            IdentityType.Email -> {
                Localytics.setCustomerEmail("")
            }
            IdentityType.CustomerId -> {
                Localytics.setCustomerId("")
            }
            else -> {
                Localytics.setIdentifier(identityType.name, "")
            }
        }
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    fun getDimensionIndexForAttribute(key: String?): Int {
        if (customDimensionJson == null) {
            try {
                customDimensionJson = JSONArray(settings[CUSTOM_DIMENSIONS])
            } catch (jse: Exception) {
            }
        }
        customDimensionJson?.let { customDimensionJson ->
            try {
                for (i in 0 until customDimensionJson.length()) {
                    val dimension = customDimensionJson.getJSONObject(i)
                    if (dimension.getString("maptype") == "UserAttributeClass.Name") {
                        val attributeName = dimension.getString("map")
                        if (key.equals(attributeName, true)) {
                            return dimension
                                .getString("value")
                                .substring("Dimension ".length)
                                .toInt()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.debug("Exception while mapping mParticle user attributes to Localytics custom dimensions: $e")
            }
        }
        return -1
    }

    override fun setUserAttribute(
        key: String,
        value: String,
    ) {
        val dimensionIndex = getDimensionIndexForAttribute(key)
        if (dimensionIndex >= 0) {
            Localytics.setCustomDimension(dimensionIndex, value)
        }
        when {
            key.equals(MParticle.UserAttributes.FIRSTNAME, true) -> {
                Localytics.setCustomerFirstName(value)
            }
            key.equals(MParticle.UserAttributes.LASTNAME, true) -> {
                Localytics.setCustomerLastName(value)
            }
            else -> {
                Localytics.setProfileAttribute(KitUtils.sanitizeAttributeKey(key), value)
            }
        }
    }

    override fun setUserAttributeList(
        key: String,
        list: List<String>,
    ) {
        val array = list.toTypedArray()
        Localytics.setProfileAttribute(key, array)
    }

    override fun supportsAttributeLists(): Boolean = true

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
    ) {
        for ((key, value) in attributes) {
            setUserAttribute(key, value)
        }
        for ((key, value) in attributeLists) {
            setUserAttributeList(key, value)
        }
    }

    override fun removeUserAttribute(key: String) {
        Localytics.deleteProfileAttribute(key)
    }

    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> {
        Localytics.setOptedOut(optOutStatus)
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messageList
    }

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>,
    ): List<ReportingMessage> = emptyList()

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String,
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        val duration = event.length
        var info = event.customAttributeStrings
        if (duration != null) {
            if (info == null) {
                info = HashMap()
            }
            info["event_duration"] = duration.toString()
        }
        Localytics.tagEvent(event.eventName, info)
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(ReportingMessage.fromEvent(this, event))
        return messageList
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>,
    ): List<ReportingMessage> {
        Localytics.tagScreen(screenName)
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                eventAttributes,
            ).setScreenName(screenName),
        )
        return messageList
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        totalValue: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>,
    ): List<ReportingMessage> {
        val multiplier = if (trackAsRawLtv) 1 else 100
        Localytics.tagEvent(eventName, contextInfo, valueIncreased.toDouble().toLong() * multiplier)
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage.fromEvent(
                this,
                MPEvent
                    .Builder(eventName, MParticle.EventType.Transaction)
                    .customAttributes(contextInfo)
                    .build(),
            ),
        )
        return messageList
    }

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()
        if (!KitUtils.isEmpty(event.productAction) &&
            event.productAction.equals(
                Product.PURCHASE,
                true,
            ) ||
            event.productAction.equals(Product.REFUND, true)
        ) {
            val eventAttributes: Map<String, String> = HashMap()
            CommerceEventUtils.extractActionAttributes(event, eventAttributes)
            var multiplier = if (trackAsRawLtv) 1 else 100
            if (event.productAction.equals(Product.REFUND, true)) {
                multiplier *= -1
            }
            val total = event.transactionAttributes?.revenue?.times(multiplier)
            if (total != null) {
                Localytics.tagEvent(
                    String.format("eCommerce - %s", event.productAction),
                    eventAttributes,
                    total.toLong(),
                )
            }
            messages.add(ReportingMessage.fromEvent(this, event))
            return messages
        }
        val eventList = CommerceEventUtils.expand(event)
        if (eventList != null) {
            for (i in eventList.indices) {
                try {
                    logEvent(eventList[i])
                    messages.add(ReportingMessage.fromEvent(this, event))
                } catch (e: Exception) {
                    Logger.warning("Failed to call tagEvent to Localytics kit: $e")
                }
            }
        }
        return messages
    }

    override fun willHandlePushMessage(intent: Intent): Boolean =
        (
            intent.extras?.containsKey("ll") == true ||
                intent.extras?.containsKey("localyticsUninstallTrackingPush") == true
            ) &&
            MPUtility.isFirebaseAvailable()

    override fun onPushMessageReceived(
        context: Context,
        extras: Intent,
    ) {
        val service: Intent
        if (MPUtility.isFirebaseAvailable()) {
            service = Intent(context, FirebaseService::class.java)
            service.action = "com.google.firebase.MESSAGING_EVENT"
            service.putExtras(extras)
            context.startService(service)
        }
    }

    override fun onPushRegistration(
        token: String,
        senderId: String,
    ): Boolean {
        Localytics.setPushRegistrationId(token)
        return true
    }

    public override fun reset() {
        Localytics.setPrivacyOptedOut(true)
    }

    override fun localyticsShouldDeeplink(
        s: String,
        campaign: Campaign,
    ): Boolean = false

    override fun localyticsDidOptOut(
        b: Boolean,
        campaign: Campaign,
    ) {}

    override fun localyticsDidPrivacyOptOut(
        optedOut: Boolean,
        campaign: Campaign,
    ) {
        if (optedOut && !kitManager.isOptedOut) {
            Localytics.setPrivacyOptedOut(false)
        }
    }

    override fun localyticsShouldPromptForLocationPermissions(campaign: Campaign): Boolean = false

    override fun localyticsShouldPromptForNotificationPermissions(campaign: Campaign): Boolean = false

    companion object {
        const val API_KEY = "appKey"
        const val CUSTOM_DIMENSIONS = "customDimensions"
        private const val RAW_LTV = "trackClvAsRawValue"
        private const val KIT_NAME = "Localytics"
    }
}
