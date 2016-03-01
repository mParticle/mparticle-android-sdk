package com.mparticle.internal;

import android.content.Context;

import com.mparticle.mock.MockContext;

/**
 * Created by sdozor on 3/1/16.
 */
public class MockKitManager extends KitManager {
    public MockKitManager(Context context) {
        super();
    }

    @Override
    public String getActiveModuleIds() {
        return "this is a fake module id string";
    }
}
