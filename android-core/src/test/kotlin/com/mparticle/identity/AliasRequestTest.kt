package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.MockMParticle
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.Random
import kotlin.math.abs

class AliasRequestTest {
    private var random = Random()

    @Test
    fun testAliasRequestBuilder() {
        val sourceUser = Mockito.mock(MParticleUser::class.java)
        val destinationUser = Mockito.mock(MParticleUser::class.java)
        val startTime = System.currentTimeMillis() - 100
        val endTime = System.currentTimeMillis()
        val sourceMpid = random.nextLong()
        val destinationMpid = random.nextLong()
        Mockito.`when`(sourceUser.id).thenReturn(sourceMpid)
        Mockito.`when`(sourceUser.firstSeenTime).thenReturn(startTime)
        Mockito.`when`(sourceUser.lastSeenTime).thenReturn(endTime)
        Mockito.`when`(destinationUser.id).thenReturn(destinationMpid)
        val request = AliasRequest.builder(sourceUser, destinationUser).build()
        Assert.assertEquals(destinationMpid, request.destinationMpid)
        Assert.assertEquals(sourceMpid, request.sourceMpid)
        Assert.assertEquals(request.startTime, startTime)
        Assert.assertEquals(request.endTime, endTime)
    }

    @Test
    fun testBasicBuilder() {
        val startTime = random.nextLong()
        val endTime = random.nextLong()
        val sourceMpid = random.nextLong()
        val destinationMpid = random.nextLong()
        val builder = AliasRequest.builder()
            .destinationMpid(destinationMpid)
            .sourceMpid(sourceMpid)
            .startTime(startTime)
            .endTime(endTime)
        val request = builder.build()
        Assert.assertEquals(sourceMpid, request.sourceMpid)
        Assert.assertEquals(destinationMpid, request.destinationMpid)
        Assert.assertEquals(startTime, request.startTime)
        Assert.assertEquals(endTime, request.endTime)
    }

    @Test
    fun testMaxWindowEnforcement() {
        MParticle.setInstance(MockMParticle())
        Mockito.`when`(MParticle.getInstance()?.Internal()?.configManager?.aliasMaxWindow)
            .thenReturn(1)
        val sourceUser = Mockito.mock(MParticleUser::class.java)
        val destinationUser = Mockito.mock(MParticleUser::class.java)
        val earliestLegalStartTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val startTime: Long = 0
        val endTime = System.currentTimeMillis()
        val sourceMpid = random.nextLong()
        val destinationMpid = random.nextLong()
        Mockito.`when`(sourceUser.id).thenReturn(sourceMpid)
        Mockito.`when`(sourceUser.firstSeenTime).thenReturn(startTime)
        Mockito.`when`(sourceUser.lastSeenTime).thenReturn(endTime)
        Mockito.`when`(destinationUser.id).thenReturn(destinationMpid)
        val request = AliasRequest.builder(sourceUser, destinationUser).build()
        Assert.assertEquals(sourceMpid, request.sourceMpid)
        Assert.assertEquals(destinationMpid, request.destinationMpid)

        // since request.getStartTime generates it's own earliestLegalStartTime based on System.currentTimeMillis(),
        // we need to give some wiggle room for time elapsed during test
        Assert.assertTrue(abs(earliestLegalStartTime - request.startTime) < 50)
        Assert.assertEquals(endTime, request.endTime)
    }
}
