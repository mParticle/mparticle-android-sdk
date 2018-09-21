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
import org.mockito.Mockito;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class KitFrameworkWrapperTest extends BaseCleanStartedEachTest {

    private void setKitManager(KitFrameworkWrapper kitManager) {
        AccessUtils.setKitManager(kitManager);
        com.mparticle.identity.AccessUtils.setKitManager(kitManager);
    }

    @Test
    public void testIdentify() throws InterruptedException {
        final Long mpid = new Random().nextLong();
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
        final Long mpid = new Random().nextLong();
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
        final Long mpid = new Random().nextLong();
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
        MParticle.getInstance().getConfigManager().setMpid(0);
        latch.await();
        final CountDownLatch latch2 = new MPLatch(1);
        final Long mpid2 = new Random().nextLong();
        MParticle.getInstance().getConfigManager().setMpid(mpid2);
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
        MParticle.getInstance().getConfigManager().setMpid(new Random().nextLong());
        latch2.await();
        assertTrue(called.value);
    }

    static class StubKitManager extends KitFrameworkWrapper {

        public StubKitManager(Context context) {
            super(context, null, null, null, null, true);
            //using a Mockito instance for KitManager means that whenever a call is made to it, it will crash
            //this way we can override the methods we expect to be called, and if an unexpected one gets called, we will know about it
            setKitManager(Mockito.mock(KitManager.class));
        }

        @Override
        public void loadKitLibrary() {

        }
    }
}
