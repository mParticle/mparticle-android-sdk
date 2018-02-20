package com.mparticle.networking;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.testutils.BaseAbstractTest;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PinningTestNetworkOptionsEnabled extends PinningTest {

    @Override
    protected boolean shouldPin() {
        return false;
    }

    @Override
    protected MParticleOptions.Builder transformMParticleOptions(MParticleOptions.Builder builder) {
        return builder
                .environment(MParticle.Environment.Development)
                .networkOptions(NetworkOptions.builder()
                    .setPinningDisabledInDevelopment(true)
                    .build());
    }
}
