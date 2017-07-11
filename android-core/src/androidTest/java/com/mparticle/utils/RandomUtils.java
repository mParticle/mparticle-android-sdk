package com.mparticle.utils;

import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class RandomUtils {
    private static final String sAlpha = "abcdefghijklmnopqrstuvwxyzABC ,.";
    private static final String sNumbers = "0123456789";

    private static RandomUtils instance;

    public static RandomUtils getInstance() {
        if  (instance == null) {
            instance = new RandomUtils();
        }
        return instance;
    }


    public String getAlphaNumericString(int length) {
        String characters = getAlphNumeric();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++){
            builder.append(characters.charAt(randomInt(0, characters.length() - 1)));
        }
        return builder.toString();
    }

    public int randomInt(int from, int to) {
        int random = Math.abs(new Random().nextInt());
        int range = random % (to - from);
        return (range) + from;
    }

    private String getAlphNumeric() {
        return sAlpha + sNumbers;
    }

    @Test
    public void testRandomInt() throws Exception {
        for (int i = 0; i < 50; i++) {
            int random = randomInt(-10, 10);
            assertTrue(random >= -10);
            assertTrue(random <= 10);
        }
    }
}
