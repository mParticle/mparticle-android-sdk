package com.mparticle.testutils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;

import com.mparticle.testutils.Server;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

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
        if (mServer == null) {
            mServer = new Server();
        } else {
            mServer.reset();
        }
        mContext = InstrumentationRegistry.getContext();
    }

    @After
    public void tearDown() {
        mServer.stop();
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
