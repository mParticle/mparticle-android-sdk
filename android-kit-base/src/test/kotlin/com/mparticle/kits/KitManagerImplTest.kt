package com.mparticle.kits

import android.content.Context
import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MParticleTask
import com.mparticle.WrapperSdk
import com.mparticle.WrapperSdkVersion
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiResult
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.MPUtility
import com.mparticle.internal.SideloadedKit
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockKitConfiguration
import com.mparticle.mock.MockKitManagerImpl
import com.mparticle.mock.MockMParticle
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.withSettings
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Arrays
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class, SystemClock::class, MPUtility::class)
class KitManagerImplTest {
    var mparticle: MParticle? = null
    var mockIdentity: IdentityApi? = null

    @Before
    fun before() {
        PowerMockito.mockStatic(Looper::class.java)
        PowerMockito.mockStatic(SystemClock::class.java)
        mockIdentity = Mockito.mock(IdentityApi::class.java)
        val instance = MockMParticle()
        instance.setIdentityApi(mockIdentity)
        MParticle.setInstance(instance)
    }

    @Test
    fun testSetKitFactory() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        Assert.assertNotNull(manager.mKitIntegrationFactory)
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Assert.assertEquals(factory, manager.mKitIntegrationFactory)
    }

    private fun createKitsMap(
        ids: List<Int>,
        type: Class<*> = KitIntegration::class.java
    ): HashMap<Int, Class<*>> {
        val map = hashMapOf<Int, Class<*>>()
        ids.forEach { map.put(it, type) }
        return map
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKit() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        val state = ConsentState.builder().build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{\"id\":1}"))
        kitConfiguration.put(JSONObject("{\"id\":2}"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)

        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(2, manager.providers.size)
        TestCase.assertEquals(mockKit, manager.providers.values.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun testShouldNotEnableKitBasedOnConsent() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(1, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKitBasedOnConsent() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(2, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldDisableActiveKitBasedOnConsent() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(2, manager.providers.size)
        Mockito.`when`(mockUser.consentState).thenReturn(ConsentState.builder().build())
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(1, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKitBasedOnActiveUser() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(true)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(3, manager.providers.size)
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class)
    fun testShouldNotEnableKitBasedOnActiveUser() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(false)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        TestCase.assertEquals(1, manager.providers.size)
        TestCase.assertTrue(manager.isKitActive(2))
        TestCase.assertFalse(manager.isKitActive(1))
        TestCase.assertFalse(manager.isKitActive(3))
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class, InterruptedException::class)
    fun testShouldEnableDisabledKitBasedOnActiveUser() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(false)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(0, manager.providers.size)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(true)
        Mockito.`when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        manager.onUserIdentified(mockUser, null)
        TestCase.assertEquals(3, manager.providers.size)
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class)
    fun testShouldDisableEnabledKitBasedOnActiveUser() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(true)
        val mockCoreCallbacks = Mockito.mock(
            CoreCallbacks::class.java
        )
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state = ConsentState.builder()
            .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
            .build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"))
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(3, manager.providers.size)
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(false)
        Mockito.`when`(mockCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        manager.onUserIdentified(mockUser, null)
        TestCase.assertEquals(0, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUserAttributesReceived() {
        MParticle.setInstance(MockMParticle())
        val manager: KitManagerImpl = MockKitManagerImpl()
        val integration = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        val integration2 = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        Mockito.`when`((integration as AttributeListener).supportsAttributeLists()).thenReturn(true)
        Mockito.`when`((integration2 as AttributeListener).supportsAttributeLists())
            .thenReturn(false)
        Mockito.`when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        Mockito.`when`(integration2.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        manager.providers[5] = integration
        manager.providers[6] = integration2
        val userAttributeSingles: MutableMap<String, String> = HashMap()
        userAttributeSingles["test"] = "whatever"
        userAttributeSingles["test 2"] = "whatever 2"
        val userAttributeLists: MutableMap<String, List<String>> = HashMap()
        val attributeList: MutableList<String> = LinkedList()
        attributeList.add("1")
        attributeList.add("2")
        attributeList.add("3")
        userAttributeLists["test 3"] = attributeList
        manager.onUserAttributesReceived(userAttributeSingles, userAttributeLists, 1L)
        Mockito.verify(integration as AttributeListener, Mockito.times(1))
            .setAllUserAttributes(userAttributeSingles, userAttributeLists)
        val userAttributesCombined: MutableMap<String, String> = HashMap()
        userAttributesCombined["test"] = "whatever"
        userAttributesCombined["test 2"] = "whatever 2"
        userAttributesCombined["test 3"] = "1,2,3"
        val clearedOutList: Map<String, List<String>> = HashMap()
        Mockito.verify(integration2 as AttributeListener, Mockito.times(1))
            .setAllUserAttributes(userAttributesCombined, clearedOutList)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttributeList() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        val integration = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        val integration2 = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        Mockito.`when`((integration as AttributeListener).supportsAttributeLists()).thenReturn(true)
        Mockito.`when`((integration2 as AttributeListener).supportsAttributeLists())
            .thenReturn(false)
        Mockito.`when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        Mockito.`when`(integration2.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        manager.providers[5] = integration
        manager.providers[6] = integration2
        val attributeList: MutableList<String> = LinkedList()
        attributeList.add("1")
        attributeList.add("2")
        attributeList.add("3")
        manager.setUserAttributeList("test key", attributeList, 1)
        Mockito.verify(integration as AttributeListener, Mockito.times(1))
            .setUserAttributeList("test key", attributeList)
        Mockito.verify(integration2 as AttributeListener, Mockito.times(1))
            .setUserAttribute("test key", "1,2,3")
    }

    @Test
    @Throws(JSONException::class)
    fun testLogEventCalledOne() {
        val manager = KitManagerEventCounter()
        val integration = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        val integration2 = Mockito.mock(
            KitIntegration::class.java,
            Mockito.withSettings().extraInterfaces(AttributeListener::class.java)
        )
        Mockito.`when`((integration as AttributeListener).supportsAttributeLists()).thenReturn(true)
        Mockito.`when`((integration2 as AttributeListener).supportsAttributeLists())
            .thenReturn(false)
        Mockito.`when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        Mockito.`when`(integration2.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        (manager as KitManagerImpl).providers[5] = integration
        (manager as KitManagerImpl).providers[6] = integration2
        val mpEvent = TestingUtils().randomMPEventSimple
        manager.logEvent(mpEvent)
        TestCase.assertEquals(1, manager.logBaseEventCalled)
        TestCase.assertEquals(1, manager.logMPEventCalled)
        TestCase.assertEquals(0, manager.logCommerceEventCalled)
        manager.logBaseEventCalled = 0
        manager.logMPEventCalled = 0
        val commerceEvent =
            CommerceEvent.Builder(Product.CHECKOUT, Product.Builder("name", "sku", 100.0).build())
                .build()
        manager.logEvent(commerceEvent)
        TestCase.assertEquals(1, manager.logBaseEventCalled)
        TestCase.assertEquals(0, manager.logMPEventCalled)
        TestCase.assertEquals(1, manager.logCommerceEventCalled)
    }

    @Test
    fun testMParticleConfigureKitsFromOptions() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000
        val configJSONObj = JSONObject().apply { put("id", kitId) }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.configureKits(mutableListOf(mockedKitConfig))
        Assert.assertEquals(1, manager.providers.size)
        Assert.assertTrue(manager.providers.containsKey(kitId))
    }

    @Test
    fun testMParticleUpdateEmptyConfigKitWithKitOptions() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000
        val configJSONObj = JSONObject().apply { put("id", kitId) }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        Mockito.`when`(factory.getSupportedKits())
            .thenReturn(createKitsMap(listOf(kitId), MPSideloadedKit::class.java).keys)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.configureKits(mutableListOf(mockedKitConfig))
        Assert.assertEquals(1, manager.providers.size)
        Assert.assertTrue(manager.providers.containsKey(kitId))

        manager.updateKits(JSONArray())
        Assert.assertEquals(0, manager.providers.size)
        Assert.assertFalse(manager.providers.containsKey(kitId))
    }

    @Test
    fun testSideloadedKitAdded() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        val idOne = 6000000
        val idTwo = 6000001
        val kitConfiguration = JSONArray()
            .apply {
                put(JSONObject().apply { put("id", 1) })
                put(JSONObject().apply { put("id", idOne) })
                put(JSONObject().apply { put("id", idTwo) })
            }
        Mockito.`when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits())
            .thenReturn(createKitsMap(listOf(1, idOne, idTwo), MPSideloadedKit::class.java).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val sideloadedKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(
            Mockito.mock(KitConfiguration::class.java)
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(3, manager.providers.size)
        Assert.assertTrue(manager.providers.containsKey(idOne))
        Assert.assertTrue(manager.providers.containsKey(idOne))
    }

    @Test
    fun shouldFilterKitsFromKnownIntegrations() {
        val options = MParticleOptions.builder(MockContext()).build()
        val filteredKitOptions = MParticleOptions.builder(MockContext())
            .disabledKits(
                Arrays.asList(
                    MParticle.ServiceProviders.ADJUST,
                    MParticle.ServiceProviders.APPBOY,
                    MParticle.ServiceProviders.CLEVERTAP
                )
            )
            .build()

        val filteredKitIntegrationFactory = KitIntegrationFactory(filteredKitOptions)
        val kitIntegrationFactory = KitIntegrationFactory(options)
        val knownIntegrationsField = KitIntegrationFactory::class.java.getDeclaredField("knownIntegrations")
        knownIntegrationsField.isAccessible = true

        val withoutFilterIntegration = knownIntegrationsField.get(kitIntegrationFactory) as Map<*, *>
        val filteredKitIntegrations = knownIntegrationsField.get(filteredKitIntegrationFactory) as Map<*, *>

        val knownKitsSize = withoutFilterIntegration.size
        val filteredKnownKitsSize = filteredKitIntegrations.size
        Assert.assertEquals(knownKitsSize - 3, filteredKnownKitsSize)
        // list of All the kits without Filter
        Assert.assertNotNull(withoutFilterIntegration[MParticle.ServiceProviders.ADJUST])
        Assert.assertNotNull(withoutFilterIntegration[MParticle.ServiceProviders.APPBOY])
        Assert.assertNotNull(withoutFilterIntegration[MParticle.ServiceProviders.CLEVERTAP])

        // Filtered kits; the specified kit should not be present (should be null)
        Assert.assertNull(filteredKitIntegrations[MParticle.ServiceProviders.ADJUST])
        Assert.assertNull(filteredKitIntegrations[MParticle.ServiceProviders.APPBOY])
        Assert.assertNull(filteredKitIntegrations[MParticle.ServiceProviders.CLEVERTAP])
    }

    @Test
    fun shouldNotFilterKitsFromKnownIntegrationsWhenFilterIsEmpty() {
        val options = MParticleOptions.builder(MockContext()).build()
        val filteredKitOptions = MParticleOptions.builder(MockContext())
            .disabledKits(emptyList())
            .build()

        val filteredKitIntegrationFactory = KitIntegrationFactory(filteredKitOptions)
        val kitIntegrationFactory = KitIntegrationFactory(options)
        val knownIntegrationsField = KitIntegrationFactory::class.java.getDeclaredField("knownIntegrations")
        knownIntegrationsField.isAccessible = true

        val knownIntegrations = knownIntegrationsField.get(kitIntegrationFactory) as Map<*, *>
        val filteredKnownIntegrations = knownIntegrationsField.get(filteredKitIntegrationFactory) as Map<*, *>

        val knownKitsSize = knownIntegrations.size
        val filteredKnownKitsSize = filteredKnownIntegrations.size
        Assert.assertEquals(knownKitsSize, filteredKnownKitsSize)
    }

    @Test
    fun shouldIgnoreUnknownKitInFilter() {
        val options = MParticleOptions.builder(MockContext()).build()
        val filteredKitOptions = MParticleOptions.builder(MockContext())
            .disabledKits(listOf(1231, 132132))
            .build()

        val filteredKitIntegrationFactory = KitIntegrationFactory(filteredKitOptions)
        val kitIntegrationFactory = KitIntegrationFactory(options)
        val knownIntegrationsField = KitIntegrationFactory::class.java.getDeclaredField("knownIntegrations")
        knownIntegrationsField.isAccessible = true

        val knownIntegrations = knownIntegrationsField.get(kitIntegrationFactory) as Map<*, *>
        val filteredKnownIntegrations = knownIntegrationsField.get(filteredKitIntegrationFactory) as Map<*, *>

        val knownKitsSize = knownIntegrations.size
        val filteredKnownKitsSize = filteredKnownIntegrations.size
        Assert.assertEquals(knownKitsSize, filteredKnownKitsSize)
    }

    @Test
    fun shouldRetainUnfilteredKits() {
        val filteredKitId = MParticle.ServiceProviders.ADJUST
        val options = MParticleOptions.builder(MockContext())
            .disabledKits(listOf(filteredKitId))
            .build()

        val factory = KitIntegrationFactory(options)

        val field = KitIntegrationFactory::class.java.getDeclaredField("knownIntegrations")
        field.isAccessible = true
        val knownIntegrations = field.get(factory) as Map<*, *>
        Assert.assertNull(knownIntegrations[MParticle.ServiceProviders.ADJUST])
        // Verify that a different kit is still present
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.APPBOY])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.URBAN_AIRSHIP])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.TUNE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.KOCHAVA])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.COMSCORE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.FORESEE_ID])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.BRANCH_METRICS])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.FLURRY])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.LOCALYTICS])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.CRITTERCISM])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.WOOTRIC])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.APPSFLYER])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.APPTENTIVE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.APPTIMIZE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.BUTTON])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.LEANPLUM])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.REVEAL_MOBILE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.RADAR])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.ITERABLE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.SKYHOOK])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.SINGULAR])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.ADOBE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.TAPLYTICS])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.OPTIMIZELY])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.RESPONSYS])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.CLEVERTAP])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.ONETRUST])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE_GA4])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.PILGRIM])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.SWRVE])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.BLUESHIFT])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.NEURA])
        Assert.assertNotNull(knownIntegrations[MParticle.ServiceProviders.ROKT])
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKitOnOptIn() {
        val mockUser = Mockito.mock(MParticleUser::class.java)
        val state = ConsentState.builder().build()
        Mockito.`when`(mockUser.consentState).thenReturn(state)
        Mockito.`when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{\"id\":1}"))
        kitConfiguration.put(JSONObject("{\"id\":2}"))
        Mockito.`when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(mockKit.isDisabled).thenReturn(true)
        Mockito.`when`(mockKit.configuration).thenReturn(
            Mockito.mock(
                KitConfiguration::class.java
            )
        )
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(mockKit)
        manager.setOptOut(true)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(0, manager.providers.size)
        Mockito.`when`(mockKit.isDisabled).thenReturn(false)
        manager.setOptOut(false)
        Assert.assertEquals(2, manager.providers.size)
    }

    @Test
    fun testRokt_non_standard_partner_user_attrs() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
            {"map": "number", "value": "no"},
            {"map": "customerId", "value": "minorcatid"}
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("number", "(123) 456-9898"),
            Pair("customerId", "55555"),
            Pair("country", "US")
        )

        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["no"])
        Assert.assertEquals("55555", attributes["minorcatid"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testExecute_shouldNotModifyAttributes_ifMappedKeysDoNotExist() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
              {"map": "number", "value": "no"},
            {"map": "customerId", "value": "minorcatid"}
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("call", "(123) 456-9898"),
            Pair("postal", "5-45555"),
            Pair("country", "US")
        )

        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)

        Assert.assertEquals("(123) 456-9898", attributes["call"])
        Assert.assertEquals("5-45555", attributes["postal"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testExecute_shouldNotModifyAttributes_ifMapAndValueKeysAreSame() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
              {"map": "number", "value": "no"},
            {"map": "customerId", "value": "minorcatid"}
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("no", "(123) 456-9898"),
            Pair("minorcatid", "5-45555"),
            Pair("country", "US")
        )

        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["no"])
        Assert.assertEquals("5-45555", attributes["minorcatid"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testRokt_non_standard_partner_user_attrs_When_placementAttributes_is_empty() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
           
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("number", "(123) 456-9898"),
            Pair("customerId", "55555"),
            Pair("country", "US")
        )

        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["number"])
        Assert.assertEquals("55555", attributes["customerId"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testConfirmEmail_When_EmailSyncSuccess() {
        var runnable: Runnable = Mockito.mock(Runnable::class.java)
        var user: MParticleUser = Mockito.mock(MParticleUser::class.java)
        val instance = MockMParticle()
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)
        val identityApi = mock(IdentityApi::class.java)
        val oldEmail = "old@example.com"
        val mockTask = mock(MParticleTask::class.java) as MParticleTask<IdentityApiResult>
        `when`(identityApi.identify(any())).thenReturn(mockTask)
        val identities: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities.put(MParticle.IdentityType.Email, oldEmail)
        `when`(user.userIdentities).thenReturn(identities)
        instance.setIdentityApi(identityApi)
        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
           
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val method: Method = KitManagerImpl::class.java.getDeclaredMethod(
            "confirmEmail",
            String::class.java,
            MParticleUser::class.java,
            IdentityApi::class.java,
            Runnable::class.java
        )
        method.isAccessible = true
        val result = method.invoke(manager, "Test@gmail.com", user, identityApi, runnable)
        verify(mockTask).addSuccessListener(any())
    }

    @Test
    fun testConfirmEmail_When_EmailAlreadySynced() {
        var runnable: Runnable = Mockito.mock(Runnable::class.java)
        var user: MParticleUser = Mockito.mock(MParticleUser::class.java)
        val instance = MockMParticle()
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)
        val identityApi = mock(IdentityApi::class.java)
        val oldEmail = "Test@gmail.com"
        val mockTask = mock(MParticleTask::class.java) as MParticleTask<IdentityApiResult>
        `when`(identityApi.identify(any())).thenReturn(mockTask)
        val identities: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities.put(MParticle.IdentityType.Email, oldEmail)
        `when`(user.userIdentities).thenReturn(identities)
        instance.setIdentityApi(identityApi)
        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
           
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val method: Method = KitManagerImpl::class.java.getDeclaredMethod(
            "confirmEmail",
            String::class.java,
            MParticleUser::class.java,
            IdentityApi::class.java,
            Runnable::class.java
        )
        method.isAccessible = true
        val result = method.invoke(manager, "Test@gmail.com", user, identityApi, runnable)
        Mockito.verify(runnable).run()
    }

    @Test
    fun testConfirmEmail_When_mailIsNull() {
        var runnable: Runnable = Mockito.mock(Runnable::class.java)
        var user: MParticleUser = Mockito.mock(MParticleUser::class.java)
        val instance = MockMParticle()
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)
        val identityApi = mock(IdentityApi::class.java)
        val oldEmail = "Test@gmail.com"
        val mockTask = mock(MParticleTask::class.java) as MParticleTask<IdentityApiResult>
        `when`(identityApi.identify(any())).thenReturn(mockTask)
        val identities: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities.put(MParticle.IdentityType.Email, oldEmail)
        `when`(user.userIdentities).thenReturn(identities)
        instance.setIdentityApi(identityApi)
        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
           
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val method: Method = KitManagerImpl::class.java.getDeclaredMethod(
            "confirmEmail",
            String::class.java,
            MParticleUser::class.java,
            IdentityApi::class.java,
            Runnable::class.java
        )
        method.isAccessible = true
        val result = method.invoke(manager, null, user, identityApi, runnable)
        Mockito.verify(runnable).run()
    }

    @Test
    fun testConfirmEmail_When_User_IsNull() {
        var runnable: Runnable = Mockito.mock(Runnable::class.java)
        var user: MParticleUser = Mockito.mock(MParticleUser::class.java)
        val instance = MockMParticle()
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)
        val identityApi = mock(IdentityApi::class.java)
        val oldEmail = "Test@gmail.com"
        val mockTask = mock(MParticleTask::class.java) as MParticleTask<IdentityApiResult>
        `when`(identityApi.identify(any())).thenReturn(mockTask)
        val identities: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities.put(MParticle.IdentityType.Email, oldEmail)
        `when`(user.userIdentities).thenReturn(identities)
        instance.setIdentityApi(identityApi)
        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        [
           
        ]
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val method: Method = KitManagerImpl::class.java.getDeclaredMethod(
            "confirmEmail",
            String::class.java,
            MParticleUser::class.java,
            IdentityApi::class.java,
            Runnable::class.java
        )
        method.isAccessible = true
        val result = method.invoke(manager, null, user, identityApi, runnable)
        Mockito.verify(runnable).run()
    }

    @Test
    fun testRokt_SandboxMode_When_Default_Environment() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        []
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("number", "(123) 456-9898"),
            Pair("customerId", "55555"),
            Pair("country", "US")
        )
        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["number"])
        Assert.assertEquals("55555", attributes["customerId"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testRokt_SandboxMode_When_Environment_IS_Development() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        []
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.isDevEnv()).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("number", "(123) 456-9898"),
            Pair("customerId", "55555"),
            Pair("country", "US")
        )
        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["number"])
        Assert.assertEquals("55555", attributes["customerId"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("true", attributes["sandbox"])
    }

    @Test
    fun testRokt_SandboxMode_When_SandBox_is_Pass_In_Attributes_And_Environment_Is_DEV() {
        val sideloadedKit = Mockito.mock(MPSideloadedKit::class.java)
        val kitId = 6000000

        val configJSONObj = JSONObject().apply {
            put("id", kitId)
        }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)

        val settingsMap = hashMapOf(
            "placementAttributesMapping" to """
        []
            """.trimIndent()
        )
        val field = KitConfiguration::class.java.getDeclaredField("settings")
        field.isAccessible = true
        field.set(mockedKitConfig, settingsMap)

        val mockedProvider = mockProvider(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        PowerMockito.mockStatic(MPUtility::class.java)
        Mockito.`when`(MPUtility.isDevEnv()).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        Mockito.`when`(manager.supportedKits).thenReturn(supportedKit)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(
            factory.createInstance(
                Mockito.any(
                    KitManagerImpl::class.java
                ),
                Mockito.any(KitConfiguration::class.java)
            )
        ).thenReturn(sideloadedKit)
        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(42, mockedProvider)
        }

        val attributes = hashMapOf(
            Pair("test", "Test"),
            Pair("lastname", "Test1"),
            Pair("number", "(123) 456-9898"),
            Pair("customerId", "55555"),
            Pair("country", "US"),
            Pair("sandbox", "false")
        )
        manager.execute("Test", attributes, null, null, null, null)
        Assert.assertEquals(6, attributes.size)
        Assert.assertEquals("(123) 456-9898", attributes["number"])
        Assert.assertEquals("55555", attributes["customerId"])
        Assert.assertEquals("Test1", attributes["lastname"])
        Assert.assertEquals("Test", attributes["test"])
        Assert.assertEquals("US", attributes["country"])
        Assert.assertEquals("false", attributes["sandbox"])
    }

    @Test
    fun testSetWrapperSdkVersion() {
        val manager: KitManagerImpl = MockKitManagerImpl()

        val enabledRoktListener = mock(
            KitIntegration::class.java,
            withSettings().extraInterfaces(KitIntegration.RoktListener::class.java)
        )
        `when`(enabledRoktListener.isDisabled).thenReturn(false)

        val disabledRoktListener = mock(
            KitIntegration::class.java,
            withSettings().extraInterfaces(KitIntegration.RoktListener::class.java)
        )
        `when`(disabledRoktListener.isDisabled).thenReturn(true)

        val nonRoktListener = mock(KitIntegration::class.java)
        `when`(nonRoktListener.isDisabled).thenReturn(false)

        manager.providers = ConcurrentHashMap<Int, KitIntegration>().apply {
            put(1, enabledRoktListener)
            put(2, disabledRoktListener)
            put(3, nonRoktListener)
        }

        val wrapperSdkVersion = WrapperSdkVersion(WrapperSdk.WrapperFlutter, "1.2.3")
        manager.setWrapperSdkVersion(wrapperSdkVersion)

        verify(enabledRoktListener as KitIntegration.RoktListener)
            .setWrapperSdkVersion(wrapperSdkVersion)
        verify(disabledRoktListener as KitIntegration.RoktListener, never())
            .setWrapperSdkVersion(wrapperSdkVersion)
    }

    internal inner class mockProvider(val config: KitConfiguration) : KitIntegration(), KitIntegration.RoktListener {
        override fun isDisabled(): Boolean = false
        override fun getName(): String = "FakeProvider"
        override fun onKitCreate(settings: MutableMap<String, String>?, context: Context?): MutableList<ReportingMessage> {
            TODO("Not yet implemented")
        }

        override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage> {
            TODO("Not yet implemented")
        }

        override fun getConfiguration(): KitConfiguration {
            return config
        }

        override fun execute(
            viewName: String,
            attributes: MutableMap<String, String>,
            mpRoktEventCallback: MParticle.MpRoktEventCallback?,
            placeHolders: MutableMap<String, WeakReference<RoktEmbeddedView>>?,
            fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
            user: FilteredMParticleUser?,
            config: RoktConfig?
        ) {
            println("Executed with $attributes")
        }

        override fun setWrapperSdkVersion(wrapperSdkVersion: WrapperSdkVersion) {
            println("setWrapperSdkVersion with $wrapperSdkVersion")
        }

        override fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
            println("purchaseFinalized with placementId: $placementId  catalogItemId : $catalogItemId status : $status ")
        }
    }

    internal inner class KitManagerEventCounter : MockKitManagerImpl() {
        var logBaseEventCalled = 0
        var logCommerceEventCalled = 0
        var logMPEventCalled = 0
        override fun logEvent(event: BaseEvent) {
            super.logEvent(event)
            logBaseEventCalled++
        }

        override fun logMPEvent(event: MPEvent) {
            super.logMPEvent(event)
            logMPEventCalled++
        }

        override fun logCommerceEvent(event: CommerceEvent) {
            super.logCommerceEvent(event)
            logCommerceEventCalled++
        }
    }
}
