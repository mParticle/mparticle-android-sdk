package com.mparticle.internal

import android.content.Context
import com.mparticle.internal.MPUtility.isEmpty
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

/**
 * This class is responsible for pulling persistence from files from *other* SDKs, and serializing itself as a part of a batch.
 * The idea here is that a customer may want to remove an SDK from their app and move it
 * server side via mParticle. Rather than start from stratch, it's crucial that we can query data that
 * the given SDK had been storing client-side.
 */
internal class ProviderPersistence(config: JSONObject, context: Context) : JSONObject() {
    init {
        val configPersistence = config.getJSONArray(KEY_PERSISTENCE)
        for (i in 0 until configPersistence.length()) {
            val values = JSONObject()
            if (configPersistence.getJSONObject(i).has(KEY_PERSISTENCE_ANDROID)) {
                val files = configPersistence.getJSONObject(i).getJSONArray(KEY_PERSISTENCE_ANDROID)

                for (fileIndex in 0 until files.length()) {
                    val fileObject = files.getJSONObject(fileIndex)
                    val preferences =
                        context.getSharedPreferences(fileObject.getString(KEY_PERSISTENCE_FILE), fileObject.getInt(KEY_PERSISTENCE_MODE))
                    val fileObjects = fileObject.getJSONArray(KEY_PERSISTENCE_KEY_LIST)
                    val editor = preferences.edit()
                    for (keyIndex in 0 until fileObjects.length()) {
                        val type = fileObjects.getJSONObject(keyIndex).getInt(KEY_PERSISTENCE_TYPE)
                        val key = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_KEY)
                        val mpKey = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_MPVAR)
                        val mpPersistenceKey = MPPREFIX + mpKey
                        if (preferences.contains(mpPersistenceKey)) {
                            values.put(mpKey, preferences.getString(mpPersistenceKey, null))
                        } else {
                            var resolvedValue: String? = null
                            if (preferences.contains(key)) {
                                when (type) {
                                    PERSISTENCE_TYPE_STRING -> resolvedValue = preferences.getString(key, resolvedValue)
                                    PERSISTENCE_TYPE_INT -> resolvedValue = preferences.getInt(key, 0).toString()
                                    PERSISTENCE_TYPE_BOOLEAN -> resolvedValue = preferences.getBoolean(key, false).toString()
                                    PERSISTENCE_TYPE_FLOAT -> resolvedValue = preferences.getFloat(key, 0f).toString()
                                    PERSISTENCE_TYPE_LONG -> resolvedValue = preferences.getLong(key, 0).toString()
                                }
                            } else {
                                resolvedValue = applyMacro(fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_DEFAULT))
                            }

                            editor.putString(mpPersistenceKey, resolvedValue)
                            editor.apply()
                            values.put(mpKey, resolvedValue)
                        }
                    }
                }
            }
            put(configPersistence.getJSONObject(i).getInt(KEY_PERSISTENCE_ID).toString(), values)
        }
    }

    companion object {
        const val KEY_PERSISTENCE: String = "cms"
        private const val KEY_PERSISTENCE_ID = "id"
        private const val KEY_PERSISTENCE_ANDROID = "pr"
        private const val KEY_PERSISTENCE_FILE = "f"
        private const val KEY_PERSISTENCE_MODE = "m"
        private const val KEY_PERSISTENCE_KEY_LIST = "ps"
        private const val KEY_PERSISTENCE_KEY = "k"
        private const val KEY_PERSISTENCE_TYPE = "t"
        private const val KEY_PERSISTENCE_MPVAR = "n"
        private const val KEY_PERSISTENCE_DEFAULT = "d"
        private const val MPPREFIX = "mp::"

        private const val PERSISTENCE_TYPE_STRING = 1
        private const val PERSISTENCE_TYPE_INT = 2
        private const val PERSISTENCE_TYPE_BOOLEAN = 3
        private const val PERSISTENCE_TYPE_FLOAT = 4
        private const val PERSISTENCE_TYPE_LONG = 5


        private const val MACRO_GUID_NO_DASHES = "%gn%"
        private const val MACRO_OMNITURE_AID = "%oaid%"
        private const val MACRO_GUID = "%g%"
        private const val MACRO_TIMESTAMP = "%ts%"
        private const val MACRO_GUID_LEAST_SIG = "%glsb%"

        /**
         * Macros are used so that the /config API call can come from a CDN (not user-specific).
         */
        private fun applyMacro(defaultString: String): String {
            var defaultString = defaultString
            if (!isEmpty(defaultString) && defaultString.startsWith("%")) {
                defaultString = defaultString.lowercase(Locale.getDefault())
                if (defaultString.equals(MACRO_GUID_NO_DASHES, ignoreCase = true)) {
                    return UUID.randomUUID().toString().replace("-", "")
                } else if (defaultString == MACRO_OMNITURE_AID) {
                    return generateAID()
                } else if (defaultString == MACRO_GUID) {
                    return UUID.randomUUID().toString()
                } else if (defaultString == MACRO_TIMESTAMP) {
                    return System.currentTimeMillis().toString()
                } else if (defaultString == MACRO_GUID_LEAST_SIG) {
                    return UUID.randomUUID().leastSignificantBits.toString()
                }
            }
            return defaultString
        }

        private fun generateAID(): String {
            var uuid = UUID.randomUUID().toString().replace("-", "")
            uuid = uuid.uppercase(Locale.getDefault())

            val firstPattern = Pattern.compile("^[89A-F]")
            val secondPattern = Pattern.compile("^[4-9A-F]")
            val firstMatcher = firstPattern.matcher(uuid.substring(0, 16))
            val secondMatcher = secondPattern.matcher(uuid.substring(16, 32))

            val r = SecureRandom()
            val vi_hi = firstMatcher.replaceAll(r.nextInt(7).toString())
            val vi_lo = secondMatcher.replaceAll(r.nextInt(3).toString())

            val aidBuilder = StringBuilder(33)
            aidBuilder.append(vi_hi)
            aidBuilder.append("-")
            aidBuilder.append(vi_lo)

            return aidBuilder.toString()
        }
    }
}
