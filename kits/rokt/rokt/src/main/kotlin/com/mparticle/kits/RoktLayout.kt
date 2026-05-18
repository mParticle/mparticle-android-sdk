package com.mparticle.kits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig

@Composable
@Suppress("FunctionName")
fun RoktLayout(
    sdkTriggered: Boolean,
    identifier: String,
    attributes: Map<String, String>,
    location: String,
    modifier: Modifier = Modifier,
    config: RoktConfig? = null,
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
            config = config?.toRoktSdkConfig(),
            placementOptions = placementOptions?.toRoktSdkPlacementOptions(),
        )
    }
}
