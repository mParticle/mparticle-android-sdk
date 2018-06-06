package com.mparticle.internal;

import android.content.Context;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.internal.database.services.AccessUtils;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

public class AppStateManagerInstrumentedTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;

    @Before
    public void before() throws Exception {
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

    @Test
    public void testOnApplicationForeground() throws InterruptedException {
        boolean[] checked = new boolean[2];
        com.mparticle.AccessUtils.setKitManager(new KitManagerTester(mContext, checked));
        goToBackground();
        assertNull(mAppStateManager.getCurrentActivity());
        Thread.sleep(AppStateManager.ACTIVITY_DELAY + 100);
        goToForeground();
        assertNotNull(mAppStateManager.getCurrentActivity().get());
        TestingUtils.checkAllBool(checked);
    }

    class KitManagerTester extends KitFrameworkWrapper {
        boolean[] checked;

        public KitManagerTester(Context context, boolean[] checked) {
            super(context,
                    new ReportingManager() {
                        @Override
                        public void log(JsonReportingMessage message) {
                            //do nothing
                        }

                        @Override
                        public void logAll(List<? extends JsonReportingMessage> messageList) {
                            //do nothing
                        }
                    },
                    MParticle.getInstance().getConfigManager(),
                    MParticle.getInstance().getAppStateManager(),
                    com.mparticle.internal.AccessUtils.getUploadHandler());
            this.checked = checked;
        }

        @Override
        public void onApplicationBackground() {
            assertNull(getCurrentActivity());
            checked[0] = true;
        }

        @Override
        public void onApplicationForeground() {
            assertNotNull(getCurrentActivity().get());
            checked[1] = true;
        }
    }

}
