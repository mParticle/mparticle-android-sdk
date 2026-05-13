package com.mparticle

import android.graphics.Typeface
import com.mparticle.internal.KitManager
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.RoktKitBridge
import com.mparticle.kits.RoktKitRequestHelper
import com.mparticle.rokt.RoktEmbeddedView
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.Rokt.RoktCallback
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

class Rokt internal constructor(private val mConfigManager: Any, private val mKitManager: KitManager) {

    /**
     * Display a Rokt placement with the specified parameters.
     *
     * @param identifier The placement identifier
     * @param attributes User attributes to pass to Rokt
     * @param callbacks Optional callback for Rokt events
     * @param embeddedViews Optional map of embedded view placeholders
     * @param fontTypefaces Optional map of font typefaces
     * @param config Optional Rokt configuration
     */
    @JvmOverloads
    fun selectPlacements(
        identifier: String,
        attributes: Map<String, String>,
        callbacks: RoktCallback? = null,
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
                    roktCallback = callbacks,
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
     * @return A Flow emitting native RoktEvent objects
     */
    fun events(identifier: String): Flow<RoktEvent> = if (isEnabled()) {
        resolveRoktKit()?.second?.events(identifier) ?: flowOf()
    } else {
        flowOf()
    }

    /**
     * Notify Rokt that a purchase has been finalized.
     *
     * @param placementId The placement identifier
     * @param catalogItemId The catalog item identifier
     * @param status Whether the purchase was successful
     */
    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.purchaseFinalized(placementId, catalogItemId, status)
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
     * Set the session id to use for the next execute call.
     *
     * This is useful for cases where you have a session id from a non-native integration,
     * e.g. WebView, and you want the session to be consistent across integrations.
     *
     * **Note:** Empty strings are ignored and will not update the session.
     *
     * @param sessionId The session id to be set. Must be a non-empty string.
     */
    fun setSessionId(sessionId: String) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.setSessionId(sessionId)
        }
    }

    /**
     * Get the session id to use within a non-native integration e.g. WebView.
     *
     * @return The session id or null if no session is present or SDK is not initialized.
     */
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
    fun prepareAttributesAsync(attributes: Map<String, String>) {
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

    private fun isEnabled(): Boolean = try {
        val field = mConfigManager.javaClass.getMethod("isEnabled")
        field.invoke(mConfigManager) as? Boolean ?: false
    } catch (ignored: Exception) {
        false
    }

    private fun buildPlacementOptions(): PlacementOptions = PlacementOptions(
        jointSdkSelectPlacements = System.currentTimeMillis(),
        dynamicPerformanceMarkers = mapOf(),
    )
}
