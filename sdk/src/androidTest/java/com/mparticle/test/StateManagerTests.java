package com.mparticle.test;


import android.test.ActivityInstrumentationTestCase2;

import com.mparticle.MParticle;
import com.mparticle.AppStateManager;
import org.junit.Test;

public class StateManagerTests extends ActivityInstrumentationTestCase2 {


    public StateManagerTests() {
        super(MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        MParticle.start(getActivity());
        assertTrue(AppStateManager.mInitialized);
    }

    @Test
    public void testBackgroundForeground() {
        assertFalse(MParticle.getInstance().internal().isBackgrounded());
        getInstrumentation().callActivityOnStop(getActivity());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        assertTrue(MParticle.getInstance().internal().isBackgrounded());
        getInstrumentation().callActivityOnStart(getActivity());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        assertFalse(MParticle.getInstance().internal().isBackgrounded());


    }
}
