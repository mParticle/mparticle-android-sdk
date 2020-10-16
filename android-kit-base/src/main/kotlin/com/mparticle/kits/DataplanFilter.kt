package com.mparticle.kits;

import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticleOptions;
import com.mparticle.commerce.CommerceEvent
import com.mparticle.internal.Logger
import org.json.JSONArray

import org.json.JSONObject;

internal interface DataplanFilter {
    fun <T: BaseEvent> transformEventForEvent(event: T): T?
}

internal class DataplanFilterImpl constructor(val dataPoints: Map<String, HashSet<String>?>,
                                              private val blockEvents: Boolean,
                                              private val blockEventAttributes: Boolean,
                                              private val blockUserAttributes: Boolean,
                                              private val blockUserIdentities: Boolean): DataplanFilter {

    constructor(dataplanOptions: MParticleOptions.DataplanOptions):
            this(extractDataPoints(dataplanOptions.dataplan),
                    dataplanOptions.isBlockEvents,
                    dataplanOptions.isBlockEventAttributes,
                    dataplanOptions.isBlockUserAttributes,
                    dataplanOptions.isBlockUserIdentities)

    /**
     * filters out events and their attributes if
     * 1) they are not defined within the dataplan
     * 2) the corresponding flag for the filtering area is true
     *
     * note: this will NOT return a copy of the event, it will return either the original event filtered or null
     */
    override fun <T: BaseEvent> transformEventForEvent(event: T): T? {
        if (blockEvents || blockEventAttributes) {
            val name = when {
                (event is MPEvent) ->
                    when {
                        event.isScreenEvent -> DataplanPoint(SCREEN_EVENT_KEY, event.eventName)
                        else -> DataplanPoint(CUSTOM_EVENT_KEY, event.eventName, event.eventType.ordinal.toString())
                    }
                event is CommerceEvent ->
                    when {
                        !event.productAction.isNullOrBlank() -> DataplanPoint(PRODUCT_ACTION_KEY, event.productAction)
                        !event.promotionAction.isNullOrBlank() -> DataplanPoint(PROMOTION_ACTION_KEY, event.promotionAction)
                        !event.impressions.isNullOrEmpty() -> DataplanPoint(PRODUCT_IMPRESSION_KEY)
                        else -> null
                    }
                else -> null
            }
            //shouldn't happen but we have to handle it
            name ?: return event
            //if there is no valid datapoint then it is an unplanned event
            if (blockEvents && !dataPoints.containsKey(name.toString())) {
                Logger.verbose("Blocking unplanned event: ${name}")
                return null
            }
            val dataPoint = dataPoints[name.toString()]
            if (blockEventAttributes) {
                event.customAttributes = event.customAttributes?.filterKeys {
                    //null dataPoint means there are no constraints for custom attributes
                    if (dataPoint == null || dataPoint.contains(it)) {
                        true
                    } else {
                        Logger.verbose("Blocking unplanned attribute: $it")
                        false
                    }
                }
            }
            return event
        }
        return event
    }

    internal companion object{
        const val SCREEN_EVENT_KEY = "screen_view"
        const val CUSTOM_EVENT_KEY = "custom_event"
        const val PRODUCT_ACTION_KEY = "product_action"
        const val PROMOTION_ACTION_KEY = "promotion_action"
        const val PRODUCT_IMPRESSION_KEY = "product_impression"
        const val USER_IDENTITIES_KEY = "user_identities"
        const val USER_ATTRIBUTES_KEY = "user_attributes"


        @JvmField
        val EMPTY: DataplanFilter = EmptyDataplanFilter()

        /**
         * parse dataplan into memory. data structure consists of a key composed of the
         * event type concatenated to the event name (if applicable) concatenated to the custom
         * event type (if applicable), with periods, pointing to a set of "legal" attribute keys
         **/
        fun extractDataPoints(dataplan: JSONObject): Map<String, HashSet<String>> {
            return dataplan
                    .tryGetJSONObject("version_document", JSONObject())
                    .tryGetJSONArray("data_points", JSONArray())
                    .toList()
                    .filterIsInstance<JSONObject>()
                    .associate {
                        val match = it.getJSONObject("match")
                        val key = generateDatapointKey(match)
                        val properties = getAllowedKeys(key?.type, it)
                        key?.toString() to properties
                    }
                    .filterKeys { it != null } as Map<String, HashSet<String>>
        }

        fun generateDatapointKey(match: JSONObject): DataplanPoint? {
            val criteria = match.tryGetJSONObject("criteria")
            val matchType = match.optString("type")
            when (matchType) {
                CUSTOM_EVENT_KEY -> {
                    val eventName = criteria?.optString("event_name")
                    val eventType = criteria?.optString("custom_event_type")
                    if (!eventName.isNullOrBlank() && !eventType.isNullOrBlank()) {
                        return DataplanPoint(matchType, eventName, eventType)
                    }
                }
                PRODUCT_ACTION_KEY, PROMOTION_ACTION_KEY -> {
                    val commerceEventType = criteria?.optString("action")
                    if (!commerceEventType.isNullOrBlank()) {
                        return DataplanPoint(matchType, commerceEventType)
                    }
                }
                PRODUCT_IMPRESSION_KEY, USER_ATTRIBUTES_KEY, USER_IDENTITIES_KEY -> {
                    return DataplanPoint(matchType)
                }
                SCREEN_EVENT_KEY -> {
                    val screenName = criteria?.optString("screen_name")
                    if (!screenName.isNullOrBlank()) {
                        return DataplanPoint(matchType, screenName)
                    }
                }
            }
            return null
        }

        //returns a set of "allowed" keys, or `null` if all keys are allowed
        private fun getAllowedKeys(type: String?, jsonObject: JSONObject): HashSet<String>? {
            val definition = jsonObject.optJSONObject("validator")
                    ?.optJSONObject("definition")
            when (type) {
                CUSTOM_EVENT_KEY, SCREEN_EVENT_KEY, PRODUCT_IMPRESSION_KEY, PROMOTION_ACTION_KEY, PRODUCT_ACTION_KEY ->
                    definition
                            ?.optJSONObject("properties")
                            ?.optJSONObject("data")?.let {
                                val customAttributes = it.optJSONObject("properties")
                                        ?.optJSONObject("custom_attributes")
                                if (customAttributes == null) {
                                    //if there are no custom attributes listed, allow all if additionalProperties are allowed or none if they are not
                                    return if (it.optBoolean("additionalProperties", true)) null else hashSetOf()
                                }
                                //if additional custom attributes are allowed, return allow all, otherwise return list of allowed custom attributes
                                if (customAttributes.optBoolean("additionalProperties", true)) {
                                    return null
                                } else {
                                    return customAttributes.optJSONObject("properties")?.keys()?.toHashSet()
                                            ?: hashSetOf()
                                }

                                //if nothing can be gathered, allow all attributes
                            } ?: return null

                else -> {
                    val additionalProperties = definition?.optBoolean("additionalProperties", true)
                            ?: true
                    if (additionalProperties) {
                        return null
                    } else {
                        return definition?.optJSONObject("properties")?.keys()?.toHashSet()
                                ?: hashSetOf()
                    }
                }
            }
        }

        /**
         * utility extension functions, mostly for parsing JSON
         */

        fun JSONObject.tryGetJSONObject(name: String, default: JSONObject): JSONObject {
            return tryGetJSONObject(name) ?: default
        }

        fun JSONObject.tryGetJSONObject(name: String): JSONObject? {
            return this.optJSONObject(name).also {
                if (it == null) {
                    Logger.warning("Dataplan key \"$name\" is missing")
                }
            }
        }

        fun JSONObject.tryGetJSONArray(name: String, default: JSONArray): JSONArray {
            return tryGetJSONArray(name) ?: default
        }

        fun JSONObject.tryGetJSONArray(name: String): JSONArray? {
            return optJSONArray(name).also {
                if (it == null) {
                    Logger.warning("Dataplan key \"$name\" is missing")
                }
            }
        }

        fun JSONArray.toList(): List<Any> {
            val list = ArrayList<Any>()
            for (i in 0 until this.length()) {
                list.add(this[i])
            }
            return list
        }

        fun <T> Iterator<T>.toHashSet(): HashSet<T> {
            val set = HashSet<T>()
            this.forEach { set.add(it) }
            return set
        }
    }

    internal class DataplanPoint(val type: String, val name: String? = null, val eventType: String? = null) {
        override fun toString() = "$type${if (name != null) ".$name" else ""}${if(eventType != null) ".$eventType" else ""}"
    }
}

class EmptyDataplanFilter: DataplanFilter {
    override fun <T : BaseEvent> transformEventForEvent(event: T) = event
}
