package com.mparticle.internal;

import org.json.JSONObject;

public interface JsonReportingMessage {
    void setDevMode(boolean development);

    long getTimestamp();

    int getModuleId();

    JSONObject toJson();

    String getSessionId();

    void setSessionId(String sessionId);
}
