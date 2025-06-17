package com.mparticle

import android.graphics.Typeface
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.internal.listeners.ApiClass
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import java.lang.ref.WeakReference

@ApiClass
class Rokt internal constructor(
    private val mConfigManager: ConfigManager,
    private val mKitManager: KitManager
) {

    fun selectPlacements(
        identifier: String,
        attributes: Map<String, String>,
        callbacks: MpRoktEventCallback? = null,
        embeddedViews: Map<String, WeakReference<RoktEmbeddedView>>? = null,
        fontTypefaces: Map<String, WeakReference<Typeface>>? = null,
        config: RoktConfig? = null
    ) {
        if (mConfigManager.isEnabled) {
            mKitManager.execute(identifier, attributes, callbacks, embeddedViews, fontTypefaces, config)
        }
    }

    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        if (mConfigManager.isEnabled) {
            mKitManager.purchaseFinalized(placementId, catalogItemId, status)
        }
    }
}
