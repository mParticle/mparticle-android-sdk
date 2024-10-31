package com.mparticle.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.internal.ApplicationContextWrapper.ActivityLifecycleCallbackRecorder
import com.mparticle.internal.ApplicationContextWrapper.LifeCycleEvent
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.lang.ref.WeakReference
import java.util.LinkedList

/**
 * Created by wpassidomo on 2/21/17.
 */
/**
 * Test that if you add a bunch on mock activiies that it will play back in the same order.
 * Test that it won't crash if there aren't anuy activiies added.
 * Test that it will max out at 10.
 * Test that it will take weird, out of order combinations.
 * Test that it will ONLY replay activies that match current activity.
 * Test that if you call stopRecordLifecycles, you will not record anymore lifecycles.
 */
class ApplicationContextWrapperTest {
    private lateinit var instance: MParticle
    var activity1 = Mockito.mock(Activity::class.java)
    var activity2 = Mockito.mock(Activity::class.java)
    var activity3 = Mockito.mock(Activity::class.java)
    private lateinit var activityLifecycleCallbackRecorder: ActivityLifecycleCallbackRecorder
    var applicationContextWrapper = MockApplicationContextWrapper(
        Mockito.mock(
            Application::class.java
        )
    )
    var activity1Ref = WeakReference(activity1)
    var activity2Ref = WeakReference(activity2)
    var activity3Ref = WeakReference(activity3)
    var bundle1 = Mockito.mock(Bundle::class.java)
    var bundle2 = Mockito.mock(Bundle::class.java)

    inner class MockApplicationContextWrapper internal constructor(application: Application?) :
        ApplicationContextWrapper(application!!) {
        override fun attachBaseContext(base: Context) {}
    }

    @Before
    fun setup() {
        activityLifecycleCallbackRecorder =
            applicationContextWrapper.activityLifecycleCallbackRecorderInstance
        instance = MockMParticle()
        MParticle.setInstance(instance)
    }

    private lateinit var recorder: ActivityLifecycleCallbackRecorder
    private lateinit var tester: ActivityLifecycleCallbackRecordTester

    @Test
    fun testRegisterActivityLifecycleCallbacksActivity1() {
        Mockito.`when`(instance.Internal().kitManager.currentActivity).thenReturn(activity1Ref)
        recorder = mixedActivityCallbacks
        tester = ActivityLifecycleCallbackRecordTester()
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder)
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester, true)
        assertListEquals(tester.recordedLifecycleList, mixedTestListActivity1)
    }

    @Test
    fun testRegisterActivityLifecycleCallbacksActivity2() {
        Mockito.`when`(instance.Internal().kitManager.currentActivity).thenReturn(activity2Ref)
        recorder = mixedActivityCallbacks
        tester = ActivityLifecycleCallbackRecordTester()
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder)
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester, true)
        assertListEquals(tester.recordedLifecycleList, mixedTestListActivity2)
    }

    @Test
    fun testRegisterActivityLifecycleCallbacksActivity3() {
        Mockito.`when`(instance.Internal().kitManager.currentActivity).thenReturn(activity3Ref)
        recorder = mixedActivityCallbacks
        tester = ActivityLifecycleCallbackRecordTester()
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder)
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester, true)
        assertListEquals(tester.recordedLifecycleList, LinkedList<Any?>())
    }

    @Test
    fun testRegisterActivityLifecycleCallbacksEmpty() {
        Mockito.`when`(instance.Internal().kitManager.currentActivity).thenReturn(activity1Ref)
        recorder = emptyActivityCallbacks
        tester = ActivityLifecycleCallbackRecordTester()
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder)
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester, true)
        assertListEquals(tester.recordedLifecycleList, LinkedList<Any?>())
    }

    @Test
    fun testStopReplaying() {
        Mockito.`when`(instance.Internal().kitManager.currentActivity).thenReturn(activity1Ref)
        recorder = mixedActivityCallbacks
        tester = ActivityLifecycleCallbackRecordTester()
        applicationContextWrapper.isReplayActivityLifecycle = false
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder)
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester, true)
        assertListEquals(tester.recordedLifecycleList, LinkedList<Any?>())
        Assert.assertTrue(recorder.lifeCycleEvents.size > 0)
    }

    private val emptyActivityCallbacks: ActivityLifecycleCallbackRecorder
        get() = applicationContextWrapper.activityLifecycleCallbackRecorderInstance
    private val mixedActivityCallbacks: ActivityLifecycleCallbackRecorder
        get() {
            activityLifecycleCallbackRecorder =
                applicationContextWrapper.activityLifecycleCallbackRecorderInstance
            activityLifecycleCallbackRecorder.onActivityCreated(activity1, bundle1)
            activityLifecycleCallbackRecorder.onActivityStarted(activity1)
            activityLifecycleCallbackRecorder.onActivityResumed(activity1)
            activityLifecycleCallbackRecorder.onActivityPaused(activity1)
            activityLifecycleCallbackRecorder.onActivityCreated(activity2, bundle2)
            activityLifecycleCallbackRecorder.onActivitySaveInstanceState(activity1, bundle1)
            activityLifecycleCallbackRecorder.onActivityStopped(activity1)
            activityLifecycleCallbackRecorder.onActivityStarted(activity2)
            activityLifecycleCallbackRecorder.onActivityResumed(activity2)
            activityLifecycleCallbackRecorder.onActivityDestroyed(activity1)
            return activityLifecycleCallbackRecorder
        }
    private val mixedTestListActivity2: LinkedList<LifeCycleEvent>
        get() {
            val testList = LinkedList<LifeCycleEvent>()
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_CREATED,
                    WeakReference(activity2),
                    bundle2
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_STARTED,
                    WeakReference(activity2)
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_RESUMED,
                    WeakReference(activity2)
                )
            )
            return testList
        }
    private val mixedTestListActivity1: LinkedList<LifeCycleEvent>
        get() {
            val testList = LinkedList<LifeCycleEvent>()
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_CREATED,
                    WeakReference(activity1),
                    bundle1
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_STARTED,
                    WeakReference(activity1)
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_RESUMED,
                    WeakReference(activity1)
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_PAUSED,
                    WeakReference(activity1)
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_SAVE_INSTANCE_STATE,
                    WeakReference(activity1),
                    bundle1
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_STOPPED,
                    WeakReference(activity1)
                )
            )
            testList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_DESTROYED,
                    WeakReference(activity1)
                )
            )
            return testList
        }

    private fun assertListEquals(list1: List<Any?>, list2: List<Any?>) {
        Assert.assertEquals(list1.size.toLong(), list2.size.toLong())
        for (i in list1.indices) {
            Assert.assertTrue(list1[i] == list2[i])
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    internal inner class ActivityLifecycleCallbackRecordTester : ActivityLifecycleCallbacks {
        var lifeCycleEvents = LinkedList<LifeCycleEvent>()
        var MAX_LIST_SIZE = 10
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_CREATED,
                    WeakReference(activity),
                    savedInstanceState
                )
            )
        }

        override fun onActivityStarted(activity: Activity) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_STARTED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivityResumed(activity: Activity) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_RESUMED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivityPaused(activity: Activity) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_PAUSED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivityStopped(activity: Activity) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_STOPPED,
                    WeakReference(activity)
                )
            )
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_SAVE_INSTANCE_STATE,
                    WeakReference(activity),
                    outState
                )
            )
        }

        override fun onActivityDestroyed(activity: Activity) {
            recordedLifecycleList.addLast(
                applicationContextWrapper.getLifeCycleEventInstance(
                    ApplicationContextWrapper.MethodType.ON_DESTROYED,
                    WeakReference(activity)
                )
            )
        }

        val recordedLifecycleList: LinkedList<LifeCycleEvent>
            get() {
                if (lifeCycleEvents.size > MAX_LIST_SIZE) {
                    lifeCycleEvents.removeFirst()
                    return recordedLifecycleList
                }
                return lifeCycleEvents
            }
    }
}
