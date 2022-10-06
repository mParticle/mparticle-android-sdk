package com.mparticle;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.services.MessageService;
import com.mparticle.internal.database.services.SessionService;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static junit.framework.Assert.fail;


public final class SessionMessagesTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;
    Handler mHandler;

    @Before
    public void before() {
        mAppStateManager = MParticle.getInstance().Internal().getAppStateManager();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Test
    public void testSessionStartMessage() throws Exception {
        final boolean[] sessionStartReceived = new boolean[1];
        sessionStartReceived[0] = false;
        assertFalse(mAppStateManager.getSession().isActive());
        final CountDownLatch latch = new MPLatch(1);
        final AndroidUtils.Mutable<String> sessionId = new AndroidUtils.Mutable<String>(null);
        mAppStateManager.ensureActiveSession();
        sessionId.value = mAppStateManager.getSession().mSessionID;

        AccessUtils.awaitMessageHandler();
        MParticle.getInstance().upload();
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                try {
                    JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                    if (jsonArray == null) {
                        return false;
                    }
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject eventObject = jsonArray.getJSONObject(i);
                        if (eventObject.getString("dt").equals(Constants.MessageType.SESSION_START)) {
                            assertEquals(eventObject.getLong("ct"), mAppStateManager.getSession().mSessionStartTime, 1000);
                            assertEquals("started sessionID = " + sessionId.value + " \ncurrent sessionId = " + mAppStateManager.getSession().mSessionID + " \nsent sessionId = " + eventObject.getString("id"),
                                    mAppStateManager.getSession().mSessionID, eventObject.getString("id"));
                            sessionStartReceived[0] = true;
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                return false;
            };
        }));

        Assert.assertTrue(sessionStartReceived[0]);
    }
}
