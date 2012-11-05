package com.mparticle;

import static org.mockito.Mockito.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import android.test.AndroidTestCase;

public class EventLoggingTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(MessageManager.class);
      mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    // should fail silently. a warning message is logged but the application continues.
    public void testEventNameIsNull() {
        mMParticleAPI.logEvent(null);
        verify(mMockMessageManager, never()).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), any(JSONObject.class));
    }

    public void testEventNameTooLong() {
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_NAME) {
            longString += longString;
        }
        mMParticleAPI.logEvent(longString);
        verify(mMockMessageManager, never()).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), any(JSONObject.class));
    }

    public void testEventLogging() throws JSONException {
        // log an event with data
        JSONObject eventData=new JSONObject();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        eventData.put("testKeyInt", 42);
        mMParticleAPI.logEvent("testEvent", eventData);

        // make sure the MockMessageManager got called with the correct parameters in the correct order
        InOrder inOrder = inOrder(mMockMessageManager);
        inOrder.verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong());

        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        inOrder.verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertEquals("testValue1",loggedAttributes.get("testKey1"));
        assertEquals("testValue2",loggedAttributes.get("testKey2"));
        assertEquals(42,loggedAttributes.get("testKeyInt"));
    }

    public void testTooManyAttributes() throws JSONException {
        JSONObject eventData=new JSONObject();
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            eventData.put("testKey"+i, "testValue"+i);
        }
        mMParticleAPI.logEvent("testEvent", eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertTrue(eventData.length()>Constants.LIMIT_ATTR_COUNT);
        assertEquals(Constants.LIMIT_ATTR_COUNT,loggedAttributes.length());
    }

    public void testAttributesValueTooLarge() throws JSONException {
        JSONObject eventData=new JSONObject();
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        eventData.put("testKey1", longString);
        mMParticleAPI.logEvent("testEvent", eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertFalse(loggedAttributes.has("testKey1"));
    }

    public void testAttributesKeyTooLarge() throws JSONException {
        JSONObject eventData=new JSONObject();
        String longString="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length()<Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        eventData.put(longString, "testValue1");
        mMParticleAPI.logEvent("testEvent", eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertEquals(1,eventData.length());
        assertEquals(0,loggedAttributes.length());
    }

    public void testEventCountExceeded() {
        for (int i = 0; i < 1 + Constants.EVENT_LIMIT; i++) {
            mMParticleAPI.logEvent("testEvent");
        }
        verify(mMockMessageManager, times(Constants.EVENT_LIMIT)).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), any(JSONObject.class));
    }

}
