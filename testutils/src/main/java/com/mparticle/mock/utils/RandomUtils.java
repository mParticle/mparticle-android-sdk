package com.mparticle.mock.utils;

import static org.junit.Assert.assertTrue;

import com.mparticle.MParticle;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utilities for generating tests with Randomness.
 */
public class RandomUtils {
    private static final String sAlpha = "abcdefghijklmnopqrstuvwxyzABC ,.";
    private static final String sNumbers = "0123456789";
    private Random random = new Random();

    private static RandomUtils instance;

    /**
     * Every IdentityType that can legitimately appear in a user identity map. Alias is excluded
     * from the pool rather than removed after the draw -- removing it afterwards can leave the
     * map empty, which silently changes the meaning of any request built from it.
     */
    private static final List<MParticle.IdentityType> ASSIGNABLE_IDENTITY_TYPES =
            assignableIdentityTypes();

    private static List<MParticle.IdentityType> assignableIdentityTypes() {
        List<MParticle.IdentityType> types =
                new ArrayList<MParticle.IdentityType>(Arrays.asList(MParticle.IdentityType.values()));
        types.remove(MParticle.IdentityType.Alias);
        return Collections.unmodifiableList(types);
    }

    public static RandomUtils getInstance() {
        if (instance == null) {
            instance = new RandomUtils();
        }
        return instance;
    }

    /**
     * @return between 1 and the number of assignable IdentityTypes distinct random user
     * identities. Never empty.
     */
    public Map<MParticle.IdentityType, String> getRandomUserIdentities() {
        int poolSize = ASSIGNABLE_IDENTITY_TYPES.size();
        int numIdentities = randomInt(1, poolSize + 1);

        Map<MParticle.IdentityType, String> randomIdentities = new HashMap<MParticle.IdentityType, String>();
        Set<Integer> identityIndices = randomIntSet(0, poolSize, numIdentities);
        for (Integer identityIndex : identityIndices) {
            randomIdentities.put(ASSIGNABLE_IDENTITY_TYPES.get(identityIndex), getAlphaNumericString(randomInt(1, 55)));
        }
        return randomIdentities;
    }

    public Map<String, List<String>> getRandomCustomFlags(int count) {
        Map<String, List<String>> customFlags = new HashMap<>();
        for (Map.Entry<String, String> entry : getRandomAttributes(count).entrySet()) {
            List<String> flags = new ArrayList<>();
            if (entry.getValue() != null) {
                flags.add(entry.getValue());
                flags.add(getAlphaNumericString(20));
                flags.add(getAlphaNumericString(20));
                customFlags.put(entry.getKey(), flags);
            } else {
                List<String> nullList = new ArrayList<>();
                nullList.add(null);
                customFlags.put(entry.getKey(), nullList);
            }
        }
        return customFlags;
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

    public String getAlphaNumericString(int lengthLowerBound, int lengthUpperBound) {
        return getAlphaNumericString(randomInt(24, 256));
    }

    public String getAlphaNumericString(int length) {
        String characters = getAlphNumeric();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(characters.charAt(randomInt(0, characters.length() - 1)));
        }
        return builder.toString();
    }

    //inclusive of low range, less than high range
    public int randomInt(int from, int to) {
        int randomInt = Math.abs(random.nextInt());
        int range = randomInt % (to - from);
        return (range) + from;
    }

    public Set<Integer> randomIntSet(int fromRange, int toRange, int num) {
        if (toRange < fromRange) {
            throw new IllegalArgumentException("toRange must be greater than fromRange.");
        }
        if (toRange - fromRange < num) {
            throw new IllegalArgumentException("Range must be grater than num, since a Set may only contain one instance of an Entry, you will be unable to fill the Set with these parameters.");
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
            int randomInt = randomInt(-10, 10);
            assertTrue(randomInt >= -10);
            assertTrue(randomInt <= 10);
        }
    }
}
