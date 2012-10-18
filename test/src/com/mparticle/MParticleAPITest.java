package com.mparticle;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import android.test.AndroidTestCase;

import com.mparticle.MessageManager.MessageType;

public class MParticleAPITest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
      super.setUp();
    }

    public void testGetSameInstance() {
        MParticleAPI mParticleAPI1 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        MParticleAPI mParticleAPI2 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        assertSame(mParticleAPI1, mParticleAPI2);
    }

    public void testGetDifferentInstance() {
        MParticleAPI mParticleAPI1 = MParticleAPI.getInstance(getContext(), "01234567890123456789012345678901", "secret");
        MParticleAPI mParticleAPI3 = MParticleAPI.getInstance(getContext(), "99999999999999999999999999999999", "secret");
        assertNotSame(mParticleAPI1, mParticleAPI3);
    }

    public void testSessionLifecycleEvents() {

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testEventLogging() {
        // create mock MessageManager & set MParticleAPI to use it
        MessageManager mockMessageManager = mock(MessageManager.class);
        MParticleAPI mParticleAPI = new MParticleAPI(getContext(),"test","secret", mockMessageManager);

        // log an event with data
        Map<String, String> eventData=new HashMap<String, String>();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        mParticleAPI.logEvent("testEvent", eventData);

        // make sure the MockMessageManager got called with the correct parameters in the correct order
        InOrder inOrder = inOrder(mockMessageManager);
        inOrder.verify(mockMessageManager).storeMessage(MessageType.SESSION_START, null);

        ArgumentCaptor<Map> eventDataArgument = ArgumentCaptor.forClass(Map.class);
        inOrder.verify(mockMessageManager).storeMessage(eq(MessageType.CUSTOM_EVENT), eventDataArgument.capture());
        assertTrue(eventDataArgument.getValue().containsKey("n"));
        assertEquals("testEvent",eventDataArgument.getValue().get("n"));
        assertEquals("testValue1",eventDataArgument.getValue().get("testKey1"));
        assertEquals("testValue2",eventDataArgument.getValue().get("testKey2"));
    }

    public void testSessionDataLogging() {

    }

}
