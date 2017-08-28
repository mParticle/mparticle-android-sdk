package com.mparticle;

import android.content.Context;
import android.util.Log;

import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.AccessUtils.IdentityApiClient;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.utils.MParticleUtils;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;


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
        MParticleUtils.clear();
        mStartingMpid = new Random().nextLong();
        new ConfigManager(mContext, null, null, null).setMpid(mStartingMpid);
        mServer.setupHappyIdentify(mStartingMpid);
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        AppStateManager.mInitialized = false;
        Thread.sleep(3000);
    }

    protected void setMpidAfterInitialIdentityCall(long mpid) {
        mServer.addConditionalIdentityResponse(mStartingMpid, mpid);
    }
}
