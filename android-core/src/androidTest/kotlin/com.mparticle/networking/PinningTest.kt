package com.mparticle.networking

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.database.UploadSettings
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.RandomUtils
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.CountDownLatch

open class PinningTest : BaseCleanStartedEachTest() {
    var called: AndroidUtils.Mutable<Boolean> = AndroidUtils.Mutable(false)
    var latch: CountDownLatch = MPLatch(1)

    protected open fun shouldPin(): Boolean {
        return true
    }

    @Before
    fun before() {
        called = AndroidUtils.Mutable(false)
        latch = MPLatch(1)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientLogin() {
        PinningTestHelper(mContext, "/login") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()
            ?.Identity()?.login(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientLogout() {
        PinningTestHelper(mContext, "/logout") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()
            ?.Identity()?.logout(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientIdentify() {
        PinningTestHelper(mContext, "/identify") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()
            ?.Identity()?.identify(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentityClientModify() {
        PinningTestHelper(mContext, "/modify") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()
            ?.Identity()?.modify(
                IdentityApiRequest.withEmptyUser()
                    .customerId(RandomUtils().getAlphaNumericString(25)).build()
            )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testMParticleClientSendMessage() {
        PinningTestHelper(mContext, "/events") { pinned ->
            Assert.assertEquals(shouldPin(), pinned)
            called.value = true
            latch.countDown()
        }
        try {
            AccessUtils.getApiClient().sendMessageBatch(JSONObject().toString(), UploadSettings("apiKey", "secret", NetworkOptions.builder().build(), "", ""))
        } catch (_: Exception) {
        }
        latch.await()
        Assert.assertTrue(called.value)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            MParticle.reset(InstrumentationRegistry.getInstrumentation().context)
        }
    }
}
