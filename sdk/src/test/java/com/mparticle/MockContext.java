package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.Assert.fail;

public class MockContext extends android.test.mock.MockContext {

    SharedPreferences sharedPreferences = new MockSharedPreferences();
    Resources resources = new MockResources();
    @Override
    public Context getApplicationContext() {
        return this;
    }

    public void setSharedPreferences(SharedPreferences prefs){
        sharedPreferences = prefs;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return sharedPreferences;
    }

    @Override
    public PackageManager getPackageManager() {
        PackageManager manager = Mockito.mock(PackageManager.class);
        PackageInfo info = Mockito.mock(PackageInfo.class);
        info.versionName = "42";
        info.versionCode = 42;
        ApplicationInfo appInfo = Mockito.mock(ApplicationInfo.class);
        try {
            Mockito.when(manager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(info);
            Mockito.when(manager.getInstallerPackageName(Mockito.anyString())).thenReturn("com.mparticle.test.installer");

            Mockito.when(manager.getApplicationInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(appInfo);
            Mockito.when(manager.getApplicationLabel(appInfo)).thenReturn("test label");
        }catch (Exception e){
            fail(e.toString());
        }
        return manager;
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
