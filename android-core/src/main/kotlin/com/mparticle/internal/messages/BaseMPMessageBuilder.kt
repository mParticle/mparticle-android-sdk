package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


open class BaseMPMessageBuilder(messageType: String) : JSONObject() {
    private var mLength: Double? = null

    init {
        try {
            put(Constants.MessageKey.TYPE, messageType)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    open fun timestamp(timestamp: Long): BaseMPMessageBuilder {
        try {
            put(Constants.MessageKey.TIMESTAMP, timestamp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return this
    }

    val timestamp: Long?
        get() {
            val timestamp = optLong(Constants.MessageKey.TIMESTAMP, -1)
            return if (timestamp != -1L) {
                timestamp
            } else {
                null
            }
        }

    fun name(name: String): BaseMPMessageBuilder {
        try {
            put(Constants.MessageKey.NAME, name)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return this
    }

    fun attributes(attributes: JSONObject?): BaseMPMessageBuilder {
        if (attributes != null && attributes.length() > 0) {
            try {
                put(Constants.MessageKey.ATTRIBUTES, attributes)
                mLength?.let { addEventLengthAttributes(it) }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return this
    }

    val messageType: String
        get() = optString(Constants.MessageKey.TYPE)

    @Throws(JSONException::class)
    open fun build(session: InternalSession, location: Location?, mpId: Long): BaseMPMessage {
        return BaseMPMessage(this, session, location, mpId)
    }

    fun dataConnection(dataConnection: String): BaseMPMessageBuilder {
        try {
            put(Constants.MessageKey.STATE_INFO_DATA_CONNECTION, dataConnection)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return this
    }

    fun length(length: Double?): BaseMPMessageBuilder {
        mLength = length
        if (length != null) {
            try {
                put(Constants.MessageKey.EVENT_DURATION, length)
                addEventLengthAttributes(length)
            } catch (e: JSONException) {
                e.printStackTrace()
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
                //can't be longer than max int milliseconds
                getJSONObject(Constants.MessageKey.ATTRIBUTES).put("EventLength", length.toInt().toString())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun flags(customFlags: Map<String?, List<String?>>?): BaseMPMessageBuilder {
        if (customFlags != null) {
            try {
                val flagsObject = JSONObject()
                for ((key, values) in customFlags) {
                    val valueArray = JSONArray(values)
                    flagsObject.put(key, valueArray)
                }
                put(Constants.MessageKey.EVENT_FLAGS, flagsObject)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return this
    }

    val keys: Array<String?>
        get() {
            val strings: MutableList<String?> = ArrayList<String?>()
            val iterator = keys()
            while (iterator.hasNext()) {
                strings.add(iterator.next())
            }
            return strings.toTypedArray<String?>()
        }
}