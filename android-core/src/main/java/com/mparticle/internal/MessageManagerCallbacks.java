package com.mparticle.internal;

import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.messages.BaseMPMessage;

public interface MessageManagerCallbacks {
    String getApiKey() throws MParticleApiClientImpl.MPNoConfigException;

    UploadSettings getUploadSettings() throws MParticleApiClientImpl.MPNoConfigException;

    void delayedStart();

    void endUploadLoop();

    void checkForTrigger(BaseMPMessage message);

    void logNotification(int contentId, String payload, String appState, int behavior);

    void attributeRemoved(String key, long mpId);

    BaseMPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId);

    DeviceAttributes getDeviceAttributes();

    void messagesClearedForUpload();
}
