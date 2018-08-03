package com.mparticle.testutils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MPLatch extends CountDownLatch {
    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *              before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public MPLatch(int count) {
        super(count);
    }

    @Override
    public void await() throws InterruptedException {
        this.await(1000, TimeUnit.MILLISECONDS);
    }
}
