package com.mparticle.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

/**
 * Created by sdozor on 3/23/15.
 */
public class MockContext extends android.test.mock.MockContext {

    SharedPreferences sharedPreferences = new MockSharedPreferences();
    Resources resources = new MockResources();
    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return sharedPreferences;
    }

    @Override
    public String getPackageName() {
        return "com.mparticle.test";
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return new ApplicationInfo();
    }

    @Override
    public Resources getResources() {
        return resources;
    }
}
