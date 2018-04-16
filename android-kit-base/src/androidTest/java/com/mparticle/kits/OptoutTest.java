package com.mparticle.kits;

import com.mparticle.MParticle;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class OptoutTest extends BaseKitManagerStarted {
    @Override
    protected Map<String, JSONObject> registerCustomKits() {
        return new HashMap<String, JSONObject>(){{put(TestKit.class.getName(), new JSONObject());}};
    }

    TestKit kitInstance;

    @Override
    protected void before() throws Exception {
        super.before();
        TestKit.setOnKitCreate(new BaseTestKit.OnKitCreateListener() {
            @Override
            public void onKitCreate(BaseTestKit kitIntegration) {
                if (kitIntegration instanceof TestKit) {
                    kitInstance = (TestKit)kitIntegration;
                }
            }
        });
        long endTime = System.currentTimeMillis() + (10 * 1000);
        while (kitInstance == null && System.currentTimeMillis() < endTime) {}
        if (kitInstance == null) {
            fail("Kit not started");
        }
    }

    @Test
    public void testOptOut() {
        MParticle.getInstance().setOptOut(true);
        assertTrue(kitInstance.getOptedOut());
        MParticle.getInstance().setOptOut(false);
        assertFalse(kitInstance.getOptedOut());
        MParticle.getInstance().setOptOut(true);
        assertTrue(kitInstance.getOptedOut());
        MParticle.getInstance().setOptOut(false);
        assertFalse(kitInstance.getOptedOut());
        MParticle.getInstance().setOptOut(true);
        assertTrue(kitInstance.getOptedOut());

    }

    public static class TestKit extends BaseTestKit {
        boolean optedOut;
        @Override
        public List<ReportingMessage> setOptOut(boolean optedOut) {
            this.optedOut = optedOut;
            return null;
        }

        boolean getOptedOut() {
            return optedOut;
        }
    }
}
