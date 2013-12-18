package com.mparticle;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.mparticle.MParticleAPI.EventType;

import android.test.AndroidTestCase;

public class EventLoggingTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMockMessageManager = mock(MockableMessageManager.class);
        mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    // should fail silently. a warning message is logged but the application continues.
    public void testEventNameIsNull() {
        mMParticleAPI.logEvent(null, EventType.UserContent);
        verify(mMockMessageManager, never()).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                any(EventType.class), any(JSONObject.class));
    }

    public void testEventNameTooLong() {
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length() < Constants.LIMIT_NAME) {
            longString += longString;
        }
        mMParticleAPI.logEvent(longString, EventType.UserContent);
        verify(mMockMessageManager, never()).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                any(EventType.class), any(JSONObject.class));
    }

    public void testEventLogging() throws JSONException {
        // log an event with data
        HashMap<String, String> eventData = new HashMap<String, String>();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        eventData.put("testKeyInt", "42");
        mMParticleAPI.logEvent("testEvent", EventType.UserContent, eventData);

        // make sure the MockMessageManager got called with the correct
        // parameters in the correct order
        InOrder inOrder = inOrder(mMockMessageManager);
        inOrder.verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());

        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        inOrder.verify(mMockMessageManager).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                eq(EventType.UserContent), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertEquals("testValue1", loggedAttributes.get("testKey1"));
        assertEquals("testValue2", loggedAttributes.get("testKey2"));
        assertEquals("42", loggedAttributes.get("testKeyInt"));
    }

    public void testTooManyAttributes() throws JSONException {
        HashMap<String, String> eventData = new HashMap<String, String>();
        for (int i = 0; i < Constants.LIMIT_ATTR_COUNT + 1; i++) {
            eventData.put("testKey" + i, "testValue" + i);
        }
        mMParticleAPI.logEvent("testEvent", EventType.UserContent, eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                any(EventType.class), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertTrue(eventData.size() > Constants.LIMIT_ATTR_COUNT);
        assertEquals(Constants.LIMIT_ATTR_COUNT, loggedAttributes.length());
    }

    public void testAttributesValueTooLarge() throws JSONException {
        HashMap<String, String> eventData = new HashMap<String, String>();
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length() < Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        eventData.put("testKey1", longString);
        mMParticleAPI.logEvent("testEvent", EventType.UserContent, eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                any(EventType.class), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertFalse(loggedAttributes.has("testKey1"));
    }

    public void testAttributesKeyTooLarge() throws JSONException {
        HashMap<String, String> eventData = new HashMap<String, String>();
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        while (longString.length() < Constants.LIMIT_ATTR_VALUE) {
            longString += longString;
        }
        eventData.put(longString, "testValue1");
        mMParticleAPI.logEvent("testEvent", EventType.UserContent, eventData);
        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        verify(mMockMessageManager).logEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(),
                any(EventType.class), eventDataArgument.capture());

        JSONObject loggedAttributes = eventDataArgument.getValue();
        assertEquals(1, eventData.size());
        assertEquals(0, loggedAttributes.length());
    }

    public void testEventCountExceeded() {
        for (int i = 0; i < 1 + Constants.EVENT_LIMIT; i++) {
            mMParticleAPI.logEvent("testEvent", EventType.UserContent);
        }
        verify(mMockMessageManager, times(Constants.EVENT_LIMIT)).logEvent(eq(mMParticleAPI.mSessionID), anyLong(),
                anyLong(), anyString(), any(EventType.class), any(JSONObject.class));
    }

    public void testLogErrorMessage() {
        mMParticleAPI.logErrorEvent("errorMessage1");
        verify(mMockMessageManager, times(1)).logErrorEvent(anyString(), anyLong(), anyLong(), eq("errorMessage1"),
                any(Throwable.class), anyBoolean());
    }

    public void testLogNullErrorMessage() {
        mMParticleAPI.logErrorEvent((String) null);
        verify(mMockMessageManager, times(0)).logErrorEvent(anyString(), anyLong(), anyLong(), anyString(),
                any(Throwable.class));
    }

    public void testLogException() {
        mMParticleAPI.logErrorEvent(new Exception("testException"));
        verify(mMockMessageManager, times(1)).logErrorEvent(anyString(), anyLong(), anyLong(), eq((String) null),
                any(Throwable.class), anyBoolean());
    }

    public void testLogNullException() {
        mMParticleAPI.logErrorEvent((Exception) null);
        verify(mMockMessageManager, times(0)).logErrorEvent(anyString(), anyLong(), anyLong(), anyString(),
                any(Throwable.class));
    }

}
