package com.mparticle.messaging;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.mparticle.internal.MPUtility;

/**
 * mParticle implementation of InstanceIDListenerService. In order to support push notifications, you must
 * include this Service within your app's AndroidManifest.xml with an intent-filter for 'com.google.android.gms.iid.InstanceID'
 */
public class InstanceIdService extends Service {
    Service gcmInstanceIdService;
    Service firebaseInstanceIdService;

    public InstanceIdService() {
        Runnable runnable = null;
        switch (MPUtility.getAvailableInstanceId()) {
            case FCM:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        firebaseInstanceIdService = new FcmInstanceIdService(InstanceIdService.this);
                    }
                };
                break;
            case GCM:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        gcmInstanceIdService = new GcmInstanceIdService(InstanceIdService.this);
                    }
                };
        }
        runnable.run();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (getWrapperedInstance() != null) {
            getWrapperedInstance().setBaseContext(base);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (getInstance() != null) {
            return getInstance().onBind(intent);
        }
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        if (getInstance() != null) {
            getInstance().onRebind(intent);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        if (getInstance() != null) {
            return getInstance().onUnbind(intent);
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (getInstance() != null) {
            return getInstance().onStartCommand(intent, flags, startId);
        }
        return 0;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (getInstance() != null) {
            getInstance().onStart(intent, startId);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (getInstance() != null) {
            getInstance().onCreate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (getInstance() != null) {
            getInstance().onLowMemory();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (getInstance() != null) {
            getInstance().onTaskRemoved(rootIntent);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getInstance() != null) {
            getInstance().onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getInstance() != null) {
            getInstance().onDestroy();
        }
    }

    public interface WrappedService {
        void setBaseContext(Context context);
    }

    private Service getInstance() {
        if (gcmInstanceIdService != null) {
            return gcmInstanceIdService;
        }
        if (firebaseInstanceIdService != null) {
            return firebaseInstanceIdService;
        }
        stopSelf();
        return null;
    }

    private WrappedService getWrapperedInstance() {
        if (gcmInstanceIdService != null) {
            return (WrappedService)gcmInstanceIdService;
        }
        if (firebaseInstanceIdService != null) {
            return (WrappedService)firebaseInstanceIdService;
        }
        return null;
    }
}