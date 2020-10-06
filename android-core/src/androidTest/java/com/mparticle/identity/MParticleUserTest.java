package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MParticleUserTest extends BaseCleanStartedEachTest {

    @Test
    public void testFirstLastSeenTime() throws InterruptedException {
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        long userFirstSeen = user.getFirstSeenTime();
        assertNotNull(user.getFirstSeenTime());
        assertEquals(user.getLastSeenTime(), System.currentTimeMillis(), 10);

        assertTrue(user.getFirstSeenTime() <= user.getLastSeenTime());

        long newMpid = ran.nextLong();
        mServer.addConditionalLoginResponse(mStartingMpid, newMpid);
        final MPLatch latch = new MPLatch(1);
        MParticle.getInstance().Identity().login()
                .addFailureListener(new TaskFailureListener() {
                    @Override
                    public void onFailure(@Nullable IdentityHttpResponse result) {
                        fail("Identity Request Failed");
                    }
                })
                .addSuccessListener(new TaskSuccessListener() {
                    @Override
                    public void onSuccess(@NonNull IdentityApiResult result) {
                        latch.countDown();
                    }
                });
        latch.await();
        MParticleUser user1 = MParticle.getInstance().Identity().getCurrentUser();
        assertEquals(newMpid, user1.getId());
        assertNotNull(user1.getFirstSeenTime());
        assertTrue(user1.getFirstSeenTime() >= user.getLastSeenTime());
        assertEquals(user1.getLastSeenTime(), System.currentTimeMillis(), 10);
        assertEquals(userFirstSeen, user.getFirstSeenTime());
    }
}
