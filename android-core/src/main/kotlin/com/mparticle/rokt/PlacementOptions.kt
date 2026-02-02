package com.mparticle.rokt

data class PlacementOptions(
    val jointSdkSelectPlacements: Long,
    val dynamicPerformanceMarkers: MutableMap<String, Long> = mutableMapOf(),
)
