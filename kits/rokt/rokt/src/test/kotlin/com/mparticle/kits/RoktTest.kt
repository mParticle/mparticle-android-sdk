package com.mparticle.kits

import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.KitManager
import com.rokt.roktsdk.PlacementOptions
import com.rokt.roktsdk.RoktConfig
import com.rokt.roktsdk.RoktEvent
import com.rokt.roktsdk.payment.PaymentExtension
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
import org.mockito.Mockito.withSettings
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class, SystemClock::class)
class RoktTest {
    @Mock
    lateinit var kitManager: KitManager

    @Mock
    lateinit var identityApi: IdentityApi

    @Mock
    lateinit var mParticle: MParticle

    @Mock
    lateinit var mParticleUser: MParticleUser

    private lateinit var roktKit: KitIntegration
    private lateinit var roktListener: RoktKitBridge

    private lateinit var configManager: FakeConfigManager
    private lateinit var rokt: Rokt

    private data class RoktFacadeFixture(
        val mParticle: MParticle,
        val roktListener: RoktKitBridge,
    )

    class FakeConfigManager(var enabled: Boolean = true) {
        fun isEnabled(): Boolean = enabled
    }

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
        configManager = FakeConfigManager(enabled = true)
        roktKit =
            org.mockito.Mockito.mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(RoktKitBridge::class.java),
            )
        roktListener = roktKit as RoktKitBridge
        MParticle.setInstance(mParticle)
        `when`(mParticle.Identity()).thenReturn(identityApi)
        `when`(identityApi.currentUser).thenReturn(mParticleUser)
        `when`(kitManager.isKitActive(MParticle.ServiceProviders.ROKT)).thenReturn(true)
        `when`(kitManager.getKitInstance(MParticle.ServiceProviders.ROKT)).thenReturn(roktKit)
        `when`(kitManager.isEnabled).thenAnswer { configManager.enabled }
        rokt = Rokt(kitManager)
    }

    @Test
    fun testSelectPlacements_withFullParams_whenEnabled() {
        configManager.enabled = true

        val attributes = mutableMapOf<String, String>()
        attributes["key"] = "value"

        val placeholders: Map<String, WeakReference<RoktEmbeddedView>> = HashMap()
        val fonts: Map<String, WeakReference<Typeface>> = HashMap()

        val config = RoktConfig.Builder().colorMode(RoktConfig.ColorMode.DARK).build()

        rokt.selectPlacements(
            identifier = "testView",
            attributes = attributes,
            embeddedViews = placeholders,
            fontTypefaces = fonts,
            config = config,
        )

        verify(roktListener).selectPlacements(
            eq("testView"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenEnabled() {
        configManager.enabled = true

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.selectPlacements(attributes = attributes, identifier = "basicView")

        verify(roktListener).selectPlacements(
            eq("basicView"),
            any(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any(),
        )
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenDisabled() {
        configManager.enabled = false

        rokt.selectPlacements(
            identifier = "basicView",
            attributes = HashMap(),
        )

        verify(roktListener, never()).selectPlacements(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun testRoktSetWrapperSdk_whenDisabled_kitManagerNotCalled() {
        configManager.enabled = false

        rokt.selectPlacements(
            identifier = "basicView",
            attributes = HashMap(),
        )

        verify(kitManager, never()).setWrapperSdkVersion(any())
    }

    @Test
    fun testReportConversion_withBasicParams_whenEnabled() {
        configManager.enabled = true

        val attributes = mutableMapOf<String, String>()
        attributes["a"] = "b"

        rokt.purchaseFinalized("132", "1111", true)

        verify(roktListener).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testReportConversion_withBasicParams_whenDisabled() {
        configManager.enabled = false

        rokt.purchaseFinalized("132", "1111", true)

        verify(roktListener, never()).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testEvents_whenEnabled_delegatesToKitManager() {
        configManager.enabled = true

        val testIdentifier = "test-identifier"
        val expectedFlow: Flow<RoktEvent> = flowOf()
        `when`(roktListener.events(testIdentifier)).thenReturn(expectedFlow)

        val result = rokt.events(testIdentifier)

        verify(roktListener).events(testIdentifier)
        assertEquals(expectedFlow, result)
    }

    @Test
    fun testEvents_whenDisabled_returnsEmptyFlow() {
        configManager.enabled = false

        val testIdentifier = "test-identifier"

        val result = rokt.events(testIdentifier)

        verify(roktListener, never()).events(any())
        runTest {
            val elements = result.toList()
            assertTrue(elements.isEmpty())
        }
    }

    @Test
    fun testRegisterPaymentExtension_whenEnabled_delegatesToKitManager() {
        configManager.enabled = true
        val paymentExtension = org.mockito.Mockito.mock(PaymentExtension::class.java)
        `when`(roktListener.registerPaymentExtension(paymentExtension)).thenReturn(true)

        val result = rokt.registerPaymentExtension(paymentExtension)

        assertTrue(result)
        verify(roktListener).registerPaymentExtension(paymentExtension)
    }

    @Test
    fun testRegisterPaymentExtension_whenDisabled_returnsFalse() {
        configManager.enabled = false
        val paymentExtension = org.mockito.Mockito.mock(PaymentExtension::class.java)

        val result = rokt.registerPaymentExtension(paymentExtension)

        assertFalse(result)
        verify(roktListener, never()).registerPaymentExtension(any())
    }

    @Test
    fun testRegisterPaymentExtension_whenRoktKitMissing_returnsFalse() {
        val paymentExtension = org.mockito.Mockito.mock(PaymentExtension::class.java)
        `when`(kitManager.isKitActive(MParticle.ServiceProviders.ROKT)).thenReturn(false)

        val result = rokt.registerPaymentExtension(paymentExtension)

        assertFalse(result)
        verify(roktListener, never()).registerPaymentExtension(any())
    }

    @Test
    fun testSelectShoppableAds_whenEnabled_delegatesToKitManager() {
        configManager.enabled = true
        val attributes = mapOf("key" to "value")
        val config = RoktConfig.Builder().colorMode(RoktConfig.ColorMode.DARK).build()

        rokt.selectShoppableAds(
            identifier = "shoppableView",
            attributes = attributes,
            config = config,
        )

        verify(roktListener).selectShoppableAds(
            eq("shoppableView"),
            any(),
            any(),
            eq(config),
        )
    }

    @Test
    fun testSelectShoppableAds_whenDisabled_doesNotCallKitManager() {
        configManager.enabled = false

        rokt.selectShoppableAds(
            identifier = "shoppableView",
            attributes = emptyMap(),
        )

        verify(roktListener, never()).selectShoppableAds(any(), any(), any(), any())
    }

    @Test
    fun testSetSessionId_whenEnabled_delegatesToKitManager() {
        configManager.enabled = true
        rokt.setSessionId("test-session-id")
        verify(roktListener).setSessionId("test-session-id")
    }

    @Test
    fun testSetSessionId_whenDisabled_doesNotCallKitManager() {
        configManager.enabled = false
        rokt.setSessionId("test-session-id")
        verify(roktListener, never()).setSessionId(any())
    }

    @Test
    fun testGetSessionId_whenEnabled_delegatesToKitManager() {
        configManager.enabled = true
        `when`(roktListener.getSessionId()).thenReturn("expected-session-id")
        val result = rokt.getSessionId()
        verify(roktListener).getSessionId()
        assertEquals("expected-session-id", result)
    }

    @Test
    fun testGetSessionId_whenDisabled_returnsNull() {
        configManager.enabled = false
        val result = rokt.getSessionId()
        verify(roktListener, never()).getSessionId()
        assertNull(result)
    }

    @Test
    fun testSelectPlacements_withOptions_whenEnabled() {
        configManager.enabled = true
        val currentTimeMillis = System.currentTimeMillis()

        val attributes = mutableMapOf<String, String>()

        rokt.selectPlacements(
            identifier = "testView",
            attributes = attributes,
        )

        // Verify call is forwarded
        val optionsCaptor = ArgumentCaptor.forClass(PlacementOptions::class.java)
        verify(roktListener).selectPlacements(
            eq("testView"),
            any(),
            isNull(),
            isNull(),
            any(),
            isNull(),
            capture(optionsCaptor),
        )
        assertTrue(optionsCaptor.value.jointSdkSelectPlacements >= currentTimeMillis)
    }

    @Test
    fun testMParticleRoktExtensionEvents_delegatesToCurrentInstance() {
        val fixture = createMParticleWithRoktKit()
        MParticle.setInstance(fixture.mParticle)
        val expectedFlow: Flow<RoktEvent> = flowOf()
        `when`(fixture.roktListener.events("identifier")).thenReturn(expectedFlow)

        val result = MParticle.getInstance()!!.rokt.events("identifier")

        verify(fixture.roktListener).events("identifier")
        assertEquals(expectedFlow, result)
    }

    @Test
    fun testMParticleRoktExtensionUsesNewFacadeAfterInstanceChanges() {
        val firstFixture = createMParticleWithRoktKit()
        val secondFixture = createMParticleWithRoktKit()
        val firstFlow: Flow<RoktEvent> = flowOf()
        val secondFlow: Flow<RoktEvent> = flowOf()
        `when`(firstFixture.roktListener.events("first")).thenReturn(firstFlow)
        `when`(secondFixture.roktListener.events("second")).thenReturn(secondFlow)

        MParticle.setInstance(firstFixture.mParticle)
        val firstResult = MParticle.getInstance()!!.rokt.events("first")

        MParticle.setInstance(secondFixture.mParticle)
        val secondResult = MParticle.getInstance()!!.rokt.events("second")

        assertEquals(firstFlow, firstResult)
        assertEquals(secondFlow, secondResult)
        verify(firstFixture.roktListener).events("first")
        verify(secondFixture.roktListener).events("second")
        verify(firstFixture.roktListener, never()).events("second")
    }

    private fun createMParticleWithRoktKit(): RoktFacadeFixture {
        val mParticle = org.mockito.Mockito.mock(MParticle::class.java)
        val internal = org.mockito.Mockito.mock(MParticle.Internal::class.java)
        val kitManager =
            org.mockito.Mockito.mock(MParticle.Internal::class.java.getMethod("getKitManager").returnType) as KitManager
        val roktKit =
            org.mockito.Mockito.mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(RoktKitBridge::class.java),
            )
        val roktListener = roktKit as RoktKitBridge

        `when`(mParticle.Internal()).thenReturn(internal)
        org.mockito.Mockito.doReturn(kitManager).`when`(internal).kitManager
        `when`(kitManager.isEnabled).thenReturn(true)
        `when`(kitManager.isKitActive(MParticle.ServiceProviders.ROKT)).thenReturn(true)
        `when`(kitManager.getKitInstance(MParticle.ServiceProviders.ROKT)).thenReturn(roktKit)

        return RoktFacadeFixture(mParticle, roktListener)
    }
}
