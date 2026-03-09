package com.mparticle.kits

import com.mparticle.rokt.CacheConfig
import com.mparticle.rokt.RoktConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.rokt.roktsdk.RoktConfig as SdkRoktConfig

class RoktConfigExtensionsTest {

    @Test
    fun `toRoktSdkConfig maps color mode and edgeToEdge`() {
        val source = mockk<RoktConfig>()
        every { source.colorMode } returns RoktConfig.ColorMode.DARK
        every { source.edgeToEdgeDisplay } returns true
        every { source.cacheConfig } returns null

        val result: SdkRoktConfig = source.toRoktSdkConfig()

        assertEquals(SdkRoktConfig.ColorMode.DARK, result.colorMode)
        assertEquals(true, result.edgeToEdgeDisplay)
    }

    @Test
    fun `toRoktSdkConfig maps cacheConfig when present`() {
        val cacheAttributes = mapOf(
            "key1" to "value1",
            "key2" to "value2",
        )

        val cacheConfig = mockk<CacheConfig>()
        every { cacheConfig.cacheDurationInSeconds } returns 3600
        every { cacheConfig.cacheAttributes } returns cacheAttributes

        val source = mockk<RoktConfig>()
        every { source.colorMode } returns RoktConfig.ColorMode.LIGHT
        every { source.edgeToEdgeDisplay } returns false
        every { source.cacheConfig } returns cacheConfig

        val result: SdkRoktConfig = source.toRoktSdkConfig()

        assertEquals(SdkRoktConfig.ColorMode.LIGHT, result.colorMode)
        assertEquals(false, result.edgeToEdgeDisplay)
        assertNotNull(result.cacheConfig)
        assertEquals(3600L, result.cacheConfig?.cacheDurationInSeconds)
        assertEquals(cacheAttributes, result.cacheConfig?.cacheAttributes)
    }

    @Test
    fun `toRoktSdkCacheConfig maps fields`() {
        val cacheAttributes = mapOf(
            "a" to "1",
            "b" to "2",
        )
        val mpCache = mockk<CacheConfig>()
        every { mpCache.cacheDurationInSeconds } returns 120
        every { mpCache.cacheAttributes } returns cacheAttributes

        val sdkCache = mpCache.toRoktSdkCacheConfig()

        assertEquals(120, sdkCache.cacheDurationInSeconds)
        assertEquals(cacheAttributes, sdkCache.cacheAttributes)
    }
}
