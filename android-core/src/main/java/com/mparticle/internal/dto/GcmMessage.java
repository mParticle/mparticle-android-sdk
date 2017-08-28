package com.mparticle.internal.dto;

import com.mparticle.messaging.AbstractCloudMessage;

public class GcmMessage {
    private int id;
    private String payload;
    private String appState;
    private int cloudMessage;

    public GcmMessage(int id, String payload, String appState, int cloudMessage) {
        this.id = id;
        this.payload = payload;
        this.appState = appState;
        this.cloudMessage = cloudMessage;
    }

    public int getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public String getAppState() {
        return appState;
    }

    public int getCloudMessage() {
        return cloudMessage;
    }
}
