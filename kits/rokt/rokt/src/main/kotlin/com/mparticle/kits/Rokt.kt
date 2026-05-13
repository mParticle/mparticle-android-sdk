package com.mparticle.kits

import android.graphics.Typeface
import com.mparticle.MParticle
import com.mparticle.internal.KitManager
import com.mparticle.internal.Logger
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.Rokt.RoktCallback
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference

class Rokt internal constructor(private val mKitManager: KitManager, private val isEnabledProvider: () -> Boolean) {
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

    fun events(identifier: String): Flow<RoktEvent> = if (isEnabled()) {
        resolveRoktKit()?.second?.events(identifier) ?: flowOf()
    } else {
        flowOf()
    }

    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.purchaseFinalized(placementId, catalogItemId, status)
        }
    }

    fun close() {
        if (isEnabled()) {
            resolveRoktKit()?.second?.close()
        }
    }

    fun setSessionId(sessionId: String) {
        if (isEnabled()) {
            resolveRoktKit()?.second?.setSessionId(sessionId)
        }
    }

    fun getSessionId(): String? = if (isEnabled()) {
        resolveRoktKit()?.second?.getSessionId()
    } else {
        null
    }

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

    private fun isEnabled(): Boolean = isEnabledProvider()

    private fun buildPlacementOptions(): PlacementOptions = PlacementOptions(
        jointSdkSelectPlacements = System.currentTimeMillis(),
        dynamicPerformanceMarkers = mapOf(),
    )
}
