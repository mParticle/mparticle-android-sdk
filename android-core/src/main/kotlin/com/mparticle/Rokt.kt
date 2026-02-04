package com.mparticle

import android.graphics.Typeface
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.internal.Logger
import com.mparticle.internal.listeners.ApiClass
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

@ApiClass
class Rokt internal constructor(private val mConfigManager: ConfigManager, private val mKitManager: KitManager) {

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
        callbacks: MpRoktEventCallback? = null,
        embeddedViews: Map<String, WeakReference<RoktEmbeddedView>>? = null,
        fontTypefaces: Map<String, WeakReference<Typeface>>? = null,
        config: RoktConfig? = null,
    ) {
        if (mConfigManager.isEnabled) {
            val roktApi = mKitManager.roktKitApi
            if (roktApi != null) {
                roktApi.execute(identifier, HashMap(attributes), callbacks, embeddedViews, fontTypefaces, config, buildPlacementOptions())
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
    fun events(identifier: String): Flow<RoktEvent> = if (mConfigManager.isEnabled) {
        mKitManager.roktKitApi?.events(identifier) ?: flowOf()
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
        if (mConfigManager.isEnabled) {
            mKitManager.roktKitApi?.purchaseFinalized(placementId, catalogItemId, status)
        }
    }

    /**
     * Close any active Rokt placements.
     */
    fun close() {
        if (mConfigManager.isEnabled) {
            mKitManager.roktKitApi?.close()
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
        if (mConfigManager.isEnabled) {
            mKitManager.roktKitApi?.setSessionId(sessionId)
        }
    }

    /**
     * Get the session id to use within a non-native integration e.g. WebView.
     *
     * @return The session id or null if no session is present or SDK is not initialized.
     */
    fun getSessionId(): String? = if (mConfigManager.isEnabled) {
        mKitManager.roktKitApi?.getSessionId()
    } else {
        null
    }

    /**
     * Prepare attributes asynchronously before executing a placement.
     *
     * @param attributes The attributes to prepare
     */
    fun prepareAttributesAsync(attributes: Map<String, String>) {
        if (mConfigManager.isEnabled) {
            mKitManager.roktKitApi?.prepareAttributesAsync(attributes)
        }
    }

    private fun buildPlacementOptions(): PlacementOptions = PlacementOptions(
        jointSdkSelectPlacements = System.currentTimeMillis(),
    )
}
