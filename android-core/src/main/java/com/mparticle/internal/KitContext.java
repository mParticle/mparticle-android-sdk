package com.mparticle.internal;


import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;


public class KitContext extends ContextWrapper {
    ApplicationContextWrapper applicationContextWrapper;

    public KitContext(Context base) {
        super(base);
        applicationContextWrapper = new ApplicationContextWrapper((Application) base.getApplicationContext());
    }

    @Override
    public Context getApplicationContext() {
        return applicationContextWrapper;
    }
}
