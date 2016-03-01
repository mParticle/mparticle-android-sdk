package com.mparticle.internal;

import org.json.JSONObject;

public interface ReportingMessage {
    void setDevMode(boolean development);

    long getTimestamp();

    int getModuleId();

    JSONObject toJson();
}
