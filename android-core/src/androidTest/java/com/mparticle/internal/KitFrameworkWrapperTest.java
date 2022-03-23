package com.mparticle.internal;

import android.content.Context;
import android.util.MutableBoolean;

import com.mparticle.AccessUtils;
import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KitFrameworkWrapperTest extends BaseCleanStartedEachTest {

    private void setKitManager(KitFrameworkWrapper kitManager) {
        AccessUtils.setKitManager(kitManager);
        com.mparticle.identity.AccessUtils.setKitManager(kitManager);
    }

    @Test
    public void testIdentify() throws InterruptedException {
        final Long mpid = ran.nextLong();
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean called = new MutableBoolean(false);
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onIdentifyCompleted(MParticleUser user, IdentityApiRequest request) {
                if (user.getId() == mStartingMpid) {
                    return;
                }
                assertEquals(mpid.longValue(), user.getId());
                called.value = true;
                latch.countDown();
            }
        });
        mServer.setupHappyIdentify(mpid);
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogin() throws InterruptedException {
        final Long mpid = ran.nextLong();
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean called = new MutableBoolean(false);
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onLoginCompleted(MParticleUser user, IdentityApiRequest request) {
                if (user.getId() == mStartingMpid) {
                    return;
                }
                assertEquals(mpid.longValue(), user.getId());
                called.value = true;
                latch.countDown();
            }
        });
        mServer.setupHappyLogin(mpid);
        MParticle.getInstance().Identity().login(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testLogout() throws InterruptedException {
        final Long mpid = ran.nextLong();
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean called = new MutableBoolean(false);
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onLogoutCompleted(MParticleUser user, IdentityApiRequest request) {
                if (user.getId() == mStartingMpid) {
                    return;
                }
                assertEquals(mpid.longValue(), user.getId());
                called.value = true;
                latch.countDown();
            }
        });
        mServer.setupHappyLogout(mpid);
        MParticle.getInstance().Identity().logout(IdentityApiRequest.withEmptyUser().build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testModify() throws InterruptedException {
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean called = new MutableBoolean(false);
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onModifyCompleted(MParticleUser user, IdentityApiRequest request) {
                assertEquals(mStartingMpid.longValue(), user.getId());
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withUser(MParticle.getInstance().Identity().getCurrentUser()).build());
        latch.await();
        assertTrue(called.value);
    }

    @Test
    public void testModifyUserChanged() throws InterruptedException {
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean called = new MutableBoolean(false);
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onModifyCompleted(MParticleUser user, IdentityApiRequest request) {
                assertEquals(mStartingMpid.longValue(), user.getId());
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build());
        MParticle.getInstance().Internal().getConfigManager().setMpid(0, ran.nextBoolean());
        latch.await();
        final CountDownLatch latch2 = new MPLatch(1);
        final Long mpid2 = ran.nextLong();
        MParticle.getInstance().Internal().getConfigManager().setMpid(mpid2, ran.nextBoolean());
        assertTrue(called.value);
        called.value = false;
        setKitManager(new StubKitManager(mContext) {
            @Override
            public void onModifyCompleted(MParticleUser user, IdentityApiRequest request) {
                assertEquals(mpid2.longValue(), user.getId());
                called.value = true;
                latch2.countDown();
            }
        });
        MParticle.getInstance().Identity().modify(IdentityApiRequest.withUser(MParticle.getInstance().Identity().getCurrentUser()).build());
        MParticle.getInstance().Internal().getConfigManager().setMpid(ran.nextLong(), ran.nextBoolean());
        latch2.await();
        assertTrue(called.value);
    }

    static class StubKitManager extends KitFrameworkWrapper {

        public StubKitManager(Context context) {
            super(context, null, null, null, true, null);
            setKitManager(null);
        }

        @Override
        public void loadKitLibrary() {

        }
    }
}
