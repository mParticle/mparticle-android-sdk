package com.mparticle.utils;

import com.mparticle.MParticle;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Utilities for generating tests with Randomness
 */
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

    public Map<MParticle.IdentityType, String> getRandomUserIdentities() {
        Map<MParticle.IdentityType, String> randomIdentities = new HashMap<MParticle.IdentityType, String>();

        int identityTypeLength = MParticle.IdentityType.values().length;
        int numIdentities = randomInt(0, identityTypeLength);
        Set<Integer> identityIndecis = randomIntSet(0, identityTypeLength, numIdentities);
        for (Integer identityIndex: identityIndecis) {
            randomIdentities.put(MParticle.IdentityType.values()[identityIndex], getAlphaNumericString(randomInt(1, 55)));
        }
        randomIdentities.remove(MParticle.IdentityType.Alias);
        return randomIdentities;
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

    public Set<Integer> randomIntSet(int fromRange, int toRange, int num) {
        if (toRange < fromRange) {
            throw new IllegalArgumentException("toRange must be greater than fromRange");
        }
        if (toRange - fromRange < num) {
            throw new IllegalArgumentException("range must be grater than num, since a Set may only contain one instance of an Entry, you will be unable to fill the Set with these parameters");
        }
        Set<Integer> set = new TreeSet<Integer>();
        for (int i = 0; i < num; i++) {
            int randomInt = randomInt(fromRange, toRange);
            if (set.contains(randomInt)) {
                i--;
            } else {
                set.add(randomInt);
            }
        }
        return set;
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
