package com.mparticle.testutils;

import com.mparticle.MParticle;

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
    private static final String sAlpha = "abcdefghijklmnopqrstuvwxyzABC";
    private static final String sNumbers = "0123456789";
    private static final String sCharacters = " ,.";

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

    public Map<MParticle.IdentityType, String> getRandomUserIdentities() {
        return getRandomUserIdentities(null);
    }

    /**
     * @param max the most identities to return, or null for no limit
     * @return between 1 and {@code max} distinct random user identities. Never empty.
     */
    public Map<MParticle.IdentityType, String> getRandomUserIdentities(Integer max) {
        int poolSize = ASSIGNABLE_IDENTITY_TYPES.size();
        int upperBound = (max != null && max < poolSize) ? Math.max(1, max) : poolSize;
        int numIdentities = randomInt(1, upperBound + 1);

        Map<MParticle.IdentityType, String> randomIdentities = new HashMap<MParticle.IdentityType, String>();
        Set<Integer> identityIndices = randomIntSet(0, poolSize, numIdentities);
        for (Integer identityIndex : identityIndices) {
            randomIdentities.put(ASSIGNABLE_IDENTITY_TYPES.get(identityIndex), getAlphaNumericString(randomInt(1, 55)));
        }
        return randomIdentities;
    }

    public Map<String, String> getRandomAttributes(int count) {
        return getRandomAttributes(count, true);
    }

    public Map<String, String> getRandomAttributes(int count, boolean allowNull) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (int i = 0; i < count; i++) {
            String key = "";
            String value = "";
            if (randomInt(0, 5) == 0 && allowNull) {
                key = null;
            } else {
                key = getAlphaNumericString(20);
            }
            if (randomInt(0, 5) == 0 && allowNull) {
                value = null;
            } else {
                value = getAlphaNumericString(20);
            }
            attributes.put(key, value);
        }
        return attributes;
    }

    public Map<String, Object> getRandomUserAttributes(int count) {
        Map<String, Object> attributes = new HashMap<>();
        for (Map.Entry<String, String> entry : getRandomAttributes(count).entrySet()) {
            attributes.put(entry.getKey(), entry.getValue());
        }
        return attributes;
    }

    public String getAlphaString(int length) {
        return getRandomString(length, sAlpha);
    }

    public String getAlphaNumericString(int length) {
        return getRandomString(length, sAlpha + sNumbers + sCharacters);

    }

    private String getRandomString(int length, String characters) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(characters.charAt(randomInt(0, characters.length() - 1)));
        }
        return builder.toString();
    }

    //Inclusive of low range, less than high range.
    public int randomInt(int from, int to) {
        int random = Math.abs(new Random().nextInt());
        int range = random % (to - from);
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

    public long randomLong(long from, long to) {
        long random = Math.abs(new Random().nextLong());
        long range = random % (to - from);
        return range + from;
    }
}
