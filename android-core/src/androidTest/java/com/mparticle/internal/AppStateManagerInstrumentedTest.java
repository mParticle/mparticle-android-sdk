package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.services.MParticleDBManagerTest;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.utils.AndroidUtils;
import com.mparticle.utils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class AppStateManagerInstrumentedTest {
    Context mContext;
    AppStateManager mAppStateManager;

    @BeforeClass
    public static void setup() {
        Looper.prepare();
    }

    @Before
    public void preConditions() {
        AndroidUtils.getInstance().deleteDatabase();
        mContext = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions.builder(mContext).credentials("key", "secret").build();
        MParticle.start(options);
        mAppStateManager = MParticle.getInstance().getAppStateManager();
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
        MParticleDBManagerTest.setMessageListener(new MParticleDBManager.MessageListener() {
            @Override
            public void onMessageStored(MPMessage message) {
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

        TestingUtils.checkAllBool(checked, 1, 10);
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
        MParticleDBManagerTest.setMessageListener(new MParticleDBManager.MessageListener() {
            @Override
            public void onMessageStored(MPMessage message) {
                if (message.getMessageType().equals(Constants.MessageType.SESSION_END)) {
                    try {
                        JSONArray mpidsArray = message.getJSONArray(Constants.MessageKey.SESSION_SPANNING_MPIDS);
                        assertEquals(mpidsArray.length(), mpids.size() - 1);
                        for (int i = 0; i < mpidsArray.length(); i++) {
                            if (!mpids.contains(mpidsArray.getLong(i)) || mpidsArray.getLong(i) == Constants.TEMPORARY_MPID) {
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

        TestingUtils.checkAllBool(checked, 1, 10);
    }

}
