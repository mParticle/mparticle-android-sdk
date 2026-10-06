package com.mparticle.integrationtests;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IdentityTest extends BaselineTest {

    private static void await(MParticleTask<IdentityApiResult> task) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        task.addSuccessListener(result -> done.countDown());
        task.addFailureListener(response -> done.countDown());
        assertTrue("identity call did not complete", done.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void loginAndLogout() throws Exception {
        MParticle mParticle = start();
        mParticle.logEvent(new MPEvent.Builder("Anonymous Event").build());
        await(mParticle.Identity().login(IdentityApiRequest.withEmptyUser()
                .email("login@example.com")
                .customerId("login-customer")
                .build()));
        mParticle.logEvent(new MPEvent.Builder("Logged In Event").build());
        await(mParticle.Identity().logout());
        mParticle.logEvent(new MPEvent.Builder("Logged Out Event").build());
        uploadAndVerify();
    }

    @Test
    public void modify() throws Exception {
        MParticle mParticle = start();
        await(mParticle.Identity().modify(IdentityApiRequest.withUser(mParticle.Identity().getCurrentUser())
                .email("modified@example.com")
                .userIdentity(MParticle.IdentityType.Other, "other-id")
                .build()));
        mParticle.logEvent(new MPEvent.Builder("After Modify").build());
        uploadAndVerify();
    }

    @Test
    public void alias() throws Exception {
        MParticle mParticle = start();
        mParticle.Identity().aliasUsers(AliasRequest.builder()
                .sourceMpid(1000000000000000001L)
                .destinationMpid(1000000000000000002L)
                .startTime(1700000000000L)
                .endTime(1700000100000L)
                .build());
        uploadAndVerify();
    }

    @Test
    public void audiences() throws Exception {
        // The audience API is gated by a config flag, so serve a config that enables it.
        WireMockAdmin.stub(new JSONObject()
                .put("priority", 1)
                .put("request", new JSONObject().put("method", "GET").put("urlPathPattern", "/v4/[^/]+/config"))
                .put("response", new JSONObject()
                        .put("status", 200)
                        .put("jsonBody", new JSONObject()
                                .put("id", "integration-tests")
                                .put("ct", 1700000000000L)
                                .put("flags", new JSONObject().put("AudienceAPI", true)))));
        MParticle mParticle = start();
        // Until the config is applied the task fails without a request, so retry until it succeeds.
        boolean succeeded = false;
        for (int attempt = 0; attempt < 40 && !succeeded; attempt++) {
            CountDownLatch done = new CountDownLatch(1);
            AtomicBoolean success = new AtomicBoolean();
            mParticle.Identity().getCurrentUser().getUserAudiences()
                    .addSuccessListener(result -> {
                        success.set(true);
                        done.countDown();
                    })
                    .addFailureListener(result -> done.countDown());
            assertTrue("audience call did not complete", done.await(30, TimeUnit.SECONDS));
            succeeded = success.get();
            if (!succeeded) {
                SystemClock.sleep(250);
            }
        }
        assertTrue("audience call never succeeded", succeeded);
        uploadAndVerify();
    }
}
