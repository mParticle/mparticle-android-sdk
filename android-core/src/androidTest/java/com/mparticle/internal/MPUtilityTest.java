package com.mparticle.internal;

import com.mparticle.BaseCleanInstallEachTest;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;

public class MPUtilityTest extends BaseCleanInstallEachTest {

    @Test
    public void testInstantAppDetectionTest() {
        assertFalse(MPUtility.isInstantApp(mContext));
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {

    }
}
