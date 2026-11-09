package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class BaseMPMessageBuilder(messageType: String?) : JSONObject() {
    private var mLength: Double? = null

    init {
        try {
            put(Constants.MessageKey.TYPE, messageType)
        } catch (e: JSONException) {
            Logger.error(e)
        }
    }

    open fun timestamp(timestamp: Long): BaseMPMessageBuilder {
        try {
            put(Constants.MessageKey.TIMESTAMP, timestamp)
        } catch (e: JSONException) {
            Logger.error(e)
        }
        return this
    }

    open val timestamp: Long?
        get() {
            val timestamp = optLong(Constants.MessageKey.TIMESTAMP, -1)
            if (timestamp != -1L) {
                return timestamp
            } else {
                return null
            }
        }

    open fun name(name: String?): BaseMPMessageBuilder {
        try {
            put(Constants.MessageKey.NAME, name)
        } catch (e: JSONException) {
            Logger.error(e)
        }
        return this
    }

    open fun attributes(attributes: JSONObject?): BaseMPMessageBuilder {
        if (attributes != null && attributes.length() > 0) {
            try {
                put(Constants.MessageKey.ATTRIBUTES, attributes)
                if (mLength != null) {
                    addEventLengthAttributes(mLength!!)
                }
            } catch (e: JSONException) {
                Logger.error(e)
            }
        }
        return this
    }

    open val messageType: String
        get() = optString(Constants.MessageKey.TYPE)

    @Throws(JSONException::class)
    open fun build(session: InternalSession?, location: Location?, mpId: Long): BaseMPMessage = BaseMPMessage(this, session, location, mpId)

    open fun dataConnection(dataConnection: String?): BaseMPMessageBuilder {
        if (dataConnection != null) {
            try {
                put(Constants.MessageKey.STATE_INFO_DATA_CONNECTION, dataConnection)
            } catch (e: JSONException) {
                Logger.error(e)
            }
        }
        return this
    }

    open fun length(length: Double?): BaseMPMessageBuilder {
        mLength = length
        if (length != null) {
            try {
                put(Constants.MessageKey.EVENT_DURATION, length)
                addEventLengthAttributes(length)
            } catch (e: JSONException) {
                Logger.error(e)
            }
        }
        return this
    }

    private fun addEventLengthAttributes(length: Double) {
        try {
            if (!has(Constants.MessageKey.ATTRIBUTES)) {
                put(Constants.MessageKey.ATTRIBUTES, JSONObject())
            }
            if (!getJSONObject(Constants.MessageKey.ATTRIBUTES).has("EventLength")) {
                // can't be longer than max int milliseconds
                getJSONObject(Constants.MessageKey.ATTRIBUTES).put("EventLength", length.toInt().toString())
            }
        } catch (e: JSONException) {
            Logger.error(e)
        }
    }

    open fun flags(customFlags: @JvmSuppressWildcards Map<String, List<String>>?): BaseMPMessageBuilder {
        if (customFlags != null) {
            try {
                val flagsObject = JSONObject()
                for (entry in customFlags.entries) {
                    val values = entry.value
                    val valueArray = JSONArray(values)
                    flagsObject.put(entry.key, valueArray)
                }
                put(Constants.MessageKey.EVENT_FLAGS, flagsObject)
            } catch (e: JSONException) {
                Logger.error(e)
            }
        }
        return this
    }

    internal fun getKeys(): Array<String> {
        val strings: MutableList<String> = ArrayList()
        val iterator = keys()
        while (iterator.hasNext()) {
            strings.add(iterator.next())
        }
        return strings.toTypedArray()
    }
}
