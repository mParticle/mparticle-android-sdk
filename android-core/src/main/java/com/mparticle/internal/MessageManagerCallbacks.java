package com.mparticle.internal;

import com.mparticle.messaging.CloudAction;

import org.json.JSONException;
import org.json.JSONObject;

interface MessageManagerCallbacks {
    MPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes) throws JSONException;
    String getApiKey();
    void delayedStart();
    void endUploadLoop();
    void checkForTrigger(MPMessage message);
    void logNotification(int contentId, String payload, CloudAction action, String appState, int behavior);
    void attributeRemoved(String key);
    MPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time);
}
