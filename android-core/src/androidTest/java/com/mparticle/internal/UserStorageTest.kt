package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.context
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class UserStorageTest : BaseStartedTest() {
    @Before
    fun before() {
        MParticle.reset(context)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSetFirstSeenTime() {
        val startTime = System.currentTimeMillis()
        val storage = UserStorage.create(context, Random.Default.nextLong())
        val firstSeen = storage.firstSeenTime
        Assert.assertTrue(firstSeen >= startTime && firstSeen <= System.currentTimeMillis())

        // make sure that the firstSeenTime does not update if it has already been set
        storage.setFirstSeenTime(10L)
        Assert.assertEquals(firstSeen, storage.firstSeenTime)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSetLastSeenTime() {
        val storage = UserStorage.create(context, 2)
        val time = System.currentTimeMillis()
        storage.setLastSeenTime(time)
        Assert.assertEquals(time, storage.lastSeenTime)
    }

    internal interface UserConfigRunnable {
        fun run(userStorage: UserStorage?)
    }
}
