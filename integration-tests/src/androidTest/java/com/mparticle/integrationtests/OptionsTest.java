package com.mparticle.integrationtests;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.junit.Test;
import org.junit.runner.RunWith;

/** MParticleOptions that change what goes on the wire. */
@RunWith(AndroidJUnit4.class)
public class OptionsTest extends BaselineTest {

    @Test
    public void developmentEnvironment() throws Exception {
        MParticle mParticle = start(options().environment(MParticle.Environment.Development));
        mParticle.logEvent(new MPEvent.Builder("Development Event").build());
        uploadAndVerify();
    }

    @Test
    public void dataplan() throws Exception {
        MParticle mParticle = start(options().dataplan("integration_plan", 3));
        mParticle.logEvent(new MPEvent.Builder("Planned Event").build());
        uploadAndVerify();
    }

    @Test
    public void sessionTimeoutAndPerformanceMetricsDisabled() throws Exception {
        MParticle mParticle = start(options()
                .sessionTimeout(120)
                .devicePerformanceMetricsDisabled(true));
        mParticle.logEvent(new MPEvent.Builder("Configured Event").build());
        uploadAndVerify();
    }
}
