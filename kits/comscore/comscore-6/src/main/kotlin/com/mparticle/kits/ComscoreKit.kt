package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.comscore.Analytics
import com.comscore.PartnerConfiguration
import com.comscore.PublisherConfiguration
import com.comscore.util.log.LogLevel
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.kits.KitIntegration.ActivityListener
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.LogoutListener
import java.util.HashMap
import java.util.LinkedList

class ComscoreKit :
    KitIntegration(),
    KitIntegration.EventListener,
    AttributeListener,
    LogoutListener,
    ActivityListener {
    private var isEnterprise = false

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

    override fun logEvent(event: MPEvent): List<ReportingMessage>? {
        if (!isEnterprise) {
            return null
        }
        val messages = LinkedList<ReportingMessage>()
        val comscoreLabels: HashMap<String, String?>
        when (val attributes = event.customAttributeStrings) {
            null -> {
                comscoreLabels = HashMap()
            }
            !is HashMap<String, String?> -> {
                comscoreLabels = HashMap()
                for ((key, value) in attributes) {
                    comscoreLabels[key] = value
                }
            }
            else -> {
                comscoreLabels = attributes
            }
        }
        comscoreLabels[COMSCORE_DEFAULT_LABEL_KEY] = event.eventName
        if (MParticle.EventType.Navigation == event.eventType) {
            Analytics.notifyViewEvent(comscoreLabels)
        } else {
            Analytics.notifyHiddenEvent(comscoreLabels)
        }
        messages.add(
            ReportingMessage.fromEvent(
                this,
                MPEvent.Builder(event).customAttributes(comscoreLabels).build(),
            ),
        )
        return messages
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>,
    ): List<ReportingMessage> =
        logEvent(
            MPEvent
                .Builder(screenName, MParticle.EventType.Navigation)
                .customAttributes(eventAttributes)
                .build(),
        )!!

    private fun applyEnterpriseScalarUserAttribute(
        key: String,
        value: String,
    ) {
        if (isEnterprise) {
            Analytics
                .getConfiguration()
                .setPersistentLabel(KitUtils.sanitizeAttributeKey(key), value)
        }
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: List<String>?,
        user: FilteredMParticleUser?,
    ) {
        // not supported
    }

    override fun supportsAttributeLists(): Boolean = !isEnterprise

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>,
    ) {
        if (isEnterprise) {
            for ((key, value) in attributes) {
                applyEnterpriseScalarUserAttribute(key, value)
            }
        }
    }

    override fun onSetAllUserAttributes(
        userAttributes: Map<String, String>,
        userAttributeLists: Map<String, List<String>>,
        user: FilteredMParticleUser,
    ) {
        setAllUserAttributes(userAttributes, userAttributeLists)
    }

    override fun onRemoveUserAttribute(
        key: String,
        user: FilteredMParticleUser,
    ) {
        if (isEnterprise) {
            Analytics.getConfiguration().removePersistentLabel(KitUtils.sanitizeAttributeKey(key))
        }
    }

    override fun onSetUserAttribute(
        key: String?,
        value: Any?,
        user: FilteredMParticleUser?,
    ) {
        if (key == null || value == null || value !is String) {
            return
        }
        applyEnterpriseScalarUserAttribute(key, value)
    }

    override fun removeUserIdentity(identityType: IdentityType) {
        if (isEnterprise) {
            Analytics.getConfiguration().removePersistentLabel(identityType.toString())
        }
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    override fun setUserIdentity(
        identityType: IdentityType,
        id: String,
    ) {
        if (isEnterprise) {
            Analytics.getConfiguration().setPersistentLabel(identityType.toString(), id)
        }
    }

    override fun getName(): String = NAME

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context,
    ): List<ReportingMessage> {
        val partnerConfiguration =
            PartnerConfiguration
                .Builder()
                .partnerId(settings[PARTNER_ID])
                .build()
        Analytics.getConfiguration().addClient(partnerConfiguration)
        val builder = PublisherConfiguration.Builder()
        builder.publisherId(getSettings()[CLIENT_ID])
        builder.secureTransmission(true)
        if (MParticle.getInstance()?.environment == MParticle.Environment.Development) {
            Analytics.setLogLevel(LogLevel.VERBOSE)
        }
        isEnterprise = ENTERPRISE_STRING == getSettings()[PRODUCT]
        val publisherConfiguration = builder.build()
        Analytics.getConfiguration().addClient(publisherConfiguration)
        Analytics.start(context)
        return emptyList()
    }

    override fun onActivityPaused(activity: Activity): List<ReportingMessage> {
        Analytics.notifyExitForeground()
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messageList
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle?,
    ): List<ReportingMessage> = emptyList()

    override fun onActivityDestroyed(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ): List<ReportingMessage> = emptyList()

    override fun onActivityResumed(activity: Activity): List<ReportingMessage> {
        Analytics.notifyEnterForeground()
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null,
            ),
        )
        return messageList
    }

    override fun onActivityStarted(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityStopped(activity: Activity): List<ReportingMessage> = emptyList()

    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> {
        if (!optOutStatus) {
            Analytics.getConfiguration().disable()
        }
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null,
            ).setOptOut(optOutStatus),
        )
        return messageList
    }

    companion object {
        private const val CLIENT_ID = "CustomerC2Value"
        private const val PRODUCT = "product"
        private const val PARTNER_ID = "partnerId"
        private const val COMSCORE_DEFAULT_LABEL_KEY = "name"
        private const val ENTERPRISE_STRING = "enterprise"
        const val NAME = "Comscore"
    }
}
