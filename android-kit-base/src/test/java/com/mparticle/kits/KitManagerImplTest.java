package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.mock.MockMParticle;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.ReportingManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockKitConfiguration;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KitManagerImplTest {
    BackgroundTaskHandler mockBackgroundTaskHandler = new BackgroundTaskHandler(){

        @Override
        public void executeNetworkRequest(Runnable runnable) {
            //do nothing
        }
    };


    @Test
    public void testSetKitFactory() {
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.Identity()).thenReturn(Mockito.mock(IdentityApi.class));
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                Mockito.mock(BackgroundTaskHandler.class)
        );
        Assert.assertNotNull(manager.mKitIntegrationFactory);
        KitIntegrationFactory factory = Mockito.mock(KitIntegrationFactory.class);
        manager.setKitFactory(factory);
        Assert.assertEquals(factory, manager.mKitIntegrationFactory);
    }

    @Test
    public void testShouldEnableKit() throws Exception {
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.getKitManager()).thenReturn(Mockito.mock(KitFrameworkWrapper.class));
        IdentityApi mockIdentity = Mockito.mock(IdentityApi.class);
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        ConsentState state = ConsentState.builder().build();
        Mockito.when(mockUser.getConsentState()).thenReturn(state);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mparticle.Identity()).thenReturn(mockIdentity);
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                Mockito.mock(BackgroundTaskHandler.class)
        );
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
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.getKitManager()).thenReturn(Mockito.mock(KitFrameworkWrapper.class));
        IdentityApi mockIdentity = Mockito.mock(IdentityApi.class);
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mparticle.Identity()).thenReturn(mockIdentity);
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                Mockito.mock(BackgroundTaskHandler.class)
        );
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
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.getKitManager()).thenReturn(Mockito.mock(KitFrameworkWrapper.class));
        IdentityApi mockIdentity = Mockito.mock(IdentityApi.class);
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mparticle.Identity()).thenReturn(mockIdentity);
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                Mockito.mock(BackgroundTaskHandler.class)
        );
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
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.getKitManager()).thenReturn(Mockito.mock(KitFrameworkWrapper.class));
        IdentityApi mockIdentity = Mockito.mock(IdentityApi.class);
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);
        Mockito.when(mockIdentity.getCurrentUser()).thenReturn(mockUser);
        Mockito.when(mparticle.Identity()).thenReturn(mockIdentity);
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                Mockito.mock(BackgroundTaskHandler.class)
        );
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
    public void testOnUserAttributesReceived() throws Exception {
        MParticle.setInstance(new MockMParticle());
        KitManagerImpl manager  = new KitManagerImpl(
                new MockContext(),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                mockBackgroundTaskHandler
                );
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
        KitManagerImpl manager  = new KitManagerImpl(
                new MockContext(),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(KitFrameworkWrapper.CoreCallbacks.class),
                mockBackgroundTaskHandler);
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
}