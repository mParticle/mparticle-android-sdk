package com.mparticle.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import com.mparticle.MParticle;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ApplicationContextWrapper extends Application {
    private Application mBaseApplication;
    private boolean mReplay = true;
    private boolean mRecord = true;
    private ActivityLifecycleCallbackRecorder mActivityLifecycleCallbackRecorder;

    enum MethodType { ON_CREATED, ON_STARTED, ON_RESUMED, ON_PAUSED, ON_STOPPED, ON_SAVE_INSTANCE_STATE, ON_DESTROYED};

    public ApplicationContextWrapper(Application application) {
        mBaseApplication = application;
        attachBaseContext(mBaseApplication);
        mActivityLifecycleCallbackRecorder = new ActivityLifecycleCallbackRecorder();
        startRecordLifecycles();
    }

    public void setReplayActivityLifecycle(boolean replay) {
        this.mReplay = replay;
    }

    public boolean isReplayActivityLifecycle() {
        return mReplay;
    }

    public void setRecordActivityLifecycle(boolean record){
        if (this.mRecord = record) {
            startRecordLifecycles();
        } else {
            stopRecordLifecycles();
        }
    }

    public void setActivityLifecycleCallbackRecorder(ActivityLifecycleCallbackRecorder activityLifecycleCallbackRecorder) {
        mActivityLifecycleCallbackRecorder = activityLifecycleCallbackRecorder;
    }

    public boolean isRecordActivityLifecycle() {
        return mRecord;
    }

    @Override
    public void onCreate() {
        mBaseApplication.onCreate();
    }

    @Override
    public void onTerminate() {
        mBaseApplication.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mBaseApplication.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        mBaseApplication.onLowMemory();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        mBaseApplication.onTrimMemory(level);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        mBaseApplication.registerComponentCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        mBaseApplication.unregisterComponentCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        mBaseApplication.registerActivityLifecycleCallbacks(callback);
        if (callback != null && mActivityLifecycleCallbackRecorder != null && mReplay) {
            WeakReference<Activity> reference = MParticle.getInstance().getKitManager().getCurrentActivity();
            if (reference != null) {
                Activity currentActivity = reference.get();
                if (currentActivity != null) {
                    LinkedList<LifeCycleEvent> recordedLifecycleList = mActivityLifecycleCallbackRecorder.getRecordedLifecycleListCopy();
                    while(recordedLifecycleList.size() > 0) {
                        LifeCycleEvent lifeCycleEvent = recordedLifecycleList.removeFirst();
                        if (lifeCycleEvent.activityRef != null) {
                            Activity recordedActivity = lifeCycleEvent.activityRef.get();
                            if (recordedActivity != null) {
                                if (recordedActivity == currentActivity) {
                                    switch (lifeCycleEvent.methodType) {
                                        case ON_CREATED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnCreate");
                                            callback.onActivityCreated(recordedActivity, lifeCycleEvent.bundle);
                                            break;
                                        case ON_STARTED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnStart");
                                            callback.onActivityStarted(recordedActivity);
                                            break;
                                        case ON_RESUMED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnResume");
                                            callback.onActivityResumed(recordedActivity);
                                            break;
                                        case ON_PAUSED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnPause");
                                            callback.onActivityPaused(recordedActivity);
                                            break;
                                        case ON_SAVE_INSTANCE_STATE:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnSaveInstance");
                                            callback.onActivitySaveInstanceState(recordedActivity, lifeCycleEvent.bundle);
                                            break;
                                        case ON_STOPPED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnStop");
                                            callback.onActivityStopped(recordedActivity);
                                            break;
                                        case ON_DESTROYED:
                                            ConfigManager.log(MParticle.LogLevel.DEBUG,"Forward OnDestroy");
                                            callback.onActivityDestroyed(recordedActivity);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        mBaseApplication.unregisterActivityLifecycleCallbacks(callback);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        mBaseApplication.registerOnProvideAssistDataListener(callback);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        mBaseApplication.unregisterOnProvideAssistDataListener(callback);
    }

    @Override
    public int hashCode() {
        return mBaseApplication.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return mBaseApplication.equals(obj);
    }

    @Override
    public String toString() {
        return mBaseApplication.toString();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void startRecordLifecycles() {
        stopRecordLifecycles();
        mBaseApplication.registerActivityLifecycleCallbacks(mActivityLifecycleCallbackRecorder);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void stopRecordLifecycles() {
        mBaseApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbackRecorder);
    }

    public ActivityLifecycleCallbackRecorder getActivityLifecycleCallbackRecorderInstance() {
        return new ActivityLifecycleCallbackRecorder();
    }

    public LifeCycleEvent getLifeCycleEventInstance(MethodType methodType, WeakReference<Activity> activityRef) {
        return new LifeCycleEvent(methodType, activityRef);
    }

    public LifeCycleEvent getLifeCycleEventInstance(MethodType methodType, WeakReference<Activity> activityRef, Bundle bundle) {
        return new LifeCycleEvent(methodType, activityRef, bundle);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    class ActivityLifecycleCallbackRecorder implements ActivityLifecycleCallbacks {
        List<LifeCycleEvent> lifeCycleEvents = Collections.synchronizedList(new LinkedList<LifeCycleEvent>());
        int MAX_LIST_SIZE = 10;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_CREATED, new WeakReference<Activity>(activity), savedInstanceState));
        }

        @Override
        public void onActivityStarted(Activity activity) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_STARTED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityResumed(Activity activity) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_RESUMED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityPaused(Activity activity) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_PAUSED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityStopped(Activity activity) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_STOPPED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_SAVE_INSTANCE_STATE, new WeakReference<Activity>(activity), outState));
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            getRecordedLifecycleList().add(new LifeCycleEvent(MethodType.ON_DESTROYED, new WeakReference<Activity>(activity)));
        }

        private List<LifeCycleEvent> getRecordedLifecycleList() {
            if (lifeCycleEvents.size() > MAX_LIST_SIZE) {
                lifeCycleEvents.remove(0);
                return getRecordedLifecycleList();
            }
            return lifeCycleEvents;
        }

        private LinkedList<LifeCycleEvent> getRecordedLifecycleListCopy() {
            LinkedList<LifeCycleEvent> list;
            synchronized (lifeCycleEvents) {
                list = new LinkedList<LifeCycleEvent>(lifeCycleEvents);
            }
            return list;
        }
    }

    class LifeCycleEvent {
        private MethodType methodType;
        private WeakReference<Activity> activityRef;
        private Bundle bundle;

        public LifeCycleEvent(MethodType methodType, WeakReference<Activity> activityRef) {
            this(methodType, activityRef, null);
        }

        LifeCycleEvent(MethodType methodType, WeakReference<Activity> activityRef, Bundle bundle) {
            this.methodType = methodType;
            this.activityRef = activityRef;
            this.bundle = bundle;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof LifeCycleEvent) {
                LifeCycleEvent l = (LifeCycleEvent)o;
                boolean matchingActivityRef = false;
                if (l.activityRef == null && activityRef == null) {
                    matchingActivityRef = true;
                } else if (l.activityRef != null && activityRef != null) {
                    matchingActivityRef = l.activityRef.get() == activityRef.get();
                }
                return matchingActivityRef &&
                        l.methodType == methodType &&
                        l.bundle == bundle;
            }
            return false;
        }
    }
}
