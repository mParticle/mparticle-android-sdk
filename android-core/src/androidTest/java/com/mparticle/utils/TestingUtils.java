package com.mparticle.utils;

import static junit.framework.Assert.fail;

/**
 * Utility methods for validating tests, mostly control-flow type stuff
 */
public class TestingUtils {

    public static void checkAllBool(boolean[] array, int everyNDeciseconds, int forDeciseconds) {
        for (int i = 0; i < forDeciseconds + 2; i++) {
            if (i % everyNDeciseconds == 0) {
                boolean allTrue = true;
                for (Boolean bool : array) {
                    if (!bool) {
                        allTrue = false;
                    }
                }
                if (allTrue) {
                    return;
                }
            }
            pause(everyNDeciseconds);
        }
        for (int i = 0; i < array.length; i++) {
            if (!array[i]) {
                fail("failed to satify condition index: " + i);
            }
        }
    }

    private static void pause(int seconds) {
        try {
            Thread.sleep(seconds * 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
