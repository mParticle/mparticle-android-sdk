package com.mparticle;

import com.mparticle.internal.Constants;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DataplanTest extends BaseCleanInstallEachTest {
    TestingUtils testingUtils = TestingUtils.getInstance();


    @Test
    public void noDataPlanTest() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan(null, null));

        final AndroidUtils.Mutable<Integer> messageCount = new AndroidUtils.Mutable<Integer>(0);
        final MPLatch latch = new MPLatch(1);
        MockServer.getInstance().waitForVerify(new Matcher().bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject bodyJson) {
                try {
                    assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT));
                    messageCount.value += getMessageCount(bodyJson);
                    if (messageCount.value == 3) {
                        latch.countDown();
                        return true;
                    }
                } catch (JSONException ex) {}
                return false;
            }
        }), latch);

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().upload();

        latch.await();
        assertEquals(3, messageCount.value.intValue());
    }

    @Test
    public void dataplanPartialTest() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan("plan1", null));

        final AndroidUtils.Mutable<Integer> messageCount = new AndroidUtils.Mutable<Integer>(0);
        final MPLatch latch = new MPLatch(1);
        MockServer.getInstance().waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject bodyJson) {
                try {
                    assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT));
                    JSONObject dataplanContext = bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT);
                    JSONObject dataplanJSON = dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY);
                    assertEquals("plan1", dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID));
                    assertNull(dataplanJSON.optString(Constants.MessageKey.DATA_PLAN_VERSION, null));
                    messageCount.value += getMessageCount(bodyJson);
                    if (messageCount.value == 3) {
                        latch.countDown();
                        return true;
                    }
                } catch (JSONException ex) {}
                return false;
            }
        }), latch);

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().upload();

        latch.await();
        assertEquals(3, messageCount.value.intValue());
    }

    @Test
    public void noDataPlanIdTest() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan(null, 1));

        final AndroidUtils.Mutable<Integer> messageCount = new AndroidUtils.Mutable<Integer>(0);
        final MPLatch latch = new MPLatch(1);
        MockServer.getInstance().waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject bodyJson) {
                try {
                    assertNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT));
                    messageCount.value += getMessageCount(bodyJson);
                    if (messageCount.value == 3) {
                        latch.countDown();
                        return true;
                    }
                } catch (JSONException ex) {}
                return false;
            }
        }), latch);

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().upload();


        latch.await();
        assertEquals(3, messageCount.value.intValue());
    }

    @Test
    public void dataPlanSetTest() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 1));

        final AndroidUtils.Mutable<Integer> messageCount = new AndroidUtils.Mutable<Integer>(0);
        final MPLatch latch = new MPLatch(1);
        MockServer.getInstance().waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject bodyJson) {
                try {
                    assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT));
                    JSONObject dataplanContext = bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT);
                    JSONObject dataplanJSON = dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY);
                    assertEquals("dataplan1", dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID));
                    assertEquals("1", dataplanJSON.optString(Constants.MessageKey.DATA_PLAN_VERSION, null));
                    JSONArray messages = bodyJson.optJSONArray("msgs");
                    messageCount.value += getMessageCount(bodyJson);
                    if (messageCount.value == 3) {
                        latch.countDown();
                        return true;
                    }
                } catch (Exception ex) {
                    fail(ex.toString());
                }
                return false;
            }
        }), latch);

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().upload();

        latch.await();
        assertEquals(3, messageCount.value.intValue());
    }

    @Test
    public void dataplanChanged() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 1));

        final AndroidUtils.Mutable<Integer> totalMessageCount = new AndroidUtils.Mutable<Integer>(0);
        final AndroidUtils.Mutable<Integer> dataplan1MessageCount = new AndroidUtils.Mutable<Integer>(0);
        final AndroidUtils.Mutable<Integer> dataplan2MessageCount = new AndroidUtils.Mutable<Integer>(0);
        final MPLatch latch = new MPLatch(1);
        MockServer.getInstance().waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject bodyJson) {
                try {
                    assertNotNull(bodyJson.optJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT));
                    JSONObject dataplanContext = bodyJson.getJSONObject(Constants.MessageKey.DATA_PLAN_CONTEXT);
                    JSONObject dataplanJSON = dataplanContext.getJSONObject(Constants.MessageKey.DATA_PLAN_KEY);
                    String dataplanId = dataplanJSON.getString(Constants.MessageKey.DATA_PLAN_ID);
                    Integer dataplanVersion = dataplanJSON.optInt(Constants.MessageKey.DATA_PLAN_VERSION, -1);

                    int messageCount = getMessageCount(bodyJson);
                    if (new Integer(1).equals(dataplanVersion)) {
                        assertEquals("dataplan1", dataplanId);
                        dataplan1MessageCount.value += messageCount;
                    }
                    if (new Integer(2).equals(dataplanVersion)) {
                        assertEquals("dataplan1", dataplanId);
                        dataplan2MessageCount.value += messageCount;
                    }
                    totalMessageCount.value += messageCount;
                    if (totalMessageCount.value == 5) {
                        latch.countDown();
                    }
                } catch (Exception ex) {
                    fail(ex.toString());
                }
                return false;
            }
        }), latch);

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());


        MParticle.setInstance(null);
        startMParticle(MParticleOptions.builder(mContext)
                .dataplan("dataplan1", 2));

        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().logEvent(testingUtils.getRandomMPEventRich());
        MParticle.getInstance().upload();

        //not sure why it needs upload() twice, but this cuts the runtime down from 10s to .7s
        MParticle.getInstance().upload();
        MParticle.getInstance().upload();
        latch.await();
        assertEquals(3, dataplan1MessageCount.value.intValue());
        assertEquals(2, dataplan2MessageCount.value.intValue());

        assertEquals(5, totalMessageCount.value.intValue());
    }

    private int getMessageCount(JSONObject bodyJson) throws JSONException {
        int count = 0;
        JSONArray messages = bodyJson.optJSONArray("msgs");
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                JSONObject messageJSON = messages.getJSONObject(i);
                if (messageJSON.getString("dt").equals("e")) {
                    count++;
                }
            }
        }
        return count;
    }
}
