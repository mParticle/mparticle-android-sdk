package com.mparticle;



public class MockConfigManager extends ConfigManager {
    public MockConfigManager() {
        super(new MockContext(), MParticle.Environment.Production);
    }
}
