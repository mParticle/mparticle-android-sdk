package com.mparticle;

import com.mparticle.MParticleAPI;

import android.test.AndroidTestCase;

public class MParticleAPITest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
      super.setUp();
    }

    public void testGetSameInstance() {
        MParticleAPI mParticleAPI1 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        MParticleAPI mParticleAPI2 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        assertSame(mParticleAPI1, mParticleAPI2);
    }

    public void testGetDifferentInstance() {
        MParticleAPI mParticleAPI1 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        MParticleAPI mParticleAPI3 = MParticleAPI.getInstance(getContext(), "99999999999999999999999999999999", "secret");
        assertNotSame(mParticleAPI1, mParticleAPI3);
    }

    public void testSessionLifecycleEvents() {

    }

    public void testSessionEventLogging() {

    }

    public void testSessionDataLogging() {

    }

}
