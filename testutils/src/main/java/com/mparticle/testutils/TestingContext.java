package com.mparticle.testutils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;

public class TestingContext extends ContextWrapper {
    TestingContext(Context context) {
        super(context);
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        if (permission.equals("com.google.android.c2dm.permission.RECEIVE")) {
            return PackageManager.PERMISSION_GRANTED;
        }
        return super.checkCallingOrSelfPermission(permission);
    }
}