package com.mparticle;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by sdozor on 10/9/14.
 */
public class MPMessage extends JSONObject{

    private MPMessage(){}
    private MPMessage(Builder builder) throws JSONException{
        put(Constants.MessageKey.TYPE, builder.mMessageType);
        put(Constants.MessageKey.TIMESTAMP, builder.mTimestamp);
        if (Constants.MessageType.SESSION_START == builder.mMessageType) {
            put(Constants.MessageKey.ID, builder.mSessionId);
        } else {
            if (builder.mSessionId != null) {
                put(Constants.MessageKey.SESSION_ID, builder.mSessionId);
            }

            put(Constants.MessageKey.ID, UUID.randomUUID().toString());
            if (builder.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, builder.mSessionStartTime);
            }
        }
        if (builder.mName != null) {
            put(Constants.MessageKey.NAME, builder.mName);
        }
        if (builder.mAttributes != null) {
            put(Constants.MessageKey.ATTRIBUTES, builder.mAttributes);
        }
        if (!(Constants.MessageType.ERROR.equals(builder.mMessageType) &&
                !(Constants.MessageType.OPT_OUT.equals(builder.mMessageType)))) {
            if (builder.mLocation != null) {
                JSONObject locJSON = new JSONObject();
                locJSON.put(Constants.MessageKey.LATITUDE, builder.mLocation .getLatitude());
                locJSON.put(Constants.MessageKey.LONGITUDE, builder.mLocation .getLongitude());
                locJSON.put(Constants.MessageKey.ACCURACY, builder.mLocation .getAccuracy());
                put(Constants.MessageKey.LOCATION, locJSON);
            }
        }
    }

    public static class Builder {
        private final String mMessageType;
        private final String mSessionId;
        private long mSessionStartTime;
        private long mTimestamp;
        private String mName;
        private JSONObject mAttributes;
        private Location mLocation;

        public Builder(String messageType, String sessionId){
            mMessageType = messageType;
            mSessionId = sessionId;
        }

        public Builder sessionStartTime(long sessionStartTime){
            mSessionStartTime = sessionStartTime;
            return this;
        }
        public Builder timestamp(long timestamp){
            mTimestamp = timestamp;
            return this;
        }
        public Builder name(String name){
            mName = name;
            return this;
        }
        public Builder attributes(JSONObject attributes){
            mAttributes = attributes;
            return this;
        }

        public Builder location(Location location){
            mLocation = location;
            return this;
        }
        public MPMessage build() throws JSONException {
            return new MPMessage(this);
        }
    }
}
