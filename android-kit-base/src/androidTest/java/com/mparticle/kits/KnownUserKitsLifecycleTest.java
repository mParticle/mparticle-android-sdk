package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;
import com.mparticle.kits.testkits.BaseTestKit;
import com.mparticle.kits.testkits.ListenerTestKit;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

public class KnownUserKitsLifecycleTest extends BaseKitManagerStarted {

    @Override
    protected Map<Class<? extends KitIntegration>, JSONObject> registerCustomKits() {
        Map<Class<? extends KitIntegration>, JSONObject> map = new HashMap<>();
        try {
            map.put(TestKit1.class, new JSONObject().put("eau", true).put("id", -1));
            map.put(TestKit2.class, new JSONObject().put("eau", false).put("id", -2));
            map.put(TestKit3.class, new JSONObject().put("eau", true).put("id", -3));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Test
    public void testExcludeUnknownUsers() throws InterruptedException {
        MParticle.getInstance().Internal().getConfigManager().setMpid(123, true);
        waitForNewIdentityKitStart(123);

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(123, false);
        waitForNewIdentityKitStart(123);

        assertFalse(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertFalse(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(321, false);
        waitForNewIdentityKitStart(321);

        assertFalse(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertFalse(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(123, true);
        waitForNewIdentityKitStart(123);

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

        MParticle.getInstance().Internal().getConfigManager().setMpid(456, true);
        waitForNewIdentityKitStart(456);

        assertTrue(MParticle.getInstance().isKitActive(-1));
        assertTrue(MParticle.getInstance().isKitActive(-2));
        assertTrue(MParticle.getInstance().isKitActive(-3));

    }

    private void waitForNewIdentityKitStart(final long mpid) throws InterruptedException {
        final CountDownLatch latch = new MPLatch(1);
        setKitStartedListener(new KitStartedListener() {
            @Override
            public void onKitStarted(JSONArray jsonArray) {
                if (jsonArray != null && jsonArray.length() == 3) {
                    latch.countDown();
                    Logger.error("kits started: " + mKitManager.providers.size());
                }
            }
        });
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(mpid, MParticle.getInstance().Identity().getCurrentUser().getId());
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
