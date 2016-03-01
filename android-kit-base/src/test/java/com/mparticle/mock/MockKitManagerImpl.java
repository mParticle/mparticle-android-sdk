package com.mparticle.mock;

import com.mparticle.kits.KitConfiguration;
import com.mparticle.kits.KitManagerImpl;

import org.json.JSONException;
import org.json.JSONObject;

public class MockKitManagerImpl extends KitManagerImpl {

    @Override
    protected KitConfiguration createKitConfiguration(JSONObject configuration) throws JSONException {
        return MockKitConfiguration.createKitConfiguration(configuration);
    }

    @Override
    public int getUserBucket() {
        return 50;
    }
}
