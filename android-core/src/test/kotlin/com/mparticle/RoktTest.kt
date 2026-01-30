package com.mparticle

import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class, SystemClock::class)
class RoktTest {
    @Mock
    lateinit var kitManager: KitManager

    @Mock
    lateinit var configManager: ConfigManager
    private lateinit var rokt: Rokt

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        rokt = Rokt(configManager, kitManager)
    }

    @Test
    fun testSelectPlacements_withFullParams_whenEnabled() {
        `when`(configManager.isEnabled).thenReturn(true)

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

        verify(kitManager)?.execute("testView", attributes, callbacks, placeholders, fonts, config)
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenEnabled() {
        `when`(configManager.isEnabled()).thenReturn(true)

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.selectPlacements(attributes = attributes, identifier = "basicView")

        verify(kitManager).execute("basicView", attributes, null, null, null, null)
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenDisabled() {
        `when`(configManager.isEnabled()).thenReturn(false)

        rokt.selectPlacements(
            identifier = "basicView",
            attributes = HashMap(),
        )

        verify(kitManager, never()).execute(any(), any(), any(), any(), any(), any())
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

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.purchaseFinalized("132", "1111", true)

        verify(kitManager).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testReportConversion_withBasicParams_whenDisabled() {
        `when`(configManager.isEnabled()).thenReturn(false)

        rokt.purchaseFinalized("132", "1111", true)

        verify(kitManager, never()).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testEvents_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)

        val testIdentifier = "test-identifier"
        val expectedFlow: Flow<RoktEvent> = flowOf()
        `when`(kitManager.events(testIdentifier)).thenReturn(expectedFlow)

        val result = rokt.events(testIdentifier)

        verify(kitManager).events(testIdentifier)
        assertEquals(expectedFlow, result)
    }

    @Test
    fun testEvents_whenDisabled_returnsEmptyFlow() {
        `when`(configManager.isEnabled).thenReturn(false)

        val testIdentifier = "test-identifier"

        val result = rokt.events(testIdentifier)

        verify(kitManager, never()).events(any())
        runTest {
            val elements = result.toList()
            assertTrue(elements.isEmpty())
        }
    }

    @Test
    fun testSetSessionId_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)
        rokt.setSessionId("test-session-id")
        verify(kitManager).setSessionId("test-session-id")
    }

    @Test
    fun testSetSessionId_whenDisabled_doesNotCallKitManager() {
        `when`(configManager.isEnabled).thenReturn(false)
        rokt.setSessionId("test-session-id")
        verify(kitManager, never()).setSessionId(any())
    }

    @Test
    fun testGetSessionId_whenEnabled_delegatesToKitManager() {
        `when`(configManager.isEnabled).thenReturn(true)
        `when`(kitManager.getSessionId()).thenReturn("expected-session-id")
        val result = rokt.getSessionId()
        verify(kitManager).getSessionId()
        assertEquals("expected-session-id", result)
    }

    @Test
    fun testGetSessionId_whenDisabled_returnsNull() {
        `when`(configManager.isEnabled).thenReturn(false)
        val result = rokt.getSessionId()
        verify(kitManager, never()).getSessionId()
        assertNull(result)
    }
}
