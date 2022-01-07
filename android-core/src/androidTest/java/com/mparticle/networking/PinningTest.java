package com.mparticle.networking;

import androidx.test.platform.app.InstrumentationRegistry;
import android.util.MutableBoolean;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mparticle.testutils.MPLatch;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PinningTest extends BaseCleanStartedEachTest {
    MutableBoolean called;
    CountDownLatch latch;

    protected boolean shouldPin() {
        return true;
    }

    @BeforeClass
    public static void beforeClass() {
        MParticle.reset(InstrumentationRegistry.getInstrumentation().getContext());
    }

    @Before
    public void before() {
        called = new MutableBoolean(false);
        latch = new MPLatch(1);
    }

    @Test
    public void testIdentityClientLogin() throws Exception {
        new PinningTestHelper(mContext, "/login", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testIdentityClientLogout() throws Exception {
        new PinningTestHelper(mContext, "/logout", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testIdentityClientIdentify() throws Exception {
        new PinningTestHelper(mContext, "/identify", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testIdentityClientModify() throws Exception {
        new PinningTestHelper(mContext, "/modify", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().customerId(mRandomUtils.getAlphaNumericString(25)).build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testMParticleClientFetchConfig() throws Exception {
        try {
            new PinningTestHelper(mContext, "/config", new PinningTestHelper.Callback() {
                @Override
                public void onPinningApplied(boolean pinned) {
                    assertEquals(shouldPin(), pinned);
                    called.value = true;
                    latch.countDown();
                }
            });
            com.mparticle.internal.AccessUtils.getApiClient().fetchConfig(true);
        } catch (Exception e) {
        }
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testMParticleClientSendMessage() throws Exception {
        new PinningTestHelper(mContext, "/events", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called.value = true;
                latch.countDown();
            }
        });
        try {
            com.mparticle.internal.AccessUtils.getApiClient().sendMessageBatch(new JSONObject().toString());
        }
        catch (Exception e) {}
        latch.await();
        assertTrue(called.value);
    }
}
