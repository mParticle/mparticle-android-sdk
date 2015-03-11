package com.mparticle.test;

import android.content.Intent;
import android.test.AndroidTestCase;

import com.mparticle.MParticle;
import com.mparticle.ReferrerReceiver;
import com.mparticle.internal.Constants;
import com.mparticle.media.MPMediaAPI;

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

    public void testMediaTimeout(){
        MPMediaAPI instance = MParticle.getInstance().Media();
        assertFalse(instance.getAudioPlaying());
        MParticle.getInstance().Media().setAudioPlaying(true);
        assertTrue(instance.getAudioPlaying());
        instance.setAudioPlaying(false);
        assertFalse(MParticle.getInstance().Media().getAudioPlaying());
    }

    public void testReferrerReceiver(){
        String referrer = "this is a test referrer string";
        Intent fakeReferralIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        fakeReferralIntent.putExtra(Constants.REFERRER, referrer);
        new ReferrerReceiver().onReceive(getContext(), fakeReferralIntent);
        String persistedReferrer = MParticle.getInstance().getInstallReferrer();
        assertEquals(referrer, persistedReferrer);
    }

    public void testReferrerSetter(){
        String referrer = "this is another test referrer string";
        MParticle.getInstance().setInstallReferrer(referrer);
        String persistedReferrer = MParticle.getInstance().getInstallReferrer();
        assertEquals(referrer, persistedReferrer);
    }
}
