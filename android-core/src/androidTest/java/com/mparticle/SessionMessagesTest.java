package com.mparticle;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;


public final class SessionMessagesTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;
    Handler mHandler;

    @Before
    public void before() throws Exception {
        mAppStateManager = MParticle.getInstance().Internal().getAppStateManager();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Test
    public void testSessionStartMessage() throws Exception {
        final boolean[] sessionStartReceived = new boolean[1];
        sessionStartReceived[0] = false;
        assertFalse(mAppStateManager.getSession().isActive());
        final CountDownLatch latch = new MPLatch(1);
        AccessUtils.setMParticleApiClient(new AccessUtils.EmptyMParticleApiClient() {
            @Override
            public int sendMessageBatch(final String message) {
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
                                    latch.countDown();
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

        AccessUtils.awaitMessageHandler();
        MParticle.getInstance().upload();
        latch.await();
        Assert.assertTrue(sessionStartReceived[0]);
    }
}
