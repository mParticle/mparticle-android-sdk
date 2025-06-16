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
        viewName: String,
        attributes: Map<String, String>,
        callbacks: MpRoktEventCallback? = null,
        placeHolders: Map<String, WeakReference<RoktEmbeddedView>>? = null,
        fontTypefaces: Map<String, WeakReference<Typeface>>? = null,
        config: RoktConfig? = null
    ) {
        if (mConfigManager.isEnabled) {
            mKitManager.execute(viewName, attributes, callbacks, placeHolders, fontTypefaces, config)
        }
    }

    fun selectPlacements(viewName: String, attributes: Map<String, String>) {
        if (mConfigManager.isEnabled) {
            mKitManager.execute(viewName, attributes, null, null, null, null)
        }
    }

    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        if (mConfigManager.isEnabled) {
            mKitManager.purchaseFinalized(placementId, catalogItemId, status)
        }
    }
}
