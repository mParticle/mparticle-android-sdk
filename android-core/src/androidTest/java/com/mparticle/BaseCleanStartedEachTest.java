package com.mparticle;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.AccessUtils.IdentityApiClient;
import com.mparticle.utils.MParticleUtils;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

public abstract class BaseCleanStartedEachTest extends BaseAbstractTest {
    protected static Context mContext;
    protected static Long mStartingMpid;

    @Override
    protected void beforeClassBase() throws Exception {

    }

    @Override
    public void beforeBase() {
        MParticleUtils.clear();

        mContext = InstrumentationRegistry.getContext();
        mStartingMpid = new Random().nextLong();
        final CountDownLatch latch = new CountDownLatch(1);
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        AccessUtils.setIdentityApiClient(new IdentityApiClient(latch), true);
        MParticle.start(options);
        try {
            latch.await();
        } catch (InterruptedException e) {
           fail(e.getMessage());
        }
        AccessUtils.clearIdentityApiClient();
        MParticle.getInstance().getConfigManager().setMpid(mStartingMpid);
    }
}
