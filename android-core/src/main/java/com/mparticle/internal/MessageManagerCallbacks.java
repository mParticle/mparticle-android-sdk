package com.mparticle.internal;

import com.mparticle.messaging.CloudAction;

import org.json.JSONException;
import org.json.JSONObject;

public interface MessageManagerCallbacks {
    String getApiKey() throws MParticleApiClientImpl.MPNoConfigException;
    void delayedStart();
    void endUploadLoop();
    void checkForTrigger(MPMessage message);
    void logNotification(int contentId, String payload, CloudAction action, String appState, int behavior);
    void attributeRemoved(String key);
    MPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId);
    DeviceAttributes getDeviceAttributes();
}
