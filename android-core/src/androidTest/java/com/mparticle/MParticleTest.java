package com.mparticle;

import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.mparticle.internal.MessageManager;
import com.mparticle.utils.MParticleUtils;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


public class MParticleTest {

    @BeforeClass
    public static void setup() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MParticleUtils.clear();
    }

    @Before
    public void preConditions() {
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertNotNull(MParticle.getInstance());
    }

    @Test
    public void testAndroidIdDisabled() throws Exception {
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .setAndroidIdDisabled(true)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);
        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .setAndroidIdDisabled(false)
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

    private void ensureSessionActive() {
        if (!MParticle.getInstance().isSessionActive()) {
            MParticle.getInstance().logEvent("Thing started", MParticle.EventType.Other);
            assertTrue(MParticle.getInstance().isSessionActive());
        }
    }

    public static MessageManager getMessageManager() {
        return MParticle.getInstance().mMessageManager;
    }

}
