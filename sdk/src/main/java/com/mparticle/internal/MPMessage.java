package com.mparticle.internal;

import android.location.Location;

import com.mparticle.commerce.CommerceEvent;

import org.json.JSONException;
import org.json.JSONObject;

public class MPMessage extends JSONObject{

    private MPMessage(){}

    public MPMessage(String json) throws JSONException {
        super(json);
    }

    private MPMessage(Builder builder) throws JSONException{
        put(Constants.MessageKey.TYPE, builder.mMessageType);
        put(Constants.MessageKey.TIMESTAMP, builder.mTimestamp);
        if (Constants.MessageType.SESSION_START == builder.mMessageType) {
            put(Constants.MessageKey.ID, builder.mSession.mSessionID);
        } else {
            put(Constants.MessageKey.SESSION_ID, builder.mSession.mSessionID);

            if (builder.mSession.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, builder.mSession.mSessionStartTime);
            }
        }

        if (builder.mName != null) {
            put(Constants.MessageKey.NAME, builder.mName);
        }

        if (builder.mLength != null){
            put(Constants.MessageKey.EVENT_DURATION, builder.mLength);
            if (builder.mAttributes == null){
                builder.mAttributes = new JSONObject();
            }
            if (!builder.mAttributes.has("EventLength")) {
                //can't be longer than max int milliseconds
                builder.mAttributes.put("EventLength", Integer.toString(builder.mLength.intValue()));
            }
        }

        if (builder.mAttributes != null) {
            put(Constants.MessageKey.ATTRIBUTES, builder.mAttributes);
        }

        if (builder.mDataConnection != null) {
            put(Constants.MessageKey.STATE_INFO_DATA_CONNECTION, builder.mDataConnection);
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
        if (builder.commerceEvent != null){
            CommerceEventUtil.addCommerceEventInfo(this, builder.commerceEvent);
        }
    }

    public JSONObject getAttributes(){
        return optJSONObject(Constants.MessageKey.ATTRIBUTES);
    }

    public long getTimestamp(){
        try {
            return getLong(Constants.MessageKey.TIMESTAMP);
        }catch (JSONException jse){

        }
        return 0;
    }

    public String getSessionId() {
        if (Constants.MessageType.SESSION_START.equals(getMessageType())) {
            return optString(Constants.MessageKey.ID, Constants.NO_SESSION_ID);
        } else {
            return optString(Constants.MessageKey.SESSION_ID, Constants.NO_SESSION_ID);
        }
    }

    public String getMessageType() {
        return optString(Constants.MessageKey.TYPE);
    }

    public int getTypeNameHash() {
        return MPUtility.mpHash(getType() + getName());
    }

    public String getType() {
        return optString(Constants.MessageKey.TYPE);
    }

    public String getName() {
        return optString(Constants.MessageKey.NAME);
    }

    public static class Builder {
        private final String mMessageType;
        private final Session mSession;
        private CommerceEvent commerceEvent = null;
        private long mTimestamp;
        private String mName;
        private JSONObject mAttributes;
        private Location mLocation;
        private String mDataConnection;
        private Double mLength = null;

        public Builder(String messageType, Session session, Location location){
            mMessageType = messageType;
            mSession = new Session(session);
            mLocation = location;
        }

        public Builder(CommerceEvent event, Session session, Location location) {
            this(Constants.MessageType.COMMERCE_EVENT, session, location);
            commerceEvent = event;
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

        public Builder dataConnection(String dataConnection) {
            mDataConnection = dataConnection;
            return this;
        }

        public Builder length(Double length) {
            mLength = length;
            return this;
        }
    }
}
