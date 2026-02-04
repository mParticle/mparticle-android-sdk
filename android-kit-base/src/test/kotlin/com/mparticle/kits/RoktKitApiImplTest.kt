package com.mparticle.kits

import com.mparticle.MParticle
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.MPUtility
import com.mparticle.mock.MockMParticle
import com.mparticle.rokt.PlacementOptions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.withSettings

class RoktKitApiImplTest {
    @Before
    fun setUp() {
        val identityApi = mock(IdentityApi::class.java)
        val instance = MockMParticle()
        instance.setIdentityApi(identityApi)
        MParticle.setInstance(instance)
    }

    @Test
    fun testExecute_mapsAttributesAndAddsSandbox() {
        val kitConfig = KitConfiguration.createKitConfiguration(JSONObject().put("id", 42))
        val settingsMap =
            hashMapOf(
                "placementAttributesMapping" to
                    """
                    [
                      {"map": "number", "value": "no"},
                      {"map": "customerId", "value": "minorcatid"}
                    ]
                    """.trimIndent(),
            )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(kitConfig, settingsMap)

        val kitIntegration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(kitIntegration.configuration).thenReturn(kitConfig)
        val roktListener = kitIntegration as KitIntegration.RoktListener
        val roktApi = RoktKitApiImpl(roktListener, kitIntegration)

        val attributes =
            hashMapOf(
                "number" to "(123) 456-9898",
                "customerId" to "55555",
                "country" to "US",
            )

        roktApi.execute("Test", attributes, null, null, null, null, null)

        @Suppress("UNCHECKED_CAST")
        val attributesCaptor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, String>>
        verify(roktListener).execute(
            any(),
            attributesCaptor.capture(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
        val captured = attributesCaptor.value
        assertEquals("(123) 456-9898", captured["no"])
        assertEquals("55555", captured["minorcatid"])
        assertEquals("US", captured["country"])
        assertEquals(MPUtility.isDevEnv().toString(), captured["sandbox"])
    }

    @Test
    fun testExecute_passesPlacementOptions() {
        val kitConfig = KitConfiguration.createKitConfiguration(JSONObject().put("id", 42))
        val kitIntegration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(kitIntegration.configuration).thenReturn(kitConfig)
        val roktListener = kitIntegration as KitIntegration.RoktListener
        val roktApi = RoktKitApiImpl(roktListener, kitIntegration)

        val placementOptions = PlacementOptions(jointSdkSelectPlacements = 123L)

        roktApi.execute("Test", emptyMap(), null, null, null, null, placementOptions)

        val optionsCaptor = ArgumentCaptor.forClass(PlacementOptions::class.java)
        verify(roktListener).execute(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            optionsCaptor.capture(),
        )
        assertEquals(placementOptions, optionsCaptor.value)
    }

    @Test
    fun testEvents_returnsEmptyFlowWhenProviderThrows() = runTest {
        val kitConfig = KitConfiguration.createKitConfiguration(JSONObject().put("id", 42))
        val kitIntegration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(kitIntegration.configuration).thenReturn(kitConfig)
        val roktListener = kitIntegration as KitIntegration.RoktListener
        `when`(roktListener.events(any())).thenThrow(RuntimeException("Test exception"))
        val roktApi = RoktKitApiImpl(roktListener, kitIntegration)

        val result = roktApi.events("test-identifier")

        assertTrue(result.toList().isEmpty())
    }

    @Test
    fun testPrepareAttributesAsync_delegatesToEnrichAttributes() {
        val kitConfig = KitConfiguration.createKitConfiguration(JSONObject().put("id", 42))
        val settingsMap =
            hashMapOf(
                "placementAttributesMapping" to
                    """
                    [
                      {"map": "number", "value": "no"}
                    ]
                    """.trimIndent(),
            )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(kitConfig, settingsMap)

        val kitIntegration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(kitIntegration.configuration).thenReturn(kitConfig)
        val roktListener = kitIntegration as KitIntegration.RoktListener
        val roktApi = RoktKitApiImpl(roktListener, kitIntegration)

        roktApi.prepareAttributesAsync(mapOf("number" to "(123) 456-9898"))

        @Suppress("UNCHECKED_CAST")
        val attributesCaptor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, String>>
        verify(roktListener).enrichAttributes(attributesCaptor.capture(), any())
        val captured = attributesCaptor.value
        assertEquals("(123) 456-9898", captured["no"])
        assertEquals(MPUtility.isDevEnv().toString(), captured["sandbox"])
    }
}
