package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.MParticle
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import org.json.JSONException

class MPEventMessage protected constructor(builder: Builder, session: InternalSession, location: Location?, mpId: Long) :
    BaseMPMessage(builder, session, location, mpId) {
    class Builder(messageType: String) : BaseMPMessageBuilder(messageType) {
        fun customEventType(eventType: MParticle.EventType): BaseMPMessageBuilder {
            try {
                put(Constants.MessageKey.EVENT_TYPE, eventType)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return this
        }

        @Throws(JSONException::class)
        override fun build(session: InternalSession, location: Location?, mpId: Long): BaseMPMessage {
            return MPEventMessage(this, session, location, mpId)
        }
    }
}