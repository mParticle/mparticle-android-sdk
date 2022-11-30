package com.mparticle.kits

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.mparticle.MParticle
import com.mparticle.internal.MPUtility
import java.lang.StringBuilder
import java.math.BigInteger

/**
 * Mixin/Utility class for use in Kit implementations.
 */
object KitUtils {
    /**
     *
     * mParticle attribute keys defined by the `MParticle.UserAttributes` interface are
     * preceded by a dollar-sign ie `"$FirstName"`. This method just removes the dollar-sign if present.
     *
     * @param key
     * @return a key without a preceding dollar-sign
     */
    fun sanitizeAttributeKey(key: String): String {
        if (MPUtility.isEmpty(key)) {
            return key
        }
        return if (key.startsWith("$")) {
            key.substring(1)
        } else key
    }

    /**
     * Hash using the Fnv1a algorithm. This is the same hashing algorithm that the mParticle
     * backend uses when hashing user and device identities prior to forwarding. In the case
     * of hybrid kit-and-server integrations, it's important that hashed IDs match, whether they're
     * send server-to-server, or via a Kit.
     *
     * @param bytes
     * @return returns the hashed bytes
     */
    fun hashFnv1a(bytes: ByteArray?): BigInteger {
        return MPUtility.hashFnv1A(bytes)
    }

    /**
     * Simple bit-shifting hash for use with filtering. mParticle's backend uses this same hash
     * to compute hashes of events and their attributes, and will include those hashes with
     * configuration settings for kits.
     *
     * @param input
     * @return return int hash
     */
    @JvmStatic
    fun hashForFiltering(input: String?): Int {
        return MPUtility.mpHash(input)
    }

    /**
     * Determine if the given CharSequence is null or 0-length. This is the same
     * implementation as android.text.TextUtils, but is provided here due since
     * TextUtils is not available while unit testing.
     *
     * @param str
     * @return true if the given value is null or 0-length
     */
    fun isEmpty(str: CharSequence?): Boolean {
        return MPUtility.isEmpty(str)
    }
    /**
     * Combine the given list into a single string separated by the given delimiter.
     *
     * @param list
     * @param delimiter
     * @return
     */
    /**
     * Combine the given list into a single string separated by a comma.
     *
     * @param list
     * @return
     */
    @JvmOverloads
    fun join(list: List<String?>?, delimiter: String? = ","): String? {
        if (list == null) {
            return null
        }
        if (delimiter == null) {
            return null
        }
        val builder = StringBuilder()
        for (item in list) {
            builder.append(item).append(delimiter)
        }
        return if (builder.length > 0) {
            builder.substring(0, builder.length - delimiter.length)
        } else builder.toString()
    }

    /**
     * Check if the given Service class is present in the hosting app's AndroidManifest.
     *
     * @param context
     * @param service
     * @return
     */
    fun isServiceAvailable(context: Context, service: Class<*>?): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(context, service)
        val resolveInfo: List<*> = packageManager.queryIntentServices(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return if (resolveInfo.size > 0) {
            true
        } else false
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
    /**
     * Parse a boolean setting value from mParticle's server configuration. mParticle
     * will return "True" and "False" (rather than "true") which trips up Boolean.valueOf
     * so this provides a consistent way for kits to handle such settings.
     *
     * @param settings the Map of settings given to a kit.
     * @param key the key of the setting to parse
     * @return the parsed boolean value of the setting, or false if the setting is not found.
     */
    @JvmOverloads
    fun parseBooleanSetting(
        settings: Map<String?, String?>?,
        key: String?,
        defaultValue: Boolean = false
    ): Boolean {
        if (settings != null && settings.containsKey(key)) {
            val value = settings[key]
            if (value != null && value.length > 0) {
                return java.lang.Boolean.valueOf(value.lowercase())
            }
        }
        return defaultValue
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    fun getAndroidID(context: Context): String? {
        return if (!MParticle.isAndroidIdDisabled()) {
            Settings.Secure.getString(context.contentResolver, "android_id")
        } else {
            null
        }
    }
}
