package com.mparticle.kits

import android.app.Activity
import android.location.Location
import android.net.Uri
import android.os.Looper
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockCoreCallbacks
import com.mparticle.mock.MockKitConfiguration
import com.mparticle.mock.MockKitIntegrationFactory
import com.mparticle.mock.MockKitManagerImpl
import com.mparticle.mock.MockMParticle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class KitManagerTest {
    private lateinit var manager: KitManagerImpl

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val mockMp: MParticle = MockMParticle()
        MParticle.setInstance(mockMp)
        manager = MockKitManagerImpl(MockContext(), null, MockCoreCallbacks())
        Assert.assertNotNull(manager.providers)
        val mockKitFactory =
            MockKitIntegrationFactory(MParticleOptions.builder(MockContext()).build())
        manager.setKitFactory(mockKitFactory)
    }

    @Test
    @PrepareForTest(Looper::class)
    @Throws(Exception::class)
    fun testUpdateKits() {
        PowerMockito.mockStatic(Looper::class.java)
        val looper = PowerMockito.mock(Looper::class.java)
        Mockito.`when`(Looper.myLooper()).thenReturn(looper)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)
        val configJson = JSONObject(TestConstants.SAMPLE_EK_CONFIG)
        manager.updateKits(null)
        Assert.assertNotNull(manager.providers)
        manager.updateKits(JSONArray())
        Assert.assertNotNull(manager.providers)
        val array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS)
        manager.mKitIntegrationFactory.supportedKits.putAll(
            hashMapOf<Int, Class<*>>(
                Pair(37, KitIntegration::class.java),
                Pair(56, KitIntegration::class.java),
                Pair(64, KitIntegration::class.java),
                Pair(68, KitIntegration::class.java)
            )
        )
        Assert.assertNotNull(array)
        manager.updateKits(array)
        val providers = manager.providers
        if (array != null) {
            Assert.assertEquals(array.length().toLong(), providers.size.toLong())
        }
        Assert.assertNotNull(providers[37])
        Assert.assertNotNull(providers[56])
        Assert.assertNotNull(providers[64])
        Assert.assertNotNull(providers[68])
        manager.updateKits(JSONArray())
        Assert.assertEquals(0, providers.size.toLong())
        manager.updateKits(configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS))
        Assert.assertEquals(4, providers.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testLogEvent() {
        manager.logEvent(MPEvent.Builder("test name", MParticle.EventType.Location).build())
    }

    @Test
    @Throws(Exception::class)
    fun testLogScreen() {
        manager.logScreen(null)
        manager.logScreen(MPEvent.Builder("name").build())
    }

    @Test
    @Throws(Exception::class)
    fun testSetLocation() {
        manager.setLocation(null)
        manager.setLocation(Location("passive"))
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttribute() {
        manager.setUserAttribute("key", "value", 1)
        manager.setUserAttribute("key", null, 1)
        manager.setUserAttribute(null, null, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveUserAttribute() {
        manager.removeUserAttribute(null, 1)
        manager.removeUserAttribute("", 1)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserIdentity() {
        manager.setUserIdentity("", null)
        manager.setUserIdentity("", MParticle.IdentityType.CustomerId)
        manager.setUserIdentity(null, MParticle.IdentityType.CustomerId)
    }

    @Test
    @Throws(Exception::class)
    fun testLogout() {
        manager.logout()
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityStarted() {
        manager.onActivityStarted(null)
        manager.onActivityStarted(Mockito.mock(Activity::class.java))
    }

    @Test
    @PrepareForTest(Looper::class)
    @Throws(Exception::class)
    fun testGetActiveModuleIds() {
        PowerMockito.mockStatic(Looper::class.java)
        val looper = PowerMockito.mock(Looper::class.java)
        Mockito.`when`(Looper.myLooper()).thenReturn(looper)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)
        val configJson = JSONObject(TestConstants.SAMPLE_EK_CONFIG)
        val array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS)
        manager.mKitIntegrationFactory.supportedKits.putAll(
            hashMapOf<Int, Class<*>>(
                Pair(37, KitIntegration::class.java),
                Pair(56, KitIntegration::class.java),
                Pair(64, KitIntegration::class.java),
                Pair(68, KitIntegration::class.java)
            )
        )
        manager.updateKits(array)
        val kitStatus = manager.kitStatus
        val testIds = arrayOf("56", "64", "37", "68")
        val idList = listOf(*testIds)
        for ((key, value) in kitStatus) {
            if (value == KitStatus.ACTIVE) {
                Assert.assertTrue(idList.contains(key.toString()))
            } else {
                Assert.assertFalse(idList.contains(key.toString()))
            }
        }
    }

    @Test
    @PrepareForTest(Looper::class)
    @Throws(Exception::class)
    fun testGetSurveyUrl() {
        PowerMockito.mockStatic(Looper::class.java)
        val looper = PowerMockito.mock(Looper::class.java)
        Mockito.`when`(Looper.myLooper()).thenReturn(looper)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)
        val configJson = JSONObject(TestConstants.SAMPLE_EK_CONFIG)
        val array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS)
        manager.updateKits(array)
        val mockForesee = Mockito.mock(KitIntegration::class.java)
        val config = JSONObject()
        config.put(KitConfiguration.KEY_ID, 100)
        val mockConfig = MockKitConfiguration().parseConfiguration(config)
        Mockito.`when`(mockForesee.configuration).thenReturn(mockConfig)
        val uri = Mockito.mock(Uri::class.java)
        Mockito.`when`(
            mockForesee.getSurveyUrl(
                Mockito.any(
                    MutableMap::class.java
                ) as MutableMap<String, String>?,
                Mockito.any(
                    MutableMap::class.java
                ) as MutableMap<String, MutableList<String>>?
            )
        ).thenReturn(uri)
        manager.providers[MParticle.ServiceProviders.FORESEE_ID] = (mockForesee as KitIntegration)
        Assert.assertNull(manager.getSurveyUrl(56, HashMap(), HashMap()))
        Assert.assertTrue(
            manager.getSurveyUrl(
                MParticle.ServiceProviders.FORESEE_ID,
                HashMap(),
                HashMap()
            ) === uri
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetContext() {
        Assert.assertNotNull(manager.context)
    }
}
