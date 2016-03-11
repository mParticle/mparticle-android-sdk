package com.mparticle.internal;

import android.app.Activity;
import android.os.Handler;

import com.mparticle.MParticle;
import com.mparticle.kits.KitManager;
import com.mparticle.mock.MockApplication;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class AppStateManagerTest {

    AppStateManager manager;
    private MockApplication mockContext;
    private Activity activity = Mockito.mock(Activity.class);
    private MockSharedPreferences prefs;

    @Before
    public void setup(){
        MockContext context = new MockContext();

        mockContext = (MockApplication) context.getApplicationContext();
        manager = new AppStateManager(mockContext, true);
        prefs = (MockSharedPreferences) mockContext.getSharedPreferences(null, 0);
        manager.setConfigManager(Mockito.mock(ConfigManager.class));
        manager.setKitManager(Mockito.mock(KitManager.class));
        manager.setMessageManager(Mockito.mock(MessageManager.class));
        MParticle.setInstance(Mockito.mock(MParticle.class));
        manager.delayedBackgroundCheckHandler = Mockito.mock(Handler.class);
    }

    @Test
    public void testInit() throws Exception {
        manager.init(10);
        assertNull(mockContext.mCallbacks);
        manager.init(14);
        assertNotNull(mockContext.mCallbacks);
    }

    @Test
    public void testOnActivityStarted() throws Exception {
        assertEquals(true, manager.isBackgrounded());
        manager.onActivityStarted(activity, 0);
        assertTrue(manager.mInitialized);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityStarted(activity, 0);
    }

    /**
     * This tests what happens if we're started in something other than the launch Activity
     *
     * @throws Exception
     */
    @Test
    public void testSecondActivityStart() throws Exception {
        manager.onActivityStopped(activity, 0);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
        manager.onActivityStarted(activity, 0);
        Activity activity2 = Mockito.mock(Activity.class);
        Activity activity3 = Mockito.mock(Activity.class);
        manager.onActivityStopped(activity2, 0);
        manager.onActivityStopped(activity3, 0);
        Thread.sleep(1000);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityStopped(activity, 0);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
    }

    @Test
    public void testOnActivityStopped() throws Exception {
        manager.onActivityStarted(activity, 0);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityStopped(activity, 0);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
        assertTrue(manager.mInitialized);
        assertTrue(manager.mLastStoppedTime.get() > 0);
        manager.onActivityStarted(activity, 0);
        assertTrue(manager.getSession().getBackgroundTime() >= 1000 && manager.getSession().getBackgroundTime() < 1200);
    }

    @Test
    public void testEndSession() throws Exception {
        manager.startSession();
        manager.endSession();
        assertTrue(manager.getSession().mSessionID.equals("NO-SESSION"));
    }

    @Test
    public void testStartSession() throws Exception {
        Session session = manager.getSession();
        assertTrue(session.mSessionID.equals("NO-SESSION"));
        manager.startSession();
        assertNotEquals(manager.getSession().mSessionID, session.mSessionID);
    }
}