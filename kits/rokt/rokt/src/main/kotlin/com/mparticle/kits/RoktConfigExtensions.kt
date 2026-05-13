package com.mparticle.kits

import com.mparticle.rokt.RoktConfig
import com.rokt.roktsdk.CacheConfig
import com.mparticle.rokt.CacheConfig as MpCacheConfig
import com.rokt.roktsdk.RoktConfig as RoktSdkConfig

fun MpCacheConfig.toRoktSdkCacheConfig(): CacheConfig = CacheConfig(
    cacheDurationInSeconds = this.cacheDurationInSeconds,
    cacheAttributes = this.cacheAttributes,
)

fun RoktConfig.toRoktSdkConfig(): RoktSdkConfig {
    val colorMode = when (this.colorMode) {
        RoktConfig.ColorMode.LIGHT -> RoktSdkConfig.ColorMode.LIGHT
        RoktConfig.ColorMode.DARK -> RoktSdkConfig.ColorMode.DARK
        RoktConfig.ColorMode.SYSTEM -> RoktSdkConfig.ColorMode.SYSTEM
        else -> RoktSdkConfig.ColorMode.SYSTEM
    }

    val cacheConfig = this.cacheConfig?.toRoktSdkCacheConfig()

    val edgeToEdgeDisplay = this.edgeToEdgeDisplay

    val builder = RoktSdkConfig.Builder()
        .colorMode(colorMode)
        .edgeToEdgeDisplay(edgeToEdgeDisplay)

    cacheConfig?.let { builder.cacheConfig(it) }

    return builder.build()
}
