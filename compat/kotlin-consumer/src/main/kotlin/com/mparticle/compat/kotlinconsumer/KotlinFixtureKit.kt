/*
 * Compile-only fixture. A kit written the way third-party integrations are, in Kotlin, against the
 * published android-kit-base. It implements every listener interface a kit can opt into.
 */
package com.mparticle.compat.kotlinconsumer

import android.content.Context
import android.content.Intent
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.FilteredIdentityApiRequest
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.KitUtils
import com.mparticle.kits.ReportingMessage
import java.math.BigDecimal

class KotlinFixtureKit :
    KitIntegration(),
    KitIntegration.EventListener,
    KitIntegration.CommerceListener,
    KitIntegration.IdentityListener,
    KitIntegration.UserAttributeListener,
    KitIntegration.PushListener,
    KitIntegration.ApplicationStateListener {
    override fun getName(): String = "KotlinFixtureKit"

    override fun onKitCreate(settings: Map<String, String>, context: Context): List<ReportingMessage> {
        require(!MPUtility.isEmpty(settings) && !KitUtils.isEmpty(settings["apiKey"])) { "apiKey is required" }
        Logger.debug("KotlinFixtureKit ${configuration.kitId} created on ${kitManager.javaClass.simpleName}")
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    // EventListener

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage>? = null

    override fun logError(message: String, errorAttributes: Map<String, String>?): List<ReportingMessage>? = null

    override fun logException(exception: Exception, exceptionAttributes: Map<String, String>?, message: String?): List<ReportingMessage>? = null

    override fun logEvent(event: MPEvent): List<ReportingMessage> = listOf(ReportingMessage.fromEvent(this, event))

    override fun logScreen(screenName: String, screenAttributes: Map<String, String>?): List<ReportingMessage>? = null

    // CommerceListener

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>?,
    ): List<ReportingMessage>? = null

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> =
        CommerceEventUtils.expand(event).map { ReportingMessage.fromEvent(this, it) }

    // IdentityListener

    override fun onIdentifyCompleted(user: MParticleUser, request: FilteredIdentityApiRequest) {}

    override fun onLoginCompleted(user: MParticleUser, request: FilteredIdentityApiRequest) {}

    override fun onLogoutCompleted(user: MParticleUser, request: FilteredIdentityApiRequest) {}

    override fun onModifyCompleted(user: MParticleUser, request: FilteredIdentityApiRequest) {}

    override fun onUserIdentified(user: MParticleUser) {}

    // UserAttributeListener

    override fun supportsAttributeLists(): Boolean = true

    override fun onRemoveUserAttribute(key: String) {}

    override fun onSetUserAttribute(key: String, value: Any) {}

    override fun onSetUserAttributeList(attributeKey: String?, attributeValueList: List<String>?) {}

    override fun onSetAllUserAttributes(userAttributes: Map<String, String>, userAttributeLists: Map<String, List<String>>) {}

    override fun onIncrementUserAttribute(key: String, incrementedBy: Number, value: String) {}

    override fun onSetUserTag(key: String) {}

    override fun onConsentStateUpdated(oldState: ConsentState, newState: ConsentState) {}

    // PushListener

    override fun willHandlePushMessage(intent: Intent): Boolean = false

    override fun onPushMessageReceived(context: Context, pushIntent: Intent) {}

    override fun onPushRegistration(instanceId: String, senderId: String): Boolean = false

    // ApplicationStateListener

    override fun onApplicationForeground() {}

    override fun onApplicationBackground() {}
}
