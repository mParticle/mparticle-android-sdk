package com.mparticle;

import com.mparticle.utils.MParticleUtils;

/**
 * Base class that will replicate the scenario or an app that has started, but has not called
 * MParticle.start(). This base class is useful for testing initialization behavior.
 */
abstract public class BaseCleanInstallEachTest extends BaseAbstractTest {

    @Override
    protected void beforeClassBase() throws Exception {

    }

    @Override
    protected void beforeBase() throws Exception {
        MParticle.setInstance(null);
        MParticle.sAndroidIdDisabled = false;
        MParticle.sDevicePerformanceMetricsDisabled = false;
        MParticleUtils.clear();
    }

}
