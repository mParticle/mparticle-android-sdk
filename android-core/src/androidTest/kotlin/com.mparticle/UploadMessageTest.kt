package com.mparticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Handler;
import android.os.Looper;

import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleApiClientImpl;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public final class UploadMessageTest extends BaseCleanStartedEachTest {

    /**
     * set MPID, log between 0 and 20 random MPEvents, and check to make sure each one is properly
     * attributed to the correct MPID, and there are no duplicates
     */
    @Test
    public void testCorrectlyAttributeEventsToMpid() throws Exception {
        int numberOfEvents = 3;
        final Handler handler = new Handler(Looper.getMainLooper());
        long mpid = ran.nextLong();
        MParticle.getInstance().Internal().getConfigManager().setMpid(mpid, ran.nextBoolean());
        final Map<String, MPEvent> events = new HashMap<String, MPEvent>();
        final CountDownLatch latch = new MPLatch(numberOfEvents);

        final Map<Long, Map<String, JSONObject>> matchingJSONEvents = new HashMap<Long, Map<String, JSONObject>>();
        com.mparticle.internal.AccessUtils.setMParticleApiClient(new com.mparticle.internal.AccessUtils.EmptyMParticleApiClient() {
            @Override
            public int sendMessageBatch(final String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);

                            long mpid = Long.valueOf(jsonObject.getString("mpid"));
                            Map<String, JSONObject> matchingMpidJSONEvents = matchingJSONEvents.get(mpid);
                            if (matchingMpidJSONEvents == null) {
                                matchingJSONEvents.put(mpid, matchingMpidJSONEvents = new HashMap<String, JSONObject>());
                            }
                            if (!MPUtility.isEmpty(jsonArray)) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject eventObject = jsonArray.getJSONObject(i);
                                    if (eventObject.getString("dt").equals(Constants.MessageType.EVENT)) {
                                        String eventName = eventObject.getString("n");
                                        MPEvent matchingEvent = events.get(eventName);
                                        if (matchingEvent != null) {
                                            String eventType = eventObject.getString("et");
                                            if (matchingEvent.getEventType().toString().equals(eventType)) {
                                                if (matchingMpidJSONEvents.containsKey(eventName)) {
                                                    fail("Duplicate Event Message Sent");
                                                } else {
                                                    matchingMpidJSONEvents.put(eventName, eventObject);
                                                }
                                            } else {
                                                fail("Unknown Event");
                                            }
                                        } else {
                                            fail("Unknown Event");
                                        }
                                        latch.countDown();
                                    }
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            fail(e.toString());
                        }
                    }
                });
                return 202;
            }
        });

        for (int j = 0; j < 3; j++) {
            MPEvent event = TestingUtils.getInstance().getRandomMPEventRich();
            if (events.containsKey(event.getEventName())) {
                j--;
            } else {
                events.put(event.getEventName(), event);
                MParticle.getInstance().logEvent(event);
            }
        }

        MParticle.getInstance().upload();
        latch.await();

        Map<String, JSONObject> jsonMap = matchingJSONEvents.get(mpid);
        if (events.size() > 0) {
            assertNotNull(jsonMap);
        }
        if (events != null && events.size() != 0 && events.size() != jsonMap.size()) {
            assertEquals(events.size(), jsonMap.size());
        }
    }

    @Test
    public void testEventAccuracy() throws Exception {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Map<String, MPEvent> receivedEvents = new HashMap<String, MPEvent>();
        final Map<String, JSONObject> sentEvents = new HashMap<String, JSONObject>();
        final CountDownLatch latch = new MPLatch(1);
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                try {
                    JSONObject jsonObject = request.getBodyJson();
                    JSONArray jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES);
                    if (!MPUtility.isEmpty(jsonArray)) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject eventObject = jsonArray.getJSONObject(i);
                            if (eventObject.getString("dt").equals(Constants.MessageType.EVENT)) {
                                String eventName = eventObject.getString("n");
                                if (sentEvents.containsKey(eventName)) {
                                    fail("Duplicate Event");
                                } else {
                                    sentEvents.put(eventName, eventObject);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.toString());
                }
                if (sentEvents.size() == receivedEvents.size())
                latch.countDown();
            }
        });

        for (int j = 0; j < 3; j++) {
            MPEvent event = TestingUtils.getInstance().getRandomMPEventRich();
            if (receivedEvents.containsKey(event.getEventName())) {
                j--;
            } else {
                receivedEvents.put(event.getEventName(), event);
                MParticle.getInstance().logEvent(event);
            }
        }

        MParticle.getInstance().upload();
        latch.await();
        for (Map.Entry<String, MPEvent> entry : receivedEvents.entrySet()) {
            if (!sentEvents.containsKey(entry.getKey())) {
                assertNull(entry.getValue());
            } else {
                assertTrue(sentEvents.containsKey(entry.getKey()));
                JSONObject jsonObject = sentEvents.get(entry.getKey());
                assertEventEquals(entry.getValue(), jsonObject);
            }
        }
    }

    void assertEventEquals(MPEvent mpEvent, JSONObject jsonObject) throws JSONException {
        if (jsonObject.optString("n") != mpEvent.getEventName()) {
            assertTrue(mpEvent.getEventName().equals(jsonObject.getString("n")));
        }
        if (mpEvent.getLength() != null || jsonObject.has("el")) {
            assertEquals(mpEvent.getLength(), jsonObject.getDouble("el"), .1);
        }
        if (!mpEvent.getEventType().toString().equals(jsonObject.optString("et"))) {
            assertTrue(mpEvent.getEventType().toString().equals(jsonObject.getString("et")));
        }

        Map<String, String> customAttributesTarget = mpEvent.getCustomAttributeStrings() == null ? new HashMap<String, String>() : mpEvent.getCustomAttributeStrings();
        JSONObject customAttributes = jsonObject.optJSONObject("attrs");
        if (customAttributes != null) {
            Iterator<String> keysIterator = customAttributes.keys();
            while (customAttributes != null && keysIterator.hasNext()) {
                String key = customAttributes.keys().next();
                String jsonVal = keysIterator.next();
                String objVal = customAttributesTarget.get(key);
                if (jsonVal != objVal) {
                    String val = customAttributes.getString(key);
                    if (!val.equals(customAttributesTarget.get(key)) && !key.equals("EventLength") && !(val.equals("null") && objVal == null)) {
                        assertTrue(customAttributes.getString(key).equals(customAttributesTarget.get(key)));
                    }
                }
            }
        }

        Map<String, List<String>> customFlagTarget = mpEvent.getCustomFlags();
        JSONObject customFlags = jsonObject.optJSONObject("flags");
        if (customFlags != null) {
            Iterator<String> flagsIterator = customFlags.keys();
            while (flagsIterator.hasNext()) {
                String key = flagsIterator.next();
                JSONArray values = customFlags.getJSONArray(key);
                List<String> flags = customFlagTarget.get(key);
                assertArraysEqual(values, flags);
            }
        }
    }


    void assertArraysEqual(JSONArray jsonArray, List<String> list) throws JSONException {
        List<String> jsonArrayList = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonArrayList.add(jsonArray.getString(i));
        }
        assertEquals(list.size(), jsonArrayList.size());
        Collections.sort(list);
        Collections.sort(jsonArrayList);
        for (int i = 0; i < list.size(); i++) {
            String a = list.get(i);
            String b = jsonArrayList.get(i);
            if (a == null) {
                assertTrue(b.equals("null"));
            } else {
                assertTrue(a.equals(b));
            }
        }
    }
}