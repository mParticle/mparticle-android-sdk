package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.ReportingManager;

import junit.framework.Assert;

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
}
