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
      mMParticleAPI = new MParticleAPI(getContext(),"test","secret", mMockMessageManager);
    }

    // should fail silently. a warning message is logged but the application continues.
    public void testInvalidEventName() {
        mMParticleAPI.logEvent(null);
    }

    public void testEventLogging() throws JSONException {
        // log an event with data
        JSONObject eventData=new JSONObject();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        mMParticleAPI.logEvent("testEvent", eventData);

        // make sure the MockMessageManager got called with the correct parameters in the correct order
        InOrder inOrder = inOrder(mMockMessageManager);
        inOrder.verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong());

        ArgumentCaptor<JSONObject> eventDataArgument = ArgumentCaptor.forClass(JSONObject.class);
        inOrder.verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyLong(), anyString(), eventDataArgument.capture());

        assertEquals("testValue1",eventDataArgument.getValue().get("testKey1"));
        assertEquals("testValue2",eventDataArgument.getValue().get("testKey2"));
    }

}
