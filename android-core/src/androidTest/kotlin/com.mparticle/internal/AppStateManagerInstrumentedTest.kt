package com.mparticle.internal

import android.content.Context
import android.util.MutableBoolean
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.database.services.AccessUtils
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AppStateManagerInstrumentedTest : BaseCleanStartedEachTest() {
    var mAppStateManager: AppStateManager? = null
    @Before
    @Throws(Exception::class)
    fun before() {
        mAppStateManager = MParticle.getInstance()?.Internal()?.appStateManager
        MParticle.getInstance()?.Internal()?.configManager?.setMpid(Constants.TEMPORARY_MPID, false)
    }

    @Test
    @Throws(Exception::class)
    fun testEndSessionMultipleMpids() {
        val mpids: MutableSet<Long> = HashSet()
        for (i in 0..4) {
            mpids.add(ran.nextLong())
        }
        mAppStateManager?.ensureActiveSession()
        for (mpid in mpids) {
            mAppStateManager?.session?.addMpid(mpid)
        }
        val checked = BooleanArray(1)
        val latch: CountDownLatch = MPLatch(1)
        AccessUtils.setMessageStoredListener(
            MParticleDBManager.MessageListener { message ->
                if (message.messageType == Constants.MessageType.SESSION_END) {
                    try {
                        val mpidsArray =
                            message.getJSONArray(Constants.MessageKey.SESSION_SPANNING_MPIDS)
                        Assert.assertEquals(mpidsArray.length().toLong(), mpids.size.toLong())
                        for (i in 0 until mpidsArray.length()) {
                            if (!mpids.contains(mpidsArray.getLong(i))) {
                                return@MessageListener
                            }
                        }
                        checked[0] = true
                        latch.countDown()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
        mAppStateManager?.endSession()
        latch.await()
        Assert.assertTrue(checked[0])
    }

    @Test
    @Throws(Exception::class)
    fun testDontIncludeDefaultMpidSessionEnd() {
        val mpids: MutableSet<Long> = HashSet()
        for (i in 0..4) {
            mpids.add(ran.nextLong())
        }
        mpids.add(Constants.TEMPORARY_MPID)
        mAppStateManager?.ensureActiveSession()
        for (mpid in mpids) {
            mAppStateManager?.session?.addMpid(mpid)
        }
        val latch: CountDownLatch = MPLatch(1)
        val checked = MutableBoolean(false)
        AccessUtils.setMessageStoredListener(
            MParticleDBManager.MessageListener { message ->
                if (message.messageType == Constants.MessageType.SESSION_END) {
                    try {
                        val mpidsArray =
                            message.getJSONArray(Constants.MessageKey.SESSION_SPANNING_MPIDS)
                        if (mpidsArray.length() == mpids.size - 1) {
                            for (i in 0 until mpidsArray.length()) {
                                if (!mpids.contains(mpidsArray.getLong(i)) || mpidsArray.getLong(i) == Constants.TEMPORARY_MPID) {
                                    return@MessageListener
                                }
                            }
                            checked.value = true
                            latch.countDown()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
        mAppStateManager?.endSession()
        latch.await()
        Assert.assertTrue(checked.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testOnApplicationForeground() {
        val latch: CountDownLatch = MPLatch(2)
        val kitManagerTester = KitManagerTester(mContext, latch)
        com.mparticle.AccessUtils.setKitManager(kitManagerTester)
        goToBackground()
        Assert.assertNull(mAppStateManager?.currentActivity)
        Thread.sleep(AppStateManager.ACTIVITY_DELAY + 100)
        goToForeground()
        Assert.assertNotNull(mAppStateManager?.currentActivity?.get())
        latch.await()
        Assert.assertTrue(kitManagerTester.onApplicationBackgroundCalled)
        Assert.assertTrue(kitManagerTester.onApplicationForegroundCalled)
    }

    internal inner class KitManagerTester(context: Context?, var latch: CountDownLatch) :
        KitFrameworkWrapper(
            context,
            object : ReportingManager {
                override fun log(message: JsonReportingMessage) {
                    //do nothing
                }

                override fun logAll(messageList: List<JsonReportingMessage>) {
                    //do nothing
                }
            },
            MParticle.getInstance()?.Internal()?.configManager,
            MParticle.getInstance()?.Internal()?.appStateManager,
            MParticleOptions.builder(mContext).credentials("some", "key").build()
        ) {
        var onApplicationBackgroundCalled = false
        var onApplicationForegroundCalled = false
        override fun onApplicationBackground() {
            Assert.assertNull(currentActivity)
            onApplicationBackgroundCalled = true
            latch.countDown()
        }

        override fun onApplicationForeground() {
            Assert.assertNotNull(currentActivity.get())
            onApplicationForegroundCalled = true
            latch.countDown()
        }
    }
}