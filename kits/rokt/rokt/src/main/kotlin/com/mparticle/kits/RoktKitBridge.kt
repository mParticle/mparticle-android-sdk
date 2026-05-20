package com.mparticle.kits

import android.graphics.Typeface
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import kotlinx.coroutines.flow.Flow
import java.lang.ref.WeakReference

internal interface RoktKitBridge {
    fun selectPlacements(
        viewName: String,
        attributes: Map<String, String>,
        placeHolders: MutableMap<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
        user: FilteredMParticleUser?,
        config: RoktConfig?,
        options: PlacementOptions?,
    )

    fun events(identifier: String): Flow<RoktEvent>

    fun enrichAttributes(attributes: MutableMap<String, String>, user: FilteredMParticleUser?)

    fun purchaseFinalized(identifier: String, catalogItemId: String, success: Boolean)

    fun close()

    fun setSessionId(sessionId: String)

    fun getSessionId(): String?
}
