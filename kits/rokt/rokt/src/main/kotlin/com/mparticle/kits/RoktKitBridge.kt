package com.mparticle.kits

import android.graphics.Typeface
import com.mparticle.rokt.RoktEmbeddedView
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.Rokt.RoktCallback
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import kotlinx.coroutines.flow.Flow
import java.lang.ref.WeakReference

internal interface RoktKitBridge {
    fun selectPlacements(
        viewName: String,
        attributes: Map<String, String>,
        roktCallback: RoktCallback?,
        placeHolders: MutableMap<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
        user: FilteredMParticleUser?,
        config: RoktConfig?,
        options: PlacementOptions?,
    )

    fun events(identifier: String): Flow<RoktEvent>

    fun enrichAttributes(attributes: MutableMap<String, String>, user: FilteredMParticleUser?)

    fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean)

    fun close()

    fun setSessionId(sessionId: String)

    fun getSessionId(): String?
}
