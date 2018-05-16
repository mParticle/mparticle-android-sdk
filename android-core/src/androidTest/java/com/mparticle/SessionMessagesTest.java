package com.mparticle;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.internal.Session;
import com.mparticle.testutils.AssertObject;
import com.mparticle.testutils.AssertTrue;
import com.mparticle.testutils.MParticleUtils;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.StreamAssert;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.mparticle.testutils.AndroidUtils.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


public final class SessionMessagesTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;
    Handler mHandler;
    RandomUtils mRandom;

    @Before
    public void before() throws Exception {
        mAppStateManager = MParticle.getInstance().getAppStateManager();
        mHandler = new Handler(Looper.getMainLooper());
        mRandom = RandomUtils.getInstance();
    }

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
}
