package com.mparticle.internal;

import android.app.Activity;
import android.net.Uri;

import com.mparticle.MParticleOptions;

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
    MParticleOptions.DataplanOptions getDataplanOptions();
    boolean isPushEnabled();
    String getPushSenderId();
    String getPushInstanceId();
    Uri getLaunchUri();
    String getLaunchAction();
    void replayAndDisableQueue();
    KitListener getKitListener();

    interface KitListener {

        void kitFound(int kitId);
        void kitConfigReceived(int kitId, String configuration);
        void kitExcluded(int kitId, String reason);
        void kitStarted(int kitId);
        void onKitApiCalled(int kitId, Boolean used, Object... objects);
        void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects);

        KitListener EMPTY = new KitListener() {
            public void kitFound(int kitId) {}
            public void kitConfigReceived(int kitId, String configuration) { }
            public void kitExcluded(int kitId, String reason) { }
            public void kitStarted(int kitId) { }
            public void onKitApiCalled(int kitId, Boolean used, Object... objects) { }
            public void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects) { }
        };
    }
}
