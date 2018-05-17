package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

public class KitManagerImplTest {

    @Test
    public void testSetKitFactory() {
        MParticle mparticle = Mockito.mock(MParticle.class);
        Mockito.when(mparticle.Identity()).thenReturn(Mockito.mock(IdentityApi.class));
        MParticle.setInstance(mparticle);
        KitManagerImpl manager = new KitManagerImpl(
                Mockito.mock(Context.class),
                null,
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
}