package com.mparticle.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mparticle.internal.listeners.InternalListenerManager
import java.util.concurrent.CountDownLatch

open class BaseHandler : Handler {
    @Volatile
    private var disabled = false

    @Volatile
    private var handling = false

    constructor() : super()

    constructor(looper: Looper?) : super(looper!!)

    open fun disable(disable: Boolean) {
        this.disabled = disable
        removeCallbacksAndMessages(null)
        // Wait for any in-flight handleMessage() to finish, but never spin without yielding: a
        // tight `while (handling) {}` loop contains no suspend point, so it can starve the
        // garbage collector indefinitely. If the handler thread is itself blocked waiting on a
        // GC that this thread is preventing, neither side can progress. Thread.yield() gives the
        // runtime a suspend point, and the deadline bounds the wait either way.
        val deadline = SystemClock.uptimeMillis() + DISABLE_DRAIN_TIMEOUT_MS
        while (handling && SystemClock.uptimeMillis() < deadline) {
            Thread.`yield`()
        }
        if (handling) {
            Logger.error(
                "Handler: " + javaClass.name + " still had a message in flight after " +
                    DISABLE_DRAIN_TIMEOUT_MS + "ms; giving up waiting for it to drain.",
            )
        }
    }

    open val isDisabled: Boolean
        get() = disabled

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun await(latch: CountDownLatch?) {
        this.sendMessage(obtainMessage(-1, latch))
    }

    final override fun handleMessage(msg: Message) {
        if (disabled) {
            Logger.error("Handler: " + javaClass.name + " is destroyed! Message: \"" + msg.toString() + "\" will not be processed")
            return
        }
        handling = true
        try {
            if (msg.what == -1 && msg.obj is CountDownLatch) {
                (msg.obj as CountDownLatch).countDown()
            } else {
                if (InternalListenerManager.isEnabled) {
                    InternalListenerManager.listener.onThreadMessage(javaClass.name, msg, true)
                }
                try {
                    handleMessageImpl(msg)
                } catch (error: OutOfMemoryError) {
                    Logger.error("Out of memory")
                }
            }
        } finally {
            handling = false
        }
    }

    override fun sendMessageAtTime(msg: Message, uptimeMillis: Long): Boolean {
        if (disabled) {
            return false
        }
        if (InternalListenerManager.isEnabled) {
            InternalListenerManager.listener.onThreadMessage(javaClass.name, msg, false)
        }
        return super.sendMessageAtTime(msg, uptimeMillis)
    }

    // Override this in order to handle messages
    open fun handleMessageImpl(msg: Message?) {
    }

    private companion object {
        /**
         * Longest that [disable] will wait for an in-flight message to finish before
         * giving up and returning. The handler is already flagged disabled and its queue already
         * cleared by that point, so returning early only means one in-flight message may still be
         * completing on the handler thread.
         */
        private const val DISABLE_DRAIN_TIMEOUT_MS = 5000L
    }
}
