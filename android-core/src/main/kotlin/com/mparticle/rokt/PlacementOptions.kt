package com.mparticle.rokt

data class PlacementOptions(val jointSdkSelectPlacements: Long, val dynamicPerformanceMarkers: Map<String, Long> = mapOf())
