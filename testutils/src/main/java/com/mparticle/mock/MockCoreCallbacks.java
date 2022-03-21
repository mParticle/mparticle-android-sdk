package com.mparticle.mock;

import android.app.Activity;
import android.net.Uri;

import com.mparticle.MParticleOptions;
import com.mparticle.internal.CoreCallbacks;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.Map;

public class MockCoreCallbacks implements CoreCallbacks {

    @Override
    public boolean isBackgrounded() {
        return false;
    }

    @Override
    public int getUserBucket() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setIntegrationAttributes(int kitId, Map<String, String> integrationAttributes) {

    }

    @Override
    public Map<String, String> getIntegrationAttributes(int kitId) {
        return null;
    }

    @Override
    public WeakReference<Activity> getCurrentActivity() {
        return null;
    }

    @Override
    public JSONArray getLatestKitConfiguration() {
        return null;
    }

    @Override
    public MParticleOptions.DataplanOptions getDataplanOptions() {
        return null;
    }

    @Override
    public boolean isPushEnabled() {
        return false;
    }

    @Override
    public String getPushSenderId() {
        return null;
    }

    @Override
    public String getPushInstanceId() {
        return null;
    }

    @Override
    public Uri getLaunchUri() {
        return null;
    }

    @Override
    public String getLaunchAction() {
        return null;
    }

    @Override
    public KitListener getKitListener() {
        return KitListener.EMPTY;
    }
}
