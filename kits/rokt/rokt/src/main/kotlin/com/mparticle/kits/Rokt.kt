package com.mparticle.kits

import android.graphics.Typeface
import com.mparticle.MParticle
import com.mparticle.internal.KitManager
import com.mparticle.internal.Logger
import com.mparticle.rokt.RoktSession
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import com.rokt.roktsdk.payment.PaymentExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

/**
 * Public facade for interacting with the Rokt Kit through mParticle.
 */
class Rokt internal constructor(private val mKitManager: KitManager) {
    /**
     * Display a Rokt placement with the specified parameters.
     *
     * @param identifier The placement identifier
     * @param attributes User attributes to pass to Rokt
     * @param embeddedViews Optional map of embedded view placeholders
     * @param fontTypefaces Optional map of font typefaces
     * @param config Optional Rokt configuration
     */
    @JvmOverloads
    fun selectPlacements(
        identifier: String,
        attributes: Map<String, String>,
        embeddedViews: Map<String, WeakReference<RoktEmbeddedView>>? = null,
        fontTypefaces: Map<String, WeakReference<Typeface>>? = null,
        config: RoktConfig? = null,
    ) {
        if (isEnabled()) {
            val resolved = resolveRoktKit()
            if (resolved != null) {
                val (kitIntegration, roktListener) = resolved
                RoktKitRequestHelper.selectPlacements(
                    kitIntegration = kitIntegration,
                    roktListener = roktListener,
                    viewName = identifier,
                    attributes = HashMap(attributes),
                    placeHolders = embeddedViews,
                    fontTypefaces = fontTypefaces,
                    config = config,
                    options = buildPlacementOptions(),
                )
            } else {
                Logger.warning("Rokt Kit is not available. Make sure the Rokt Kit is included in your app.")
            }
        }
    }

    /**
     * Get a Flow of Rokt events for the specified identifier.
     *
     * @param identifier The placement identifier to listen for events
     * @return A Flow emitting RoktEvent objects
     */
    fun events(identifier: String): Flow<RoktEvent> = if (isEnabled()) {
        resolveRoktKit()?.second?.events(identifier) ?: flowOf()
    } else {
        flowOf()
    }

    /**
     * Register a payment extension for Shoppable Ads.
     *
     * The Rokt Kit adds mParticle dashboard configuration before forwarding to the Rokt SDK.
     *
     * @param paymentExtension The payment extension implementation to register
     * @return true if the Rokt SDK accepts the payment extension configuration
     */
    fun registerPaymentExtension(paymentExtension: PaymentExtension): Boolean = if (isEnabled()) {
        val resolved = resolveRoktKit()
        if (resolved != null) {
            resolved.second.registerPaymentExtension(paymentExtension)
        } else {
            Logger.warning("Rokt Kit is not available. Make sure the Rokt Kit is included in your app.")
            false
        }
    } else {
        false
    }

    /**
     * Display a Rokt Shoppable Ads placement with the specified parameters.
     *
     * @param identifier The placement identifier
     * @param attributes User attributes to pass to Rokt
     * @param config Optional Rokt configuration
     */
    @JvmOverloads
    fun selectShoppableAds(
        identifier: String,
        attributes: Map<String, String> = emptyMap(),
        config: RoktConfig? = null,
    ) {
        if (isEnabled()) {
            val resolved = resolveRoktKit()
            if (resolved != null) {
                val (kitIntegration, roktListener) = resolved
                RoktKitRequestHelper.selectShoppableAds(
                    kitIntegration = kitIntegration,
                    roktListener = roktListener,
                    viewName = identifier,
                    attributes = HashMap(attributes),
                    config = config,
                )
            } else {
                Logger.warning("Rokt Kit is not available. Make sure the Rokt Kit is included in your app.")
            }
        }
    }

    /**
     * Notify Rokt that a purchase has been finalized.
     *
     * @param identifier The placement identifier
     * @param catalogItemId The catalog item identifier
     * @param success Whether the purchase was successful
     */
    fun purchaseFinalized(identifier: String, catalogItemId: String, success: Boolean) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.purchaseFinalized(identifier, catalogItemId, success)
        }
    }

    /**
     * Close any active Rokt placements.
     */
    fun close() {
        if (isEnabled()) {
            resolveRoktKit()?.second?.close()
        }
    }

    /**
     * Set the session to use for the next execute call.
     *
     * Use this when you have a session from a non-native integration (e.g. WebView)
     * and want the session to stay consistent across integrations. Call before the next
     * selectPlacements.
     *
     * Matches Web launcher options: pass [RoktSession.sessionId] with optional
     * [RoktSession.sessionToken]. When the token is present, offers/events can send
     * `Authorization: Bearer`. When only the id is present, the id is applied without Bearer
     * seeding. Empty `sessionId` (or token without id) is ignored.
     *
     * @param session The session id and optional JWT session token (optional expiry).
     */
    fun setSession(session: RoktSession) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.setSession(session)
        }
    }

    /**
     * Get the current session (id + token) for use within a non-native integration e.g. WebView.
     *
     * @return The session, or null if disabled, no session is present, or the token has expired.
     */
    fun getSession(): RoktSession? = if (isEnabled()) {
        resolveRoktKit()?.second?.getSession()
    } else {
        null
    }

    /**
     * Set the session id to use for the next execute call.
     *
     * This is useful for cases where you have a session id from a non-native integration,
     * e.g. WebView, and you want the session to be consistent across integrations.
     *
     * **Note:** Empty strings are ignored and will not update the session.
     * Prefer [setSession] so the session token is also applied for offers and events.
     *
     * @param sessionId The session id to be set. Must be a non-empty string.
     */
    @Deprecated("Use setSession to set session id and session token.")
    fun setSessionId(sessionId: String) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.setSessionId(sessionId)
        }
    }

    /**
     * Get the session id to use within a non-native integration e.g. WebView.
     *
     * Prefer [getSession] to also read the session token.
     *
     * @return The session id or null if no session is present or SDK is not initialized.
     */
    @Deprecated("Use getSession to read session id and session token.")
    fun getSessionId(): String? = if (isEnabled()) {
        resolveRoktKit()?.second?.getSessionId()
    } else {
        null
    }

    /**
     * Prepare attributes asynchronously before executing a placement.
     *
     * @param attributes The attributes to prepare
     */
    internal fun prepareAttributesAsync(attributes: Map<String, String>) {
        if (isEnabled()) {
            val resolved = resolveRoktKit()
            if (resolved != null) {
                val (kitIntegration, roktListener) = resolved
                RoktKitRequestHelper.prepareAttributesAsync(
                    kitIntegration = kitIntegration,
                    roktListener = roktListener,
                    attributes = attributes,
                )
            }
        }
    }

    private fun resolveRoktKit(): Pair<KitIntegration, RoktKitBridge>? {
        if (!mKitManager.isKitActive(MParticle.ServiceProviders.ROKT)) {
            return null
        }
        val kitInstance = mKitManager.getKitInstance(MParticle.ServiceProviders.ROKT) as? KitIntegration ?: return null
        val roktBridge = kitInstance as? RoktKitBridge ?: return null
        return kitInstance to roktBridge
    }

    private fun isEnabled(): Boolean = mKitManager.isEnabled

    private fun buildPlacementOptions(): PlacementOptions = PlacementOptions(
        jointSdkSelectPlacements = System.currentTimeMillis(),
        dynamicPerformanceMarkers = mapOf(),
    )
}
