package com.mparticle.rokt

class RoktConfig private constructor(
    val colorMode: ColorMode?,
    val cacheConfig: CacheConfig?,
    val edgeToEdgeDisplay: Boolean,
) {
    data class Builder(
        private var colorMode: ColorMode? = null,
        private var cacheConfig: CacheConfig? = null,
        private var edgeToEdgeDisplay: Boolean = true,
    ) {
        fun colorMode(mode: ColorMode) = apply { this.colorMode = mode }
        fun cacheConfig(cacheConfig: CacheConfig) = apply { this.cacheConfig = cacheConfig }
        fun edgeToEdgeDisplay(edgeToEdgeDisplay: Boolean) = apply { this.edgeToEdgeDisplay = edgeToEdgeDisplay }
        fun build(): RoktConfig = RoktConfig(colorMode, cacheConfig, edgeToEdgeDisplay)
    }

    enum class ColorMode { LIGHT, DARK, SYSTEM }
}

class CacheConfig(
    val cacheDurationInSeconds: Long = DEFAULT_CACHE_DURATION_SECS,
    val cacheAttributes: Map<String, String>? = null,
) {
    companion object {
        const val DEFAULT_CACHE_DURATION_SECS: Long = 90 * 60
    }
}