package com.mparticle.testutils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MPLatch extends CountDownLatch {
    int countDowned = 0;
    int count;
    Handler mHandler;
    AndroidUtils.Mutable<Boolean> timedOut = new AndroidUtils.Mutable<>(false);
    Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            timedOut.value = true;
        }
    };

    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *              before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public MPLatch(int count) {
        super(count);
        this.count = count;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public MPLatch() {
        super(1);
        mHandler = new Handler();
    }

    @Override
    public void countDown() {
        synchronized (this) {
            countDowned++;
            if (countDowned == count) {
                mHandler.removeCallbacks(timeoutRunnable);
            }
            super.countDown();
        }
    }

    @Override
    public void await() throws InterruptedException {
        int timeoutTimeMs = 5 * 1000;
        synchronized (this) {
            if (count == countDowned) {
                return;
            }
        }
        this.await(timeoutTimeMs, TimeUnit.MILLISECONDS);
        mHandler.postDelayed(timeoutRunnable, timeoutTimeMs - 100L);
    }
}
