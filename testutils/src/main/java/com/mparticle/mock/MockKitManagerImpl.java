package com.mparticle.mock;

import android.content.Context;

import com.mparticle.MParticleOptions;
import com.mparticle.internal.BackgroundTaskHandler;
import com.mparticle.internal.CoreCallbacks;
import com.mparticle.internal.ReportingManager;
import com.mparticle.kits.KitConfiguration;
import com.mparticle.kits.KitManagerImpl;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

public class MockKitManagerImpl extends KitManagerImpl {

    public MockKitManagerImpl() {
        this(new MockContext(), Mockito.mock(ReportingManager.class), Mockito.mock(CoreCallbacks.class));
        Mockito.when(mCoreCallbacks.getKitListener()).thenReturn(CoreCallbacks.KitListener.EMPTY);
    }

    public MockKitManagerImpl(Context context, ReportingManager reportingManager, CoreCallbacks coreCallbacks) {
        super(context, reportingManager, coreCallbacks, new BackgroundTaskHandler() {
            @Override
            public void executeNetworkRequest(Runnable runnable) {
                
            }
        }, Mockito.mock(MParticleOptions.class));
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
