package com.mparticle.internal.networking;

import android.location.Location;

import com.mparticle.internal.InternalSession;

import org.json.JSONException;

public class MPEventMessage extends BaseMPMessage {

    protected MPEventMessage(Builder builder) throws JSONException {
        super(builder);
    }

    public static class Builder extends BaseMPMessageBuilder {

        public Builder(String messageType, InternalSession session, Location location, long mpId) {
            super(messageType, session, location, mpId);
        }

        @Override
        public BaseMPMessage build() throws JSONException {
            return new MPEventMessage(this);
        }
    }
}
