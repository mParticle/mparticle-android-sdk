package com.mparticle.internal.messages;

import android.location.Location;

import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;

import org.json.JSONException;

public class MPEventMessage extends BaseMPMessage {

    protected MPEventMessage(Builder builder, InternalSession session, @Nullable Location location, long mpId) throws JSONException {
        super(builder, session, location, mpId);
    }

    public static class Builder extends BaseMPMessageBuilder {

        public Builder(String messageType) {
            super(messageType);
        }

        public BaseMPMessageBuilder customEventType(MParticle.EventType eventType) {
            try {
                put(Constants.MessageKey.EVENT_TYPE, eventType);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        @Override
        public BaseMPMessage build(InternalSession session, Location location, long mpId) throws JSONException {
            return new MPEventMessage(this, session, location, mpId);
        }
    }
}