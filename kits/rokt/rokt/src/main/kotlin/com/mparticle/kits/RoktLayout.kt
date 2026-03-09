package com.mparticle.kits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mparticle.MpRoktEventCallback
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.rokt.roktsdk.Rokt

@Composable
@Suppress("FunctionName")
fun RoktLayout(
    sdkTriggered: Boolean,
    identifier: String,
    attributes: Map<String, String>,
    location: String,
    modifier: Modifier = Modifier,
    mpRoktEventCallback: MpRoktEventCallback? = null,
    config: RoktConfig? = null,
) {
    var placementOptions: PlacementOptions? = null
    val instance = RoktKit.instance
    val resultMapState = remember { mutableStateOf<RoktResult?>(null) }
    if (sdkTriggered) {
        // Capture the timestamp when the SDK is triggered
        placementOptions = PlacementOptions(
            jointSdkSelectPlacements = System.currentTimeMillis(),
            dynamicPerformanceMarkers = mapOf(),
        )
        LaunchedEffect(Unit) {
            instance?.runComposableWithCallback(
                HashMap(attributes),
                mpRoktEventCallback,
                { resultMap, callback ->
                    resultMapState.value = RoktResult(resultMap, callback)
                },
            )
        }
    }

    resultMapState.value?.let { resultMap ->
        com.rokt.roktsdk.RoktLayout(
            sdkTriggered, identifier, modifier, resultMap.attributes, location,
            onLoad = { resultMap.callback.onLoad() },
            onShouldShowLoadingIndicator = { resultMap.callback.onShouldShowLoadingIndicator() },
            onShouldHideLoadingIndicator = { resultMap.callback.onShouldHideLoadingIndicator() },
            onUnload = { reason -> resultMap.callback.onUnload(reason) },
            config = config?.toRoktSdkConfig(),
            placementOptions = placementOptions?.toRoktSdkPlacementOptions(),
        )
    }
}

data class RoktResult(val attributes: Map<String, String>, val callback: Rokt.RoktCallback)
