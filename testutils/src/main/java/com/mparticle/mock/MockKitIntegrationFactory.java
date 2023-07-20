package com.mparticle.mock;

import com.mparticle.MParticleOptions;
import com.mparticle.kits.KitIntegration;
import com.mparticle.kits.KitIntegrationFactory;
import com.mparticle.kits.KitManagerImpl;

import org.json.JSONException;

public class MockKitIntegrationFactory extends KitIntegrationFactory {

    public MockKitIntegrationFactory(MParticleOptions options) {
        super(options);
    }

    @Override
    public boolean isSupported(int kitModuleId) {
        return true;
    }

    @Override
    public KitIntegration createInstance(KitManagerImpl manager, int moduleId) throws JSONException, ClassNotFoundException {
        return new MockKit().setKitManager(manager);
    }
}

