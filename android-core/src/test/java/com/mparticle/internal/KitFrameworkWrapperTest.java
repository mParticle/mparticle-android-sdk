package com.mparticle.internal;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.mparticle.BaseEvent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.testutils.RandomUtils;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class KitFrameworkWrapperTest {

    private BackgroundTaskHandler mockBackgroundTaskHandler = new BackgroundTaskHandler() {
        @Override
        public void executeNetworkRequest(Runnable runnable) {

        }
    };

    @Test
    public void testLoadKitLibrary() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);

        assertFalse(wrapper.getKitsLoaded());
        assertFalse(wrapper.getFrameworkLoadAttempted());

        wrapper.loadKitLibrary();

        assertTrue(wrapper.getFrameworkLoadAttempted());
        assertTrue(wrapper.getKitsLoaded());

    }

    @Test
    public void testDisableQueuing() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
        true);
        assertFalse(wrapper.getKitsLoaded());
        wrapper.setKitsLoaded(false);
        MPEvent event = new MPEvent.Builder("example").build();

        wrapper.logEvent(event);
        wrapper.setUserAttribute("a key", "a value", 1);
        assertEquals(event, wrapper.getEventQueue().peek());
        assertEquals("a key", wrapper.getAttributeQueue().peek().key);
        assertEquals("a value", wrapper.getAttributeQueue().peek().value);
        wrapper.disableQueuing();
        assertTrue(wrapper.getKitsLoaded());
        assertNull(wrapper.getEventQueue());
        assertNull(wrapper.getAttributeQueue());
    }

    @Test
    @PrepareForTest({CommerceEvent.class})
    public void testReplayEvents() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        Mockito.when(wrapper.mCoreCallbacks.getPushInstanceId()).thenReturn("instanceId");
        Mockito.when(wrapper.mCoreCallbacks.getPushSenderId()).thenReturn("1234545");
        MParticle.setInstance(new MockMParticle());
        wrapper.replayEvents();
        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        PushRegistrationHelper.PushRegistration registration = new PushRegistrationHelper.PushRegistration("instance id", "1234545");
        Mockito.when(
                MParticle.getInstance().Internal().getConfigManager().getPushRegistration()
        ).thenReturn(registration);

        wrapper.replayEvents();
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).onPushRegistration(Mockito.anyString(), Mockito.anyString());

        wrapper.onPushRegistration("whatever", "whatever");
        wrapper.replayEvents();

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).onPushRegistration(Mockito.anyString(), Mockito.anyString());

        wrapper.replayEvents();

        wrapper.setKitsLoaded(false);
        MPEvent event = new MPEvent.Builder("example").build();
        MPEvent screenEvent = Mockito.mock(MPEvent.class);
        CommerceEvent commerceEvent = PowerMockito.mock(CommerceEvent.class);
        Mockito.when(screenEvent.isScreenEvent()).thenReturn(true);
        wrapper.logEvent(event);
        wrapper.logEvent(screenEvent);
        wrapper.setUserAttribute("a key", "a value", 1);
        wrapper.logEvent(commerceEvent);
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logScreen(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(CommerceEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong());

        wrapper.replayEvents();

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logScreen(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(CommerceEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"), Mockito.anyLong());
    }

    @Test
    public void testReplayAndDisableQueue() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        wrapper.setKitsLoaded(false);
        wrapper.replayAndDisableQueue();
        assertTrue(wrapper.getKitsLoaded());
    }

    @Test
    public void testQueueStringAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttributeSet("a key", "a value", 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, "a value");
        assertEquals(wrapper.getAttributeQueue().peek().type, KitFrameworkWrapper.AttributeChange.SET_ATTRIBUTE);
    }

    @Test
    public void testQueueNullAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttributeTag("a key", 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertNull(wrapper.getAttributeQueue().peek().value);
        assertEquals(wrapper.getAttributeQueue().peek().type, KitFrameworkWrapper.AttributeChange.TAG);
    }

    @Test
    public void testQueueListAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttributeSet("a key", new ArrayList<String>(), 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, new ArrayList<String>());
        assertEquals(wrapper.getAttributeQueue().peek().type, KitFrameworkWrapper.AttributeChange.SET_ATTRIBUTE);
    }

    @Test
    public void testQueueAttributeRemoval() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttributeRemove("a key", 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, null);
        assertEquals(wrapper.getAttributeQueue().peek().type, KitFrameworkWrapper.AttributeChange.REMOVE_ATTRIBUTE);
    }

    @Test
    public void testQueueAttributeIncrement() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttributeIncrement("a key", 3, "3", 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, "3");
        assertEquals(wrapper.getAttributeQueue().peek().type, KitFrameworkWrapper.AttributeChange.INCREMENT_ATTRIBUTE);
    }

    @Test
    public void testQueueEvent() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        MPEvent event = Mockito.mock(MPEvent.class);
        wrapper.queueEvent(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.queueEvent(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());
    }

    @Test
    public void testSetUserAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);

        wrapper.setUserAttribute("a key", "a value", 1);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, "a value");

        wrapper.setKitsLoaded(true);
        wrapper.setUserAttribute("a key", "a value", 1);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong());

        wrapper.setUserAttribute("a key", "a value", 1);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"), Mockito.eq(1L));
    }

    @Test
    public void testLogEvent() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        MPEvent event = Mockito.mock(MPEvent.class);
        wrapper.logEvent(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.logEvent(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());

        wrapper.setKitsLoaded(true);

        wrapper.logEvent(event);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(MPEvent.class));

        wrapper.logEvent(event);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(MPEvent.class));
    }

    @Test
    @PrepareForTest({CommerceEvent.class})
    public void testLogCommerceEvent() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        CommerceEvent event = Mockito.mock(CommerceEvent.class);
        wrapper.logEvent(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.logEvent(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());

        wrapper.setKitsLoaded(true);

        wrapper.logEvent(event);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(CommerceEvent.class));

        wrapper.logEvent(event);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(CommerceEvent.class));
    }

    @Test
    @PrepareForTest({CommerceEvent.class})
    public void testLogBaseEvent() {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        BaseEvent event = Mockito.mock(BaseEvent.class);
        wrapper.logEvent(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.logEvent(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());

        wrapper.setKitsLoaded(true);

        wrapper.logEvent(event);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(BaseEvent.class));

        wrapper.logEvent(event);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(BaseEvent.class));
    }

    @Test
    public void testLogScreen() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        MPEvent event = Mockito.mock(MPEvent.class);
        Mockito.when(event.isScreenEvent()).thenReturn(true);
        wrapper.logScreen(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.logScreen(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());

        wrapper.setKitsLoaded(true);

        wrapper.logScreen(event);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logScreen(Mockito.any(MPEvent.class));

        wrapper.logScreen(event);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logScreen(Mockito.any(MPEvent.class));
    }

    @Test
    public void testIsKitActive() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertFalse(wrapper.isKitActive(0));

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        assertFalse(wrapper.isKitActive(0));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).isKitActive(Mockito.anyInt());

        Mockito.when(mockKitManager.isKitActive(Mockito.anyInt())).thenReturn(true);
        assertTrue(wrapper.isKitActive(0));
    }

    @Test
    public void testGetSupportedKits() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                mockBackgroundTaskHandler,
                true);
        assertNull(wrapper.getSupportedKits());

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        Set<Integer> supportedKits = new HashSet<Integer>();
        supportedKits.add(3);
        Mockito.when(mockKitManager.getSupportedKits()).thenReturn(supportedKits);
        assertEquals(wrapper.getSupportedKits(), supportedKits);
    }

    @Test
    public void testCoreCallbacksImpl() {
        RandomUtils randomUtils = new RandomUtils();
        Random ran = new Random();

        ConfigManager mockConfigManager = Mockito.mock(ConfigManager.class);
        AppStateManager mockAppStateManager = Mockito.mock(AppStateManager.class);
        Activity mockActivity = Mockito.mock(Activity.class);
        JSONArray mockKitConfiguration = new JSONArray();
        for (int i = 0; i < randomUtils.randomInt(1, 10); i++) {
            mockKitConfiguration.put(randomUtils.getAlphaNumericString(randomUtils.randomInt(1, 30)));
        }
        Uri mockLaunchUri = Mockito.mock(Uri.class);
        String mockPushInstanceId = randomUtils.getAlphaNumericString(15);
        String mockPushSenderId = randomUtils.getAlphaNumericString(10);
        int mockUserBucket = randomUtils.randomInt(-100, 100);
        boolean isBackground = ran.nextBoolean();
        boolean isEnabled = ran.nextBoolean();
        boolean isPushEnabled = ran.nextBoolean();
        Map<String, String> mockIntegrationAttributes1 = randomUtils.getRandomAttributes(4);
        Map<String, String> mockIntegrationAttributes2 = randomUtils.getRandomAttributes(5);

        Mockito.when(mockAppStateManager.getLaunchUri()).thenReturn(mockLaunchUri);
        Mockito.when(mockAppStateManager.getCurrentActivity()).thenReturn(new WeakReference<Activity>(mockActivity));
        Mockito.when(mockAppStateManager.isBackgrounded()).thenReturn(isBackground);
        Mockito.when(mockConfigManager.getLatestKitConfiguration()).thenReturn(mockKitConfiguration);
        Mockito.when(mockConfigManager.getPushInstanceId()).thenReturn(mockPushInstanceId);
        Mockito.when(mockConfigManager.getPushSenderId()).thenReturn(mockPushSenderId);
        Mockito.when(mockConfigManager.getUserBucket()).thenReturn(mockUserBucket);
        Mockito.when(mockConfigManager.isEnabled()).thenReturn(isEnabled);
        Mockito.when(mockConfigManager.isPushEnabled()).thenReturn(isPushEnabled);
        Mockito.when(mockConfigManager.getIntegrationAttributes(1)).thenReturn(mockIntegrationAttributes1);
        Mockito.when(mockConfigManager.getIntegrationAttributes(2)).thenReturn(mockIntegrationAttributes2);

        CoreCallbacks coreCallbacks = new KitFrameworkWrapper.CoreCallbacksImpl(Mockito.mock(KitFrameworkWrapper.class), mockConfigManager, mockAppStateManager);

        assertEquals(mockActivity, coreCallbacks.getCurrentActivity().get());
        assertEquals(mockKitConfiguration, coreCallbacks.getLatestKitConfiguration());
        assertEquals(mockLaunchUri, coreCallbacks.getLaunchUri());
        assertEquals(mockPushInstanceId, coreCallbacks.getPushInstanceId());
        assertEquals(mockPushSenderId, coreCallbacks.getPushSenderId());
        assertEquals(mockUserBucket, coreCallbacks.getUserBucket());
        assertEquals(isBackground, coreCallbacks.isBackgrounded());
        assertEquals(isEnabled, coreCallbacks.isEnabled());
        assertEquals(isPushEnabled, coreCallbacks.isPushEnabled());
        assertEquals(mockIntegrationAttributes1, coreCallbacks.getIntegrationAttributes(1));
        assertEquals(mockIntegrationAttributes2, coreCallbacks.getIntegrationAttributes(2));
    }
}