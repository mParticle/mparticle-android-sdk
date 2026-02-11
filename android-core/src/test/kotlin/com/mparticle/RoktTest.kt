package com.mparticle

import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.internal.RoktKitApi
import com.mparticle.rokt.PlacementOptions
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class, SystemClock::class)
class RoktTest {
    @Mock
    lateinit var kitManager: KitManager

    @Mock
    lateinit var roktKitApi: RoktKitApi

    @Mock
    lateinit var configManager: ConfigManager
    private lateinit var rokt: Rokt

    // Helpers to make Mockito matchers work in Kotlin with non-nullable types.
    // Mockito matchers return null, which Kotlin rejects for non-nullable params.
    // These helpers call the matcher (to register it) then return a cast null.
    private fun <T> capture(captor: ArgumentCaptor<T>): T {
        captor.capture()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun <T> any(): T {
        ArgumentMatchers.any<T>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private fun <T> eq(value: T): T {
        ArgumentMatchers.eq(value)
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        rokt = Rokt(configManager, kitManager)
    }

    @Test
    fun testSelectPlacements_withFullParams_whenEnabled() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        val attributes = mutableMapOf<String, String>()
        attributes["key"] = "value"

        val placeholders: Map<String, WeakReference<RoktEmbeddedView>> = HashMap()
        val fonts: Map<String, WeakReference<Typeface>> = HashMap()

        val config = RoktConfig.Builder().colorMode(RoktConfig.ColorMode.DARK).build()

        val callbacks =
            object : MpRoktEventCallback {
                override fun onLoad() {
                    println("View loaded")
                }

                override fun onUnload(reason: UnloadReasons) {
                    println("View unloaded due to: $reason")
                }

                override fun onShouldShowLoadingIndicator() {
                    println("Show loading indicator")
                }

                override fun onShouldHideLoadingIndicator() {
                    println("Hide loading indicator")
                }
            }
        rokt.selectPlacements(
            identifier = "testView",
            attributes = attributes,
            callbacks = callbacks,
            embeddedViews = placeholders,
            fontTypefaces = fonts,
            config = config,
        )

        verify(roktKitApi).selectPlacements(
            eq("testView"),
            eq(attributes),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenEnabled() {
        `when`(configManager.isEnabled()).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.selectPlacements(attributes = attributes, identifier = "basicView")

        verify(roktKitApi).selectPlacements(
            eq("basicView"),
            eq(attributes),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any(),
        )
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenDisabled() {
        `when`(configManager.isEnabled()).thenReturn(false)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        rokt.selectPlacements(
            identifier = "basicView",
            attributes = HashMap(),
        )

        verify(roktKitApi, never()).selectPlacements(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun testRoktSetWrapperSdk_whenDisabled_kitManagerNotCalled() {
        `when`(configManager.isEnabled()).thenReturn(false)

        rokt.selectPlacements(
            identifier = "basicView",
            attributes = HashMap(),
        )

        verify(kitManager, never()).setWrapperSdkVersion(any())
    }

    @Test
    fun testReportConversion_withBasicParams_whenEnabled() {
        `when`(configManager.isEnabled()).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.purchaseFinalized("132", "1111", true)

        verify(roktKitApi).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testReportConversion_withBasicParams_whenDisabled() {
        `when`(configManager.isEnabled()).thenReturn(false)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        rokt.purchaseFinalized("132", "1111", true)

        verify(roktKitApi, never()).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testEvents_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        val testIdentifier = "test-identifier"
        val expectedFlow: Flow<RoktEvent> = flowOf()
        `when`(roktKitApi.events(testIdentifier)).thenReturn(expectedFlow)

        val result = rokt.events(testIdentifier)

        verify(roktKitApi).events(testIdentifier)
        assertEquals(expectedFlow, result)
    }

    @Test
    fun testEvents_whenDisabled_returnsEmptyFlow() {
        `when`(configManager.isEnabled).thenReturn(false)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)

        val testIdentifier = "test-identifier"

        val result = rokt.events(testIdentifier)

        verify(roktKitApi, never()).events(any())
        runTest {
            val elements = result.toList()
            assertTrue(elements.isEmpty())
        }
    }

    @Test
    fun testSetSessionId_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)
        rokt.setSessionId("test-session-id")
        verify(roktKitApi).setSessionId("test-session-id")
    }

    @Test
    fun testSetSessionId_whenDisabled_doesNotCallKitManager() {
        `when`(configManager.isEnabled).thenReturn(false)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)
        rokt.setSessionId("test-session-id")
        verify(roktKitApi, never()).setSessionId(any())
    }

    @Test
    fun testGetSessionId_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)
        `when`(roktKitApi.getSessionId()).thenReturn("expected-session-id")
        val result = rokt.getSessionId()
        verify(roktKitApi).getSessionId()
        assertEquals("expected-session-id", result)
    }

    @Test
    fun testGetSessionId_whenDisabled_returnsNull() {
        `when`(configManager.isEnabled).thenReturn(false)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)
        val result = rokt.getSessionId()
        verify(roktKitApi, never()).getSessionId()
        assertNull(result)
    }

    @Test
    fun testSelectPlacements_withOptions_whenEnabled() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.roktKitApi).thenReturn(roktKitApi)
        val currentTimeMillis = System.currentTimeMillis()

        val attributes = mutableMapOf<String, String>()

        rokt.selectPlacements(
            identifier = "testView",
            attributes = attributes,
        )

        // Verify call is forwarded
        val viewNameCaptor = ArgumentCaptor.forClass(String::class.java)
        val optionsCaptor = ArgumentCaptor.forClass(PlacementOptions::class.java)
        verify(roktKitApi).selectPlacements(
            eq("testView"),
            any(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            capture(optionsCaptor),
        )
        assertTrue(optionsCaptor.value.jointSdkSelectPlacements >= currentTimeMillis)
    }
}
