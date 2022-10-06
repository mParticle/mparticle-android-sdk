package com.mparticle.internal;

import android.content.Context;
import android.util.MutableBoolean;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.database.services.AccessUtils;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppStateManagerInstrumentedTest extends BaseCleanStartedEachTest {
    AppStateManager mAppStateManager;

    @Before
    public void before() throws Exception {
        mAppStateManager = MParticle.getInstance().Internal().getAppStateManager();
        MParticle.getInstance().Internal().getConfigManager().setMpid(Constants.TEMPORARY_MPID, false);
    }

    @Test
    public void testEndSessionMultipleMpids() throws Exception {
        final Set<Long> mpids = new HashSet<Long>();
        for (int i = 0; i < 5; i++) {
            mpids.add(ran.nextLong());
        }
        mAppStateManager.ensureActiveSession();
        for (Long mpid: mpids) {
            mAppStateManager.getSession().addMpid(mpid);
        }
        final boolean[] checked = new boolean[1];
        final CountDownLatch latch = new MPLatch(1);
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
                        latch.countDown();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mAppStateManager.endSession();
        latch.await();
        assertTrue(checked[0]);
    }

    @Test
    public void testDontIncludeDefaultMpidSessionEnd() throws Exception {
        final Set<Long> mpids = new HashSet<Long>();
        for (int i = 0; i < 5; i++) {
            mpids.add(ran.nextLong());
        }
        mpids.add(Constants.TEMPORARY_MPID);
        mAppStateManager.ensureActiveSession();
        for (Long mpid: mpids) {
            mAppStateManager.getSession().addMpid(mpid);
        }
        final CountDownLatch latch = new MPLatch(1);
        final MutableBoolean checked = new MutableBoolean(false);
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
                            checked.value = true;
                            latch.countDown();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mAppStateManager.endSession();
        latch.await();
        assertTrue(checked.value);
    }

    @Test
    public void testOnApplicationForeground() throws InterruptedException {
        CountDownLatch latch = new MPLatch(2);
        KitManagerTester kitManagerTester = new KitManagerTester(mContext, latch);
        com.mparticle.AccessUtils.setKitManager(kitManagerTester);
        goToBackground();
        assertNull(mAppStateManager.getCurrentActivity());
        Thread.sleep(AppStateManager.ACTIVITY_DELAY + 100);

        goToForeground();
        assertNotNull(mAppStateManager.getCurrentActivity().get());
        latch.await();
        assertTrue(kitManagerTester.onApplicationBackgroundCalled);
        assertTrue(kitManagerTester.onApplicationForegroundCalled);
    }

    class KitManagerTester extends KitFrameworkWrapper {
        boolean onApplicationBackgroundCalled, onApplicationForegroundCalled = false;
        CountDownLatch latch;

        public KitManagerTester(Context context, CountDownLatch latch) {
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
                    MParticle.getInstance().Internal().getConfigManager(),
                    MParticle.getInstance().Internal().getAppStateManager(),
                    MParticleOptions.builder(mContext).credentials("some", "key").build());
            this.latch = latch;
        }

        @Override
        public void onApplicationBackground() {
            assertNull(getCurrentActivity());
            onApplicationBackgroundCalled = true;
            latch.countDown();
        }

        @Override
        public void onApplicationForeground() {
            assertNotNull(getCurrentActivity().get());
            onApplicationForegroundCalled = true;
            latch.countDown();
        }
    }

}
