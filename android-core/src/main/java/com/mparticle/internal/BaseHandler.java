package com.mparticle.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.mparticle.internal.listeners.InternalListenerManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class BaseHandler extends Handler {
    /**
     * Longest that {@link #disable(boolean)} will wait for an in-flight message to finish before
     * giving up and returning. The handler is already flagged disabled and its queue already
     * cleared by that point, so returning early only means one in-flight message may still be
     * completing on the handler thread.
     */
    private static final long DISABLE_DRAIN_TIMEOUT_MS = 5000;

    private volatile boolean disabled;
    private volatile boolean handling;

    public BaseHandler() {
    }

    public BaseHandler(Looper looper) {
        super(looper);
    }

    public void disable(boolean disable) {
        this.disabled = disable;
        removeCallbacksAndMessages(null);
        // Wait for any in-flight handleMessage() to finish, but never spin without yielding: a
        // tight `while (handling) {}` loop contains no suspend point, so it can starve the
        // garbage collector indefinitely. If the handler thread is itself blocked waiting on a
        // GC that this thread is preventing, neither side can progress. Thread.yield() gives the
        // runtime a suspend point, and the deadline bounds the wait either way.
        long deadline = SystemClock.uptimeMillis() + DISABLE_DRAIN_TIMEOUT_MS;
        while (handling && SystemClock.uptimeMillis() < deadline) {
            Thread.yield();
        }
        if (handling) {
            Logger.error("Handler: " + getClass().getName() + " still had a message in flight after "
                    + DISABLE_DRAIN_TIMEOUT_MS + "ms; giving up waiting for it to drain.");
        }
    }


    public boolean isDisabled() {
        return disabled;
    }

    void await(CountDownLatch latch) {
        this.sendMessage(obtainMessage(-1, latch));
    }

    @Override
    public final void handleMessage(Message msg) {
        if (disabled) {
            Logger.error("Handler: " + getClass().getName() + " is destroyed! Message: \"" + msg.toString() + "\" will not be processed");
            return;
        }
        handling = true;
        try {
            if (msg != null && msg.what == -1 && msg.obj instanceof CountDownLatch) {
                ((CountDownLatch) msg.obj).countDown();
            } else {
                if (InternalListenerManager.isEnabled()) {
                    InternalListenerManager.getListener().onThreadMessage(getClass().getName(), msg, true);
                }
                try {
                    handleMessageImpl(msg);
                } catch (OutOfMemoryError error) {
                    Logger.error("Out of memory");
                }
            }
        } finally {
            handling = false;
        }
    }

    @Override
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        if (disabled) {
            return false;
        }
        if (InternalListenerManager.isEnabled()) {
            InternalListenerManager.getListener().onThreadMessage(getClass().getName(), msg, false);
        }
        return super.sendMessageAtTime(msg, uptimeMillis);
    }

    //Override this in order to handle messages
    public void handleMessageImpl(Message msg) {
    }
}
