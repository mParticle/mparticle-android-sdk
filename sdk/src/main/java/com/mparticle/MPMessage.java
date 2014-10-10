package com.mparticle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 10/9/14.
 */
public class MPMessage extends JSONObject{

    private MPMessage(){}
    private MPMessage(Builder builder) throws JSONException{
        put(Constants.MessageKey.TYPE, builder.mMessageType);

    }

    public static class Builder {
        private final String mMessageType;
        private final String mSessionId;
        private long mSessionStartTime;
        private long mTimestamp;
        private String mName;
        private JSONObject mAttributes;

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
        public MPMessage build() throws JSONException {
            return new MPMessage(this);
        }
    }
}
