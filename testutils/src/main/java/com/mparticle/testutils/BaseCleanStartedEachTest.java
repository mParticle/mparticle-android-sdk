package com.mparticle.testutils;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.internal.AppStateManager;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;


/**
 * Base class that will replicate the scenario that MParticle has been started and is running. This
 * state also includes the initial IdentityApi.Identify call has completed.
 *
 * That being said, there is no need to call MParticle.start() in your before or beforeClass methods,
 * or in your tests.
 *
 * If you want to test the behavior that occurs during initialization, you should either invoke
 * MParticle.setInstance(null), or use BaseCleanInstallEachTest as your base class.
 */
public class BaseCleanStartedEachTest extends BaseAbstractTest {

    public void beforeSetup() {
    }

    @Before
    public final void beforeBase() throws InterruptedException {
        if (MParticle.getInstance() != null) {
            MParticle.reset(mContext);
        }
        MParticle.setInstance(null);
        beforeSetup();
        startMParticle(transformMParticleOptions(getBaseMParticleOptionBuilder()));
        AppStateManager.mInitialized = false;
    }

    protected MParticleOptions.Builder getBaseMParticleOptionBuilder() {
        return MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .identify(IdentityApiRequest.withEmptyUser().build())
                .environment(MParticle.Environment.Production);
    }

    protected void restartWithOptions(@NotNull MParticleOptions.Builder options) throws InterruptedException {
        startMParticle(options);
    }

    //Override this if you need to do something simple like add or remove a network options before.
    //Just don't mess with the "identitfyTask" that will break things
    protected MParticleOptions.Builder transformMParticleOptions(MParticleOptions.Builder builder) {
        return builder;
    }
}