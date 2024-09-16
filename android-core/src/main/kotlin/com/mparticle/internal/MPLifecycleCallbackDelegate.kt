package com.mparticle.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import android.os.Bundle

/**
 * This class is used by the AppStateManager to determine when the app is visible or in the background.
 *
 * Separated into its own class to avoid annoying logcat messages on pre-ICS devices.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
internal class MPLifecycleCallbackDelegate(private val mStateManager: AppStateManager) :
    ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        mStateManager.onActivityCreated(activity, savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity) {
        mStateManager.onActivityStarted(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        mStateManager.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        mStateManager.onActivityPaused(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        mStateManager.onActivityStopped(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        mStateManager.onActivitySaveInstanceState(activity, outState)
    }

    override fun onActivityDestroyed(activity: Activity) {
        mStateManager.onActivityDestroyed(activity)
    }
}
