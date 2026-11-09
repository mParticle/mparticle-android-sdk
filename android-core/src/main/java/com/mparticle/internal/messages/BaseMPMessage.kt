package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.MPUtility
import org.json.JSONException
import org.json.JSONObject

open class BaseMPMessage : JSONObject {
    private var mMpId: Long = 0

    protected constructor() : super()

    @Throws(JSONException::class)
    internal constructor(
        builder: BaseMPMessageBuilder?,
        session: InternalSession?,
        location: Location?,
        mpId: Long,
    ) : super(builder, builder!!.getKeys()) {
        mMpId = mpId
        if (!has(Constants.MessageKey.TIMESTAMP)) {
            put(Constants.MessageKey.TIMESTAMP, session!!.mLastEventTime)
        }
        if (Constants.MessageType.SESSION_START == builder!!.messageType) {
            put(Constants.MessageKey.ID, session!!.mSessionID)
        } else {
            put(Constants.MessageKey.SESSION_ID, session!!.mSessionID)

            if (session.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, session.mSessionStartTime)
            }
        }

        if (!(Constants.MessageType.ERROR == builder.messageType && Constants.MessageType.OPT_OUT != builder.messageType)) {
            if (location != null) {
                val locJSON = JSONObject()
                locJSON.put(Constants.MessageKey.LATITUDE, location.latitude)
                locJSON.put(Constants.MessageKey.LONGITUDE, location.longitude)
                locJSON.put(Constants.MessageKey.ACCURACY, location.accuracy.toDouble())
                put(Constants.MessageKey.LOCATION, locJSON)
            }
        }
    }

    open val attributes: JSONObject?
        get() = optJSONObject(Constants.MessageKey.ATTRIBUTES)

    open val timestamp: Long
        get() {
            try {
                return getLong(Constants.MessageKey.TIMESTAMP)
            } catch (jse: JSONException) {
            }
            return 0
        }

    open val sessionId: String
        get() {
            if (Constants.MessageType.SESSION_START == messageType) {
                return optString(Constants.MessageKey.ID, Constants.NO_SESSION_ID)
            } else {
                return optString(Constants.MessageKey.SESSION_ID, Constants.NO_SESSION_ID)
            }
        }

    open val messageType: String
        get() = optString(Constants.MessageKey.TYPE)

    open val typeNameHash: Int
        get() = MPUtility.mpHash(messageType + name)

    open val name: String
        get() = optString(Constants.MessageKey.NAME)

    open val mpId: Long
        get() = mMpId

    open class Builder(messageType: String?) : BaseMPMessageBuilder(messageType)
}
