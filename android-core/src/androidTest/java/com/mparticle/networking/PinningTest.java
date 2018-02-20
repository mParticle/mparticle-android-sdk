package com.mparticle.networking;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class PinningTest extends BaseCleanStartedEachTest {
    boolean[] called;

    protected boolean shouldPin() {
        return true;
    }

    @Before
    public void before() {
        called = new boolean[1];
    }

    @Test
    public void testIdentityClientLogin() throws Exception {
        new PinningTestHelper(mContext, "/login", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testIdentityClientLogout() throws Exception {
        new PinningTestHelper(mContext, "/logout", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testIdentityClientIdentify() throws Exception {
        new PinningTestHelper(mContext, "/identify", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testIdentityClientModify() throws Exception {
        new PinningTestHelper(mContext, "/modify", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().customerId(RandomUtils.getInstance().getAlphaNumericString(25)).build());
        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testMParticleClientFetchConfig() throws Exception {
        try {
            new PinningTestHelper(mContext, "/config", new PinningTestHelper.Callback() {
                @Override
                public void onPinningApplied(boolean pinned) {
                    assertEquals(shouldPin(), pinned);
                    called[0] = true;
                }
            });
            com.mparticle.internal.AccessUtils.getApiClient().fetchConfig(true);
        }
        catch (Exception e) {
            TestingUtils.checkAllBool(called);
        }
    }

    @Test
    public void testMParticleClientSendMessage() throws Exception {
        new PinningTestHelper(mContext, "/events", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        try {
            com.mparticle.internal.AccessUtils.getApiClient().sendMessageBatch(new JSONObject().toString());
        }
        catch (Exception e) {}
        TestingUtils.checkAllBool(called);
    }

    @Test
    public void testMParticleClientFetchAudience() throws Exception {
        new PinningTestHelper(mContext, "/audience", new PinningTestHelper.Callback() {
            @Override
            public void onPinningApplied(boolean pinned) {
                assertEquals(shouldPin(), pinned);
                called[0] = true;
            }
        });
        com.mparticle.internal.AccessUtils.getApiClient().fetchAudiences();
        TestingUtils.checkAllBool(called);
    }
}
