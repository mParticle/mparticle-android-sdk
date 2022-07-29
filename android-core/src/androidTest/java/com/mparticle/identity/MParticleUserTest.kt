package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.messages.IdentityResponseMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

class MParticleUserTest : BaseStartedTest() {
    @Test
    @Throws(InterruptedException::class)
    fun testFirstLastSeenTime() {
        val user = MParticle.getInstance()!!
            .Identity().currentUser
        val userFirstSeen = user!!.firstSeenTime
        Assert.assertNotNull(user.firstSeenTime)
        Assert.assertEquals(user.lastSeenTime.toFloat(), System.currentTimeMillis().toFloat(), 10f)
        Assert.assertTrue(user.firstSeenTime <= user.lastSeenTime)
        val newMpid: Long = Random.Default.nextLong()
        Server
            .endpoint(EndpointType.Identity_Login)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(newMpid)
                }
            }
        val latch = FailureLatch()
        MParticle.getInstance()!!.Identity().login()
            .addFailureListener { Assert.fail("Identity Request Failed") }
            .addSuccessListener { latch.countDown() }
        latch.await()
        val user1 = MParticle.getInstance()!!
            .Identity().currentUser
        Assert.assertEquals(newMpid, user1!!.id)
        Assert.assertNotNull(user1.firstSeenTime)
        Assert.assertTrue(user1.firstSeenTime >= user.lastSeenTime)
        Assert.assertEquals(user1.lastSeenTime.toFloat(), System.currentTimeMillis().toFloat(), 10f)
        Assert.assertEquals(userFirstSeen, user.firstSeenTime)
    }
}
