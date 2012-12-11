package com.mparticle;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import android.test.AndroidTestCase;

public class SessionLifecycleTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      mMockMessageManager = mock(MockableMessageManager.class);
      mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    // start new session on on start() call if one was not running
    public void testSessionStartInitial() {
        mMParticleAPI.startActivity();
        assertNotNull(mMParticleAPI.mSessionID);
        assertTrue(mMParticleAPI.mSessionStartTime > 0);
        verify(mMockMessageManager, times(1)).startSession(eq(mMParticleAPI.mSessionID), eq(mMParticleAPI.mSessionStartTime), anyString());
    }

    // do not start a new session if start() called with delay < timeout
    public void testSessionStartResume() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(5000);
        mMParticleAPI.startActivity();
        String sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        mMParticleAPI.stopActivity();
        Thread.sleep(20);
        mMParticleAPI.startActivity();
        assertSame(sessionUUID, mMParticleAPI.mSessionID);
        assertEquals(sessionStartTime, mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
    }

    // do start a new session if start() called with delay > timeout and also end last session
    public void testSessionStartRestart() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(50);
        mMParticleAPI.startActivity();
        String sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(250);
        mMParticleAPI.startActivity();
        // at this point, disable the timeout it doesn't check again before the assertions
        mMParticleAPI.setSessionTimeout(0);
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), anyLong(), anyLong());
    }

    // start new session on on start() call if event logged on unstarted session
    public void testSessionStartOnEvent() {
        mMParticleAPI.logEvent("test");
        assertNotNull(mMParticleAPI.mSessionID);
        assertTrue(mMParticleAPI.mSessionStartTime>0);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
    }

    // do start a new session if events logged with delay > timeout and also end last session
    public void testSessionEventTimeout() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(50);
        mMParticleAPI.startActivity();
        mMParticleAPI.logEvent("test1");
        String sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(200);
        mMParticleAPI.logEvent("test2");
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), anyLong(), anyLong());
    }

    // do not start a new session if events logged with delay < timeout
    public void testSessionEventDoNotTimeout() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(5000);
        mMParticleAPI.startActivity();
        Thread.sleep(5);
        mMParticleAPI.logEvent("test1");
        mMParticleAPI.logEvent("test2");
        mMParticleAPI.logEvent("test3");
        assertTrue(mMParticleAPI.mLastEventTime > mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, never()).stopSession(anyString(), anyLong(), anyLong());
    }

    // start a new session if newSession() called explicitly and also end last session
    public void testSessionNewSession() throws InterruptedException{
        mMParticleAPI.startActivity();
        String sessionUUID = mMParticleAPI.mSessionID;
        long sessionStartTime = mMParticleAPI.mSessionStartTime;
        Thread.sleep(5);
        mMParticleAPI.newSession();
        assertNotSame(sessionUUID, mMParticleAPI.mSessionID);
        assertTrue(sessionStartTime < mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(2)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), anyLong(), anyLong());
    }

    // track a session stop event but do not end the session
    public void testSessionStop() {
        mMParticleAPI.startActivity();
        String sessionUUID = mMParticleAPI.mSessionID;
        mMParticleAPI.stopActivity();
        assertFalse(mMParticleAPI.mSessionStartTime == 0);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), anyLong(), anyLong());
        verify(mMockMessageManager, never()).endSession(anyString(), anyLong(), anyLong());
    }

    // do start a new session if endSession() called explicitly
    public void testSessionEndExplicit() {
        mMParticleAPI.startActivity();
        String sessionUUID = mMParticleAPI.mSessionID;
        mMParticleAPI.endSession();
        assertTrue(mMParticleAPI.mSessionStartTime == 0);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).endSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).endSession(eq(sessionUUID), anyLong(), anyLong());
    }

    // check for a timeout situation that ends a session but does not start a new session
    public void testSessionTimeoutStandalone() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(50);
        mMParticleAPI.startActivity();
        mMParticleAPI.logEvent("test1");
        String sessionUUID = mMParticleAPI.mSessionID;
        long lastEventTime = mMParticleAPI.mLastEventTime;
        Thread.sleep(200);
        mMParticleAPI.checkSessionTimeout();
        assertTrue(0 == mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), eq(lastEventTime), anyLong());
    }

    // check for a timeout situation that ends a session but does not start a new session
    public void testSessionTimeoutBackground() throws InterruptedException {
        mMParticleAPI.setSessionTimeout(50);
        mMParticleAPI.startActivity();
        mMParticleAPI.logEvent("test1");
        Thread.sleep(10);
        mMParticleAPI.logEvent("test2");
        String sessionUUID = mMParticleAPI.mSessionID;
        long lastEventTime = mMParticleAPI.mLastEventTime;
        Thread.sleep(200);
        mMParticleAPI.checkSessionTimeout();
        assertTrue(0 == mMParticleAPI.mSessionStartTime);
        verify(mMockMessageManager, times(1)).startSession(anyString(), anyLong(), anyString());
        verify(mMockMessageManager, times(1)).stopSession(anyString(), anyLong(), anyLong());
        verify(mMockMessageManager, times(1)).stopSession(eq(sessionUUID), eq(lastEventTime), anyLong());
    }

}
