package com.mparticle.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.internal.ApplicationContextWrapper.ActivityLifecycleCallbackRecorder;
import com.mparticle.internal.ApplicationContextWrapper.LifeCycleEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by wpassidomo on 2/21/17.
 */

/**
 *  Test that if you add a bunch on mock activiies that it will play back in the same order.
 *  Test that it won't crash if there aren't anuy activiies added.
 *  Test that it will max out at 10.
 *  Test that it will take wierd, out of order combinations.
 *  Test that it will ONLY replay activies that match current activity.
 *  Test that if you call stopRecordLifecycles, you will not record anymore lifecycles.
 */

public class ApplicationContextWrapperTest {
    MParticle instance;
    Activity activity1 = mock(Activity.class);
    Activity activity2 = mock(Activity.class);
    Activity activity3 = mock(Activity.class);
    ActivityLifecycleCallbackRecorder activityLifecycleCallbackRecorder;
    MockApplicationContextWrapper applicationContextWrapper = new MockApplicationContextWrapper(mock(Application.class));
    WeakReference<Activity> activity1Ref = new WeakReference<Activity>(activity1);
    WeakReference<Activity> activity2Ref = new WeakReference<Activity>(activity2);
    WeakReference<Activity> activity3Ref = new WeakReference<Activity>(activity3);

    Bundle bundle1 = mock(Bundle.class);
    Bundle bundle2 = mock(Bundle.class);

    public class MockApplicationContextWrapper extends ApplicationContextWrapper {
        MockApplicationContextWrapper(Application application) {
            super(application);
        }

        @Override
        protected void attachBaseContext(Context base) {}
    }

    @Before
    public void setup() {
        activityLifecycleCallbackRecorder = applicationContextWrapper.getActivityLifecycleCallbackRecorderInstance();
        instance = new MockMParticle();
        MParticle.setInstance(instance);
    }

    private ActivityLifecycleCallbackRecorder recorder;
    private ActivityLifecycleCallbackRecordTester tester;

    @Test
    public void testRegisterActivityLifecycleCallbacksActivity1() {
        Mockito.when(instance.Internal().getKitManager().getCurrentActivity()).thenReturn(activity1Ref);
        recorder = getMixedActivityCallbacks();
        tester = new ActivityLifecycleCallbackRecordTester();
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder);
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester);
        assertListEquals(tester.getRecordedLifecycleList(), getMixedTestListActivity1());
    }

    @Test
    public void testRegisterActivityLifecycleCallbacksActivity2() {
        Mockito.when(instance.Internal().getKitManager().getCurrentActivity()).thenReturn(activity2Ref);
        recorder = getMixedActivityCallbacks();
        tester = new ActivityLifecycleCallbackRecordTester();
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder);
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester);
        assertListEquals(tester.getRecordedLifecycleList(), getMixedTestListActivity2());
    }

    @Test
    public void testRegisterActivityLifecycleCallbacksActivity3() {
        Mockito.when(instance.Internal().getKitManager().getCurrentActivity()).thenReturn(activity3Ref);
        recorder = getMixedActivityCallbacks();
        tester = new ActivityLifecycleCallbackRecordTester();
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder);
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester);
        assertListEquals(tester.getRecordedLifecycleList(), new LinkedList());
    }

    @Test
    public void testRegisterActivityLifecycleCallbacksEmpty() {
        Mockito.when(instance.Internal().getKitManager().getCurrentActivity()).thenReturn(activity1Ref);
        recorder = getEmptyActivityCallbacks();
        tester = new ActivityLifecycleCallbackRecordTester();
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder);
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester);
        assertListEquals(tester.getRecordedLifecycleList(), new LinkedList());
    }

    @Test
    public void testStopReplaying() {
        Mockito.when(instance.Internal().getKitManager().getCurrentActivity()).thenReturn(activity1Ref);
        recorder = getMixedActivityCallbacks();
        tester = new ActivityLifecycleCallbackRecordTester();
        applicationContextWrapper.setReplayActivityLifecycle(false);
        applicationContextWrapper.setActivityLifecycleCallbackRecorder(recorder);
        applicationContextWrapper.registerActivityLifecycleCallbacks(tester);
        assertListEquals(tester.getRecordedLifecycleList(), new LinkedList());
        assertTrue(recorder.lifeCycleEvents.size() > 0);
    }

    private ActivityLifecycleCallbackRecorder getEmptyActivityCallbacks() {
        return applicationContextWrapper.getActivityLifecycleCallbackRecorderInstance();
    }

    private ActivityLifecycleCallbackRecorder getMixedActivityCallbacks() {
        activityLifecycleCallbackRecorder = applicationContextWrapper.getActivityLifecycleCallbackRecorderInstance();
        activityLifecycleCallbackRecorder.onActivityCreated(activity1, bundle1);
        activityLifecycleCallbackRecorder.onActivityStarted(activity1);
        activityLifecycleCallbackRecorder.onActivityResumed(activity1);
        activityLifecycleCallbackRecorder.onActivityPaused(activity1);
        activityLifecycleCallbackRecorder.onActivityCreated(activity2, bundle2);
        activityLifecycleCallbackRecorder.onActivitySaveInstanceState(activity1, null);
        activityLifecycleCallbackRecorder.onActivityStopped(activity1);
        activityLifecycleCallbackRecorder.onActivityStarted(activity2);
        activityLifecycleCallbackRecorder.onActivityResumed(activity2);
        activityLifecycleCallbackRecorder.onActivityDestroyed(activity1);
        return activityLifecycleCallbackRecorder;
    }

    private LinkedList<LifeCycleEvent> getMixedTestListActivity2() {
        LinkedList<LifeCycleEvent> testList = new LinkedList<LifeCycleEvent>();
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_CREATED, new WeakReference<Activity>(activity2), bundle2));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_STARTED, new WeakReference<Activity>(activity2)));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_RESUMED, new WeakReference<Activity>(activity2)));
        return testList;
    }

    private LinkedList<LifeCycleEvent> getMixedTestListActivity1() {
        LinkedList<LifeCycleEvent> testList = new LinkedList<LifeCycleEvent>();
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_CREATED, new WeakReference<Activity>(activity1), bundle1));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_STARTED, new WeakReference<Activity>(activity1)));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_RESUMED, new WeakReference<Activity>(activity1)));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_PAUSED, new WeakReference<Activity>(activity1)));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_SAVE_INSTANCE_STATE,new WeakReference<Activity>(activity1), null));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_STOPPED, new WeakReference<Activity>(activity1)));
        testList.addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_DESTROYED, new WeakReference<Activity>(activity1)));
        return testList;
    }

    private void assertListEquals(List list1, List list2) {
        assertEquals(list1.size(), list2.size());
        for (int i = 0; i < list1.size(); i++) {
            assertTrue(list1.get(i).equals(list2.get(i)));
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    class ActivityLifecycleCallbackRecordTester implements Application.ActivityLifecycleCallbacks {
        LinkedList<LifeCycleEvent> lifeCycleEvents = new LinkedList<LifeCycleEvent>();
        int MAX_LIST_SIZE = 10;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_CREATED, new WeakReference<Activity>(activity), savedInstanceState));
        }

        @Override
        public void onActivityStarted(Activity activity) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_STARTED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityResumed(Activity activity) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_RESUMED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityPaused(Activity activity) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_PAUSED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivityStopped(Activity activity) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_STOPPED, new WeakReference<Activity>(activity)));
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_SAVE_INSTANCE_STATE, new WeakReference<Activity>(activity), outState));
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            getRecordedLifecycleList().addLast(applicationContextWrapper.getLifeCycleEventInstance(ApplicationContextWrapper.MethodType.ON_DESTROYED, new WeakReference<Activity>(activity)));
        }

        private LinkedList<LifeCycleEvent> getRecordedLifecycleList() {
            if (lifeCycleEvents.size() > MAX_LIST_SIZE) {
                lifeCycleEvents.removeFirst();
                return getRecordedLifecycleList();
            }
            return lifeCycleEvents;
        }
    }
}



