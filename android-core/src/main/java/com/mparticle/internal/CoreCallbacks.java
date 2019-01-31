package com.mparticle.internal;

import android.app.Activity;
import android.net.Uri;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.Map;

public interface CoreCallbacks {
    boolean isBackgrounded();
    int getUserBucket();
    boolean isEnabled();
    void setIntegrationAttributes(int kitId, Map<String, String> integrationAttributes);
    Map<String, String> getIntegrationAttributes(int kitId);
    WeakReference<Activity> getCurrentActivity();
    JSONArray getLatestKitConfiguration();
    boolean isPushEnabled();
    String getPushSenderId();
    String getPushInstanceId();
    Uri getLaunchUri();
    String getLaunchAction();
    void replayAndDisableQueue();
}
