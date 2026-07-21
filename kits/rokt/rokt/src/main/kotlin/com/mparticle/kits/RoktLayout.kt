package com.mparticle.kits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent

/**
 * Rokt Jetpack Compose placement wrapper with mParticle attribute enrichment.
 *
 * @param sdkTriggered Whether the Rokt SDK should trigger placement selection.
 * @param identifier The placement identifier.
 * @param attributes Attributes to enrich through mParticle before rendering.
 * @param location The Rokt placement location.
 * @param modifier Optional Compose modifier for the placement.
 * @param config Optional Rokt SDK configuration.
 * @param onEvent Callback for native Rokt SDK placement events.
 */
@Composable
@Suppress("FunctionName")
fun RoktLayout(
    sdkTriggered: Boolean,
    identifier: String,
    attributes: Map<String, String>,
    location: String,
    modifier: Modifier = Modifier,
    config: RoktConfig? = null,
    onEvent: (RoktEvent) -> Unit = {},
) {
    var placementOptions: PlacementOptions? = null
    val instance = RoktKit.instance
    val resolvedAttributes = remember { mutableStateOf<Map<String, String>?>(null) }
    if (sdkTriggered) {
        // Capture the timestamp when the SDK is triggered
        placementOptions = PlacementOptions(
            jointSdkSelectPlacements = System.currentTimeMillis(),
            dynamicPerformanceMarkers = mapOf(),
        )
        LaunchedEffect(Unit) {
            instance?.prepareComposableAttributes(HashMap(attributes)) { result ->
                resolvedAttributes.value = result
            }
        }
    }

    resolvedAttributes.value?.let { finalAttributes ->
        com.rokt.roktsdk.RoktLayout(
            sdkTriggered = sdkTriggered,
            identifier = identifier,
            modifier = modifier,
            attributes = finalAttributes,
            location = location,
            config = config,
            placementOptions = placementOptions,
            onEvent = onEvent,
        )
    }
}
