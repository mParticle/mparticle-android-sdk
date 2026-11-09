package com.mparticle.internal.messages

import android.location.Location
import com.mparticle.MParticle
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.Logger
import org.json.JSONException

open class MPEventMessage : BaseMPMessage {
    @Throws(JSONException::class)
    protected constructor(
        builder: Builder?,
        session: InternalSession?,
        location: Location?,
        mpId: Long,
    ) : super(builder, session, location, mpId)

    open class Builder(messageType: String?) : BaseMPMessageBuilder(messageType) {
        open fun customEventType(eventType: MParticle.EventType?): BaseMPMessageBuilder {
            try {
                put(Constants.MessageKey.EVENT_TYPE, eventType)
            } catch (e: JSONException) {
                Logger.error(e)
            }
            return this
        }

        @Throws(JSONException::class)
        override fun build(session: InternalSession?, location: Location?, mpId: Long): BaseMPMessage = MPEventMessage(this, session, location, mpId)
    }
}
