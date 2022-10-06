package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Test

class MParticleUserTest : BaseCleanStartedEachTest() {
    @Test
    @Throws(InterruptedException::class)
    fun testFirstLastSeenTime() {
        val user = MParticle.getInstance()?.Identity()?.currentUser
        val userFirstSeen = user?.firstSeenTime
        Assert.assertNotNull(user?.firstSeenTime)
        user?.lastSeenTime?.let {
            Assert.assertEquals(
                it.toFloat(),
                System.currentTimeMillis().toFloat(),
                10f
            )
        }
        if (user != null) {
            Assert.assertTrue(user.firstSeenTime <= user.lastSeenTime)
        }
        val newMpid = ran.nextLong()
        mServer.addConditionalLoginResponse(mStartingMpid, newMpid)
        val latch = MPLatch(1)
        MParticle.getInstance()?.Identity()?.login()
            ?.addFailureListener { Assert.fail("Identity Request Failed") }
            ?.addSuccessListener { latch.countDown() }
        latch.await()
        val user1 = MParticle.getInstance()?.Identity()?.currentUser
        Assert.assertEquals(newMpid, user1?.id)
        Assert.assertNotNull(user1?.firstSeenTime)
        if (user != null) {
            Assert.assertTrue(user1?.firstSeenTime!! >= user.lastSeenTime)
        }
        if (user1 != null) {
            Assert.assertEquals(
                user1.lastSeenTime.toFloat(),
                System.currentTimeMillis().toFloat(),
                10f
            )
        }
        Assert.assertEquals(userFirstSeen, user?.firstSeenTime)
    }
}