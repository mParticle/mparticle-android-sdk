package com.mparticle.internal

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Arrays
import java.util.HashSet
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

/**
 * This class is responsible for pulling persistence from files from *other* SDKs, and serializing itself as a part of a batch.
 * The idea here is that a customer may want to remove an SDK from their app and move it
 * server side via mParticle. Rather than start from stratch, it's crucial that we can query data that
 * the given SDK had been storing client-side.
 */
internal class ProviderPersistence : JSONObject {
    @Throws(JSONException::class)
    internal constructor(
        config: JSONObject?,
        context: Context?,
        allowedKeys: Collection<String>?,
    ) : super() {
        val configPersistence = config!!.getJSONArray(KEY_PERSISTENCE)
        for (i in 0 until configPersistence.length()) {
            val values = JSONObject()
            if (configPersistence.getJSONObject(i).has(KEY_PERSISTENCE_ANDROID)) {
                val files = configPersistence.getJSONObject(i).getJSONArray(KEY_PERSISTENCE_ANDROID)

                for (fileIndex in 0 until files.length()) {
                    val fileObject = files.getJSONObject(fileIndex)
                    val fileName = fileObject.getString(KEY_PERSISTENCE_FILE)
                    if (isMParticlePreferencesFile(fileName)) {
                        Logger.warning("Ignoring provider persistence config for the SDK's own preferences file: $fileName")
                        continue
                    }
                    val preferences = context!!.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                    val fileObjects = fileObject.getJSONArray(KEY_PERSISTENCE_KEY_LIST)
                    val editor = preferences.edit()
                    for (keyIndex in 0 until fileObjects.length()) {
                        val type = fileObjects.getJSONObject(keyIndex).getInt(KEY_PERSISTENCE_TYPE)
                        val key = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_KEY)
                        if (!DEFAULT_ALLOWED_KEYS.contains(key) && !allowedKeys!!.contains(key)) {
                            Logger.warning(
                                "Ignoring a custom module preference for '$key': not a key this SDK reads. " +
                                    "Add it to MParticleOptions.Builder.customModulePreferenceKeys() to share it deliberately.",
                            )
                            continue
                        }
                        val mpKey = fileObjects.getJSONObject(keyIndex).getString(KEY_PERSISTENCE_MPVAR)
                        val mpPersistenceKey = "$MPPREFIX$mpKey"
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
                                    PERSISTENCE_TYPE_LONG -> resolvedValue = preferences.getLong(key, 0L).toString()
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
        internal const val KEY_PERSISTENCE = "cms"
        private const val KEY_PERSISTENCE_ID = "id"
        private const val KEY_PERSISTENCE_ANDROID = "pr"
        private const val KEY_PERSISTENCE_FILE = "f"
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

        /**
         * The Adobe Mobile Services values custom modules were built to carry. The config names the keys
         * to read, so without this bound it could name any preference the host app keeps. The host app
         * widens it with [com.mparticle.MParticleOptions.Builder.customModulePreferenceKeys].
         * The Adobe Mobile Services (v4) SDK stores install date, last-used date and launches as ADMS_*
         * keys on Android and as OMCK* keys on iOS; both are listed so a configuration shared with iOS
         * keeps working.
         */
        private val DEFAULT_ALLOWED_KEYS: Set<String> =
            HashSet(
                Arrays.asList(
                    "ADB_LIFETIME_VALUE",
                    "ADOBEMOBILE_STOREDDEFAULTS_AID",
                    "APP_MEASUREMENT_VISITOR_ID",
                    "ADMS_InstallDate",
                    "ADMS_LastDateUsed",
                    "ADMS_Launches",
                    "OMCK1",
                    "OMCK5",
                    "OMCK6",
                ),
            )

        private const val MACRO_GUID_NO_DASHES = "%gn%"
        private const val MACRO_OMNITURE_AID = "%oaid%"
        private const val MACRO_GUID = "%g%"
        private const val MACRO_TIMESTAMP = "%ts%"
        private const val MACRO_GUID_LEAST_SIG = "%glsb%"

        private fun isMParticlePreferencesFile(fileName: String): Boolean = fileName == Constants.PREFS_FILE ||
            fileName == ConfigManager.PREFERENCES_FILE ||
            fileName.startsWith("${ConfigManager.PREFERENCES_FILE}:") ||
            fileName == ConfigManager.KIT_CONFIG_PREFERENCES ||
            fileName.startsWith(MPPREFIX)

        /**
         * Macros are used so that the /config API call can come from a CDN (not user-specific).
         */
        private fun applyMacro(defaultString: String?): String? {
            if (!MPUtility.isEmpty(defaultString) && defaultString!!.startsWith("%")) {
                val lowered = defaultString.lowercase(Locale.getDefault())
                if (lowered.equals(MACRO_GUID_NO_DASHES, ignoreCase = true)) {
                    return UUID.randomUUID().toString().replace("-", "")
                } else if (lowered == MACRO_OMNITURE_AID) {
                    return generateAID()
                } else if (lowered == MACRO_GUID) {
                    return UUID.randomUUID().toString()
                } else if (lowered == MACRO_TIMESTAMP) {
                    return System.currentTimeMillis().toString()
                } else if (lowered == MACRO_GUID_LEAST_SIG) {
                    return UUID.randomUUID().leastSignificantBits.toString()
                }
                return lowered
            }
            return defaultString
        }

        private fun generateAID(): String {
            val uuid = UUID.randomUUID().toString().replace("-", "").uppercase()

            val firstPattern = Pattern.compile("^[89A-F]")
            val secondPattern = Pattern.compile("^[4-9A-F]")
            val firstMatcher = firstPattern.matcher(uuid.substring(0, 16))
            val secondMatcher = secondPattern.matcher(uuid.substring(16, 32))

            val r = SecureRandom()
            val viHi = firstMatcher.replaceAll(r.nextInt(7).toString())
            val viLo = secondMatcher.replaceAll(r.nextInt(3).toString())

            return "$viHi-$viLo"
        }
    }
}
