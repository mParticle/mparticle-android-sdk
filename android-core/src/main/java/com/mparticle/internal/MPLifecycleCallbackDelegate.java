package com.mparticle.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

/**
 * This class is used by the AppStateManager to determine when the app is visible or in the background.
 *
 * Separated into it's own class to avoid annoying logcat messages on pre-ICS devices.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class MPLifecycleCallbackDelegate implements Application.ActivityLifecycleCallbacks {
    private AppStateManager mStateManager;

    public MPLifecycleCallbackDelegate(AppStateManager stateManager) {
        mStateManager = stateManager;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mStateManager.onActivityCreated(activity, savedInstanceState);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mStateManager.onActivityStarted(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mStateManager.onActivityResumed(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mStateManager.onActivityPaused(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mStateManager.onActivityStopped(activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        mStateManager.onActivitySaveInstanceState(activity, outState);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mStateManager.onActivityDestroyed(activity);
    }
}
