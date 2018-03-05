package com.mparticle.mock.utils;

import com.mparticle.ConsentEvent;
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
        int numIdentities = randomInt(1, identityTypeLength);
        Set<Integer> identityIndices = randomIntSet(0, identityTypeLength, numIdentities);
        for (Integer identityIndex: identityIndices) {
            randomIdentities.put(MParticle.IdentityType.values()[identityIndex], getAlphaNumericString(randomInt(1, 55)));
        }
        randomIdentities.remove(MParticle.IdentityType.Alias);
        return randomIdentities;
    }

    public Map<String, String> getRandomAttributes(int count) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (int i = 0; i < count; i++) {
            String key = "";
            String value = "";
            if (randomInt(0, 5) == 0) {
                key = null;
            } else {
                key = getAlphaNumericString(20);
            }
            if (randomInt(0, 5) == 0) {
                value = null;
            } else {
                value = getAlphaNumericString(20);
            }
            attributes.put(key, value);
        }
        return attributes;
    }

    public ConsentEvent getRandomMPConsentEvent() {
        Random random = new Random();
        ConsentEvent.Builder builder = ConsentEvent.builder(random.nextBoolean());
        //occasionally return the most basic version
        if (randomInt(0, 5) % 5 == 0) {
            return  builder.build();
        }
        if (random.nextBoolean()) {
            builder.consentCategory(ConsentEvent.ConsentCategory.values()[randomInt(0, ConsentEvent.ConsentCategory.values().length)]);
        }
        if (random.nextBoolean()) {
            builder.consentLocation(getAlphaNumericString(randomInt(0, 255)));
        }
        if (random.nextBoolean()) {
            builder.document(getAlphaNumericString(randomInt(0, 255)));
        }
        if (random.nextBoolean()) {
            builder.regulation(ConsentEvent.Regulation.values()[randomInt(0, ConsentEvent.Regulation.values().length)]);
        }
        if (random.nextBoolean()) {
            builder.timestamp(System.currentTimeMillis() + randomInt(-100000, 100000));
        }
        if (random.nextBoolean()) {
            builder.hardwareId(getAlphaNumericString(randomInt(0, 255)));
        }
        if (random.nextBoolean()) {
            builder.purpose(getAlphaNumericString(randomInt(0, 255)));
        }
        if (random.nextBoolean()) {
            int count = randomInt(0, 10);
            for (int i = 0; i < count; i++) {
                String key = "";
                String value = "";
                if (randomInt(0, 5) % 5 == 0) {
                    key = null;
                } else {
                    key = getAlphaNumericString(20);
                }
                if (randomInt(0, 5) % 5 == 0) {
                    value = null;
                } else {
                    value = getAlphaNumericString(20);
                }
                builder.customAttribute(key, value);
            }
        }
        if (random.nextBoolean()) {
            builder.customAttributes(getRandomAttributes(randomInt(0, 10)));
        }
        return builder.build();
    }

    public String getAlphaNumericString(int length) {
        String characters = getAlphNumeric();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++){
            builder.append(characters.charAt(randomInt(0, characters.length() - 1)));
        }
        return builder.toString();
    }

    //inclusive of low range, less than high range
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

    public long randomLong(long from, long to) {
        long random = Math.abs(new Random().nextLong());
        long range = random % (to - from);
        return range + from;
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
