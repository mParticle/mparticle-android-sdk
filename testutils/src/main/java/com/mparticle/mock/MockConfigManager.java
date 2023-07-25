package com.mparticle.mock;


import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;

public class MockConfigManager extends ConfigManager {
    public MockConfigManager() {
        super(new MockContext(), MParticle.Environment.Production, null, null, null, null, null, null, null, null);
    }
}
