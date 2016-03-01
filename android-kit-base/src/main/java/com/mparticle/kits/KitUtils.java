package com.mparticle.kits;


import com.mparticle.internal.MPUtility;

import java.math.BigInteger;

/**
 * Mixin/Utility class for use in Kit implementations.
 */
public final class KitUtils {
    /**
     *
     * mParticle attribute keys defined by the `MParticle.UserAttributes` interface are
     * preceded by a dollar-sign ie `"$FirstName"`. This method just removes the dollar-sign if present.
     *
     * @param key
     * @return a key without a preceding dollar-sign
     */
    public static String sanitizeAttributeKey(String key) {
        if (MPUtility.isEmpty(key)) {
            return key;
        }
        if (key.startsWith("$")) {
            return key.substring(1);
        }
        return key;
    }

    /**
     * Hash using the Fnv1a algorithm. This is the same hashing algorithm that the mParticle
     * backend uses when hashing user and device identities prior to forwarding. In the case
     * of hybrid kit-and-server integrations, it's important that hashed IDs match, whether they're
     * send server-to-server, or via a Kit.
     *
     * @param bytes
     * @return  returns the hashed bytes
     */
    public static BigInteger hashFnv1a(byte[] bytes) {
        return MPUtility.hashFnv1A(bytes);
    }
}
