package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 11/6/14.
 */
public class MParticleSanityTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSdkStart() {
        try{
            MParticle.start(getContext());
        }catch (Exception e){
            fail("SDK failed to start(): " + e.getMessage());
        }
        MParticle instance = MParticle.getInstance();
        assertNotNull(instance);
    }
}
