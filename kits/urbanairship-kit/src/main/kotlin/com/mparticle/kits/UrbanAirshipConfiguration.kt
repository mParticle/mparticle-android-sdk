package com.mparticle.kits

import com.mparticle.MParticle.IdentityType
import org.json.JSONArray
import org.json.JSONException
import java.lang.Exception
import java.util.ArrayList

class UrbanAirshipConfiguration(
    settings: Map<String, String>,
) {
    val applicationKey: String?
    val applicationSecret: String?
    val domain: String?
    val customDomainProxyUrl: String?
    val enableTags: Boolean
    val includeUserAttributes: Boolean
    val userIdField: IdentityType?

    var eventClass: MutableMap<Int, ArrayList<String>> = mutableMapOf()
        private set
    var eventClassDetails: MutableMap<Int, ArrayList<String>> = mutableMapOf()
        private set
    var eventAttributeClass: MutableMap<Int, ArrayList<String>> = mutableMapOf()
        private set
    var eventAttributeClassDetails: MutableMap<Int, ArrayList<String>> = mutableMapOf()
        private set

    var notificationIconName: String? = null
    var notificationColor: String? = null

    private fun parseTagsJson(tagsJson: JSONArray) {
        for (i in 0 until tagsJson.length()) {
            try {
                val tagMap = tagsJson.getJSONObject(i)
                val mapType = tagMap.getString("maptype")
                val tagValue = tagMap.getString("value")
                val hash = tagMap.getInt("map")
                val eventMap: MutableMap<Int, ArrayList<String>>? =
                    when (mapType) {
                        "EventClass.Id" -> {
                            eventClass
                        }
                        "EventClassDetails.Id" -> {
                            eventClassDetails
                        }
                        "EventAttributeClass.Id" -> {
                            eventAttributeClass
                        }
                        "EventAttributeClassDetails.Id" -> {
                            eventAttributeClassDetails
                        }
                        else -> {
                            null
                        }
                    }
                if (eventMap != null) {
                    if (!eventMap.containsKey(hash)) {
                        eventMap[hash] = ArrayList()
                    }
                    eventMap[hash]?.add(tagValue)
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    companion object {
        private const val KEY_APP_KEY = "applicationKey"
        private const val KEY_APP_SECRET = "applicationSecret"
        private const val KEY_DOMAIN = "domain"
        private const val KEY_CUSTOM_DOMAIN_PROXY_URL = "customDomainProxyUrl"
        private const val KEY_ENABLE_TAGS = "enableTags"
        private const val KEY_USER_ID_FIELD = "namedUserIdField"
        private const val KEY_EVENT_USER_TAGS = "eventUserTags"
        private const val KEY_EVENT_ATTRIBUTE_USER_TAGS = "eventAttributeUserTags"
        private const val KEY_NOTIFICATION_ICON_NAME = "notificationIconName"
        private const val KEY_NOTIFICATION_COLOR = "notificationColor"
        private const val KEY_INCLUDE_USER_ATTRIBUTES = "includeUserAttributes"
        private const val NAMED_USER_TYPE_NONE = "none"
        private const val NAMED_USER_TYPE_CUSTOMER_ID = "customerId"
        private const val NAMED_USER_TYPE_EMAIL = "email"
        private const val NAMED_USER_TYPE_OTHER = "other"

        private fun parseNamedUserIdentityType(config: String?): IdentityType? =
            if (config == null) {
                null
            } else {
                when (config) {
                    NAMED_USER_TYPE_OTHER -> IdentityType.Other
                    NAMED_USER_TYPE_EMAIL -> IdentityType.Email
                    NAMED_USER_TYPE_CUSTOMER_ID -> IdentityType.CustomerId
                    NAMED_USER_TYPE_NONE -> null
                    else -> null
                }
            }
    }

    init {
        applicationKey = settings[KEY_APP_KEY]
        applicationSecret = settings[KEY_APP_SECRET]
        domain = settings[KEY_DOMAIN]
        if (settings.containsKey(KEY_CUSTOM_DOMAIN_PROXY_URL)) {
            customDomainProxyUrl = settings[KEY_CUSTOM_DOMAIN_PROXY_URL]
        } else {
            customDomainProxyUrl = null
        }
        enableTags = KitUtils.parseBooleanSetting(settings, KEY_ENABLE_TAGS, true)
        userIdField = parseNamedUserIdentityType(settings[KEY_USER_ID_FIELD])
        if (settings.containsKey(KEY_EVENT_USER_TAGS)) {
            val eventUserTagsString = settings[KEY_EVENT_USER_TAGS]
            try {
                val eventUserTagsJson = JSONArray(eventUserTagsString)
                parseTagsJson(eventUserTagsJson)
            } catch (ignored: Exception) {
            }
        }
        if (settings.containsKey(KEY_EVENT_ATTRIBUTE_USER_TAGS)) {
            val eventAttributeUserTagsString = settings[KEY_EVENT_ATTRIBUTE_USER_TAGS]
            try {
                val eventAttributeUserTagsJson = JSONArray(eventAttributeUserTagsString)
                parseTagsJson(eventAttributeUserTagsJson)
            } catch (ignored: Exception) {
            }
        }
        if (settings.containsKey(KEY_NOTIFICATION_COLOR)) {
            notificationColor = settings[KEY_NOTIFICATION_COLOR]
        }
        if (settings.containsKey(KEY_NOTIFICATION_ICON_NAME)) {
            notificationIconName = settings[KEY_NOTIFICATION_ICON_NAME]
        }
        includeUserAttributes =
            KitUtils.parseBooleanSetting(settings, KEY_INCLUDE_USER_ATTRIBUTES, false)
    }
}
