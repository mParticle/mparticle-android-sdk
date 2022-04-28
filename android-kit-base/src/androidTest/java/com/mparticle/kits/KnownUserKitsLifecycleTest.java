package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Logger;
import com.mparticle.kits.testkits.BaseTestKit;
import com.mparticle.kits.testkits.ListenerTestKit;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class KnownUserKitsLifecycleTest extends BaseKitOptionsTest {

    @Before
    public void before() throws JSONException {
        MParticleOptions.Builder builder = MParticleOptions.builder(mContext)
                .configuration(
                        new ConfiguredKitOptions()
                                .addKit(-1, TestKit1.class, new JSONObject().put("eau", true))
                                .addKit(-2, TestKit2.class, new JSONObject().put("eau", false))
                                .addKit(-3, TestKit3.class, new JSONObject().put("eau", true))
                );
        startMParticle(builder);
    }

    @Test
    public void testExcludeUnknownUsers() throws InterruptedException {
        MParticle.getInstance().Internal().getConfigManager().setMpid(123, true);
        waitForKitReload();

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(123, false);
        waitForKitReload();

        assertFalse(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertFalse(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(321, false);
        waitForKitReload();

        assertFalse(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertFalse(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(123, true);
        waitForKitReload();

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(456, true);
        waitForKitReload();

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

    }

    public static class TestKit1 extends TestKit { }

    public static class TestKit2 extends TestKit { }

    public static class TestKit3 extends TestKit { }

    public static class TestKit extends ListenerTestKit {
        static int i = 0;

        @Override
        public String getName() {
            return "test kit" + i++;
        }

        @Override
        protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) throws IllegalArgumentException {
            return null;
        }

        @Override
        public List<ReportingMessage> setOptOut(boolean optedOut) {
            return null;
        }
    }
}
