package com.mparticle;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testEventLogging() {
        // log an event with data
        Map<String, String> eventData=new HashMap<String, String>();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        mMParticleAPI.logEvent("testEvent", eventData);

        // make sure the MockMessageManager got called with the correct parameters in the correct order
        InOrder inOrder = inOrder(mMockMessageManager);
        inOrder.verify(mMockMessageManager, times(1)).beginSession(anyString(), anyLong(), anyMap());

        ArgumentCaptor<Map> eventDataArgument = ArgumentCaptor.forClass(Map.class);
        inOrder.verify(mMockMessageManager).logCustomEvent(eq(mMParticleAPI.mSessionID), anyLong(), anyString(), eventDataArgument.capture());

        assertEquals("testValue1",eventDataArgument.getValue().get("testKey1"));
        assertEquals("testValue2",eventDataArgument.getValue().get("testKey2"));
    }

}
