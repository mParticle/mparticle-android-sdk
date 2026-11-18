package com.mparticle.testutils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CountDownLatch} whose {@link #await()} fails the test when the count does not reach zero in time, so a lost
 * callback fails where it was expected instead of surfacing later as an unrelated assertion.
 */
public class MPLatch extends CountDownLatch {
    public static final long TIMEOUT_MS = 5000L;
    private final long initialCount;

    public MPLatch(int count) {
        super(count);
        initialCount = count;
    }

    public MPLatch() {
        this(1);
    }

    @Override
    public void await() throws InterruptedException {
        if (!await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            throw new AssertionError("MPLatch timed out after " + TIMEOUT_MS + " ms with "
                    + (initialCount - getCount()) + " of " + initialCount + " count-downs");
        }
    }
}
