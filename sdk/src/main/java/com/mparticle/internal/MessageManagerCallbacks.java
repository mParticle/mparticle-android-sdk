package com.mparticle.internal;

import org.json.JSONException;
import org.json.JSONObject;

interface MessageManagerCallbacks {
    void checkForTrigger(MPMessage message);
    MPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes) throws JSONException;
    String getApiKey();
}
