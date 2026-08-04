package com.mparticle.internal

import android.os.HandlerThread
import android.os.Message
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression tests for [BaseHandler.disable].
 *
 * `disable()` used to wait for an in-flight message with an unbounded, non-yielding spin:
 *
 * ```
 * while (handling) {
 * }
 * ```
 *
 * A loop like that never lets the runtime suspend the thread, so it can starve the garbage
 * collector: if the handler thread is itself blocked on an allocation that needs a collection the
 * spinning thread is preventing, neither side can progress. That is not theoretical -- it was
 * observed with the test thread in state R at 100% CPU holding the mutator lock at this line, while
 * the upload handler sat inside `handleMessage()` in an allocating call, so `handling` never
 * cleared.
 *
 * It matters well beyond the SDK's own shutdown path: `MParticle.reset()` calls it, and
 * `BaseAbstractTest.beforeImpl()` calls `MParticle.reset()` in the `@Before` of every instrumented
 * test. One wedge there stalls the entire `connectedAndroidTest` run, which is why CI saw
 * instrumented jobs burn their whole `timeout-minutes` budget and get reported as "cancelled" with
 * no test report and no stack trace.
 */
@RunWith(AndroidJUnit4::class)
class BaseHandlerDisableTest {

    /**
     * Generous relative to the 5s drain timeout, so this asserts "bounded" rather than asserting an
     * exact duration that would be sensitive to emulator scheduling.
     */
    private val disableMustReturnWithinMs = 30_000L

    /**
     * `disable()` is called on a separate thread rather than on the test thread on purpose. Against
     * the unfixed implementation it never returns, and calling it here would hang the test thread
     * itself -- reproducing the very failure this test exists to catch, rather than reporting it.
     * On a worker thread the failure is a clean, readable assertion instead.
     *
     * The worker is a daemon so it cannot hold the process open. In the failing case it does keep
     * spinning for the remainder of the run, but that only happens when the bug is already present.
     */
    @Test
    fun disableReturnsEvenWhileAMessageIsStillBeingHandled() {
        val thread = HandlerThread("mp-basehandler-disable-test").apply { start() }
        val messageIsBeingHandled = CountDownLatch(1)
        val releaseHandlerThread = CountDownLatch(1)
        try {
            val handler = object : BaseHandler(thread.looper) {
                override fun handleMessageImpl(msg: Message?) {
                    messageIsBeingHandled.countDown()
                    // Stay inside handleMessage() for the whole of the disable() call below, so
                    // that `handling` is true the entire time it is observed. This is the condition
                    // the old spin loop could not escape.
                    releaseHandlerThread.await(disableMustReturnWithinMs * 2, TimeUnit.MILLISECONDS)
                }
            }

            handler.sendMessage(handler.obtainMessage(1))
            assertTrue(
                "Handler never picked up the message, so the scenario under test was never set up",
                messageIsBeingHandled.await(10, TimeUnit.SECONDS),
            )

            val disableReturned = CountDownLatch(1)
            Thread {
                handler.disable(true)
                disableReturned.countDown()
            }.apply {
                name = "mp-basehandler-disable-caller"
                isDaemon = true
            }.start()

            assertTrue(
                "disable() did not return within ${disableMustReturnWithinMs}ms with a message in " +
                    "flight; it is spinning instead of giving up waiting",
                disableReturned.await(disableMustReturnWithinMs, TimeUnit.MILLISECONDS),
            )
        } finally {
            releaseHandlerThread.countDown()
            thread.quitSafely()
        }
    }

    /**
     * Bounding the wait must not turn into not waiting at all: a message that finishes well within
     * the drain timeout should still have completed by the time `disable()` returns.
     *
     * Safe to run on the test thread -- the in-flight message finishes in 500ms, so this cannot hang
     * even against the unfixed implementation.
     */
    @Test
    fun disableStillWaitsForAnInFlightMessageToFinish() {
        val thread = HandlerThread("mp-basehandler-disable-drain-test").apply { start() }
        val messageIsBeingHandled = CountDownLatch(1)
        val finishedHandling = CountDownLatch(1)
        try {
            val handler = object : BaseHandler(thread.looper) {
                override fun handleMessageImpl(msg: Message?) {
                    messageIsBeingHandled.countDown()
                    Thread.sleep(500)
                    finishedHandling.countDown()
                }
            }

            handler.sendMessage(handler.obtainMessage(1))
            assertTrue(
                "Handler never picked up the message, so the scenario under test was never set up",
                messageIsBeingHandled.await(10, TimeUnit.SECONDS),
            )

            handler.disable(true)

            assertTrue(
                "disable() returned before the in-flight message finished",
                finishedHandling.count == 0L,
            )
        } finally {
            thread.quitSafely()
        }
    }
}
