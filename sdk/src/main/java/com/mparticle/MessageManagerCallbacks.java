package com.mparticle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 10/14/14.
 */
interface MessageManagerCallbacks {


    void checkForTrigger(MPMessage message);

    MPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes) throws JSONException;
}
