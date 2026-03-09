package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.WrapperSdk
import com.mparticle.WrapperSdkVersion
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.kits.mocks.MockKitConfiguration
import com.rokt.roktsdk.FulfillmentAttributes
import com.rokt.roktsdk.Rokt
import com.rokt.roktsdk.RoktEvent
import com.rokt.roktsdk.logging.RoktLogLevel
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class RoktKitTests {
    private val context = mockk<Context>(relaxed = true)
    private lateinit var roktKit: RoktKit
    private val settings = HashMap<String, String>()
    private lateinit var kitManager: TestKitManager

    @Before
    @Throws(Exception::class)
    fun setUp() {
        settings["application_id"] = TEST_APPLICATION_ID
        every { context.applicationContext } returns context
        roktKit = RoktKit()
        kitManager = TestKitManager()
        MParticle.setInstance(mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()?.Identity()).thenReturn(
            mock(
                IdentityApi::class.java,
            ),
        )
        val kitManager = KitManagerImpl(
            mock(
                Context::class.java,
            ),
            null,
            emptyCoreCallbacks,
            mock(MParticleOptions::class.java),
        )
        roktKit.kitManager = kitManager
        // roktKit.configuration = KitConfiguration.createKitConfiguration(JSONObject().put("id", "-1"))
    }

    @Test
    fun test_prepareFinalAttributes_filters_out_null_user_attributes() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())

        // Include a null value and a non-null non-string value to verify toString() behavior
        val userAttributes = HashMap<String, Any?>()
        userAttributes["attr_non_null_string"] = "value"
        userAttributes["attr_null"] = null
        userAttributes["attr_non_string"] = 123
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)
        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("ShouldFilter").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("ShouldFilter_key_2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(filteredKey2, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }
        val json = JSONObject()
        json.put("ua", jsonObject)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "prepareFinalAttributes",
            FilteredMParticleUser::class.java,
            Map::class.java,
        )
        method.isAccessible = true

        val inputAttributes: Map<String, String> = emptyMap()
        val result = method.invoke(roktKit, mockFilterUser, inputAttributes) as Map<*, *>

        // Should include only non-null user attributes, and convert non-string values via toString()
        assertTrue(result.containsKey("attr_non_null_string"))
        assertEquals("value", result["attr_non_null_string"])

        assertFalse(result.containsKey("attr_null"))

        assertTrue(result.containsKey("attr_non_string"))
        assertEquals("123", result["attr_non_string"])
    }

    @Test
    fun test_prepareFinalAttributes_converts_large_double_without_scientific_notation() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        // Large Double value that would produce scientific notation with toString()
        userAttributes["large_double"] = 50352212112.0
        userAttributes["large_double_negative"] = -50352212112.0
        userAttributes["double_with_decimal"] = 123.456789
        userAttributes["double_zero"] = 0.0
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))

        // Act
        val inputAttributes: Map<String, String> = mapOf("initial_attr" to "initial_value")
        roktKit.selectPlacements(
            viewName = "test_view",
            attributes = inputAttributes,
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured
        assertEquals("50352212112", capturedAttributes["large_double"])

        assertEquals("-50352212112", capturedAttributes["large_double_negative"])

        assertTrue(capturedAttributes.containsKey("double_with_decimal"))
        assertEquals("123.456789", capturedAttributes["double_with_decimal"])

        assertEquals("0.0", capturedAttributes["double_zero"])

        // Verify initial attributes are preserved
        assertEquals("initial_value", capturedAttributes["initial_attr"])

        unmockkObject(Rokt)
    }

    @Test
    fun test_prepareFinalAttributes_converts_long_values() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        userAttributes["long_value"] = Long.MAX_VALUE // 9223372036854775807L
        userAttributes["long_negative"] = Long.MIN_VALUE // -9223372036854775808L
        userAttributes["long_zero"] = 0L
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))

        // Act
        roktKit.selectPlacements(
            viewName = "test_view",
            attributes = emptyMap(),
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured
        assertEquals("9223372036854775807", capturedAttributes["long_value"])
        assertEquals("-9223372036854775808", capturedAttributes["long_negative"])
        assertEquals("0", capturedAttributes["long_zero"])

        unmockkObject(Rokt)
    }

    @Test
    fun test_prepareFinalAttributes_converts_int_values() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        userAttributes["int_value"] = Int.MAX_VALUE // 2147483647
        userAttributes["int_negative"] = Int.MIN_VALUE // -2147483648
        userAttributes["int_zero"] = 0
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))

        // Act
        roktKit.selectPlacements(
            viewName = "test_view",
            attributes = emptyMap(),
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured
        assertEquals("2147483647", capturedAttributes["int_value"])
        assertEquals("-2147483648", capturedAttributes["int_negative"])
        assertEquals("0", capturedAttributes["int_zero"])

        unmockkObject(Rokt)
    }

    @Test
    fun test_prepareFinalAttributes_preserves_string_values() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        userAttributes["string_value"] = "test_string"
        userAttributes["string_with_numbers"] = "123abc456"
        userAttributes["empty_string"] = ""
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))

        // Act
        roktKit.selectPlacements(
            viewName = "test_view",
            attributes = emptyMap(),
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured
        assertEquals("test_string", capturedAttributes["string_value"])
        assertEquals("123abc456", capturedAttributes["string_with_numbers"])
        assertEquals("", capturedAttributes["empty_string"])

        unmockkObject(Rokt)
    }

    @Test
    fun test_prepareFinalAttributes_handlesSameKeysInAttributesAndUserAttributes() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        userAttributes["attr_non_null_string"] = "value"
        userAttributes["attr_null"] = null
        userAttributes["attr_non_string"] = 123
        userAttributes["user_key"] = "1231545"
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)
        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("ShouldFilter").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("ShouldFilter_key_2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(filteredKey2, 0)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }
        val json = JSONObject()
        json.put("ua", jsonObject)
        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))
        val inputAttributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
            "user_key" to "2223333",
            "ShouldFilter" to "testData",
        )
        // Act
        roktKit.selectPlacements(
            viewName = "test",
            attributes = inputAttributes,
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured

        assertEquals(7, capturedAttributes.size)
        assertEquals("value", capturedAttributes["attr_non_null_string"])
        assertEquals("123", capturedAttributes["attr_non_string"])
        assertEquals("2223333", capturedAttributes["user_key"])
        assertEquals("value1", capturedAttributes["key1"])
        assertEquals("value2", capturedAttributes["key2"])
        assertEquals("value3", capturedAttributes["key3"])
        assertEquals("12345", capturedAttributes["mpid"])

        assertFalse(capturedAttributes.containsKey("ShouldFilter"))
        assertFalse(capturedAttributes.containsKey("ShouldFilter_key_2"))
        unmockkObject(Rokt)
    }

    @Test
    fun test_prepareFinalAttributes_includesAttributeWhenFilterValueIsTrue() {
        // Arrange
        mockkObject(Rokt)
        val capturedAttributesSlot = slot<Map<String, String>>()
        every {
            Rokt.execute(
                any<String>(),
                capture(capturedAttributesSlot),
                any<Rokt.RoktCallback>(),
                null,
                null,
                null,
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)

        val userAttributes = HashMap<String, Any?>()
        userAttributes["attr_non_null_string"] = "value"
        userAttributes["attr_null"] = null
        userAttributes["attr_non_string"] = 123
        userAttributes["user_key"] = "1231545"
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)
        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("ShouldFilter").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("ShouldFilter_key_2").toString()
            jsonObject.put(filteredKey, 1)
            jsonObject.put(filteredKey2, 0)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }
        val json = JSONObject()
        json.put("ua", jsonObject)
        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))
        val inputAttributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
            "user_key" to "2223333",
            "ShouldFilter" to "testData",
        )
        // Act
        roktKit.selectPlacements(
            viewName = "test",
            attributes = inputAttributes,
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        // Assert
        val capturedAttributes = capturedAttributesSlot.captured

        assertEquals(8, capturedAttributes.size)
        assertEquals("value", capturedAttributes["attr_non_null_string"])
        assertEquals("123", capturedAttributes["attr_non_string"])
        assertEquals("2223333", capturedAttributes["user_key"])
        assertEquals("value1", capturedAttributes["key1"])
        assertEquals("value2", capturedAttributes["key2"])
        assertEquals("value3", capturedAttributes["key3"])
        assertEquals("12345", capturedAttributes["mpid"])

        assertTrue(capturedAttributes.containsKey("ShouldFilter"))
        assertFalse(capturedAttributes.containsKey("ShouldFilter_key_2"))
        unmockkObject(Rokt)
    }

    private inner class TestKitManager :
        KitManagerImpl(context, null, TestCoreCallbacks(), mock(MParticleOptions::class.java)) {
        var attributes = HashMap<String, String>()
        var result: AttributionResult? = null
        private var error: AttributionError? = null
        override fun getIntegrationAttributes(kitIntegration: KitIntegration): Map<String, String> = attributes

        override fun setIntegrationAttributes(
            kitIntegration: KitIntegration,
            integrationAttributes: Map<String, String>,
        ) {
            attributes = integrationAttributes as HashMap<String, String>
        }

        override fun onResult(result: AttributionResult) {
            this.result = result
        }

        override fun onError(error: AttributionError) {
            this.error = error
        }
    }

    private inner class TestKitConfiguration : KitConfiguration() {
        override fun getKitId(): Int = TEST_KIT_ID
    }

    private inner class TestMParticle : MParticle() {
        override fun Identity(): IdentityApi = mock(IdentityApi::class.java)
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_IS_Null() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(3, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_Contain_value() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(4, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_And_attributes_contains_same_key() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
            "email" to "abc@gmail.com",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(4, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
        assertEquals(
            mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "email" to "TestEmail@gamil.com",
            ),
            result,
        )
    }

    @Test
    fun testAddIdentityAttributes_bothNull() {
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, null, null) as Map<String, String>
        assertTrue(result.isEmpty())
    }

    @Test
    fun testAddIdentityAttributes_nullAttributes_validUser() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, null, mockFilterUser) as Map<String, String>
        assertEquals(1, result.size)
        assertEquals(mapOf("email" to "TestEmail@gamil.com"), result)
    }

    @Test
    fun testAddIdentityAttributes_nullUser_returnsSameAttributes() {
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, null) as Map<String, String>
        assertEquals(3, result.size)
        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_Contain_other() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        userIdentities.put(IdentityType.Other, "hashedEmail@123.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(5, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
        assertTrue(result.containsKey("other"))
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_Other_map_To_Identity() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        userIdentities.put(IdentityType.Other, "hashedEmail@123.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val hashedField = RoktKit::class.java.getDeclaredField("hashedEmailUserIdentityType")
        hashedField.isAccessible = true
        hashedField.set(roktKit, "Other")
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(5, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
        assertTrue(result.containsKey("other"))
    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_Unknown_map_To_Identity() {
        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email, "TestEmail@gamil.com")
        userIdentities.put(IdentityType.Other, "hashedEmail@123.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val hashedField = RoktKit::class.java.getDeclaredField("hashedEmailUserIdentityType")
        hashedField.isAccessible = true
        hashedField.set(roktKit, "Unknown")
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(5, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
        assertTrue(result.containsKey("other"))
    }

    @Test
    fun testSetSdkWrapper_correctlySetsRoktFramework() {
        mockkObject(Rokt)
        every { Rokt.setFrameworkType(any()) } just runs

        roktKit.setWrapperSdkVersion(WrapperSdkVersion(WrapperSdk.WrapperFlutter, "1.0.0"))
        roktKit.setWrapperSdkVersion(WrapperSdkVersion(WrapperSdk.WrapperSdkReactNative, "1.0.0"))
        roktKit.setWrapperSdkVersion(WrapperSdkVersion(WrapperSdk.WrapperSdkCordova, "1.0.0"))
        roktKit.setWrapperSdkVersion(WrapperSdkVersion(WrapperSdk.WrapperNone, "1.0.0"))
        roktKit.setWrapperSdkVersion(WrapperSdkVersion(WrapperSdk.WrapperXamarin, "1.0.0"))

        verifyOrder {
            Rokt.setFrameworkType(Rokt.SdkFrameworkType.Flutter)
            Rokt.setFrameworkType(Rokt.SdkFrameworkType.ReactNative)
            Rokt.setFrameworkType(Rokt.SdkFrameworkType.Cordova)
            Rokt.setFrameworkType(Rokt.SdkFrameworkType.Android)
            Rokt.setFrameworkType(Rokt.SdkFrameworkType.Android)
        }

        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_ShowLoadingIndicator() = runTest {
        mockkObject(Rokt)
        val roktEvent = RoktEvent.ShowLoadingIndicator
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.ShowLoadingIndicator)
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_HideLoadingIndicator() = runTest {
        mockkObject(Rokt)
        val roktEvent = RoktEvent.HideLoadingIndicator
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.HideLoadingIndicator)
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_FirstPositiveEngagement() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-123"
        val roktEvent = RoktEvent.FirstPositiveEngagement(
            placementId,
            object : FulfillmentAttributes {
                override fun sendAttributes(attributes: Map<String, String>) {
                }
            },
        )
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.FirstPositiveEngagement(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PositiveEngagement() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-456"
        val roktEvent = RoktEvent.PositiveEngagement(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PositiveEngagement(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_OfferEngagement() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-789"
        val roktEvent = RoktEvent.OfferEngagement(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.OfferEngagement(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_OpenUrl() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-url"
        val url = "https://example.com"
        val roktEvent = RoktEvent.OpenUrl(placementId, url)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.OpenUrl(placementId, url))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementClosed() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-closed"
        val roktEvent = RoktEvent.PlacementClosed(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementClosed(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementCompleted() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-completed"
        val roktEvent = RoktEvent.PlacementCompleted(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementCompleted(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementFailure() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-failure"
        val roktEvent = RoktEvent.PlacementFailure(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementFailure(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementInteractive() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-interactive"
        val roktEvent = RoktEvent.PlacementInteractive(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementInteractive(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementReady() = runTest {
        mockkObject(Rokt)
        val placementId = "test-placement-ready"
        val roktEvent = RoktEvent.PlacementReady(placementId)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementReady(placementId))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_InitComplete() = runTest {
        mockkObject(Rokt)
        val success = true
        val roktEvent = RoktEvent.InitComplete(success)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.InitComplete(success))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_CartItemInstantPurchase() = runTest {
        mockkObject(Rokt)
        val roktEvent = RoktEvent.CartItemInstantPurchase(
            placementId = "test-placement-purchase",
            cartItemId = "cart-item-123",
            catalogItemId = "catalog-item-456",
            currency = "USD",
            description = "Test product description",
            linkedProductId = "linked-product-789",
            totalPrice = 99.99,
            quantity = 2,
            unitPrice = 49.99,
        )
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(
            result,
            com.mparticle.RoktEvent.CartItemInstantPurchase(
                placementId = "test-placement-purchase",
                cartItemId = "cart-item-123",
                catalogItemId = "catalog-item-456",
                currency = "USD",
                description = "Test product description",
                linkedProductId = "linked-product-789",
                totalPrice = 99.99,
                quantity = 2,
                unitPrice = 49.99,
            ),
        )
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_PlacementFailureWithNullId() = runTest {
        mockkObject(Rokt)
        val roktEvent = RoktEvent.PlacementFailure(null)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.PlacementFailure(null))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_InitCompleteWithFailure() = runTest {
        mockkObject(Rokt)
        val success = false
        val roktEvent = RoktEvent.InitComplete(success)
        every { Rokt.events(any()) } returns flowOf(roktEvent)

        val result = roktKit.events("").first()

        assertEquals(result, com.mparticle.RoktEvent.InitComplete(success))
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_MultipleEventsInFlow() = runTest {
        mockkObject(Rokt)
        val events = listOf(
            RoktEvent.ShowLoadingIndicator,
            RoktEvent.HideLoadingIndicator,
            RoktEvent.InitComplete(true),
        )
        every { Rokt.events(any()) } returns flowOf(*events.toTypedArray())

        val results = roktKit.events("").toList()

        assertEquals(3, results.size)
        assertEquals(com.mparticle.RoktEvent.ShowLoadingIndicator, results[0])
        assertEquals(com.mparticle.RoktEvent.HideLoadingIndicator, results[1])
        assertEquals(com.mparticle.RoktEvent.InitComplete(true), results[2])

        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_EmptyFlow() = runTest {
        mockkObject(Rokt)
        every { Rokt.events(any()) } returns flowOf()

        val results = roktKit.events("").toList()

        assertTrue(results.isEmpty())
        unmockkObject(Rokt)
    }

    @Test
    fun testRoktEventMapping_WithDifferentIdentifiers() = runTest {
        mockkObject(Rokt)
        val identifier1 = "test-identifier-1"
        val identifier2 = "test-identifier-2"

        every { Rokt.events(identifier1) } returns flowOf(RoktEvent.ShowLoadingIndicator)
        every { Rokt.events(identifier2) } returns flowOf(RoktEvent.HideLoadingIndicator)

        val result1 = roktKit.events(identifier1).first()
        val result2 = roktKit.events(identifier2).first()

        assertEquals(com.mparticle.RoktEvent.ShowLoadingIndicator, result1)
        assertEquals(com.mparticle.RoktEvent.HideLoadingIndicator, result2)

        verify { Rokt.events(identifier1) }
        verify { Rokt.events(identifier2) }

        unmockkObject(Rokt)
    }

    @Test
    fun testVerifyHashedEmail_removes_when_emailsha256_is_present() {
        val attributes = mutableMapOf(
            "email" to "user@example.com",
            "emailsha256" to "hashed_email_value",
            "other" to "Test",
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "verifyHashedEmail",
            MutableMap::class.java,
        )
        method.isAccessible = true
        method.invoke(roktKit, attributes)

        assertFalse(attributes.containsKey("email"))
        assertEquals("hashed_email_value", attributes["emailsha256"])
        assertEquals("Test", attributes["other"])
    }

    @Test
    fun testVerifyHashedEmail_removes_when_neither_emailsha256_nor_other_is_present() {
        val attributes = mutableMapOf(
            "email" to "user@example.com",
        )

        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "verifyHashedEmail",
            MutableMap::class.java,
        )
        method.isAccessible = true
        method.invoke(roktKit, attributes)

        assertEquals("user@example.com", attributes["email"])
        assertFalse(attributes.containsKey("emailsha256"))
    }

    @Test
    fun testFilterAttributes() {
        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "ShouldFilter" to "shoudl_filter_value",
            "ShouldFilter_key_2" to "ShouldFilter_value",
            "allowed_key" to "allowed_value",
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java,
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("ShouldFilter").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("ShouldFilter_key_2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(filteredKey2, 0)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("ua", jsonObject)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        // Verify the results
        assertEquals(1, result.size)

        assertFalse(result.containsKey("ShouldFilter"))
        assertFalse(result.containsKey("ShouldFilter_key_2"))
        assertTrue(result.containsKey("allowed_key"))
        assertEquals("allowed_value", result["allowed_key"])
    }

    @Test
    fun testFilterAttributes_When_kitConfig_User_Attributes_Filtering_IS_NULL() {
        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "filtered_key" to "filtered_value",
            "allowed_key" to "allowed_value",
            "another_allowed_key" to "another_allowed_value",
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java,
        )
        method.isAccessible = true

        val json = JSONObject()

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        assertEquals(3, result.size)

        assertTrue(result.containsKey("allowed_key"))
        assertTrue(result.containsKey("filtered_key"))
        assertTrue(result.containsKey("another_allowed_key"))
        assertEquals("another_allowed_value", result["another_allowed_key"])
    }

    @Test
    fun testFilterAttributes_When_Attributes_IS_Empty() {
        // Create test attributes
        val emptyAttributes: Map<String, String> = emptyMap()

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java,
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("filtered_key").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("allowed_key").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(filteredKey2, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("aaa", jsonObject)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, emptyAttributes, roktKit.configuration) as Map<String, String>

        assertEquals(0, result.size)
    }

    @Test
    fun testFilterAttributes_Allows_Attributes_To_Be_Set_When_When_Filtered_User_Attributes_Differ() {
        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "filtered_key" to "filtered_value",
            "allowed_key" to "allowed_value",
            "another_allowed_key" to "another_allowed_value",
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java,
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey: String = KitUtils.hashForFiltering("Test1").toString()
            val filteredKey2: String = KitUtils.hashForFiltering("Test2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(filteredKey2, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("us", jsonObject)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        // Verify the results
        assertEquals(3, result.size)

        assertTrue(result.containsKey("filtered_key"))
        assertTrue(result.containsKey("allowed_key"))
        assertTrue(result.containsKey("another_allowed_key"))
        assertEquals("another_allowed_value", result["another_allowed_key"])
        assertEquals("filtered_value", result["filtered_key"])
        assertEquals("allowed_value", result["allowed_key"])
    }

    internal inner class TestCoreCallbacks : CoreCallbacks {
        override fun isBackgrounded(): Boolean = false
        override fun getUserBucket(): Int = 0
        override fun isEnabled(): Boolean = false
        override fun setIntegrationAttributes(kitId: Int, integrationAttributes: Map<String, String>) {}
        override fun getIntegrationAttributes(kitId: Int): Map<String, String>? = null
        override fun getCurrentActivity(): WeakReference<Activity>? = null
        override fun getLatestKitConfiguration(): JSONArray? = null
        override fun getDataplanOptions(): DataplanOptions? = null
        override fun isPushEnabled(): Boolean = false
        override fun getPushSenderId(): String? = null
        override fun getPushInstanceId(): String? = null
        override fun getLaunchUri(): Uri? = null
        override fun getLaunchAction(): String? = null
        override fun getKitListener(): KitListener = object : KitListener {
            override fun kitFound(kitId: Int) {}
            override fun kitConfigReceived(kitId: Int, configuration: String?) {}
            override fun kitExcluded(kitId: Int, reason: String?) {}
            override fun kitStarted(kitId: Int) {}
            override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {
            }

            override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {
            }
        }
    }

    companion object {
        private const val TEST_APPLICATION_ID = "app-abcdef1234567890"
        private const val TEST_ATTRIBUTION_TOKEN = "srctok-abcdef1234567890"
        private const val TEST_DEEP_LINK = "https://www.example.com/product/abc123"
        private const val TEST_KIT_ID = 0x01

        /*
         * Test Helpers
         */
        @Throws(Exception::class)
        private fun setTestSdkVersion(sdkVersion: Int) {
            setFinalStatic(VERSION::class.java.getField("SDK_INT"), sdkVersion)
        }

        @Throws(Exception::class)
        private fun setFinalStatic(field: Field, newValue: Int) {
            field.isAccessible = true
            val getDeclaredFields0 = Class::class.java.getDeclaredMethod(
                "getDeclaredFields0",
                Boolean::class.javaPrimitiveType,
            )
            getDeclaredFields0.isAccessible = true
            val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
            var modifiersField: Field? = null
            for (each in fields) {
                if ("modifiers" == each.name) {
                    modifiersField = each
                    break
                }
            }
            modifiersField!!.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[null] = newValue
        }
    }

    @Test
    fun test_prepareFinalAttributes_logs_attributes_event() {
        val mParticleMock = MParticle.getInstance()!!
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "prepareFinalAttributes",
            FilteredMParticleUser::class.java,
            Map::class.java,
        )
        method.isAccessible = true

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(9876L)
        val userAttributes = hashMapOf<String, Any?>("user_key" to "user_val")
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(userAttributes)

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))
        val attributes: Map<String, String> = mapOf("attr1" to "val1")

        method.invoke(roktKit, mockFilterUser, attributes)

        val eventCaptor = ArgumentCaptor.forClass(MPEvent::class.java)
        Mockito.verify(mParticleMock).logEvent(eventCaptor.capture())

        val loggedEvent = eventCaptor.value
        assertEquals("selectPlacements", loggedEvent.eventName)
        assertEquals(MParticle.EventType.Other, loggedEvent.eventType)
        assertEquals(
            mapOf(
                "user_key" to "user_val",
                "attr1" to "val1",
                "mpid" to "9876",
            ),
            loggedEvent.customAttributes,
        )
    }

    @Test
    fun test_execute_logsMPEventWhenMParticleInstanceIsNull() {
        // Arrange
        mockkObject(Rokt)
        every {
            Rokt.execute(
                any<String>(),
                any<Map<String, String>>(),
                any<Rokt.RoktCallback>(),
                any(),
                any(),
                any(),
            )
        } just runs

        val mockFilterUser = mock(FilteredMParticleUser::class.java)
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(HashMap())
        Mockito.`when`(mockFilterUser.id).thenReturn(12345L)
        Mockito.`when`(mockFilterUser.userAttributes).thenReturn(HashMap<String, Any?>())

        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs", JSONObject()))

        val testAttributes = mapOf("key1" to "value1")

        // Set MParticle instance to null
        MParticle.setInstance(null)

        roktKit.selectPlacements(
            viewName = "test_view",
            attributes = testAttributes,
            mpRoktEventCallback = null,
            placeHolders = null,
            fontTypefaces = null,
            filterUser = mockFilterUser,
            mpRoktConfig = null,
            placementOptions = null,
        )

        verify(exactly = 1) {
            Rokt.execute(
                any<String>(),
                any<Map<String, String>>(),
                any<Rokt.RoktCallback>(),
                any(),
                any(),
                any(),
            )
        }

        unmockkObject(Rokt)
    }

    @Test
    fun testSetSessionId_delegatesToRoktSdk() {
        mockkObject(Rokt)
        every { Rokt.setSessionId(any()) } just runs

        roktKit.setSessionId("test-session-id")

        verify { Rokt.setSessionId("test-session-id") }
        unmockkObject(Rokt)
    }

    @Test
    fun testGetSessionId_returnsValueFromRoktSdk() {
        mockkObject(Rokt)
        every { Rokt.getSessionId() } returns "expected-session-id"

        val result = roktKit.getSessionId()

        assertEquals("expected-session-id", result)
        verify { Rokt.getSessionId() }
        unmockkObject(Rokt)
    }

    private var emptyCoreCallbacks: CoreCallbacks = object : CoreCallbacks {
        var activity = Activity()
        override fun isBackgrounded(): Boolean = false

        override fun getUserBucket(): Int = 0

        override fun isEnabled(): Boolean = false

        override fun setIntegrationAttributes(kitId: Int, integrationAttributes: Map<String, String>) {}

        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null

        override fun getCurrentActivity(): WeakReference<Activity> = WeakReference(activity)

        override fun getLatestKitConfiguration(): JSONArray? = null

        override fun getDataplanOptions(): DataplanOptions? = null

        override fun isPushEnabled(): Boolean = false

        override fun getPushSenderId(): String? = null

        override fun getPushInstanceId(): String? = null

        override fun getLaunchUri(): Uri? = null

        override fun getLaunchAction(): String? = null

        override fun getKitListener(): KitListener = object : KitListener {
            override fun kitFound(kitId: Int) {}
            override fun kitConfigReceived(kitId: Int, configuration: String?) {}
            override fun kitExcluded(kitId: Int, reason: String?) {}
            override fun kitStarted(kitId: Int) {}
            override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {}
            override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {}
        }
    }

    @Test
    fun testLogLevelMapping_VERBOSE() {
        // Arrange
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true

        // Act & Assert
        assertEquals(RoktLogLevel.VERBOSE, method.invoke(roktKit, MParticle.LogLevel.VERBOSE))
    }

    @Test
    fun testLogLevelMapping_DEBUG() {
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true
        assertEquals(RoktLogLevel.DEBUG, method.invoke(roktKit, MParticle.LogLevel.DEBUG))
    }

    @Test
    fun testLogLevelMapping_INFO() {
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true
        assertEquals(RoktLogLevel.INFO, method.invoke(roktKit, MParticle.LogLevel.INFO))
    }

    @Test
    fun testLogLevelMapping_WARNING() {
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true
        assertEquals(RoktLogLevel.WARNING, method.invoke(roktKit, MParticle.LogLevel.WARNING))
    }

    @Test
    fun testLogLevelMapping_ERROR() {
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true
        assertEquals(RoktLogLevel.ERROR, method.invoke(roktKit, MParticle.LogLevel.ERROR))
    }

    @Test
    fun testLogLevelMapping_NONE() {
        val method = RoktKit::class.java.getDeclaredMethod(
            "toRoktLogLevel",
            MParticle.LogLevel::class.java,
        )
        method.isAccessible = true
        assertEquals(RoktLogLevel.NONE, method.invoke(roktKit, MParticle.LogLevel.NONE))
    }
}
