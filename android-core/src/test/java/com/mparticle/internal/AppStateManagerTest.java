package com.mparticle.internal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;

import com.mparticle.MParticle;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.MockMParticle;
import com.mparticle.mock.MockApplication;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.AndroidUtils.Mutable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class AppStateManagerTest {

    AppStateManager manager;
    private MockApplication mockContext;
    private Activity activity = Mockito.mock(Activity.class);
    private MockSharedPreferences prefs;
    private MessageManager messageManager;

    @Before
    public void setup() {
        MockContext context = new MockContext();
        mockContext = (MockApplication) context.getApplicationContext();
        manager = new AppStateManager(mockContext, true);
        prefs = (MockSharedPreferences) mockContext.getSharedPreferences(null, 0);
        ConfigManager configManager = Mockito.mock(ConfigManager.class);
        manager.setConfigManager(configManager);
        Mockito.when(configManager.isEnabled()).thenReturn(true);
        messageManager = Mockito.mock(MessageManager.class);
        manager.setMessageManager(messageManager);
        MParticle mp = new MockMParticle();
        MParticle.setInstance(mp);

        manager.delayedBackgroundCheckHandler = Mockito.mock(Handler.class);
        manager.mInitialized = false;
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
        manager.onActivityStarted(activity);
        Mockito.verify(MParticle.getInstance().Internal().getKitManager(), Mockito.times(1)).onActivityStarted(activity);
    }

    @Test
    public void testOnActivityResumed() throws Exception {
        assertEquals(true, manager.isBackgrounded());
        manager.onActivityResumed(activity);
        assertTrue(manager.mInitialized);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityResumed(activity);
    }

    @Test
    public void testIntentParameterParsing() throws Exception {
        Intent mockIntent = Mockito.mock(Intent.class);
        Mockito.when(mockIntent.getDataString()).thenReturn("this is data string 1");
        ComponentName mockCallingActivity = Mockito.mock(ComponentName.class);
        Mockito.when(mockCallingActivity.getPackageName()).thenReturn("package name 1");
        Mockito.when(activity.getCallingActivity()).thenReturn(mockCallingActivity);

        Mockito.when(activity.getIntent()).thenReturn(mockIntent);
        
        manager.onActivityResumed(activity);
        Mockito.verify(messageManager, Mockito.times(1))
                .logStateTransition(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.eq("this is data string 1"),
                        Mockito.isNull(String.class),
                        Mockito.eq( "package name 1"),
                        Mockito.anyLong(),
                        Mockito.anyLong(),
                        Mockito.anyInt());

        mockIntent = Mockito.mock(Intent.class);
        Mockito.when(mockIntent.getDataString()).thenReturn("this is data string 2");
        mockCallingActivity = Mockito.mock(ComponentName.class);
        Mockito.when(mockCallingActivity.getPackageName()).thenReturn("package name 2");
        Mockito.when(activity.getCallingActivity()).thenReturn(mockCallingActivity);
        Mockito.when(activity.getIntent()).thenReturn(mockIntent);

        manager.onActivityPaused(activity);
        Thread.sleep(1000);

        manager.onActivityResumed(activity);
        Mockito.verify(messageManager, Mockito.times(1))
                .logStateTransition(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.eq("this is data string 2"),
                        Mockito.isNull(String.class),
                        Mockito.eq( "package name 2"),
                        Mockito.anyLong(),
                        Mockito.anyLong(),
                        Mockito.anyInt());
    }

    /**
     * This tests what happens if we're started in something other than the launch Activity.
     *
     * @throws Exception
     */
    @Test
    public void testSecondActivityStart() throws Exception {
        manager.onActivityPaused(activity);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
        manager.onActivityResumed(activity);
        Activity activity2 = Mockito.mock(Activity.class);
        Activity activity3 = Mockito.mock(Activity.class);
        manager.onActivityPaused(activity2);
        manager.onActivityPaused(activity3);
        Thread.sleep(1000);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityPaused(activity);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
    }

    @Test
    public void testOnActivityPaused() throws Exception {
        manager.onActivityResumed(activity);
        assertEquals(false, manager.isBackgrounded());
        manager.onActivityPaused(activity);
        Thread.sleep(1000);
        assertEquals(true, manager.isBackgrounded());
        assertTrue(manager.mInitialized);
        assertTrue(manager.mLastStoppedTime.get() > 0);
        manager.onActivityResumed(activity);
        assertTrue(manager.getSession().getBackgroundTime() + " ms", manager.getSession().getBackgroundTime() >= 1000 && manager.getSession().getBackgroundTime() < 1200);
    }

    @Test
    public void testEndSession() throws Exception {
        manager.startSession();
        manager.endSession();
        assertTrue(manager.getSession().mSessionID.equals("NO-SESSION"));
    }

    @Test
    public void testStartSession() throws Exception {
        InternalSession session = manager.getSession();
        assertTrue(session.mSessionID.equals("NO-SESSION"));
        manager.startSession();
        assertNotEquals(manager.getSession().mSessionID, session.mSessionID);
    }

    @Test
    public void testShouldEndSession() {
        final Mutable<Boolean> isTimedOut = new Mutable(false);
        final Mutable<Boolean> isBackground = new Mutable(false);
        final Mutable<InternalSession> session = new Mutable(new InternalSession() {
            @Override
            public boolean isTimedOut(int sessionTimeout) {
                return isTimedOut.value;
            }
        });
        session.value.mSessionStartTime = 1;

        manager = new AppStateManager(new MockContext(), true) {
            @Override
            public boolean isBackgrounded() {
                return isBackground.value;
            }

            @Override
            public InternalSession getSession() {
                return session.value;
            }
        };

        ConfigManager configManager = Mockito.mock(ConfigManager.class);
        manager.setConfigManager(configManager);
        Mockito.when(MParticle.getInstance().Media().getAudioPlaying()).thenReturn(false);

        isTimedOut.value = true;
        isBackground.value = true;
        assertTrue(manager.shouldEndSession());

        isTimedOut.value = false;
        isBackground.value = true;

        assertFalse(manager.shouldEndSession());

        isTimedOut.value = true;
        isBackground.value = false;

        assertFalse(manager.shouldEndSession());

        isTimedOut.value = true;
        isBackground.value = true;
        assertTrue(manager.shouldEndSession());

        Mockito.when(MParticle.getInstance().Media().getAudioPlaying()).thenReturn(true);

        assertFalse(manager.shouldEndSession());

        MParticle.setInstance(null);
        //Make sure it doesn't crash.
        assertTrue(manager.shouldEndSession());

    }
}