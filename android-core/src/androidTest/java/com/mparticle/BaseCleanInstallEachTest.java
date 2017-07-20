package com.mparticle;

import com.mparticle.utils.MParticleUtils;

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
