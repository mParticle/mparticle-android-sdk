package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MParticle;

public class MParticleSanityTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try{
            MParticle.start(getContext());
        }catch (Exception e){
            fail("SDK failed to start(): " + e.toString());
        }
    }

    public void testSdkStarted() {
        MParticle instance = MParticle.getInstance();
        assertNotNull(instance);
    }

    public void testEnvironment() {
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);

        MParticle.getInstance().setEnvironment(MParticle.Environment.AutoDetect);
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);

        MParticle.getInstance().setEnvironment(MParticle.Environment.Development);
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);

        MParticle.getInstance().setEnvironment(MParticle.Environment.Production);
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Production);
    }

    public void testOptOut(){
        assertFalse(MParticle.getInstance().getOptOut());
        MParticle.getInstance().setOptOut(true);
        assertTrue(MParticle.getInstance().getOptOut());
        MParticle.getInstance().setOptOut(false);
    }

    public void testSessionTimeout(){
        //configured in mParticle.xml
        assertEquals(MParticle.getInstance().getSessionTimeout(), 60);
        MParticle.getInstance().setSessionTimeout(31);
        assertEquals(MParticle.getInstance().getSessionTimeout(), 31);
    }

}
