package com.mparticle.internal

import android.graphics.Typeface
import com.mparticle.MpRoktEventCallback
import com.mparticle.RoktEvent
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import java.lang.ref.WeakReference

/**
 * Interface for Rokt Kit operations.
 *
 * Implementations of this interface are provided by the Rokt Kit when it is
 * configured and active. Use [KitManager.getRoktKitApi] to obtain an instance.
 */
interface RoktKitApi {
    /**
     * Execute a Rokt placement with the specified parameters.
     *
     * @param viewName The identifier for the placement view
     * @param attributes User attributes to pass to Rokt
     * @param mpRoktEventCallback Optional callback for Rokt events
     * @param placeHolders Optional map of embedded view placeholders
     * @param fontTypefaces Optional map of font typefaces
     * @param config Optional Rokt configuration
     * @param options Optional placement options
     */
    fun execute(
        viewName: String,
        attributes: Map<String, String>,
        mpRoktEventCallback: MpRoktEventCallback?,
        placeHolders: Map<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: Map<String, WeakReference<Typeface>>?,
        config: RoktConfig?,
        options: PlacementOptions? = null,
    )

    /**
     * Get a Flow of Rokt events for the specified identifier.
     *
     * @param identifier The placement identifier to listen for events
     * @return A Flow emitting RoktEvent objects
     */
    fun events(identifier: String): Flow<RoktEvent>

    /**
     * Notify Rokt that a purchase has been finalized.
     *
     * @param placementId The placement identifier
     * @param catalogItemId The catalog item identifier
     * @param status Whether the purchase was successful
     */
    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean)

    /**
     * Close any active Rokt placements.
     */
    fun close()

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
    fun setSessionId(sessionId: String)

    /**
     * Get the session id to use within a non-native integration e.g. WebView.
     *
     * @return The session id or null if no session is present.
     */
    fun getSessionId(): String?

    /**
     * Prepare attributes asynchronously before executing a placement.
     *
     * @param attributes The attributes to prepare
     */
    fun prepareAttributesAsync(attributes: Map<String, String>)
}
