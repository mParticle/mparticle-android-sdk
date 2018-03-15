package com.mparticle;

import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Utils;

import java.util.Random;

/**
 * Base class that will replicate the scenario that MParticle has been started and is running. This
 * state also includes the initial IdentityApi.Identify call has completed.
 *
 * That being said, there is no need to call MParticle.start() in your before or beforeClass methods,
 * or in your tests.
 *
 * If you want to test the behavior that occures during initialization, you should either invoke
 * MParticle.setInstance(null), or use BaseCleanInstallEachTest as your base class
 */
public abstract class BaseCleanStartedEachTest extends BaseAbstractTest {
    protected static Long mStartingMpid;

    @Override
    protected void beforeClassBase() throws Exception {

    }

    @Override
    public void beforeBase() throws InterruptedException {
        Utils.clear();
        new ConfigManager(mContext, null, null, null).setMpid(mStartingMpid);
        MParticle.setInstance(null);
        MParticle.start(mContext, "key", "value");
        AppStateManager.mInitialized = false;
    }
}