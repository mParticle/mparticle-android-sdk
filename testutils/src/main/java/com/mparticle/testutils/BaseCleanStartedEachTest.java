package com.mparticle.testutils;

import android.support.annotation.CallSuper;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;

import org.junit.Before;

import java.util.Random;
import java.util.concurrent.CountDownLatch;


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

    @Before
    public final void beforeBase() throws InterruptedException {
        MParticleUtils.clear();
        mStartingMpid = new Random().nextLong();
        new ConfigManager(mContext, null, null, null).setMpid(mStartingMpid);
        mServer.setupHappyIdentify(mStartingMpid);
        MParticle.setInstance(null);
        final CountDownLatch latch = new CountDownLatch(1);
        MParticleOptions.Builder builder = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .identify(IdentityApiRequest.withEmptyUser().build())
                .identifyTask(new BaseIdentityTask()
                        .addSuccessListener(new TaskSuccessListener() {
                            @Override
                            public void onSuccess(IdentityApiResult result) {
                                latch.countDown();
                            }
                        })
                        .addFailureListener(new TaskFailureListener() {
                            @Override
                            public void onFailure(IdentityHttpResponse result) {
                                latch.countDown();
                            }
                        }))
                .environment(MParticle.Environment.Production);
        MParticleOptions options = transformMParticleOptions(builder).build();
        MParticle.start(options);
        AppStateManager.mInitialized = false;
        latch.await();
    }

    //Override this if you need to do something simple like add or remove a network options before.
    //Just don't mess with the "identitfyTask" that will break things
    protected MParticleOptions.Builder transformMParticleOptions(MParticleOptions.Builder builder) {
        return builder;
    }
}