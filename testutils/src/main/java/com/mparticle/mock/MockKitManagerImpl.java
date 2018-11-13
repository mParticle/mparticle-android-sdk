package com.mparticle.mock;

import android.content.Context;

import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.ReportingManager;
import com.mparticle.kits.KitConfiguration;
import com.mparticle.kits.KitManagerImpl;

import org.json.JSONException;
import org.json.JSONObject;

public class MockKitManagerImpl extends KitManagerImpl {

    public MockKitManagerImpl(Context context, ReportingManager reportingManager, KitFrameworkWrapper.CoreCallbacks coreCallbacks) {
        super(context, reportingManager, coreCallbacks, new BackgroundTaskHandler() {
            @Override
            public void executeNetworkRequest(Runnable runnable) {
                
            }
        });
    }

    @Override
    protected KitConfiguration createKitConfiguration(JSONObject configuration) throws JSONException {
        return MockKitConfiguration.createKitConfiguration(configuration);
    }

    @Override
    public int getUserBucket() {
        return 50;
    }
}
