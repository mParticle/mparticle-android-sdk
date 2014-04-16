package com.mparticle;

import android.app.Activity;

/**
 * Created by sdozor on 3/17/14.
 */
interface MPActivityCallbacks {
    void onActivityCreated(Activity activity);
    void onActivityResumed(Activity activity);
    void onActivityPaused(Activity activity);
    void onActivityStopped(Activity activity);
    void onActivityStarted(Activity activity);
}
