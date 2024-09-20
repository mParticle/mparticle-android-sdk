package com.mparticle.database

//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.encodeToString

//@Serializable
data class UploadSettings(
    val apiKey: String,
    val secret: String,
    val eventsHost: String,
    val overridesEventsSubdirectory: Boolean,
    val aliasHost: String,
    val overridesAliasSubdirectory: Boolean,
    val eventsOnly: Boolean,
    val kitIds: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadSettings

        if (apiKey != other.apiKey) return false
        if (secret != other.secret) return false
        if (eventsHost != other.eventsHost) return false
        if (overridesEventsSubdirectory != other.overridesEventsSubdirectory) return false
        if (aliasHost != other.aliasHost) return false
        if (overridesAliasSubdirectory != other.overridesAliasSubdirectory) return false
        if (eventsOnly != other.eventsOnly) return false
        if (!kitIds.contentEquals(other.kitIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = apiKey.hashCode()
        result = 31 * result + secret.hashCode()
        result = 31 * result + eventsHost.hashCode()
        result = 31 * result + overridesEventsSubdirectory.hashCode()
        result = 31 * result + aliasHost.hashCode()
        result = 31 * result + overridesAliasSubdirectory.hashCode()
        result = 31 * result + eventsOnly.hashCode()
        result = 31 * result + kitIds.contentHashCode()
        return result
    }

    fun serialize(): String {
//        return Json.encodeToString(this)
        return ""
    }

//    companion object {
//        @JvmStatic
//        fun deserialize(jsonString: String): UploadSettings {
//            return Json.decodeFromString<UploadSettings>(jsonString)
//        }
//    }
}