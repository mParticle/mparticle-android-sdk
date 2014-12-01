package com.mparticle.internal;

import android.app.Activity;

/**
 * Created by sdozor on 3/17/14.
 */
public interface MPActivityCallbacks {
    void onActivityCreated(Activity activity, int activityCount);
    void onActivityResumed(Activity activity, int activityCount);
    void onActivityPaused(Activity activity, int activityCount);
    void onActivityStopped(Activity activity, int activityCount);
    void onActivityStarted(Activity activity, int activityCount);
}
