package com.mparticle.test;

import android.content.Intent;

import com.mparticle.MParticle;
import com.mparticle.ReferrerReceiver;
import com.mparticle.internal.Constants;
import com.mparticle.media.MPMediaAPI;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MParticleSanityTest {


    final MockContext context = new MockContext();

    @Test
    public void testEnvironment() {
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);
    }



    @Test
    public void testSessionTimeout(){
        //configured in mParticle.xml
        assertEquals(MParticle.getInstance().getSessionTimeout(), 60);
        MParticle.getInstance().setSessionTimeout(31);
        assertEquals(MParticle.getInstance().getSessionTimeout(), 31);
    }

    @Test
    public void testMediaTimeout(){
        MPMediaAPI instance = MParticle.getInstance().Media();
        assertFalse(instance.getAudioPlaying());
        MParticle.getInstance().Media().setAudioPlaying(true);
        assertTrue(instance.getAudioPlaying());
        instance.setAudioPlaying(false);
        assertFalse(MParticle.getInstance().Media().getAudioPlaying());
    }

    @Test
    public void testReferrerReceiver(){
        String referrer = "this is a test referrer string";
        Intent fakeReferralIntent = new Intent("com.android.vending.INSTALL_REFERRER");
        fakeReferralIntent.putExtra(Constants.REFERRER, referrer);
        new ReferrerReceiver().onReceive(getContext(), fakeReferralIntent);
        String persistedReferrer = MParticle.getInstance().getInstallReferrer();
        assertEquals(referrer, persistedReferrer);
    }

    @Test
    public void testReferrerSetter(){
        String referrer = "this is another test referrer string";
        MParticle.getInstance().setInstallReferrer(referrer);
        String persistedReferrer = MParticle.getInstance().getInstallReferrer();
        assertEquals(referrer, persistedReferrer);
    }

    public MockContext getContext() {
        return context;
    }
}
