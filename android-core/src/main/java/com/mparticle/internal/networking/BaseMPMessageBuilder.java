package com.mparticle.internal.networking;

import android.location.Location;

import com.mparticle.internal.InternalSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BaseMPMessageBuilder {
    final String mMessageType;
    final InternalSession mSession;
    long mTimestamp;
    String mName;
    JSONObject mAttributes;
    Location mLocation;
    String mDataConnection;
    Double mLength = null;
    Map<String, List<String>> mCustomFlags;
    long mpid;

    protected BaseMPMessageBuilder(String messageType, InternalSession session, Location location, long mpId) {
        mMessageType = messageType;
        mSession = new InternalSession(session);
        mLocation = location;
        mpid = mpId;
    }

    public BaseMPMessageBuilder timestamp(long timestamp) {
        mTimestamp = timestamp;
        return this;
    }

    public BaseMPMessageBuilder name(String name) {
        mName = name;
        return this;
    }

    public BaseMPMessageBuilder attributes(JSONObject attributes) {
        mAttributes = attributes;
        return this;
    }

    public BaseMPMessage build() throws JSONException {
        return new BaseMPMessage(this);
    }

    public BaseMPMessageBuilder dataConnection(String dataConnection) {
        mDataConnection = dataConnection;
        return this;
    }

    public BaseMPMessageBuilder length(Double length) {
        mLength = length;
        return this;
    }

    public BaseMPMessageBuilder flags(Map<String, List<String>> customFlags) {
        mCustomFlags = customFlags;
        return this;
    }
}
