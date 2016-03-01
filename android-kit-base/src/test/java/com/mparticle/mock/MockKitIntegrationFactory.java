package com.mparticle.mock;

import com.mparticle.internal.KitManager;
import com.mparticle.kits.KitIntegration;
import com.mparticle.kits.KitIntegrationFactory;

import org.json.JSONException;

/**
 * Created by sdozor on 3/21/16.
 */
public class MockKitIntegrationFactory extends KitIntegrationFactory {

    @Override
    public boolean isSupported(int kitModuleId) {
        return true;
    }

    @Override
    public KitIntegration createInstance(KitManager manager, int moduleId) throws JSONException, ClassNotFoundException {
        return new MockKit().setKitManager(manager);
    }
}

