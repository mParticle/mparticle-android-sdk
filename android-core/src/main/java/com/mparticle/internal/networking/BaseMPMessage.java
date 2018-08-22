package com.mparticle.internal.networking;

import android.location.Location;

import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.InternalSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BaseMPMessage extends JSONObject{
    private long mpId;

    protected BaseMPMessage(){}

    protected BaseMPMessage(BaseMPMessageBuilder builder) throws JSONException{
        mpId = builder.mpid;
        put(Constants.MessageKey.TYPE, builder.mMessageType);
        put(Constants.MessageKey.TIMESTAMP, builder.mTimestamp);
        if (Constants.MessageType.SESSION_START.equals(builder.mMessageType)) {
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

        if (builder.mCustomFlags != null) {
            JSONObject flagsObject = new JSONObject();
            for (Map.Entry<String, List<String>> entry : builder.mCustomFlags.entrySet()) {
                List<String> values = entry.getValue();
                JSONArray valueArray = new JSONArray(values);
                flagsObject.put(entry.getKey(), valueArray);
            }
            put(Constants.MessageKey.EVENT_FLAGS, flagsObject);
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
        return MPUtility.mpHash(getMessageType() + getName());
    }

    public String getName() {
        return optString(Constants.MessageKey.NAME);
    }

    public long getMpId() {
        return mpId;
    }

    public static class Builder extends BaseMPMessageBuilder {

        public Builder(String messageType, InternalSession session, Location location, long mpId) {
            super(messageType, session, location, mpId);
        }
    }
}
