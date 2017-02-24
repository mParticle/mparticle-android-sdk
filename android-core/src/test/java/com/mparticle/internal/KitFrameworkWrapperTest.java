package com.mparticle.internal;

import android.content.Context;

import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.messaging.PushAnalyticsReceiver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class KitFrameworkWrapperTest {

    @Test
    public void testLoadKitLibrary() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                true);
        assertFalse(wrapper.getKitsLoaded());
        wrapper.setKitsLoaded(false);
        MPEvent event = new MPEvent.Builder("example").build();

        wrapper.logEvent(event);
        wrapper.setUserAttribute("a key", "a value");
        assertEquals(event, wrapper.getEventQueue().peek());
        assertEquals("a key", wrapper.getAttributeQueue().peek().key);
        assertEquals("a value", wrapper.getAttributeQueue().peek().value);
        wrapper.disableQueuing();
        assertTrue(wrapper.getKitsLoaded());
        assertNull(wrapper.getEventQueue());
        assertNull(wrapper.getAttributeQueue());
    }

    @Test
    @PrepareForTest({PushRegistrationHelper.class, CommerceEvent.class})
    public void testReplayEvents() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                true);
        wrapper.replayEvents();
        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        PowerMockito.mockStatic(PushRegistrationHelper.class);
        PushRegistrationHelper.PushRegistration registration = new PushRegistrationHelper.PushRegistration();
        registration.instanceId = "instance id";
        registration.senderId = "1234545";
        Mockito.when(
                PushRegistrationHelper.getLatestPushRegistration(Mockito.any(Context.class))
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

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).checkForDeepLink();

        wrapper.checkForDeepLink();
        wrapper.replayEvents();

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).checkForDeepLink();

        wrapper.setKitsLoaded(false);
        MPEvent event = new MPEvent.Builder("example").build();
        MPEvent screenEvent = Mockito.mock(MPEvent.class);
        CommerceEvent commerceEvent = PowerMockito.mock(CommerceEvent.class);
        Mockito.when(screenEvent.isScreenEvent()).thenReturn(true);
        wrapper.logEvent(event);
        wrapper.logEvent(screenEvent);
        wrapper.setUserAttribute("a key", "a value");
        wrapper.logCommerceEvent(commerceEvent);
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logEvent(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logScreen(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logCommerceEvent(Mockito.any(CommerceEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString());

        wrapper.replayEvents();

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logEvent(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logScreen(Mockito.any(MPEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logCommerceEvent(Mockito.any(CommerceEvent.class));
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"));
    }

    @Test
    public void testReplayAndDisableQueue() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttribute("a key", "a value");
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, "a value");
        assertFalse(wrapper.getAttributeQueue().peek().removal);
    }

    @Test
    public void testQueueNullAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttribute("a key", null);
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertNull(wrapper.getAttributeQueue().peek().value);
        assertFalse(wrapper.getAttributeQueue().peek().removal);
    }

    @Test
    public void testQueueListAttribute() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttribute("a key", new ArrayList<String>());
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, new ArrayList<String>());
        assertFalse(wrapper.getAttributeQueue().peek().removal);
    }

    @Test
    public void testQueueAttributeRemoval() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);
        wrapper.queueAttribute("a key");
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, null);
        assertTrue(wrapper.getAttributeQueue().peek().removal);
    }

    @Test
    public void testQueueEvent() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                true);
        assertNull(wrapper.getAttributeQueue());
        wrapper.setKitsLoaded(false);

        wrapper.setUserAttribute("a key", "a value");
        assertEquals(wrapper.getAttributeQueue().peek().key, "a key");
        assertEquals(wrapper.getAttributeQueue().peek().value, "a value");

        wrapper.setKitsLoaded(true);
        wrapper.setUserAttribute("a key", "a value");

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString());

        wrapper.setUserAttribute("a key", "a value");

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"));
    }

    @Test
    public void testLogEvent() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                true);
        assertNull(wrapper.getEventQueue());
        wrapper.setKitsLoaded(false);
        CommerceEvent event = Mockito.mock(CommerceEvent.class);
        wrapper.logCommerceEvent(event);
        assertEquals(wrapper.getEventQueue().peek(), event);

        for (int i = 0 ; i < 50; i++) {
            wrapper.logCommerceEvent(event);
        }
        assertEquals(10, wrapper.getEventQueue().size());

        wrapper.setKitsLoaded(true);

        wrapper.logCommerceEvent(event);

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);

        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).logCommerceEvent(Mockito.any(CommerceEvent.class));

        wrapper.logCommerceEvent(event);

        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).logCommerceEvent(Mockito.any(CommerceEvent.class));
    }

    @Test
    public void testLogScreen() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
    public void testCheckForDeepLink() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                true);
        assertFalse(wrapper.getShouldCheckForDeepLink());
        wrapper.setKitsLoaded(false);
        wrapper.checkForDeepLink();
        assertTrue(wrapper.getShouldCheckForDeepLink());
        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        wrapper.checkForDeepLink();
        Mockito.verify(
                mockKitManager, Mockito.times(0)
        ).checkForDeepLink();

        wrapper.setKitsLoaded(true);

        wrapper.checkForDeepLink();
        Mockito.verify(
                mockKitManager, Mockito.times(1)
        ).checkForDeepLink();

    }

    @Test
    public void testIsKitActive() throws Exception {
        KitFrameworkWrapper wrapper = new KitFrameworkWrapper(Mockito.mock(Context.class),
                Mockito.mock(ReportingManager.class),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
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
                true);
        assertNull(wrapper.getSupportedKits());

        KitManager mockKitManager = Mockito.mock(KitManager.class);
        wrapper.setKitManager(mockKitManager);
        Set<Integer> supportedKits = new HashSet<Integer>();
        supportedKits.add(3);
        Mockito.when(mockKitManager.getSupportedKits()).thenReturn(supportedKits);
        assertEquals(wrapper.getSupportedKits(), supportedKits);
    }
}