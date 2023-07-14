package com.mparticle.kits

import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.SideloadedKit
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockKitConfiguration
import com.mparticle.mock.MockKitManagerImpl
import com.mparticle.mock.MockMParticle
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.LinkedList

class KitManagerImplTest {
    var mparticle: MParticle? = null
    var mockIdentity: IdentityApi? = null

    @Before
    fun before() {
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

    private fun createKitsMap(ids: List<Int>, type: Class<*> = KitIntegration::class.java): HashMap<Int, Class<*>> {
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
        Mockito.`when`(manager.mCoreCallbacks.latestKitConfiguration).thenReturn(kitConfiguration)
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
        Mockito.`when`(mockCoreCallbacks.latestKitConfiguration).thenReturn(kitConfiguration)
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
        Mockito.`when`(sideloadedKit.onKitCreate(Mockito.any(), Mockito.any())).thenReturn(null)
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
        Mockito.`when`(sideloadedKit.onKitCreate(Mockito.any(), Mockito.any())).thenReturn(null)

        val kitId = 6000000
        val configJSONObj = JSONObject().apply { put("id", kitId) }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val options = MParticleOptions.builder(MockContext())
            .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>).build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = Mockito.mock(KitIntegrationFactory::class.java)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(kitId), MPSideloadedKit::class.java).keys)
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
        Mockito.`when`(manager.mCoreCallbacks.latestKitConfiguration).thenReturn(kitConfiguration)
        val factory = Mockito.mock(
            KitIntegrationFactory::class.java
        )
        manager.setKitFactory(factory)
        Mockito.`when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, idOne, idTwo), MPSideloadedKit::class.java).keys)
        Mockito.`when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val sideloadedKit = Mockito.mock(KitIntegration::class.java)
        Mockito.`when`(sideloadedKit.isDisabled).thenReturn(false)
        Mockito.`when`(sideloadedKit.configuration).thenReturn(
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
        ).thenReturn(sideloadedKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(3, manager.providers.size)
        Assert.assertTrue(manager.providers.containsKey(idOne))
        Assert.assertTrue(manager.providers.containsKey(idOne))
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
        Mockito.`when`(manager.mCoreCallbacks.latestKitConfiguration).thenReturn(kitConfiguration)
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
