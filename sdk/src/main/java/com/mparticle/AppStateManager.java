package com.mparticle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Created by sdozor on 1/15/14.
 */
public class AppStateManager {
    Context mContext;
    int mActivities = 0;
    long mLastStoppedTime;
    Handler delayedBackgroundCheckHandler = new Handler();

    Runnable backgroundChecker = new Runnable() {
        @Override
        public void run() {
            if (isBackgrounded()){
                MParticleAPI.getInstance(null).logStateTransition(Constants.StateTransitionType.STATE_TRANS_BG);
                Log.d(Constants.LOG_TAG, "APP BACKGROUNDED");
            }
        }
    };

    //it can take some time between when an activity stops and when a new one (or the same one)
    //starts again, so don't declared that we're backgrounded immediately.
    private static final long ACTIVITY_DELAY = 1000;

    public AppStateManager(Context context) {
        mContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            setupLifecycleCallbacks();
        }
    }

    @TargetApi(14)
    private void setupLifecycleCallbacks() {
        ((Application)mContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (isBackgrounded() && mLastStoppedTime > 0){
                    MParticleAPI.getInstance(null).logStateTransition(Constants.StateTransitionType.STATE_TRANS_FORE);
                    Log.d(Constants.LOG_TAG, "APP FOREGROUNDED");
                }
                mActivities++;
                Log.d(Constants.LOG_TAG, "Activity Count: " + mActivities);
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                mActivities--;
                mLastStoppedTime = System.currentTimeMillis();
                if (mActivities < 1){
                    delayedBackgroundCheckHandler.postDelayed(backgroundChecker, ACTIVITY_DELAY);
                }
                Log.d(Constants.LOG_TAG, "Activity Count: " + mActivities);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    boolean isBackgrounded() {
        return mActivities < 1 &&
                (System.currentTimeMillis() - mLastStoppedTime) > ACTIVITY_DELAY;
    }
}
