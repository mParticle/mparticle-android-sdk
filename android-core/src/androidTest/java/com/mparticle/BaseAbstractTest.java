package com.mparticle;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.github.tomakehurst.wiremock.common.Notifier;
import com.mparticle.commerce.Product;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mparticle.utils.Server;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class BaseAbstractTest {
    protected static Server mServer;
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
        if (mServer == null) {
            mServer = new Server();
        } else {
            mServer.reset();
        }
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
        mServer.stop();
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
