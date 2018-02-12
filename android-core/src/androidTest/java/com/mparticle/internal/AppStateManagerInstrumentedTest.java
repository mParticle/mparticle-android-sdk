package com.mparticle.internal;

import android.content.Context;

import com.mparticle.*;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.services.AccessUtils;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.utils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class AppStateManagerInstrumentedTest extends BaseCleanStartedEachTest {
    Context mContext;
    AppStateManager mAppStateManager;

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {
        mAppStateManager = MParticle.getInstance().getAppStateManager();

        MParticle.getInstance().getConfigManager().setMpid(Constants.TEMPORARY_MPID);
    }

    @Test
    public void testEndSessionMultipleMpids() throws Exception {
        final Set<Long> mpids = new HashSet<Long>();
        for (int i = 0; i < 5; i++) {
            mpids.add(new Random().nextLong());
        }
        mAppStateManager.ensureActiveSession();
        for (Long mpid: mpids) {
            mAppStateManager.getSession().addMpid(mpid);
        }
        final boolean[] checked = new boolean[1];
        AccessUtils.setMessageStoredListener(new MParticleDBManager.MessageListener() {
            @Override
            public void onMessageStored(BaseMPMessage message) {
                if (message.getMessageType().equals(Constants.MessageType.SESSION_END)) {
                    try {
                        JSONArray mpidsArray = message.getJSONArray(Constants.MessageKey.SESSION_SPANNING_MPIDS);
                        assertEquals(mpidsArray.length(), mpids.size());
                        for (int i = 0; i < mpidsArray.length(); i++) {
                            if (!mpids.contains(mpidsArray.getLong(i))) {
                                return;
                            }
                        }
                        checked[0] = true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mAppStateManager.endSession();

        TestingUtils.checkAllBool(checked, 1, 100);
    }

    @Test
    public void testDontIncludeDefaultMpidSessionEnd() throws Exception {
        final Set<Long> mpids = new HashSet<Long>();
        for (int i = 0; i < 5; i++) {
            mpids.add(new Random().nextLong());
        }
        mpids.add(Constants.TEMPORARY_MPID);
        mAppStateManager.ensureActiveSession();
        for (Long mpid: mpids) {
            mAppStateManager.getSession().addMpid(mpid);
        }
        final boolean[] checked = new boolean[1];
        AccessUtils.setMessageStoredListener(new MParticleDBManager.MessageListener() {
            @Override
            public void onMessageStored(BaseMPMessage message) {
                if (message.getMessageType().equals(Constants.MessageType.SESSION_END)) {
                    try {
                        JSONArray mpidsArray = message.getJSONArray(Constants.MessageKey.SESSION_SPANNING_MPIDS);
                        if (mpidsArray.length() == mpids.size() - 1) {
                            for (int i = 0; i < mpidsArray.length(); i++) {
                                if (!mpids.contains(mpidsArray.getLong(i)) || mpidsArray.getLong(i) == Constants.TEMPORARY_MPID) {
                                    return;
                                }
                            }
                            checked[0] = true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mAppStateManager.endSession();

        TestingUtils.checkAllBool(checked, 1, 10);
    }

}
