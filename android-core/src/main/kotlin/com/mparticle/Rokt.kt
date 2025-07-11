package com.mparticle

import android.graphics.Typeface
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.internal.listeners.ApiClass
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

@ApiClass
class Rokt internal constructor(
    private val mConfigManager: ConfigManager,
    private val mKitManager: KitManager
) {

    @JvmOverloads
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

    fun events(identifier: String) : Flow<RoktEvent> {
        return if (mConfigManager.isEnabled) {
            mKitManager.events(identifier)
        } else {
            flowOf()
        }
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
}