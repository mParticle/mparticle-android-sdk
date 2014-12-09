package com.mparticle.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.support.v4.app.FragmentActivity;


import com.mparticle.MParticlePushUtility;
import com.mparticle.messaging.CloudDialog;
import com.mparticle.messaging.MPCloudNotificationMessage;

import com.mparticle.MParticle;
import com.mparticle.internal.embedded.EmbeddedKitManager;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class AppStateManager implements MPActivityCallbacks{

    public static final String APP_STATE_FOREGROUND = "foreground";
    public static final String APP_STATE_BACKGROUND = "background";
    public static final String APP_STATE_NOTRUNNING = "not_running";
    private final SharedPreferences mPreferences;
    private final EmbeddedKitManager mEmbeddedKitManager;
    private final boolean mSupportLib;
    private Class unityActivity = null;
    private String mCurrentActivity;
    private boolean mInitialized;
    Context mContext;
    AtomicInteger mActivities = new AtomicInteger(0);
    long mLastStoppedTime;
    Handler delayedBackgroundCheckHandler = new Handler();
    private String previousSessionPackage;
    private String previousSessionParameters;
    private String previousSessionUri;
    AtomicInteger mInterruptionCount = new AtomicInteger(0);
    //it can take some time between when an activity stops and when a new one (or the same one)
    //starts again, so don't declared that we're backgrounded immediately.
    private static final long ACTIVITY_DELAY = 1000;
    private long mLastForegroundTime;
    public static boolean appRunning = false;

    public AppStateManager(Context context, EmbeddedKitManager embeddedKitManager) {
        mContext = context.getApplicationContext();
        mLastStoppedTime = System.currentTimeMillis();
        mEmbeddedKitManager = embeddedKitManager;

        mSupportLib = MPUtility.isSupportLibAvailable();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setupLifecycleCallbacks();
        }
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        try {
            unityActivity = Class.forName("com.unity3d.player.UnityPlayerNativeActivity");
        }catch (ClassNotFoundException cne){

        }
    }
    @Override
    public void onActivityStarted(Activity activity, int currentCount) {
        appRunning = true;
        mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, true).commit();
        mCurrentActivity = AppStateManager.getActivityName(activity);

        int interruptions = mInterruptionCount.get();
        if (!mInitialized || !MParticle.getInstance().internal().isSessionActive()) {
            gatherSourceInfo(activity);
        }

        if (!mInitialized){
            mInitialized = true;
            MParticle.getInstance().internal().logStateTransition(Constants.StateTransitionType.STATE_TRANS_INIT,
                    mCurrentActivity,
                    0,
                    0,
                    previousSessionUri,
                    previousSessionParameters,
                    previousSessionPackage,
                    0);
            mLastForegroundTime = System.currentTimeMillis();
        }else if (isBackgrounded() && mLastStoppedTime > 0) {
            long totalTimeInBackground = mPreferences.getLong(Constants.PrefKeys.TIME_IN_BG, -1);
            if (totalTimeInBackground > -1){
                totalTimeInBackground += (System.currentTimeMillis() - mLastStoppedTime);
            }else{
                totalTimeInBackground = 0;
            }

            mPreferences.edit().putLong(Constants.PrefKeys.TIME_IN_BG, totalTimeInBackground).commit();
            MParticle.getInstance().internal().logStateTransition(Constants.StateTransitionType.STATE_TRANS_FORE,
                    mCurrentActivity,
                    mLastStoppedTime - mLastForegroundTime,
                    System.currentTimeMillis() - mLastStoppedTime,
                    previousSessionUri,
                    previousSessionParameters,
                    previousSessionPackage,
                    interruptions);
            ConfigManager.log(MParticle.LogLevel.DEBUG, "App foregrounded.");
            mLastForegroundTime = System.currentTimeMillis();
        }

        mActivities.getAndIncrement();

        if (MParticle.getInstance().isAutoTrackingEnabled()) {
            MParticle.getInstance().internal().logScreen(mCurrentActivity, null, true);
        }
        mEmbeddedKitManager.onActivityStarted(activity, mActivities.get());
    }

    private void gatherSourceInfo(Activity activity) {
        mInterruptionCount = new AtomicInteger(0);
        if (activity != null){
            ComponentName callingApplication = activity.getCallingActivity();
            if (callingApplication != null) {
                previousSessionPackage = callingApplication.getPackageName();
            }
            if(activity.getIntent() != null) {
                previousSessionUri = activity.getIntent().getDataString();

                if (activity.getIntent().getExtras() != null && activity.getIntent().getExtras().getBundle(Constants.External.APPLINK_KEY) != null) {
                    JSONObject parameters = new JSONObject();
                    try {
                        parameters.put(Constants.External.APPLINK_KEY, MPUtility.wrapExtras(activity.getIntent().getExtras().getBundle(Constants.External.APPLINK_KEY)));
                    } catch (Exception e) {

                    }
                    previousSessionParameters = parameters.toString();
                }
            }
        }
    }


    @Override
    public void onActivityStopped(Activity activity, int currentCount) {
        mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).commit();
        mLastStoppedTime = System.currentTimeMillis();

        if (mActivities.decrementAndGet() < 1) {
            if (unityActivity != null && unityActivity.isInstance(activity)){
                logBackgrounded();
            }else {
                delayedBackgroundCheckHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isBackgrounded()) {
                            logBackgrounded();
                        }
                    }
                }, ACTIVITY_DELAY);
            }
        }
        if (MParticle.getInstance().isAutoTrackingEnabled()) {
            MParticle.getInstance().internal().logScreen(AppStateManager.getActivityName(activity), null, false);
        }
        mEmbeddedKitManager.onActivityStopped(activity, mActivities.get());
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount){
        mEmbeddedKitManager.onActivityCreated(activity, mActivities.get());
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount){
        mEmbeddedKitManager.onActivityResumed(activity, mActivities.get());
        showCloudDialog(activity);
    }

    private void showCloudDialog(Activity activity){
        Intent intent = activity.getIntent();
        if (mSupportLib && intent != null && activity instanceof FragmentActivity){
            Parcelable message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
            if (message != null) {
                String contentId = ((MPCloudNotificationMessage) message).getContentId();
                String shownId = intent.getStringExtra("mp_content_shown");
                if (((MPCloudNotificationMessage) message).getShowInApp() && !contentId.equals(shownId)) {
                    activity.getIntent().putExtra("mp_content_shown", ((MPCloudNotificationMessage) message).getContentId());
                    CloudDialog.newInstance((MPCloudNotificationMessage) message)
                            .show(((FragmentActivity) activity).getSupportFragmentManager(), CloudDialog.TAG);
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        mEmbeddedKitManager.onActivityPaused(activity, mActivities.get());
    }

    private void logBackgrounded(){
        MParticle.getInstance().internal().logStateTransition(Constants.StateTransitionType.STATE_TRANS_BG, mCurrentActivity);
        mCurrentActivity = null;
        ConfigManager.log(MParticle.LogLevel.DEBUG, "App backgrounded.");
        mInterruptionCount.incrementAndGet();
    }

    @TargetApi(14)
    private void setupLifecycleCallbacks() {
        ((Application) mContext).registerActivityLifecycleCallbacks(new MPLifecycleCallbackDelegate(this));
    }

    public boolean isBackgrounded() {
        return mActivities.get() < 1 && (System.currentTimeMillis() - mLastStoppedTime >= ACTIVITY_DELAY);
    }

    private static String getActivityName(Activity activity) {
        return activity.getClass().getCanonicalName();
    }

    public String getCurrentActivity() {
        return mCurrentActivity;
    }
}
