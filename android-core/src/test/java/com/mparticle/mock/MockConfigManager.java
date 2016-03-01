package com.mparticle.mock;


import com.mparticle.internal.ConfigManager;
import com.mparticle.MParticle;

public class MockConfigManager extends ConfigManager {
    public MockConfigManager() {
        super(new MockContext(), MParticle.Environment.Production, null, null);
    }
}
