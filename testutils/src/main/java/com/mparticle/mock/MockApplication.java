package com.mparticle.mock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import java.io.File;

/**
 * Created by sdozor on 4/10/15.
 */
public class MockApplication extends Application {
    MockContext mContext;
    public ActivityLifecycleCallbacks mCallbacks;

    public MockApplication(MockContext context) {
        super();
        mContext = context;
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        mCallbacks = callback;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    public void setSharedPreferences(SharedPreferences prefs) {
        mContext.setSharedPreferences(prefs);
    }

    @Override
    public Object getSystemService(String name) {
        return mContext.getSystemService(name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mContext.getSharedPreferences(name, mode);
    }

    @Override
    public PackageManager getPackageManager() {
        return mContext.getPackageManager();
    }

    @Override
    public String getPackageName() {
        return mContext.getPackageName();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mContext.getApplicationInfo();
    }

    @Override
    public Resources getResources() {
        return mContext.getResources();
    }

    @Override
    public File getFilesDir() {
        return mContext.getFilesDir();
    }
}
