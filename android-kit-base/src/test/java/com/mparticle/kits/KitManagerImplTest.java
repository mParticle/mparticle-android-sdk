package com.mparticle.kits;

import com.mparticle.BaseEvent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.CoreCallbacks;
import com.mparticle.internal.KitManager;
import com.mparticle.mock.MockKitManagerImpl;
import com.mparticle.mock.MockMParticle;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.mock.MockKitConfiguration;
import com.mparticle.testutils.TestingUtils;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class KitManagerImplTest {
    BackgroundTaskHandler mockBackgroundTaskHandler = new BackgroundTaskHandler() {

        @Override
        public void executeNetworkRequest(Runnable runnable) {
            //do nothing
        }
    };
    MParticle mparticle;
    IdentityApi mockIdentity;

    @Before
    public void before() {
        mockIdentity = Mockito.mock(IdentityApi.class);
        MockMParticle instance = new MockMParticle();
        instance.setIdentityApi(mockIdentity);
        MParticle.setInstance(instance);
        }

    @Test
    public void testSetKitFactory() {
        KitManagerImpl manager = new MockKitManagerImpl();
        Assert.assertNotNull(manager.mKitIntegrationFactory);
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Assert.assertEquals(factory, manager.mKitIntegrationFactory);
    }

    @Test
    public void testShouldEnableKit() throws Exception {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        ConsentState state = ConsentState.builder().build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        KitManagerImpl manager = new MockKitManagerImpl();
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{\"id\":1}"));
        kitConfiguration.put(new JSONObject("{\"id\":2}"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(2, manager.providers.size());
        Assert.assertEquals(mockKit, manager.providers.values().iterator().next());
    }

    @Test
    public void testShouldNotEnableKitBasedOnConsent() throws Exception {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        KitManagerImpl manager = new MockKitManagerImpl();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":false, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":3, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(1, manager.providers.size());

    }

    @Test
    public void testShouldEnableKitBasedOnConsent() throws Exception {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        KitManagerImpl manager = new MockKitManagerImpl();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(2, manager.providers.size());

    }

    @Test
    public void testShouldDisableActiveKitBasedOnConsent() throws Exception {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        KitManagerImpl manager = new MockKitManagerImpl();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(2, manager.providers.size());

        state = ConsentState.builder().build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);

        manager.configureKits(kitConfiguration);
        Assert.assertEquals(1, manager.providers.size());

    }

    @Test
    public void testShouldEnableKitBasedOnActiveUser() throws JSONException, ClassNotFoundException {
       MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mockUser.isLoggedIn()).thenReturn(true);
        KitManagerImpl manager = new MockKitManagerImpl();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":3, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"));

        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(3, manager.providers.size());
    }

    @Test
    public void testShouldNotEnableKitBasedOnActiveUser() throws JSONException, ClassNotFoundException {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mockUser.isLoggedIn()).thenReturn(false);
        KitManagerImpl manager = new MockKitManagerImpl();
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"eau\": false, \"as\":{ \"foo\":\"bar\" } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(1, manager.providers.size());
        assertTrue(manager.isKitActive(2));
        assertFalse(manager.isKitActive(1));
        assertFalse(manager.isKitActive(3));
    }

    @Test
    public void testShouldEnableDisabledKitBasedOnActiveUser() throws JSONException, ClassNotFoundException {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mockUser.isLoggedIn()).thenReturn(false);

        KitManagerImpl manager = new MockKitManagerImpl() {
            @Override
            public void updateKits(JSONArray kitConfigs) {
                configureKits(kitConfigs);
            }
        };
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(0, manager.providers.size());

        Mockito.when(mockUser.isLoggedIn()).thenReturn(true);
        Mockito.when(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration);
        manager.onUserIdentified(mockUser, null);
        assertEquals(3, manager.providers.size());
    }

    @Test
    public void testShouldDisableEnabledKitBasedOnActiveUser() throws JSONException, ClassNotFoundException {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mockUser.isLoggedIn()).thenReturn(true);

        CoreCallbacks mockCoreCallbacks = Mockito.mock(CoreCallbacks.class);
        KitManagerImpl manager = new MockKitManagerImpl() {
            @Override
            public void updateKits(JSONArray kitConfigs) {
                configureKits(kitConfigs);
            }
        };
        ConsentState state = ConsentState.builder()
                .addGDPRConsentState("Blah", GDPRConsent.builder(true).build())
                .build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{ \"id\":1, \"eau\": true, \"as\":{ \"foo\":\"bar\" }, \"crvf\":{ \"i\":true, \"v\":[ { \"c\":true, \"h\":48278946 }, { \"c\":true, \"h\":1556641 } ] } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":2, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        kitConfiguration.put(new JSONObject("{ \"id\":3, \"eau\": true, \"as\":{ \"foo\":\"bar\" } }"));
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(3, manager.providers.size());

        Mockito.when(mockUser.isLoggedIn()).thenReturn(false);
        Mockito.when(mockCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration);
        manager.onUserIdentified(mockUser, null);
        assertEquals(0, manager.providers.size());
    }
    
    @Test
    public void testOnUserAttributesReceived() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitManagerImpl manager  = new MockKitManagerImpl();
        KitIntegration integration = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        KitIntegration integration2 = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        Mockito.when(((KitIntegration.AttributeListener)integration).supportsAttributeLists()).thenReturn(true);
        Mockito.when(((KitIntegration.AttributeListener)integration2).supportsAttributeLists()).thenReturn(false);
        Mockito.when(integration.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        Mockito.when(integration2.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        manager.providers.put(5, integration);
        manager.providers.put(6, integration2);
        Map<String, String> userAttributeSingles = new HashMap<>();
        userAttributeSingles.put("test", "whatever");
        userAttributeSingles.put("test 2", "whatever 2");
        Map<String, List<String>> userAttributeLists = new HashMap<>();
        List<String> attributeList = new LinkedList<>();
        attributeList.add("1");
        attributeList.add("2");
        attributeList.add("3");
        userAttributeLists.put("test 3", attributeList);
        manager.onUserAttributesReceived(userAttributeSingles, userAttributeLists, 1L);
        Mockito.verify(((KitIntegration.AttributeListener)integration), Mockito.times(1)).setAllUserAttributes(userAttributeSingles, userAttributeLists);

        Map<String, String> userAttributesCombined = new HashMap<>();
        userAttributesCombined.put("test", "whatever");
        userAttributesCombined.put("test 2", "whatever 2");
        userAttributesCombined.put("test 3", "1,2,3");
        Map<String, List<String>> clearedOutList = new HashMap<>();

        Mockito.verify(((KitIntegration.AttributeListener)integration2), Mockito.times(1)).setAllUserAttributes(userAttributesCombined, clearedOutList);
    }

    @Test
    public void testSetUserAttributeList() throws Exception {
        KitManagerImpl manager  = new MockKitManagerImpl();
        KitIntegration integration = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        KitIntegration integration2 = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        Mockito.when(((KitIntegration.AttributeListener)integration).supportsAttributeLists()).thenReturn(true);
        Mockito.when(((KitIntegration.AttributeListener)integration2).supportsAttributeLists()).thenReturn(false);
        Mockito.when(integration.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        Mockito.when(integration2.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        manager.providers.put(5, integration);
        manager.providers.put(6, integration2);

        List<String> attributeList = new LinkedList<>();
        attributeList.add("1");
        attributeList.add("2");
        attributeList.add("3");
        manager.setUserAttributeList("test key", attributeList, 1);
        Mockito.verify(((KitIntegration.AttributeListener)integration), Mockito.times(1)).setUserAttributeList("test key", attributeList);
        Mockito.verify(((KitIntegration.AttributeListener)integration2), Mockito.times(1)).setUserAttribute("test key", "1,2,3");
    }

    @Test
    public void testLogEventCalledOne() throws JSONException {
        KitManagerEventCounter manager = new KitManagerEventCounter();

        KitIntegration integration = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        KitIntegration integration2 = Mockito.mock(
                KitIntegration.class,
                Mockito.withSettings().extraInterfaces(KitIntegration.AttributeListener.class)
        );
        Mockito.when(((KitIntegration.AttributeListener)integration).supportsAttributeLists()).thenReturn(true);
        Mockito.when(((KitIntegration.AttributeListener)integration2).supportsAttributeLists()).thenReturn(false);
        Mockito.when(integration.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        Mockito.when(integration2.getConfiguration()).thenReturn(MockKitConfiguration.createKitConfiguration());
        ((KitManagerImpl)manager).providers.put(5, integration);
        ((KitManagerImpl)manager).providers.put(6, integration2);

        MPEvent mpEvent = new TestingUtils().getRandomMPEventSimple();
        manager.logEvent(mpEvent);
        assertEquals(1, manager.logBaseEventCalled);
        assertEquals(1, manager.logMPEventCalled);
        assertEquals(0, manager.logCommerceEventCalled);

        manager.logBaseEventCalled = 0;
        manager.logMPEventCalled = 0;


        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, new Product.Builder("name", "sku", 100).build())
                .build();

        manager.logEvent(commerceEvent);
        assertEquals(1, manager.logBaseEventCalled);
        assertEquals(0, manager.logMPEventCalled);
        assertEquals(1, manager.logCommerceEventCalled);
    }

    @Test
    public void testShouldEnableKitOnOptIn() throws Exception {
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        ConsentState state = ConsentState.builder().build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        KitManagerImpl manager = new MockKitManagerImpl() {
            @Override
            public void updateKits(JSONArray kitConfigs) {
                configureKits(kitConfigs);
            }
        };
        JSONArray kitConfiguration = new JSONArray();
        kitConfiguration.put(new JSONObject("{\"id\":1}"));
        kitConfiguration.put(new JSONObject("{\"id\":2}"));

        Mockito.when(manager.mCoreCallbacks.getLatestKitConfiguration()).thenReturn(kitConfiguration);
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        KitIntegration mockKit  = Mockito.mock(KitIntegration.class);
        Mockito.when(mockKit.isDisabled()).thenReturn(true);
        Mockito.when(mockKit.getConfiguration()).thenReturn(Mockito.mock(KitConfiguration.class));
        Mockito.when(factory.createInstance(Mockito.any(KitManagerImpl.class), Mockito.any(KitConfiguration.class))).thenReturn(mockKit);

        manager.setOptOut(true);
        manager.configureKits(kitConfiguration);
        Assert.assertEquals(0, manager.providers.size());
        Mockito.when(mockKit.isDisabled()).thenReturn(false);
        manager.setOptOut(false);
        Assert.assertEquals(2, manager.providers.size());
    }

    class KitManagerEventCounter extends MockKitManagerImpl {
            int logBaseEventCalled = 0;
            int logCommerceEventCalled = 0;
            int logMPEventCalled = 0;
            
            @Override
            public void logEvent(BaseEvent event) {
            super.logEvent(event);
            logBaseEventCalled++;
        }

            @Override
            protected void logMPEvent(MPEvent event) {
            super.logMPEvent(event);
            logMPEventCalled++;
        }

            @Override
            protected void logCommerceEvent(CommerceEvent event) {
            super.logCommerceEvent(event);
            logCommerceEventCalled++;
        }
    }
}