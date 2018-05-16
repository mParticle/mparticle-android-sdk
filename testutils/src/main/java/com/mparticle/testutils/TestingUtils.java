package com.mparticle.testutils;

import static junit.framework.Assert.fail;

/**
 * Utility methods for validating tests, mostly control-flow type stuff
 */
public class TestingUtils {

    public static void checkAllBool(boolean[] array) {
        checkAllBool(array, 1, 10);
    }

    public static void checkAllBool(boolean[] array, int eveyNDecisenconds, int forDeciseconds) {
        checkAllBool(array, eveyNDecisenconds, forDeciseconds, null);
    }

    public static void checkAllBool(boolean[] array, int everyNDeciseconds, int forDeciseconds, OnFailureCallback onFailure) {
        for (int i = 0; i < forDeciseconds; i++) {
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
                if (onFailure != null) {
                    onFailure.onPreFailure();
                }
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

    public interface OnFailureCallback {
        void onPreFailure();
    }
}
