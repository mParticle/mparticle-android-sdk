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
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils.Mutable;

import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class BaseAbstractTest {
    protected MockServer mServer;
    Activity activity = new Activity();
    protected Context mContext;
    protected Random ran = new Random();
    protected RandomUtils mRandomUtils = new RandomUtils();
    protected static Long mStartingMpid;


    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void beforeImpl() throws Exception {
        Logger.setLogHandler(null);
        mContext = InstrumentationRegistry.getContext();
        mStartingMpid = new Random().nextLong();
        if (autoStartServer()) {
            mServer = MockServer.getNewInstance(mContext);
        }
    }

    protected void startMParticle() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext));
    }

    protected void startMParticle(MParticleOptions.Builder options) throws InterruptedException {
        MParticle.setInstance(null);
        final CountDownLatch latch = new MPLatch(1);
        BaseIdentityTask identityTask = com.mparticle.AccessUtils.getIdentityTask(options);
        final Mutable<Boolean> called = new Mutable<>(false);
        if (identityTask == null) {
            identityTask = new BaseIdentityTask();
        }
        identityTask.addFailureListener(new TaskFailureListener() {
            @Override
            public void onFailure(IdentityHttpResponse result) {
                fail(result.toString());
            }
        }).addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult result) {
                called.value = true;
                latch.countDown();
            }
        });

        options.identifyTask(identityTask);
        if (mServer == null) {
            mServer = MockServer.getNewInstance(mContext);
        }
        MParticle.start(com.mparticle.AccessUtils.setCredentialsIfEmpty(options).build());
        mServer.setupHappyIdentify(mStartingMpid);
        latch.await();
        assertTrue(called.value);
    }

    protected void goToBackground() {
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().Internal().getAppStateManager();
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
            AppStateManager appStateManager = MParticle.getInstance().Internal().getAppStateManager();
            appStateManager.onActivityResumed(activity);
        }
    }

    protected boolean autoStartServer() {
        return true;
    }
}
