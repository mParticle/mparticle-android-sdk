package com.mparticle.kits

import android.content.Context
import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MpRoktEventCallback
import com.mparticle.WrapperSdk
import com.mparticle.WrapperSdkVersion
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.SideloadedKit
import com.mparticle.kits.KitIntegration.ModifyIdentityListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockKitConfiguration
import com.mparticle.mock.MockKitManagerImpl
import com.mparticle.mock.MockMParticle
import com.mparticle.testutils.TestingUtils
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
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
        mockIdentity = mock(IdentityApi::class.java)
        val instance = MockMParticle()
        instance.setIdentityApi(mockIdentity)
        MParticle.setInstance(instance)
    }

    @Test
    fun testSetKitFactory() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        Assert.assertNotNull(manager.mKitIntegrationFactory)
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        Assert.assertEquals(factory, manager.mKitIntegrationFactory)
    }

    @Test
    fun testActiveKitsExcludesDisabled() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        val enabled1 = mock(KitIntegration::class.java)
        val enabled2 = mock(KitIntegration::class.java)
        val disabled = mock(KitIntegration::class.java)
        `when`(enabled1.isDisabled()).thenReturn(false)
        `when`(enabled2.isDisabled()).thenReturn(false)
        `when`(disabled.isDisabled()).thenReturn(true)
        manager.providers[1] = enabled1
        manager.providers[2] = disabled
        manager.providers[3] = enabled2
        val active = manager.activeKits()
        assertEquals(2, active.size)
        assertTrue(active.contains(enabled1))
        assertTrue(active.contains(enabled2))
        Assert.assertFalse(active.contains(disabled))
    }

    private fun createKitsMap(
        ids: List<Int>,
        type: Class<*> = KitIntegration::class.java,
    ): HashMap<Int, Class<*>> {
        val map = hashMapOf<Int, Class<*>>()
        ids.forEach { map.put(it, type) }
        return map
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKit() {
        val mockUser = mock(MParticleUser::class.java)
        val state = ConsentState.builder().build()
        `when`(mockUser.consentState).thenReturn(state)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{\"id\":1}"))
        kitConfiguration.put(JSONObject("{\"id\":2}"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)

        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        assertEquals(2, manager.providers.size)
        assertEquals(
            mockKit,
            manager.providers.values
                .iterator()
                .next(),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShouldNotEnableKitBasedOnConsent() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":2, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":3, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(1, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKitBasedOnConsent() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        assertEquals(2, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldDisableActiveKitBasedOnConsent() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        assertEquals(2, manager.providers.size)
        `when`(mockUser.consentState).thenReturn(ConsentState.builder().build())
        manager.updateKits(kitConfiguration)
        assertEquals(1, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldEnableKitBasedOnActiveUser() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        `when`(mockUser.isLoggedIn).thenReturn(true)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        assertEquals(3, manager.providers.size)
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class)
    fun testShouldNotEnableKitBasedOnActiveUser() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        `when`(mockUser.isLoggedIn).thenReturn(false)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        assertEquals(1, manager.providers.size)
        TestCase.assertTrue(manager.isKitActive(2))
        TestCase.assertFalse(manager.isKitActive(1))
        TestCase.assertFalse(manager.isKitActive(3))
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class, InterruptedException::class)
    fun testShouldEnableDisabledKitBasedOnActiveUser() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        `when`(mockUser.isLoggedIn).thenReturn(false)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(0, manager.providers.size)
        `when`(mockUser.isLoggedIn).thenReturn(true)
        `when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        manager.onUserIdentified(mockUser, null)
        assertEquals(3, manager.providers.size)
    }

    @Test
    @Throws(JSONException::class, ClassNotFoundException::class)
    fun testShouldDisableEnabledKitBasedOnActiveUser() {
        val mockUser = mock(MParticleUser::class.java)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        `when`(mockUser.isLoggedIn).thenReturn(true)
        val mockCoreCallbacks =
            mock(
                CoreCallbacks::class.java,
            )
        val manager: KitManagerImpl = MockKitManagerImpl()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build()
        `when`(mockUser.consentState).thenReturn(state)
        val kitConfiguration = JSONArray()
        kitConfiguration.put(
            JSONObject(
                "{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }",
            ),
        )
        kitConfiguration.put(JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        kitConfiguration.put(JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"))
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2, 3)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(3, manager.providers.size)
        `when`(mockUser.isLoggedIn).thenReturn(false)
        `when`(mockCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        manager.onUserIdentified(mockUser, null)
        assertEquals(0, manager.providers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUserAttributesReceived() {
        MParticle.setInstance(MockMParticle())
        val manager: KitManagerImpl = MockKitManagerImpl()
        val integration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        val integration2 =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        `when`((integration as UserAttributeListener).supportsAttributeLists()).thenReturn(true)
        `when`((integration2 as UserAttributeListener).supportsAttributeLists())
            .thenReturn(false)
        `when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        `when`(integration2.configuration)
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
        verify(integration as UserAttributeListener, Mockito.times(1))
            .onSetAllUserAttributes(eq(userAttributeSingles), eq(userAttributeLists))
        val userAttributesCombined: MutableMap<String, String> = HashMap()
        userAttributesCombined["test"] = "whatever"
        userAttributesCombined["test 2"] = "whatever 2"
        userAttributesCombined["test 3"] = "1,2,3"
        val clearedOutList: Map<String, List<String>> = HashMap()
        verify(integration2 as UserAttributeListener, Mockito.times(1))
            .onSetAllUserAttributes(eq(userAttributesCombined), eq(clearedOutList))
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttributeList() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        val integration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        val integration2 =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        `when`((integration as UserAttributeListener).supportsAttributeLists()).thenReturn(true)
        `when`((integration2 as UserAttributeListener).supportsAttributeLists())
            .thenReturn(false)
        `when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        `when`(integration2.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        manager.providers[5] = integration
        manager.providers[6] = integration2
        val attributeList: MutableList<String> = LinkedList()
        attributeList.add("1")
        attributeList.add("2")
        attributeList.add("3")
        manager.setUserAttributeList("test key", attributeList, 1)
        verify(integration as UserAttributeListener, Mockito.times(1))
            .onSetUserAttributeList(eq("test key"), eq(attributeList))
        verify(integration2 as UserAttributeListener, Mockito.times(1))
            .onSetUserAttribute(eq("test key"), eq("1,2,3"))
    }

    @Test
    @Throws(JSONException::class)
    fun testLogEventCalledOne() {
        val manager = KitManagerEventCounter()
        val integration =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        val integration2 =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(ModifyIdentityListener::class.java, UserAttributeListener::class.java),
            )
        `when`((integration as UserAttributeListener).supportsAttributeLists()).thenReturn(true)
        `when`((integration2 as UserAttributeListener).supportsAttributeLists())
            .thenReturn(false)
        `when`(integration.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        `when`(integration2.configuration)
            .thenReturn(MockKitConfiguration.createKitConfiguration())
        (manager as KitManagerImpl).providers[5] = integration
        (manager as KitManagerImpl).providers[6] = integration2
        val mpEvent = TestingUtils().randomMPEventSimple
        manager.logEvent(mpEvent)
        assertEquals(1, manager.logBaseEventCalled)
        assertEquals(1, manager.logMPEventCalled)
        assertEquals(0, manager.logCommerceEventCalled)
        manager.logBaseEventCalled = 0
        manager.logMPEventCalled = 0
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, Product.Builder("name", "sku", 100.0).build())
                .build()
        manager.logEvent(commerceEvent)
        assertEquals(1, manager.logBaseEventCalled)
        assertEquals(0, manager.logMPEventCalled)
        assertEquals(1, manager.logCommerceEventCalled)
    }

    @Test
    fun testMParticleConfigureKitsFromOptions() {
        val sideloadedKit = mock(MPSideloadedKit::class.java)
        val kitId = 6000000
        val configJSONObj = JSONObject().apply { put("id", kitId) }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        `when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val options =
            MParticleOptions
                .builder(MockContext())
                .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>)
                .build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = mock(KitIntegrationFactory::class.java)
        manager.setKitFactory(factory)

        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        `when`(manager.supportedKits).thenReturn(supportedKit)
        `when`(sideloadedKit.isDisabled).thenReturn(false)
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(sideloadedKit)
        manager.configureKits(mutableListOf(mockedKitConfig))
        Assert.assertEquals(1, manager.providers.size)
        assertTrue(manager.providers.containsKey(kitId))
    }

    @Test
    fun testMParticleUpdateEmptyConfigKitWithKitOptions() {
        val sideloadedKit = mock(MPSideloadedKit::class.java)
        val kitId = 6000000
        val configJSONObj = JSONObject().apply { put("id", kitId) }
        val mockedKitConfig = KitConfiguration.createKitConfiguration(configJSONObj)
        `when`(sideloadedKit.configuration).thenReturn(mockedKitConfig)

        val options =
            MParticleOptions
                .builder(MockContext())
                .sideloadedKits(mutableListOf(sideloadedKit) as List<SideloadedKit>)
                .build()
        val manager: KitManagerImpl = MockKitManagerImpl(options)
        val factory = mock(KitIntegrationFactory::class.java)
        `when`(factory.getSupportedKits())
            .thenReturn(createKitsMap(listOf(kitId), MPSideloadedKit::class.java).keys)
        manager.setKitFactory(factory)

        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val supportedKit = mutableSetOf(kitId)
        `when`(manager.supportedKits).thenReturn(supportedKit)
        `when`(sideloadedKit.isDisabled).thenReturn(false)
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(sideloadedKit)
        manager.configureKits(mutableListOf(mockedKitConfig))
        Assert.assertEquals(1, manager.providers.size)
        assertTrue(manager.providers.containsKey(kitId))

        manager.updateKits(JSONArray())
        Assert.assertEquals(0, manager.providers.size)
        Assert.assertFalse(manager.providers.containsKey(kitId))
    }

    @Test
    fun testSideloadedKitAdded() {
        val manager: KitManagerImpl = MockKitManagerImpl()
        val idOne = 6000000
        val idTwo = 6000001
        val kitConfiguration =
            JSONArray()
                .apply {
                    put(JSONObject().apply { put("id", 1) })
                    put(JSONObject().apply { put("id", idOne) })
                    put(JSONObject().apply { put("id", idTwo) })
                }
        `when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits())
            .thenReturn(createKitsMap(listOf(1, idOne, idTwo), MPSideloadedKit::class.java).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val sideloadedKit = mock(KitIntegration::class.java)
        `when`(sideloadedKit.isDisabled).thenReturn(false)
        `when`(sideloadedKit.configuration).thenReturn(
            mock(KitConfiguration::class.java),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(sideloadedKit)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(3, manager.providers.size)
        assertTrue(manager.providers.containsKey(idOne))
        assertTrue(manager.providers.containsKey(idOne))
    }

    @Test
    fun shouldFilterKitsFromKnownIntegrations() {
        val options = MParticleOptions.builder(MockContext()).build()
        val filteredKitOptions =
            MParticleOptions
                .builder(MockContext())
                .disabledKits(
                    Arrays.asList(
                        MParticle.ServiceProviders.ADJUST,
                        MParticle.ServiceProviders.APPBOY,
                        MParticle.ServiceProviders.CLEVERTAP,
                    ),
                ).build()

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
        val filteredKitOptions =
            MParticleOptions
                .builder(MockContext())
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
        val filteredKitOptions =
            MParticleOptions
                .builder(MockContext())
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
        val options =
            MParticleOptions
                .builder(MockContext())
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
        val mockUser = mock(MParticleUser::class.java)
        val state = ConsentState.builder().build()
        `when`(mockUser.consentState).thenReturn(state)
        `when`(mockIdentity!!.currentUser).thenReturn(mockUser)
        val manager: KitManagerImpl = MockKitManagerImpl()
        val kitConfiguration = JSONArray()
        kitConfiguration.put(JSONObject("{\"id\":1}"))
        kitConfiguration.put(JSONObject("{\"id\":2}"))
        `when`(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration)
        val factory =
            mock(
                KitIntegrationFactory::class.java,
            )
        manager.setKitFactory(factory)
        `when`(factory.getSupportedKits()).thenReturn(createKitsMap(listOf(1, 2)).keys)
        `when`(factory.isSupported(Mockito.anyInt())).thenReturn(true)
        val mockKit = mock(KitIntegration::class.java)
        `when`(mockKit.isDisabled).thenReturn(true)
        `when`(mockKit.configuration).thenReturn(
            mock(
                KitConfiguration::class.java,
            ),
        )
        `when`(
            factory.createInstance(
                any(
                    KitManagerImpl::class.java,
                ),
                any(KitConfiguration::class.java),
            ),
        ).thenReturn(mockKit)
        manager.setOptOut(true)
        manager.updateKits(kitConfiguration)
        Assert.assertEquals(0, manager.providers.size)
        `when`(mockKit.isDisabled).thenReturn(false)
        manager.setOptOut(false)
        Assert.assertEquals(2, manager.providers.size)
    }

    @Test
    fun testSetWrapperSdkVersion() {
        val manager: KitManagerImpl = MockKitManagerImpl()

        val enabledRoktListener =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(enabledRoktListener.isDisabled).thenReturn(false)

        val disabledRoktListener =
            mock(
                KitIntegration::class.java,
                withSettings().extraInterfaces(KitIntegration.RoktListener::class.java),
            )
        `when`(disabledRoktListener.isDisabled).thenReturn(true)

        val nonRoktListener = mock(KitIntegration::class.java)
        `when`(nonRoktListener.isDisabled).thenReturn(false)

        manager.providers =
            ConcurrentHashMap<Int, KitIntegration>().apply {
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
