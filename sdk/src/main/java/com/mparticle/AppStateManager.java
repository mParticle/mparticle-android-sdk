package com.mparticle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.Session;
import com.mparticle.internal.embedded.EmbeddedKitManager;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is responsible for managing sessions, by detecting how many
 *
 */
 public class AppStateManager implements MPActivityCallbacks {

    private ConfigManager mConfigManager;
    Context mContext;
    private final SharedPreferences mPreferences;
    private EmbeddedKitManager mEmbeddedKitManager;
    private Session mCurrentSession = new Session();

    private String mCurrentActivity;
    /**
     * This boolean is important in determining if the app is running due to the user opening the app,
     * or if we're running due to the reception of a Intent such as a GCM message.
     */
    public static boolean mInitialized;

    /**
     * Keep track of how many active activities there are to determine if the app is in the foreground/visible
     * to the user.
     */
    AtomicInteger mActivities = new AtomicInteger(0);
    AtomicLong mLastStoppedTime;
    /**
     * it can take some time between when an activity stops and when a new one (or the same one on a configuration change/rotation)
     * starts again, so use this handler and ACTIVITY_DELAY to determine when we're *really" in the background
     */
    Handler delayedBackgroundCheckHandler = new Handler();
    private static final long ACTIVITY_DELAY = 1000;


    /**
     * Some providers need to know for the given session, how many 'interruptions' there were - how many
     * times did the user leave and return prior to the session timing out.
     */
    AtomicInteger mInterruptionCount = new AtomicInteger(0);

    /**
     * Constants used by the messaging/push framework to describe the app state when various
     * interactions occur (receive/show/tap).
     */
    public static final String APP_STATE_FOREGROUND = "foreground";
    public static final String APP_STATE_BACKGROUND = "background";
    public static final String APP_STATE_NOTRUNNING = "not_running";

    /**
     * Important to determine foreground-time length for a given session.
     * Uses the system-uptime clock to avoid devices which wonky clocks, or clocks
     * that change while the app is running.
     */
    private long mLastForegroundTime;
    /**
     * Various fields required to log along with state-transition messages to associate
     * outside apps/packages with user activity.
     */
    private String previousSessionPackage;
    private String previousSessionParameters;
    private String previousSessionUri;


    public AppStateManager(Context context) {
        mContext = context.getApplicationContext();
        mLastStoppedTime = new AtomicLong(SystemClock.elapsedRealtime());
        mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }

    public void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setupLifecycleCallbacks();
        }
    }

    public void setEmbeddedKitManager(EmbeddedKitManager manager){
        mEmbeddedKitManager = manager;
    }

    public void setConfigManager(ConfigManager manager){
        mConfigManager = manager;
    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {
        try {
            mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, true).apply();
            mCurrentActivity = AppStateManager.getActivityName(activity);

            int interruptions = mInterruptionCount.get();
            if (!mInitialized || !MParticle.getInstance().isSessionActive()) {
                gatherSourceInfo(activity);
            }

            if (!mInitialized) {
                mInitialized = true;
                MParticle.getInstance().logStateTransition(Constants.StateTransitionType.STATE_TRANS_INIT,
                        mCurrentActivity,
                        0,
                        0,
                        previousSessionUri,
                        previousSessionParameters,
                        previousSessionPackage,
                        0);
                mLastForegroundTime = SystemClock.elapsedRealtime();
            } else if (isBackgrounded() && mLastStoppedTime.get() > 0) {
                long totalTimeInBackground = mPreferences.getLong(Constants.PrefKeys.TIME_IN_BG, -1);
                if (totalTimeInBackground > -1) {
                    totalTimeInBackground += (SystemClock.elapsedRealtime() - mLastStoppedTime.get());
                } else {
                    totalTimeInBackground = 0;
                }
                mPreferences.edit().putLong(Constants.PrefKeys.TIME_IN_BG, totalTimeInBackground).apply();

                MParticle.getInstance().logStateTransition(Constants.StateTransitionType.STATE_TRANS_FORE,
                        mCurrentActivity,
                        mLastStoppedTime.get() - mLastForegroundTime,
                        SystemClock.elapsedRealtime() - mLastStoppedTime.get(),
                        previousSessionUri,
                        previousSessionParameters,
                        previousSessionPackage,
                        interruptions);
                ConfigManager.log(MParticle.LogLevel.DEBUG, "App foregrounded.");
                mLastForegroundTime = SystemClock.elapsedRealtime();
            }

            mActivities.getAndIncrement();

            if (MParticle.getInstance().isAutoTrackingEnabled()) {
                MParticle.getInstance().logScreen(mCurrentActivity, null, true);
            }
            mEmbeddedKitManager.onActivityStarted(activity, mActivities.get());
        }catch (Exception e){
            Log.w(Constants.LOG_TAG, "Failed while trying to track activity start: " + e.getMessage());
        }
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
        try {
            mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).apply();
            mLastStoppedTime = new AtomicLong(SystemClock.elapsedRealtime());

            if (mActivities.decrementAndGet() < 1) {
                delayedBackgroundCheckHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isBackgrounded()) {
                                checkSessionTimeout();
                                logBackgrounded();
                            }
                        }catch (Exception e){

                        }
                    }
                }, ACTIVITY_DELAY);
            }
            if (MParticle.getInstance().isAutoTrackingEnabled()) {
                MParticle.getInstance().logScreen(AppStateManager.getActivityName(activity), null, false);
            }
            mEmbeddedKitManager.onActivityStopped(activity, mActivities.get());
        }catch (Exception e){
            Log.w(Constants.LOG_TAG, "Failed while trying to track activity stop: " + e.getMessage());
        }
    }

    private void checkSessionTimeout() {
        delayedBackgroundCheckHandler.postDelayed( new Runnable() {
            @Override
            public void run() {
                MParticle.getInstance().checkSessionTimeout();
            }
        }, mConfigManager.getSessionTimeout());
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount){
        mEmbeddedKitManager.onActivityCreated(activity, mActivities.get());
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount){
        mEmbeddedKitManager.onActivityResumed(activity, mActivities.get());
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        mEmbeddedKitManager.onActivityPaused(activity, mActivities.get());
    }

    private void logBackgrounded(){
        MParticle.getInstance().logStateTransition(Constants.StateTransitionType.STATE_TRANS_BG, mCurrentActivity);
        mCurrentActivity = null;
        ConfigManager.log(MParticle.LogLevel.DEBUG, "App backgrounded.");
        mInterruptionCount.incrementAndGet();
    }

    @TargetApi(14)
    private void setupLifecycleCallbacks() {
        ((Application) mContext).registerActivityLifecycleCallbacks(new MPLifecycleCallbackDelegate(this));
    }

    public boolean isBackgrounded() {
        return mActivities.get() < 1 && (SystemClock.elapsedRealtime() - mLastStoppedTime.get() >= ACTIVITY_DELAY);
    }

    private static String getActivityName(Activity activity) {
        return activity.getClass().getCanonicalName();
    }

    public String getCurrentActivity() {
        return mCurrentActivity;
    }

    public Session getSession() {
        return mCurrentSession;
    }

    public void endSession() {
        mCurrentSession = new Session();
    }

    public void startSession() {
        mCurrentSession = new Session().start();
    }
}
