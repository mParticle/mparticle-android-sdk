package com.mparticle.internal.messages;

import android.location.Location;

import androidx.annotation.Nullable;

import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseMPMessage extends JSONObject {
    private long mpId;

    protected BaseMPMessage() {
    }

    protected BaseMPMessage(BaseMPMessageBuilder builder, InternalSession session, @Nullable Location location, long mpId) throws JSONException {
        super(builder, builder.getKeys());
        this.mpId = mpId;
        if (!has(Constants.MessageKey.TIMESTAMP)) {
            put(Constants.MessageKey.TIMESTAMP, session.mLastEventTime);
        }
        if (Constants.MessageType.SESSION_START.equals(builder.getMessageType())) {
            put(Constants.MessageKey.ID, session.mSessionID);
        } else {
            put(Constants.MessageKey.SESSION_ID, session.mSessionID);

            if (session.mSessionStartTime > 0) {
                put(Constants.MessageKey.SESSION_START_TIMESTAMP, session.mSessionStartTime);
            }
        }


        if (!(Constants.MessageType.ERROR.equals(builder.getMessageType()) &&
                !(Constants.MessageType.OPT_OUT.equals(builder.getMessageType())))) {
            if (location != null) {
                JSONObject locJSON = new JSONObject();
                locJSON.put(Constants.MessageKey.LATITUDE, location.getLatitude());
                locJSON.put(Constants.MessageKey.LONGITUDE, location.getLongitude());
                locJSON.put(Constants.MessageKey.ACCURACY, location.getAccuracy());
                put(Constants.MessageKey.LOCATION, locJSON);
            }
        }
    }

    public JSONObject getAttributes() {
        return optJSONObject(Constants.MessageKey.ATTRIBUTES);
    }

    public long getTimestamp() {
        try {
            return getLong(Constants.MessageKey.TIMESTAMP);
        } catch (JSONException jse) {

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

        public Builder(String messageType) {
            super(messageType);
        }
    }
}
