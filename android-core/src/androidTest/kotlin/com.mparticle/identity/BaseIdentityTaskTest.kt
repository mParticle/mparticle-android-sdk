package com.mparticle.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.MParticle
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class BaseIdentityTaskTest : BaseCleanStartedEachTest() {
    private lateinit var result: IdentityApiResult
    private val error = IdentityHttpResponse(400, "error")

    @Before
    fun createResult() {
        result = IdentityApiResult(MParticle.getInstance()!!.Identity().currentUser!!, null)
    }

    @Test
    fun successListenerAddedAfterCompletionIsCalled() {
        val task = BaseIdentityTask()
        task.setSuccessful(result)
        idleMain()

        var received: IdentityApiResult? = null
        val calls = AtomicInteger()
        task.addSuccessListener {
            received = it
            calls.incrementAndGet()
        }
        idleMain()

        assertEquals(1, calls.get())
        assertSame(result, received)
    }

    @Test
    fun failureListenerAddedAfterFailureIsCalled() {
        val task = BaseIdentityTask()
        task.setFailed(error)
        idleMain()

        var received: IdentityHttpResponse? = null
        val calls = AtomicInteger()
        task.addFailureListener {
            received = it
            calls.incrementAndGet()
        }
        idleMain()

        assertEquals(1, calls.get())
        assertSame(error, received)
    }

    @Test
    fun listenerAddedWhileCallbackQueuedIsCalledOnce() {
        val task = BaseIdentityTask()
        val calls = AtomicInteger()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            task.setSuccessful(result)
            task.addSuccessListener { calls.incrementAndGet() }
        }
        idleMain()

        assertEquals(1, calls.get())
    }

    @Test
    fun listenerAddedBetweenCompletionsIsCalledForEach() {
        val task = BaseIdentityTask()
        task.setSuccessful(result)
        idleMain()

        val received = mutableListOf<IdentityApiResult>()
        task.addSuccessListener { received.add(it) }
        idleMain()
        val laterResult = IdentityApiResult(result.user, null)
        task.setSuccessful(laterResult)
        idleMain()

        assertEquals(2, received.size)
        assertSame(result, received[0])
        assertSame(laterResult, received[1])
    }

    @Test
    fun listenersForTheOtherOutcomeAreNotCalled() {
        val calls = AtomicInteger()
        val failed = BaseIdentityTask()
        failed.setFailed(error)
        failed.addSuccessListener { calls.incrementAndGet() }
        val succeeded = BaseIdentityTask()
        succeeded.setSuccessful(result)
        succeeded.addFailureListener { calls.incrementAndGet() }
        idleMain()

        assertEquals(0, calls.get())
    }

    @Test
    fun listenerAddedFromAnotherThreadDuringCompletionIsCalledOnce() {
        repeat(200) { iteration ->
            val task = BaseIdentityTask()
            val calls = AtomicInteger()
            val start = CountDownLatch(1)
            val completer =
                Thread {
                    start.await()
                    task.setSuccessful(result)
                }
            val adder =
                Thread {
                    start.await()
                    task.addSuccessListener { calls.incrementAndGet() }
                }
            completer.start()
            adder.start()
            start.countDown()
            completer.join()
            adder.join()
            idleMain()

            assertEquals("iteration $iteration", 1, calls.get())
        }
    }

    private fun idleMain() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
