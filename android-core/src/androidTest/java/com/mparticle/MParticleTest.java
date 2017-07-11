package com.mparticle;

import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.mparticle.internal.MessageManager;

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
        Looper.prepare();
    }

    @Before
    public void preConditions() {
        MParticle.setInstance(null);
        MParticle.start(InstrumentationRegistry.getContext());
        assertNotNull(MParticle.getInstance());
    }

    @Test
    public void testAndroidIdDisabled() throws Exception {
        assertFalse(MParticle.isAndroidIdDisabled());

        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .setAndroidIdDisabled(true)
                .credentials("key", "secret")
                .build();
        try {
            MParticle.start(options);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        ensureSessionActive();
    }

    @Test
    public void testEnsureSessionActiveAtStart() {
        assertFalse(MParticle.getInstance().isSessionActive());
    }

    @Test
    public void testSessionEndsOnOptOut() {
        ensureSessionActive();
        MParticle.getInstance().setOptOut(true);
        assertFalse(MParticle.getInstance().isSessionActive());
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
