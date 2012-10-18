package com.mparticle;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import android.test.AndroidTestCase;

import com.mparticle.MessageManager.MessageType;

public class MParticleAPITest extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(MessageManager.class);
      mMParticleAPI = new MParticleAPI(getContext(),"test","secret", mMockMessageManager);
    }

    public void testGetSameInstance() {
        MParticleAPI api1 = MParticleAPI.getInstance(getContext(), "apiKey", "secret");
        MParticleAPI api2 = MParticleAPI.getInstance(getContext(), "apiKey", "secret");
        assertSame(api1, api2);
    }

    public void testGetDifferentInstance() {
        MParticleAPI api1 = MParticleAPI.getInstance(getContext(), "apiKey1", "secret");
        MParticleAPI api2 = MParticleAPI.getInstance(getContext(), "apiKey2", "secret");
        assertNotSame(api1, api2);
    }

    // start new session on on start() call if one was not running
    public void testSessionStartInitial() {
        mMParticleAPI.start();
        assertNotNull(mMParticleAPI.mSessionID);
        assertTrue(mMParticleAPI.mSessionStartTime > 0);
        verify(mMockMessageManager, times(1)).storeMessage(MessageType.SESSION_START, null);
    }

    // do not start a new session if start() called with delay < timeout
    public void testSessionStartResume() {
        mMParticleAPI.start();
        UUID sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        mMParticleAPI.start();
        assertSame(sessionUUID, mMParticleAPI.mSessionID);
        assertEquals(sessionStartTime, mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).storeMessage(MessageType.SESSION_START, null);
    }

    // do start a new session if start() called with delay > timeout and also end last session
    @SuppressWarnings("unchecked")
    public void testSessionStartRestart() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(10);
        mMParticleAPI.start();
        UUID sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(20);
        mMParticleAPI.start();
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).storeMessage(MessageType.SESSION_START, null);
        verify(mMockMessageManager, times(1)).storeMessage(eq(MessageType.SESSION_END), anyMap());
    }

    // start new session on on start() call if event logged on unstarted session
    public void testSessionStartOnEvent() {
        mMParticleAPI.logEvent("test");
        assertNotNull(mMParticleAPI.mSessionID);
        assertTrue(mMParticleAPI.mSessionStartTime>0);
        verify(mMockMessageManager, times(1)).storeMessage(MessageType.SESSION_START, null);
    }

    // do start a new session if events logged with delay > timeout and also end last session
    @SuppressWarnings("unchecked")
    public void testSessionEventTimeout() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(50);
        mMParticleAPI.start();
        mMParticleAPI.logEvent("test1");
        UUID sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(100);
        mMParticleAPI.logEvent("test2");
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).storeMessage(MessageType.SESSION_START, null);
        verify(mMockMessageManager, times(1)).storeMessage(eq(MessageType.SESSION_END), anyMap());
    }

    // do not start a new session if events logged with delay < timeout
    @SuppressWarnings("unchecked")
    public void testSessionEventDoNotTimeout() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(5000);
        mMParticleAPI.start();
        Thread.sleep(5);
        mMParticleAPI.logEvent("test1");
        mMParticleAPI.logEvent("test2");
        mMParticleAPI.logEvent("test3");
        assertTrue(mMParticleAPI.mLastEventTime > mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).storeMessage(MessageType.SESSION_START, null);
        verify(mMockMessageManager, never()).storeMessage(eq(MessageType.SESSION_END), anyMap());
    }

    // start a new session if newSession() called explicitly and also end last session
    @SuppressWarnings("unchecked")
    public void testSessionNewSession() throws InterruptedException{
        mMParticleAPI.start();
        UUID sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(5);
        mMParticleAPI.newSession();
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).storeMessage(MessageType.SESSION_START, null);
        verify(mMockMessageManager, times(1)).storeMessage(eq(MessageType.SESSION_END), anyMap());
    }

    // do start a new session if endSession() called explicitly
    @SuppressWarnings("unchecked")
    public void testSessionEndExplicit() {
        mMParticleAPI.start();
        mMParticleAPI.endSession();
        assertTrue(mMParticleAPI.mSessionStartTime == 0);
        verify(mMockMessageManager, times(1)).storeMessage(MessageType.SESSION_START, null);
        verify(mMockMessageManager, times(1)).storeMessage(eq(MessageType.SESSION_END), anyMap());
    }

    // TODO: ?? end a session if checkEndSession called(?) - possibly allow for message manager to end a session

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testEventLogging() {
        // log an event with data
        Map<String, String> eventData=new HashMap<String, String>();
        eventData.put("testKey1", "testValue1");
        eventData.put("testKey2", "testValue2");
        mMParticleAPI.logEvent("testEvent", eventData);

        // make sure the MockMessageManager got called with the correct parameters in the correct order
        InOrder inOrder = inOrder(mMockMessageManager);
        inOrder.verify(mMockMessageManager).storeMessage(MessageType.SESSION_START, null);

        ArgumentCaptor<Map> eventDataArgument = ArgumentCaptor.forClass(Map.class);
        inOrder.verify(mMockMessageManager).storeMessage(eq(MessageType.CUSTOM_EVENT), eventDataArgument.capture());
        assertTrue(eventDataArgument.getValue().containsKey("n"));
        assertEquals("testEvent",eventDataArgument.getValue().get("n"));
        assertEquals("testValue1",eventDataArgument.getValue().get("testKey1"));
        assertEquals("testValue2",eventDataArgument.getValue().get("testKey2"));
    }

}
