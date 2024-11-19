package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.MPUtility
import org.json.JSONException
import org.json.JSONObject

open class BaseMPMessage : JSONObject {
    var mpId: Long = 0
        private set

    protected constructor()

    constructor(builder: BaseMPMessageBuilder, session: InternalSession, location: Location?, mpId: Long) : super(builder, builder.keys) {
        this.mpId = mpId
        if (!has(Constants.MessageKey.TIMESTAMP)) {
            put(Constants.MessageKey.TIMESTAMP, session.mLastEventTime)
        }
        if (Constants.MessageType.SESSION_START == builder.messageType) {
            put(Constants.MessageKey.ID, session.mSessionID)
        } else {
            put(Constants.MessageKey.SESSION_ID, session.mSessionID)

            if (session.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, session.mSessionStartTime)
            }
        }

        if (!(Constants.MessageType.ERROR == builder.messageType &&
                    Constants.MessageType.OPT_OUT != builder.messageType)
        ) {
            if (location != null) {
                val locJSON = JSONObject()
                locJSON.put(Constants.MessageKey.LATITUDE, location.latitude)
                locJSON.put(Constants.MessageKey.LONGITUDE, location.longitude)
                locJSON.put(Constants.MessageKey.ACCURACY, location.accuracy.toDouble())
                put(Constants.MessageKey.LOCATION, locJSON)
            }
        }
    }

    val attributes: JSONObject?
        get() = optJSONObject(Constants.MessageKey.ATTRIBUTES)

    val timestamp: Long
        get() {
            try {
                return getLong(Constants.MessageKey.TIMESTAMP)
            } catch (_: JSONException) {
            }
            return 0
        }

    val sessionId: String
        get() = if (Constants.MessageType.SESSION_START == messageType) {
            optString(Constants.MessageKey.ID, Constants.NO_SESSION_ID)
        } else {
            optString(Constants.MessageKey.SESSION_ID, Constants.NO_SESSION_ID)
        }

    val messageType: String
        get() = optString(Constants.MessageKey.TYPE)

    val typeNameHash: Int
        get() = MPUtility.mpHash(messageType + name)


    val name: String
        get() = optString(Constants.MessageKey.NAME)

    class Builder(messageType: String) : BaseMPMessageBuilder(messageType)
}
