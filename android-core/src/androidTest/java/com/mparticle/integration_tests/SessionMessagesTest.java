package com.mparticle.integration_tests;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.internal.Session;
import com.mparticle.utils.AssertObject;
import com.mparticle.utils.AssertTrue;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.RandomUtils;
import com.mparticle.utils.StreamAssert;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

import static com.mparticle.utils.AndroidUtils.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


public class SessionMessagesTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;
    Handler mHandler;
    RandomUtils mRandom;

    @Test
    public void testSessionStartMessage() throws Exception {
        final boolean[] sessionStartReceived = new boolean[1];
        sessionStartReceived[0] = false;
        assertFalse(mAppStateManager.getSession().isActive());

        AccessUtils.setMParticleApiClient(new AccessUtils.EmptyMParticleApiClient() {
            @Override
            public int sendMessageBatch(final String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                            if (jsonArray == null) {
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject eventObject = jsonArray.getJSONObject(i);
                                if (eventObject.getString("dt").equals(Constants.MessageType.SESSION_START)) {
                                    assertEquals(eventObject.getLong("ct"), mAppStateManager.getSession().mSessionStartTime, 1000);
                                    assertEquals(eventObject.getString("id"), mAppStateManager.getSession().mSessionID);
                                    sessionStartReceived[0] = true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.getMessage());
                        }
                    }

                });
                return 202;
            }
        });

        mAppStateManager.ensureActiveSession();

        MParticleUtils.awaitStoreMessage();
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        if (!sessionStartReceived[0]) {
            Thread.sleep(1000);
        }
        Assert.assertTrue(sessionStartReceived[0]);
    }

    @Test
    public void testSessionEndReceived() throws Exception {
        final Mutable<Session> sessionId = new Mutable<Session>(null);

        final StreamAssert<JSONObject> streamAssert = StreamAssert
                .first(new AssertTrue<JSONObject>() {
                    @Override
                    public boolean assertTrueI(JSONObject object) throws JSONException {
                        return object.getString("dt").equals(Constants.MessageType.SESSION_START);
                            }
                        })
                .then(new AssertObject<JSONObject>() {
                    @Override
                    public void assertObject(JSONObject eventObject) {
                        assertEquals(eventObject.optString("dt"), Constants.MessageType.SESSION_END);
                        assertEquals(eventObject.optString("sid"), sessionId.value.mSessionID);
                        assertFalse(eventObject.optString("id").equals(sessionId.value.mSessionID));
                        assertEquals(eventObject.opt("sct"), sessionId.value.mSessionStartTime);
                        assertEquals(eventObject.opt("ct"), sessionId.value.mLastEventTime);
                        assertEquals(eventObject.opt("en"), 0);
                        assertEquals(eventObject.optLong("slx"), sessionId.value.mLastEventTime - sessionId.value.mSessionStartTime);
                        assertEquals(eventObject.optLong("sl"), eventObject.optLong("slx") - eventObject.optLong("en"));
                    }
                });

        assertFalse(mAppStateManager.getSession().isActive());

        AccessUtils.setMParticleApiClient(new AccessUtils.EmptyMParticleApiClient() {
            @Override
            public int sendMessageBatch(final String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try

                        {
                            JSONObject jsonObject = new JSONObject(message);
                            JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                            if (jsonArray == null) {
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject eventObject = jsonArray.getJSONObject(i);
                                streamAssert.collect(eventObject);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.getMessage());
                        }
                    }

                });
                return 202;
            }
        });

        mAppStateManager.ensureActiveSession();
        Thread.sleep(1000);
        sessionId.value = mAppStateManager.getSession();

        MParticleUtils.awaitStoreMessage();
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        Thread.sleep(1000);
        mAppStateManager.endSession();
        MParticleUtils.awaitStoreMessage();
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        streamAssert.startTimer(3000, true);
    }

    @Test
    public void testSessionBackgroundTimeRecorded() throws Exception {
        final Mutable<Session> sessionId = new Mutable<Session>(null);
        final Mutable<Long> actualBackgroundTime = new Mutable<Long>(0L);

        final StreamAssert<JSONObject> streamAssert = StreamAssert
                .first(new AssertTrue<JSONObject>() {
                    @Override
                    public boolean assertTrueI(JSONObject object) throws JSONException {
                        return object.getString("dt").equals(Constants.MessageType.SESSION_START);
                    }
                })
                .then(new AssertObject<JSONObject>() {
                    @Override
                    public void assertObject(JSONObject eventObject) {
                        assertTrue(eventObject.optString("dt").equals(Constants.MessageType.SESSION_END));
                            assertEquals(eventObject.optLong("slx"), sessionId.value.mLastEventTime - sessionId.value.mSessionStartTime);
                         assertEquals(eventObject.optLong("sl"), eventObject.optLong("slx") - actualBackgroundTime.value, 200);
                    }
                });

        assertFalse(mAppStateManager.getSession().isActive());

        AccessUtils.setMParticleApiClient(new AccessUtils.EmptyMParticleApiClient() {
            @Override
            public int sendMessageBatch(final String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                            if (jsonArray == null) {
                                return;
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject eventObject = jsonArray.getJSONObject(i);
                                streamAssert.collect(eventObject);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.getMessage());
                        }
                    }

                });
                return 202;
            }
        });
        Thread.sleep(2000);
        MParticle.getInstance().getAppStateManager().ensureActiveSession();
        goToForeground();
        sessionId.value = mAppStateManager.getSession();

        MParticleUtils.awaitStoreMessage();
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        Thread.sleep(mRandom.randomLong(2000, 5000));
        goToBackground();
        Long time = System.currentTimeMillis();
        Thread.sleep(mRandom.randomLong(2000, 5000));
        actualBackgroundTime.value = System.currentTimeMillis() - time;
        goToForeground();
        mAppStateManager.endSession();
        MParticleUtils.awaitStoreMessage();
        MParticle.getInstance().upload();
        MParticleUtils.awaitUploadMessages(MParticle.getInstance().getConfigManager().getMpid());
        streamAssert.startTimer(2000, true);
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {
        mAppStateManager = MParticle.getInstance().getAppStateManager();
        mHandler = new Handler(Looper.getMainLooper());
        mRandom = RandomUtils.getInstance();
    }
}
