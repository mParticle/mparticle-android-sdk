package com.mparticle.kits;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * Simple bit-shifting hash for use with filtering. mParticle's backend uses this same hash
     * to compute hashes of events and their attributes, and will include those hashes with
     * configuration settings for kits.
     *
     * @param input
     * @return return int hash
     */
    public static int hashForFiltering(String input) {
        return MPUtility.mpHash(input);
    }


    /**
     * Determine if the given CharSequence is null or 0-length. This is the same
     * implementation as android.text.TextUtils, but is provided here due since
     * TextUtils is not available while unit testing.
     *
     * @param str
     * @return true if the given value is null or 0-length
     */
    public static boolean isEmpty(CharSequence str) {
        return MPUtility.isEmpty(str);
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

    /**
     * Parse a boolean setting value from mParticle's server configuration. mParticle
     * will return "True" and "False" (rather than "true") which trips up Boolean.valueOf
     * so this provides a consistent way for kits to handle such settings.
     *
     * @param settings the Map of settings given to a kit.
     * @param key the key of the setting to parse
     * @param defaultValue the default value if the setting is not found
     * @return the parsed boolean value of the setting.
     */
    public static boolean parseBooleanSetting(Map<String, String> settings, String key, boolean defaultValue) {
        if (settings != null && settings.containsKey(key)) {
            String value = settings.get(key);
            if (value != null && value.length() > 0) {
                return Boolean.valueOf(value.toLowerCase(Locale.US));
            }
        }
        return defaultValue;
    }

    /**
     * Parse a boolean setting value from mParticle's server configuration. mParticle
     * will return "True" and "False" (rather than "true") which trips up Boolean.valueOf
     * so this provides a consistent way for kits to handle such settings.
     *
     * @param settings the Map of settings given to a kit.
     * @param key the key of the setting to parse
     * @return the parsed boolean value of the setting, or false if the setting is not found.
     */
    public static boolean parseBooleanSetting(Map<String, String> settings, String key) {
       return parseBooleanSetting(settings, key, false);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Nullable
    public static String getAndroidID(Context context) {
        if (!MParticle.isAndroidIdDisabled()) {
            return Settings.Secure.getString(context.getContentResolver(), "android_id");
        } else {
            return null;
        }
    }
}
