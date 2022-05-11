package com.mparticle.internal.messages;


import android.location.Location;
import androidx.annotation.Nullable;

import com.mparticle.internal.Constants;
import com.mparticle.internal.InternalSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BaseMPMessageBuilder extends JSONObject {
    private Double mLength = null;

    public BaseMPMessageBuilder(String messageType) {
        try {
            put(Constants.MessageKey.TYPE, messageType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public BaseMPMessageBuilder timestamp(long timestamp) {
        try {
            put(Constants.MessageKey.TIMESTAMP, timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Long getTimestamp() {
        long timestamp = optLong(Constants.MessageKey.TIMESTAMP, -1);
        if (timestamp != -1) {
            return timestamp;
        } else {
            return null;
        }
    }

    public BaseMPMessageBuilder name(String name) {
        try {
            put(Constants.MessageKey.NAME, name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public BaseMPMessageBuilder attributes(JSONObject attributes) {
        if (attributes != null && attributes.length() > 0) {
            try {
                put(Constants.MessageKey.ATTRIBUTES, attributes);
                if (mLength != null) {
                    addEventLengthAttributes(mLength);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public String getMessageType() {
        return optString(Constants.MessageKey.TYPE);
    }

    public BaseMPMessage build(InternalSession session, @Nullable Location location, long mpId) throws JSONException {
        return new BaseMPMessage(this, session, location, mpId);
    }

    public BaseMPMessageBuilder dataConnection(String dataConnection) {
        if (dataConnection != null) {
            try {
                put(Constants.MessageKey.STATE_INFO_DATA_CONNECTION, dataConnection);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public BaseMPMessageBuilder length(Double length) {
        mLength = length;
        if (length != null){
            try {
                put(Constants.MessageKey.EVENT_DURATION, length);
                addEventLengthAttributes(length);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    private void addEventLengthAttributes(Double length) {
        try {
            if (!has(Constants.MessageKey.ATTRIBUTES)) {
                put(Constants.MessageKey.ATTRIBUTES, new JSONObject());
            }
            if (!getJSONObject(Constants.MessageKey.ATTRIBUTES).has("EventLength")) {
                //can't be longer than max int milliseconds
                getJSONObject(Constants.MessageKey.ATTRIBUTES).put("EventLength", Integer.toString(length.intValue()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public BaseMPMessageBuilder flags(Map<String, List<String>> customFlags) {
        if (customFlags != null) {
            try {

                JSONObject flagsObject = new JSONObject();
                for (Map.Entry<String, List<String>> entry : customFlags.entrySet()) {
                    List<String> values = entry.getValue();
                    JSONArray valueArray = new JSONArray(values);
                    flagsObject.put(entry.getKey(), valueArray);
                }
                put(Constants.MessageKey.EVENT_FLAGS, flagsObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    String[] getKeys() {
        List<String> strings = new ArrayList();
        Iterator<String> iterator = keys();
        while (iterator.hasNext()) {
            strings.add(iterator.next());
        }
        return strings.toArray(new String[0]);
    }
}