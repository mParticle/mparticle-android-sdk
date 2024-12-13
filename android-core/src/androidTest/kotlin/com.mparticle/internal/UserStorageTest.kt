package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UserStorageTest : BaseCleanStartedEachTest() {
    @Before
    fun before() {
        MParticle.reset(mContext)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSetFirstSeenTime() {
        val startTime = System.currentTimeMillis()
        val storage = UserStorage.create(mContext, ran.nextLong())
        val firstSeen = storage.firstSeenTime
        if (firstSeen != null) {
            Assert.assertTrue(firstSeen >= startTime && firstSeen <= System.currentTimeMillis())
        }

        // make sure that the firstSeenTime does not update if it has already been set
        storage.firstSeenTime = 10L
        Assert.assertEquals(firstSeen, storage.firstSeenTime)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSetLastSeenTime() {
        val storage = UserStorage.create(mContext, 2)
        val time = System.currentTimeMillis()
        storage.lastSeenTime = time
        Assert.assertEquals(time, storage.lastSeenTime)
    }

    internal interface UserConfigRunnable {
        fun run(userStorage: UserStorage?)
    }
}
