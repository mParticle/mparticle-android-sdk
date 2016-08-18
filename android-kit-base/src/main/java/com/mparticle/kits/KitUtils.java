package com.mparticle.kits;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.mparticle.internal.MPUtility;

import java.math.BigInteger;
import java.util.List;

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

    /**
     * Combine the given list into a single string separated by a comma.
     *
     * @param list
     * @return
     */
    public static String join(List<String> list) {
        return join(list, ",");
    }

    /**
     * Combine the given list into a single string separated by the given delimiter.
     *
     * @param list
     * @param delimiter
     * @return
     */
    public static String join(List<String> list, String delimiter) {
        if (list == null) {
            return null;
        }
        if (delimiter == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String item : list) {
            builder.append(item).append(delimiter);
        }
        if (builder.length() > 0) {
            return builder.substring(0, builder.length() - delimiter.length());
        }
        return builder.toString();
    }

    /**
     * Check if the given Service class is present in the hosting app's AndroidManifest.
     *
     * @param context
     * @param service
     * @return
     */
    public static boolean isServiceAvailable(Context context, Class<?> service) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(context, service);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
    }
}
