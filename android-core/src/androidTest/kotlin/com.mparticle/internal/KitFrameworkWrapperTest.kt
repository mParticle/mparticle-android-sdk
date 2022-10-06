package com.mparticle.internal

import android.content.Context
import android.util.MutableBoolean
import com.mparticle.AccessUtils
import com.mparticle.MParticle
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch

class KitFrameworkWrapperTest : BaseCleanStartedEachTest() {
    private fun setKitManager(kitManager: KitFrameworkWrapper) {
        AccessUtils.setKitManager(kitManager)
        com.mparticle.identity.AccessUtils.setKitManager(kitManager)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIdentify() {
        val mpid = ran.nextLong()
        val latch: CountDownLatch = MPLatch(1)
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(mContext) {
            override fun onIdentifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyIdentify(mpid)
        MParticle.getInstance()?.Identity()?.identify(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogin() {
        val mpid = ran.nextLong()
        val latch: CountDownLatch = MPLatch(1)
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(mContext) {
            override fun onLoginCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyLogin(mpid)
        MParticle.getInstance()?.Identity()?.login(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogout() {
        val mpid = ran.nextLong()
        val latch: CountDownLatch = MPLatch(1)
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(mContext) {
            override fun onLogoutCompleted(user: MParticleUser, request: IdentityApiRequest) {
                if (user.id == mStartingMpid) {
                    return
                }
                Assert.assertEquals(mpid, user.id)
                called.value = true
                latch.countDown()
            }
        })
        mServer.setupHappyLogout(mpid)
        MParticle.getInstance()?.Identity()?.logout(IdentityApiRequest.withEmptyUser().build())
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModify() {
        val latch: CountDownLatch = MPLatch(1)
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(mContext) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                Assert.assertEquals(mStartingMpid.toLong(), user.id)
                called.value = true
                latch.countDown()
            }
        })
        MParticle.getInstance()
            ?.Identity()?.modify(
                IdentityApiRequest.withUser(MParticle.getInstance()?.Identity()?.currentUser)
                    .build()
            )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyUserChanged() {
        val latch: CountDownLatch = MPLatch(1)
        val called = MutableBoolean(false)
        setKitManager(object : StubKitManager(mContext) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                Assert.assertEquals(mStartingMpid.toLong(), user.id)
                called.value = true
                latch.countDown()
            }
        })
        MParticle.getInstance()?.Identity()?.modify(IdentityApiRequest.withEmptyUser().build())
        MParticle.getInstance()?.Internal()?.configManager?.setMpid(0, ran.nextBoolean())
        latch.await()
        val latch2: CountDownLatch = MPLatch(1)
        val mpid2 = ran.nextLong()
        MParticle.getInstance()?.Internal()?.configManager?.setMpid(mpid2, ran.nextBoolean())
        Assert.assertTrue(called.value)
        called.value = false
        setKitManager(object : StubKitManager(mContext) {
            override fun onModifyCompleted(user: MParticleUser, request: IdentityApiRequest) {
                Assert.assertEquals(mpid2, user.id)
                called.value = true
                latch2.countDown()
            }
        })
        MParticle.getInstance()
            ?.Identity()?.modify(
                IdentityApiRequest.withUser(MParticle.getInstance()?.Identity()?.currentUser)
                    .build()
            )
        MParticle.getInstance()?.Internal()?.configManager?.setMpid(
            ran.nextLong(),
            ran.nextBoolean()
        )
        latch2.await()
        Assert.assertTrue(called.value)
    }

    internal open class StubKitManager(context: Context?) :
        KitFrameworkWrapper(context, null, null, null, true, null) {
        init {
            setKitManager(null)
        }

        override fun loadKitLibrary() {}
    }
}