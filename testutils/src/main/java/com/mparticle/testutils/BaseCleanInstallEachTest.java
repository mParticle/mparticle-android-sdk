package com.mparticle.testutils;

import com.mparticle.MParticle;
import com.mparticle.networking.MockServer;

import org.junit.Before;

/**
 * Base class that will replicate the scenario or an app that has started, but has not called
 * MParticle.start(). This base class is useful for testing initialization behavior.
 */
abstract public class BaseCleanInstallEachTest extends BaseAbstractTest {

    @Before
    public void beforeBase() throws Exception {
        MParticle.reset(mContext);
        mServer = MockServer.getNewInstance(mContext);
    }
}
