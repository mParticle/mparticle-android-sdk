package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.AppStateManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class BaseAbstractTest {
    private static boolean beforeClassCalled = false;
    protected Context mContext;

    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

    }

    @Before
    public void beforeImpl() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        if (!beforeClassCalled) {
            beforeClassCalled = true;
            beforeClassBase();
            beforeClass();
        }
        mContext = InstrumentationRegistry.getContext();
        beforeBase();
        before();
    }

    @AfterClass
    public static void afterClassImpl() {
        beforeClassCalled = false;
    }

    protected abstract void beforeClassBase() throws Exception;
    protected abstract void beforeClass() throws Exception;
    protected abstract void beforeBase() throws Exception;
    protected abstract void before() throws Exception;

    Activity activity = new Activity();

    protected void goToBackground() {
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().getAppStateManager();
            //need to set AppStateManager's Handler to be on the main looper, otherwise, it will not put the app in the background
            com.mparticle.internal.AccessUtils.setAppStateManagerHandler(new Handler(Looper.getMainLooper()));
            if (appStateManager.isBackgrounded()) {
                appStateManager.onActivityResumed(activity);
            }
            appStateManager.onActivityPaused(activity);
        }
    }

    protected void goToForeground() {
        activity = new Activity();
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().getAppStateManager();
            appStateManager.onActivityResumed(activity);
        }
    }
}
