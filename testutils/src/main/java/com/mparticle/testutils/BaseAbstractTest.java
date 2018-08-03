package com.mparticle.testutils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.mparticle.testutils.MPLatch;

import java.util.concurrent.CountDownLatch;

public abstract class BaseAbstractTest {
    protected Server mServer;
    Activity activity = new Activity();
    protected Context mContext;

    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void beforeImpl() throws Exception {
        Logger.setLogHandler(null);
        if (mServer == null) {
            mServer = new Server();
        } else {
            mServer.reset();
        }
        mContext = InstrumentationRegistry.getContext();
    }

    @After
    public void tearDown() {
        if (mServer != null) {
            mServer.stop();
        }
        MParticle.getInstance().reset(mContext);
    }

    protected void startMParticle() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext));
    }

    protected void startMParticle(MParticleOptions.Builder options) throws InterruptedException {
        MParticle.setInstance(null);
        final CountDownLatch latch = new MPLatch(1);
        BaseIdentityTask identityTask = com.mparticle.AccessUtils.getIdentityTask(options);
        if (identityTask == null) {
            identityTask = new BaseIdentityTask();
        }
        identityTask.addFailureListener(new TaskFailureListener() {
            @Override
            public void onFailure(IdentityHttpResponse result) {
                latch.countDown();
            }
        }).addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult result) {
                latch.countDown();
            }
        });

        options.identifyTask(identityTask);
        MParticle.start(com.mparticle.AccessUtils.setCredentialsIfEmpty(options).build());
        latch.await();
    }

    protected void goToBackground() {
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().getAppStateManager();
            //need to set AppStateManager's Handler to be on the main looper, otherwise, it will not put the app in the background
            AccessUtils.setAppStateManagerHandler(new Handler(Looper.getMainLooper()));
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
