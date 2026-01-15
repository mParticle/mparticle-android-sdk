package com.mparticle

import android.graphics.Typeface
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.internal.listeners.ApiClass
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

@ApiClass
class Rokt internal constructor(private val mConfigManager: ConfigManager, private val mKitManager: KitManager) {

    companion object {
        const val JOINT_SDK_SELECT_PLACEMENTS = "jointSdkSelectPlacements"
    }

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
            mKitManager.execute(identifier, HashMap(attributes), callbacks, embeddedViews, fontTypefaces, config, buildPlacementOptions())
        }
    }

    fun events(identifier: String): Flow<RoktEvent> = if (mConfigManager.isEnabled) {
        mKitManager.events(identifier)
    } else {
        flowOf()
    }

    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        if (mConfigManager.isEnabled) {
            mKitManager.purchaseFinalized(placementId, catalogItemId, status)
        }
    }

    fun close() {
        if (mConfigManager.isEnabled) {
            mKitManager.close()
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
            mKitManager.setSessionId(sessionId)
        }
    }

    /**
     * Get the session id to use within a non-native integration e.g. WebView.
     *
     * @return The session id or null if no session is present or SDK is not initialized.
     */
    fun getSessionId(): String? = if (mConfigManager.isEnabled) {
        mKitManager.getSessionId()
    } else {
        null
    }

    private fun buildPlacementOptions(): PlacementOptions = PlacementOptions(
        performanceMarkers = mutableMapOf(JOINT_SDK_SELECT_PLACEMENTS to System.currentTimeMillis()),
    )
}
