package com.mparticle;

import com.mparticle.internal.Utils;

public abstract class BaseCleanInstallEachTest extends BaseAbstractTest {


    @Override
    protected void beforeClassBase() throws Exception {

    }

    @Override
    protected void beforeBase() throws Exception {
        MParticle.setInstance(null);
        MParticle.setAndroidIdDisabled(false);
        MParticle.setDevicePerformanceMetricsDisabled(false);
        Utils.clear();
    }

}
