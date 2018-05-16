package com.mparticle;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.AssertTrue;
import com.mparticle.testutils.MParticleUtils;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.StreamAssert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public final class AppStateTransMessagesTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;
    Handler mHandler;
    RandomUtils mRandom;

    @Before
    public void before() throws Exception {
        mAppStateManager = MParticle.getInstance().getAppStateManager();
        mHandler = new Handler(Looper.getMainLooper());
        mRandom = RandomUtils.getInstance();
        AppStateManager.mInitialized = false;
    }

    @Test
    public void testApplicationForegroundMessage() throws Exception {
        AndroidUtils.Mutable<Long> initTime = new AndroidUtils.Mutable(0);
        AndroidUtils.Mutable<Long> backgroundTime = new AndroidUtils.Mutable(0);
        AndroidUtils.Mutable<Long> foregroundTime = new AndroidUtils.Mutable<Long>(0L);

        final StreamAssert<JSONObject> expected = StreamAssert
                .first(new AssertTrue<JSONObject>() {
            @Override
            public boolean assertTrueI(JSONObject object) throws JSONException {
                return object.getString("dt").equals(Constants.MessageType.APP_STATE_TRANSITION) &&
                        object.getString("t").equals("app_init");
                    }
                })
                .then(new AssertTrue<JSONObject>() {
                    @Override
                    public boolean assertTrueI(JSONObject object) throws JSONException {
                        return object.getString("dt").equals(Constants.MessageType.APP_STATE_TRANSITION) &&
                                object.getString("t").equals("app_back");
                    }
                });

        AccessUtils.setMParticleApiClient(new AccessUtils.EmptyMParticleApiClient(){

            @Override
            public int sendMessageBatch(final String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject eventObject = jsonArray.getJSONObject(i);
                        if (eventObject.getString("dt").equals(Constants.MessageType.APP_STATE_TRANSITION)) {
                            expected.collect(eventObject);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                return 202;
            }
        });

        goToForeground();
        initTime.value = System.currentTimeMillis();
        Thread.sleep(mRandom.randomLong(100,500));
        goToBackground();
        backgroundTime.value = System.currentTimeMillis();
        Thread.sleep(mRandom.randomLong(2000, 4000));
        goToForeground();
        foregroundTime.value = System.currentTimeMillis();
        MParticle.getInstance().getAppStateManager().ensureActiveSession();
        Thread.sleep(mRandom.randomLong(2000, 5000));
        MParticleUtils.awaitStoreMessage();
        Thread.sleep(500);
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        expected.startTimer(4000, true);
    }
}
