package com.mparticle.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.fail;

public class TestingUtils {

    private static CountDownLatch lock = new CountDownLatch(1);

    public static void checkAllBool(boolean[] array, int everySeconds, int forSeconds) {
        for (int i = 0; i < forSeconds; i++) {
            if (i % everySeconds == 0) {
                boolean allTrue = true;
                for (Boolean bool: array) {
                    if (!bool) {
                        allTrue = false;
                    }
                }
                if (allTrue) {
                    return;
                }
            }
            try {
                lock.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < array.length; i++) {
            if (!array[i]) {
                fail("failed to satify condition index: " + i);
            }
        }
    }
}
