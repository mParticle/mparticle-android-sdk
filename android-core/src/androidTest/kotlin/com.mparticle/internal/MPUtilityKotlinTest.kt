package com.mparticle.internal

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseAbstractTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.RandomUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class MPUtilityKotlinTest: BaseAbstractTest() {
    val cruft = RandomUtils().getAlphaNumericString(1000)

    @Test
    fun testFileSynchronization() {
        val thread1Handler = HandlerThread("some thread1")
            .apply { start() }
            .let { Handler(it.looper) }
        val thread2Handler = HandlerThread("some thread2")
            .apply { start() }
            .let { Handler(it.looper) }
        val assertionThreadHandler = HandlerThread("some thread2")
            .apply { start() }
            .let { Handler(it.looper) }

        val thread1Count = AndroidUtils.Mutable(100)
        val thread2Count = AndroidUtils.Mutable(100)

        var runnable = {}
        runnable = {
            val contents = MPUtilityKotlin.readFile(mContext, "testFile")
            if (contents != cruft) {
                fail("Corrupted file! File writes may not be synchronized")
            }
            MPUtilityKotlin.writeToFile(mContext, "testFile",cruft)
            val loopCount = if (Looper.myLooper() == thread1Handler.looper) {
                thread1Count
            } else {
                thread2Count
            }
            loopCount.value = loopCount.value - 1
            if (loopCount.value > 0) {
                if (Looper.myLooper() == thread1Handler.looper) {
                    thread1Handler.post(runnable)
                } else {
                    thread2Handler.post(runnable)
                }
            }
        }
        MPUtilityKotlin.writeToFile(mContext, "testFile", cruft)
        thread1Handler.post(runnable)
        thread2Handler.post(runnable)

        val latch = MPLatch(1)
        assertionThreadHandler.post {
            while (thread2Count.value > 0 || thread1Count.value > 0) {

            }
            latch.countDown()
        }
        latch.await()

        assertEquals(cruft, MPUtilityKotlin.readFile(mContext, "testFile"))



    }
}