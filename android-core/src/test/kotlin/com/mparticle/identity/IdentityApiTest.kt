package com.mparticle.identity

import com.mparticle.internal.MessageManager
import com.mparticle.mock.AbstractMParticleUser
import com.mparticle.testutils.AndroidUtils
import org.junit.Assert
import org.junit.Test
import java.util.Random

class IdentityApiTest {
    private var ran = Random()

    // IdentityApi should order their users from greatest lastSeenTime to least
    @Test
    fun testUserOrder() {
        val identityApi = IdentityApi()
        val users: MutableList<MParticleUser> = ArrayList()
        for (i in 0..9) {
            users.add(MockUser(ran.nextLong(), ran.nextLong()))
        }
        identityApi.sortUsers(users)
        var lastTime = Long.MAX_VALUE
        for (user in users) {
            Assert.assertTrue(user.lastSeenTime <= lastTime)
            lastTime = user.lastSeenTime
        }
    }

    @Test
    fun testAliasUsersValidationRejection() {
        val identityApi = IdentityApi()
        identityApi.mMessageManager = object : MessageManager() {
            override fun logAliasRequest(aliasRequest: AliasRequest) {
                Assert.fail("should not logged Alias Request:\n$aliasRequest")
            }
        }

        // missing previousMpid
        var request = AliasRequest.builder()
            .destinationMpid(123)
            .startTime(1)
            .endTime(2)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))

        // missing newMpid
        request = AliasRequest.builder()
            .sourceMpid(123)
            .startTime(1)
            .endTime(2)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))

        // newMpid and previousMpid are not unique
        request = AliasRequest.builder()
            .destinationMpid(123)
            .sourceMpid(123)
            .startTime(1)
            .endTime(2)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))

        // endTime before startTime
        request = AliasRequest.builder()
            .destinationMpid(123)
            .sourceMpid(456)
            .startTime(2)
            .endTime(1)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))

        // endTime and or startTime do not exist
        request = AliasRequest.builder()
            .destinationMpid(1)
            .sourceMpid(2)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))
        request = AliasRequest.builder()
            .destinationMpid(1)
            .sourceMpid(2)
            .startTime(3)
            .build()
        Assert.assertFalse(identityApi.aliasUsers(request))
        request = AliasRequest.builder()
            .destinationMpid(1)
            .sourceMpid(2)
            .endTime(3).build()
        Assert.assertFalse(identityApi.aliasUsers(request))
    }

    @Test
    fun testAliasUsersValidationAcceptance() {
        val identityApi = IdentityApi()
        val requestMade = AndroidUtils.Mutable(false)
        identityApi.mMessageManager = object : MessageManager() {
            override fun logAliasRequest(aliasRequest: AliasRequest) {
                requestMade.value = true
            }
        }
        val request = AliasRequest.builder()
            .destinationMpid(1)
            .sourceMpid(2)
            .startTime(3)
            .endTime(4)
            .build()
        Assert.assertTrue(identityApi.aliasUsers(request))
        Assert.assertTrue(requestMade.value)
    }

    internal inner class MockUser(private var mpid: Long, private var mpLastSeenTime: Long) :
        AbstractMParticleUser() {
        override fun getLastSeenTime(): Long {
            return mpLastSeenTime
        }

        override fun getId(): Long {
            return mpid
        }
    }
}
