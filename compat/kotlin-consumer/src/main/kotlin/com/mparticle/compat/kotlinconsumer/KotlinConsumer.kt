/*
 * Compile-only fixture. Exercises the mParticle public API from Kotlin, the way an integrating app
 * does, against the published android-core and android-kit-base artifacts rather than the modules
 * in this repository. Nothing here runs; the build fails when the shipped API stops compiling, or
 * starts warning, for a Kotlin consumer.
 */
package com.mparticle.compat.kotlinconsumer

import android.content.Context
import com.mparticle.AttributionError
import com.mparticle.AttributionListener
import com.mparticle.AttributionResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MParticleTask
import com.mparticle.SdkListener
import com.mparticle.TypedUserAttributeListener
import com.mparticle.WrapperSdk
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.CCPAConsent
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityApiResult
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.rokt.RoktSession

class KotlinConsumer(private val context: Context) {
    fun start(): MParticleOptions {
        val identifyRequest = IdentityApiRequest.withEmptyUser()
            .email("consumer@example.com")
            .userIdentity(MParticle.IdentityType.CustomerId, null)
            .build()
        val options = MParticleOptions.builder(context)
            .credentials("api-key", "api-secret")
            .environment(MParticle.Environment.Development)
            .installType(MParticle.InstallType.AutoDetect)
            .logLevel(MParticle.LogLevel.DEBUG)
            .identify(identifyRequest)
            .attributionListener(FixtureAttributionListener())
            .dataplan("plan", null)
            .build()
        MParticle.addListener(context, FixtureSdkListener())
        MParticle.start(options)
        Logger.debug("started ${options.environment} workspace ${options.apiKey}")
        return options
    }

    fun logEvents() {
        val instance = MParticle.getInstance() ?: return
        instance.setWrapperSdk(WrapperSdk.WrapperFlutter, "1.0.0")

        val event = MPEvent.Builder("Signup", MParticle.EventType.UserPreference)
            .customAttributes(mapOf("plan" to "premium"))
            .duration(120.0)
            .build()
        instance.logEvent(event)
        instance.logScreen("Home", mapOf("source" to event.eventName))
        instance.leaveBreadcrumb(event.eventName)

        val product = Product.Builder("Widget", "SKU-1", 9.99)
            .quantity(2.0)
            .category("Gadgets")
            .build()
        val transaction = TransactionAttributes("order-1")
            .setRevenue(product.totalAmount)
            .setTax(null)
        val purchase = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(transaction)
            .currency("USD")
            .build()
        instance.logEvent(purchase)
        Logger.debug("logged ${purchase.productAction} for ${purchase.products?.size} products")

        val consent = ConsentState.builder()
            .addGDPRConsentState("marketing", GDPRConsent.builder(true).document("v1").build())
            .setCCPAConsentState(CCPAConsent.builder(false).build())
            .build()
        instance.setDeviceConsentState(consent)
    }

    fun identify() {
        val identity = MParticle.getInstance()?.Identity() ?: return
        val currentMpid: Long? = identity.currentUser?.id
        val request = IdentityApiRequest.withUser(identity.currentUser)
            .email("consumer@example.com")
            .customerId(currentMpid?.toString())
            .build()
        val task: MParticleTask<IdentityApiResult> = identity.login(request)
        task
            .addSuccessListener { result -> Logger.debug("logged in as ${result.user.id}") }
            .addFailureListener { response -> Logger.debug("login failed with ${response?.httpCode}") }

        identity.currentUser?.getUserAttributes(
            object : TypedUserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, Any?>,
                    userAttributeLists: Map<String, List<String?>?>,
                    mpid: Long,
                ) {
                    Logger.debug("$mpid has ${userAttributes.size} attributes and ${userAttributeLists.size} lists")
                }
            },
        )

        val session = RoktSession(sessionId = "session-id", sessionToken = "session-token")
        if (!MPUtility.isEmpty(session.sessionToken)) {
            Logger.debug("rokt session ${session.sessionId} expires at ${session.expiresAt}")
        }
    }

    // Companion references on kept Kotlin classes are exercised separately.

    private class FixtureAttributionListener : AttributionListener {
        override fun onResult(result: AttributionResult) {
            Logger.debug("attribution from ${result.serviceProviderId}: ${result.link}")
        }

        override fun onError(error: AttributionError) {
            Logger.debug("attribution error from ${error.serviceProviderId}: ${error.message}")
        }
    }

    private class FixtureSdkListener : SdkListener() {
        override fun onApiCalled(apiName: String, objects: List<Any>, isExternal: Boolean) {
            Logger.debug("api $apiName called with ${objects.size} arguments")
        }

        override fun onKitStarted(kitId: Int) {
            Logger.debug("kit $kitId started")
        }
    }
}
