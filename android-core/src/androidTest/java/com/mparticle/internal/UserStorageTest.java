package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserStorageTest extends BaseCleanStartedEachTest {

    @Before
    public void before() {
        MParticle.reset(mContext);
    }

    @Test
    public void testSetFirstSeenTime() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        UserStorage storage = UserStorage.create(mContext, ran.nextLong());
        long firstSeen = storage.getFirstSeenTime();

        assertTrue(firstSeen >= startTime && firstSeen <= System.currentTimeMillis());

        //make sure that the firstSeenTime does not update if it has already been set
        storage.setFirstSeenTime(10L);
        assertEquals(firstSeen, storage.getFirstSeenTime());
    }

    @Test
    public void testSetLastSeenTime() throws InterruptedException {
        UserStorage storage = UserStorage.create(mContext, 2);
        long time = System.currentTimeMillis();
        storage.setLastSeenTime(time);
        assertEquals(time, storage.getLastSeenTime());
    }

    interface UserConfigRunnable {
        void run(UserStorage userStorage);
    }
}
