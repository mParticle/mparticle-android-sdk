package com.mparticle.networking

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.internal.AccessUtils
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CountDownLatch

open class PinningTest : BaseStartedTest() {
    var called: Mutable<Boolean> = Mutable(false)
    var latch: CountDownLatch = FailureLatch()

    protected open fun shouldPin(): Boolean {
        return true
    }

    @Before
    fun before() {
        called = Mutable(false)
        latch = FailureLatch()
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientLogin() {
        PinningTestHelper(context, "/login") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!
            .Identity().login(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientLogout() {
        PinningTestHelper(context, "/logout") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!
            .Identity().logout(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientIdentify() {
        PinningTestHelper(context, "/identify") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!
            .Identity().identify(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called!!.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientModify() {
        PinningTestHelper(context, "/modify") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!
            .Identity().modify(
                IdentityApiRequest.withEmptyUser()
                    .customerId(RandomUtils.getAlphaNumericString(25)).build()
            )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testMParticleClientSendMessage() {
        PinningTestHelper(context, "/events") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        try {
            AccessUtils.apiClient.sendMessageBatch(JSONObject().toString())
        } catch (e: Exception) {
        }
        latch.await()
        Assert.assertTrue(called.value)
    }

    companion object {
        @BeforeClass
        fun beforeClass() {
            MParticle.reset(InstrumentationRegistry.getInstrumentation().context)
        }
    }
}
