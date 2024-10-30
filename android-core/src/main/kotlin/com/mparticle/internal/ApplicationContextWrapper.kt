package com.mparticle.internal

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedList

open class ApplicationContextWrapper(private val mBaseApplication: Application) : Application() {
    var isReplayActivityLifecycle: Boolean = true
    private var mRecord = true
    private var mActivityLifecycleCallbackRecorder: ActivityLifecycleCallbackRecorder?

    enum class MethodType {
        ON_CREATED, ON_STARTED, ON_RESUMED, ON_PAUSED, ON_STOPPED, ON_SAVE_INSTANCE_STATE, ON_DESTROYED
    }

    init {
        attachBaseContext(mBaseApplication)
        mActivityLifecycleCallbackRecorder = ActivityLifecycleCallbackRecorder()
        startRecordLifecycles()
    }

    fun setActivityLifecycleCallbackRecorder(activityLifecycleCallbackRecorder: ActivityLifecycleCallbackRecorder?) {
        mActivityLifecycleCallbackRecorder = activityLifecycleCallbackRecorder
    }

    var isRecordActivityLifecycle: Boolean
        get() = mRecord
        set(record) {
            if (record.also { this.mRecord = it }) {
                startRecordLifecycles()
            } else {
                stopRecordLifecycles()
            }
        }

    @SuppressLint("MissingSuperCall")
    override fun onCreate() {
        mBaseApplication.onCreate()
    }

    @SuppressLint("MissingSuperCall")
    override fun onTerminate() {
        mBaseApplication.onTerminate()
    }

    @SuppressLint("MissingSuperCall")
    override fun onConfigurationChanged(newConfig: Configuration) {
        mBaseApplication.onConfigurationChanged(newConfig)
    }

    @SuppressLint("MissingSuperCall")
    override fun onLowMemory() {
        mBaseApplication.onLowMemory()
    }

    @SuppressLint("MissingSuperCall")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onTrimMemory(level: Int) {
        mBaseApplication.onTrimMemory(level)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun registerComponentCallbacks(callback: ComponentCallbacks) {
        mBaseApplication.registerComponentCallbacks(callback)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun unregisterComponentCallbacks(callback: ComponentCallbacks) {
        mBaseApplication.unregisterComponentCallbacks(callback)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        registerActivityLifecycleCallbacks(callback, false)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun registerActivityLifecycleCallbacks(
        callback: ActivityLifecycleCallbacks,
        unitTesting: Boolean
    ) {
        mBaseApplication.registerActivityLifecycleCallbacks(callback)
        val runnable = ReplayLifecycleCallbacksRunnable(callback)
        if (unitTesting) {
            runnable.run()
        } else {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            Handler().post(runnable)
        }
    }

    override fun getApplicationContext(): Context {
        return this
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
        mBaseApplication.unregisterActivityLifecycleCallbacks(callback)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun registerOnProvideAssistDataListener(callback: OnProvideAssistDataListener) {
        mBaseApplication.registerOnProvideAssistDataListener(callback)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun unregisterOnProvideAssistDataListener(callback: OnProvideAssistDataListener) {
        mBaseApplication.unregisterOnProvideAssistDataListener(callback)
    }

    override fun hashCode(): Int {
        return mBaseApplication.hashCode()
    }

    override fun equals(obj: Any?): Boolean {
        return mBaseApplication == obj
    }

    override fun toString(): String {
        return mBaseApplication.toString()
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private fun startRecordLifecycles() {
        stopRecordLifecycles()
        mBaseApplication.registerActivityLifecycleCallbacks(mActivityLifecycleCallbackRecorder)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun stopRecordLifecycles() {
        mBaseApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbackRecorder)
    }

    val activityLifecycleCallbackRecorderInstance: ActivityLifecycleCallbackRecorder
        get() = ActivityLifecycleCallbackRecorder()

    fun getLifeCycleEventInstance(
        methodType: MethodType,
        activityRef: WeakReference<Activity>?
    ): LifeCycleEvent {
        return LifeCycleEvent(methodType, activityRef)
    }

    fun getLifeCycleEventInstance(
        methodType: MethodType,
        activityRef: WeakReference<Activity>?,
        bundle: Bundle?
    ): LifeCycleEvent {
        return LifeCycleEvent(methodType, activityRef, bundle)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    inner class ActivityLifecycleCallbackRecorder : ActivityLifecycleCallbacks {
        var lifeCycleEvents: MutableList<LifeCycleEvent> =
            Collections.synchronizedList(LinkedList())
        val MAX_LIST_SIZE: Int = 10

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_CREATED,
                    WeakReference(activity),
                    savedInstanceState
                )
            )
        }

        override fun onActivityStarted(activity: Activity) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_STARTED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivityResumed(activity: Activity) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_RESUMED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivityPaused(activity: Activity) {
            recordedLifecycleList.add(LifeCycleEvent(MethodType.ON_PAUSED, WeakReference(activity)))
        }

        override fun onActivityStopped(activity: Activity) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_STOPPED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_SAVE_INSTANCE_STATE,
                    WeakReference(activity),
                    outState
                )
            )
        }

        override fun onActivityDestroyed(activity: Activity) {
            recordedLifecycleList.add(
                LifeCycleEvent(
                    MethodType.ON_DESTROYED,
                    WeakReference(activity)
                )
            )
        }

        private val recordedLifecycleList: MutableList<LifeCycleEvent>
            get() {
                if (lifeCycleEvents.size > MAX_LIST_SIZE) {
                    lifeCycleEvents.removeAt(0)
                    return recordedLifecycleList
                }
                return lifeCycleEvents
            }

        internal val recordedLifecycleListCopy: LinkedList<LifeCycleEvent>
            get() {
                var list: LinkedList<LifeCycleEvent>
                synchronized(lifeCycleEvents) {
                    list = LinkedList(lifeCycleEvents)
                }
                return list
            }
    }

    inner class LifeCycleEvent(
        val methodType: MethodType,
        val activityRef: WeakReference<Activity>?,
        val bundle: Bundle?
    ) {
        constructor(
            methodType: MethodType,
            activityRef: WeakReference<Activity>?
        ) : this(methodType, activityRef, null)

        override fun equals(o: Any?): Boolean {
            if (o is LifeCycleEvent) {
                val l = o
                var matchingActivityRef = false
                if (l.activityRef == null && activityRef == null) {
                    matchingActivityRef = true
                } else if (l.activityRef != null && activityRef != null) {
                    matchingActivityRef = l.activityRef.get() === activityRef.get()
                }
                return matchingActivityRef && l.methodType == methodType && l.bundle == bundle
            }
            return false
        }
    }

    internal inner class ReplayLifecycleCallbacksRunnable(var callback: ActivityLifecycleCallbacks) :
        Runnable {
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        override fun run() {
            if (callback != null && mActivityLifecycleCallbackRecorder != null && isReplayActivityLifecycle) {
                val reference = if (MParticle.getInstance()?.Internal()?.kitManager == null
                ) {
                    null
                } else {
                    MParticle.getInstance()?.Internal()?.kitManager?.currentActivity
                }
                if (reference != null) {
                    val currentActivity = reference.get()
                    if (currentActivity != null) {
                        val recordedLifecycleList: LinkedList<LifeCycleEvent> =
                            mActivityLifecycleCallbackRecorder?.recordedLifecycleListCopy
                                ?: LinkedList()
                        while (recordedLifecycleList.size > 0) {
                            val lifeCycleEvent = recordedLifecycleList.removeFirst()
                            if (lifeCycleEvent.activityRef != null) {
                                val recordedActivity = lifeCycleEvent.activityRef.get()
                                if (recordedActivity != null) {
                                    if (recordedActivity === currentActivity) {
                                        when (lifeCycleEvent.methodType) {
                                            MethodType.ON_CREATED -> {
                                                Logger.debug("Forwarding OnCreate")
                                                callback.onActivityCreated(
                                                    recordedActivity,
                                                    lifeCycleEvent.bundle
                                                )
                                            }

                                            MethodType.ON_STARTED -> {
                                                Logger.debug("Forwarding OnStart")
                                                callback.onActivityStarted(recordedActivity)
                                            }

                                            MethodType.ON_RESUMED -> {
                                                Logger.debug("Forwarding OnResume")
                                                callback.onActivityResumed(recordedActivity)
                                            }

                                            MethodType.ON_PAUSED -> {
                                                Logger.debug("Forwarding OnPause")
                                                callback.onActivityPaused(recordedActivity)
                                            }

                                            MethodType.ON_SAVE_INSTANCE_STATE -> {
                                                Logger.debug("Forwarding OnSaveInstance")
                                                lifeCycleEvent.bundle?.let {
                                                    callback.onActivitySaveInstanceState(
                                                        recordedActivity,
                                                        it
                                                    )
                                                }
                                            }

                                            MethodType.ON_STOPPED -> {
                                                Logger.debug("Forwarding OnStop")
                                                callback.onActivityStopped(recordedActivity)
                                            }

                                            MethodType.ON_DESTROYED -> {
                                                Logger.debug("Forwarding OnDestroy")
                                                callback.onActivityDestroyed(recordedActivity)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
