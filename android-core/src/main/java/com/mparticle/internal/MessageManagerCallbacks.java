package com.mparticle.internal;

public interface MessageManagerCallbacks {
    String getApiKey() throws MParticleApiClientImpl.MPNoConfigException;
    void delayedStart();
    void endUploadLoop();
    void checkForTrigger(MessageManager.BaseMPMessage message);
    void logNotification(int contentId, String payload, String appState, int behavior);
    void attributeRemoved(String key, long mpId);
    MessageManager.BaseMPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId);
    DeviceAttributes getDeviceAttributes();
    void messagesClearedForUpload();
}
