package com.mparticle;

import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MessageManager;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.MParticleUtils;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MParticleTest extends BaseCleanStartedEachTest {

    @Test
    public void testAndroidIdDisabled() throws Exception {
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .androidIdDisabled(true)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);
        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .androidIdDisabled(false)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertFalse(MParticle.isAndroidIdDisabled());
    }

    @Test
    public void testEnsureSessionActive() {
        MParticle.getInstance().mAppStateManager.ensureActiveSession();
        ensureSessionActive();
    }

    @Test
    public void testEnsureSessionActiveAtStart() {
        assertFalse(MParticle.getInstance().isSessionActive());
    }

    @Test
    public void testSessionEndsOnOptOut() {
        MParticle.getInstance().mAppStateManager.ensureActiveSession();
        assertTrue(MParticle.getInstance().mAppStateManager.getSession().isActive());
        MParticle.getInstance().setOptOut(true);
        assertFalse(MParticle.getInstance().mAppStateManager.getSession().isActive());
    }

    @Test
    public void testSetInstallReferrer() {
        MParticle.getInstance().setInstallReferrer("foo install referrer");
        Assert.assertEquals("foo install referrer", MParticle.getInstance().getInstallReferrer());
    }

    @Test
    public void testInstallReferrerUpdate() {
        String randomName = RandomUtils.getInstance().getAlphaNumericString(RandomUtils.getInstance().randomInt(4, 64));
        MParticle.getInstance().setInstallReferrer(randomName);
        assertTrue(MParticle.getInstance().getInstallReferrer().equals(randomName));
    }

    /**
     * These tests are to make sure that we are not missing any instances of the InstallReferrer
     * being set at any of the entry points, without the corresponding installReferrerUpdated() calls
     * being made
     * @throws Exception
     */
    @Test
    public void testCalledUpdateInstallReferrer() throws Exception {
        final boolean[] called = new boolean[2];
        MParticle.getInstance().mMessageManager = new MessageManager(){
            @Override
            public void installReferrerUpdated() {
                called[0] = true;
            }
        };

        MParticle.getInstance().mKitManager = new KitFrameworkWrapper(mContext, null,null, null, null, true) {
            @Override
            public void installReferrerUpdated() {
                called[1] = true;
            }
        };

        //test when the InstallReferrer is set directly on the InstallReferrerHelper
        String installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(mContext, installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is set through the MParticle object in the public API
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        MParticle.getInstance().setInstallReferrer(installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is received via the ReferrerReceiver Receiver
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        ReferrerReceiver.setInstallReferrer(mContext, ReferrerReceiver.getMockInstallReferrerIntent(installReferrer));

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is received through the MPReceiver Receiver
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        new MPReceiver().onReceive(mContext, ReferrerReceiver.getMockInstallReferrerIntent(installReferrer));

        assertTrue(called[1]);
        assertTrue(called[0]);

        Arrays.fill(called, false);

        //just a sanity check, if Context is null, it should not set mark the InstallReferrer as updated
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(null, installReferrer);

        org.junit.Assert.assertFalse(called[0]);
        org.junit.Assert.assertFalse(called[1]);
    }

    private void ensureSessionActive() {
        if (!MParticle.getInstance().isSessionActive()) {
            MParticle.getInstance().logEvent(MParticleUtils.getInstance().getRandomMPEventSimple());
            assertTrue(MParticle.getInstance().isSessionActive());
        }
    }
}