package com.mparticle.internal

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MockMParticle
import com.mparticle.WrapperSdk
import com.mparticle.WrapperSdkVersion
import com.mparticle.commerce.CommerceEvent
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.testutils.RandomUtils
import org.json.JSONArray
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import java.util.Random

@RunWith(PowerMockRunner::class)
class KitFrameworkWrapperTest {
    @Test
    @Throws(Exception::class)
    fun testLoadKitLibrary() {
        val mockConfigManager = Mockito.mock(
            ConfigManager::class.java
        )
        Mockito.`when`(mockConfigManager.latestKitConfiguration).thenReturn(JSONArray())
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            mockConfigManager,
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertFalse(wrapper.kitsLoaded)
        Assert.assertFalse(wrapper.frameworkLoadAttempted)
        wrapper.loadKitLibrary()
        Assert.assertTrue(wrapper.frameworkLoadAttempted)
        Assert.assertTrue(wrapper.kitsLoaded)
    }

    @Test
    @Throws(Exception::class)
    fun testDisableQueuing() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertFalse(wrapper.kitsLoaded)
        wrapper.kitsLoaded = false
        val event = MPEvent.Builder("example").build()
        wrapper.logEvent(event)
        wrapper.setUserAttribute("a key", "a value", 1)
        Assert.assertEquals(event, wrapper.eventQueue.peek())
        Assert.assertEquals("a key", wrapper.attributeQueue.peek()?.key)
        Assert.assertEquals("a value", wrapper.attributeQueue.peek()?.value)
        wrapper.disableQueuing()
        Assert.assertNull(wrapper.eventQueue)
        Assert.assertNull(wrapper.attributeQueue)
    }

    @Test
    @PrepareForTest(CommerceEvent::class)
    @Throws(Exception::class)
    fun testReplayEvents() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Mockito.`when`(wrapper.mCoreCallbacks.getPushInstanceId()).thenReturn("instanceId")
        Mockito.`when`(wrapper.mCoreCallbacks.getPushSenderId()).thenReturn("1234545")
        MParticle.setInstance(MockMParticle())
        wrapper.replayEvents()
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        val registration = PushRegistration("instance id", "1234545")
        Mockito.`when`(
            MParticle.getInstance()!!.Internal().configManager.pushRegistration
        ).thenReturn(registration)
        wrapper.replayEvents()
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).onPushRegistration(Mockito.anyString(), Mockito.anyString())
        wrapper.onPushRegistration("whatever", "whatever")
        wrapper.replayEvents()
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).onPushRegistration(Mockito.anyString(), Mockito.anyString())
        wrapper.replayEvents()
        wrapper.kitsLoaded = false
        val event = MPEvent.Builder("example").build()
        val screenEvent = Mockito.mock(MPEvent::class.java)
        val commerceEvent = PowerMockito.mock(CommerceEvent::class.java)
        Mockito.`when`(screenEvent.isScreenEvent).thenReturn(true)
        wrapper.logEvent(event)
        wrapper.logEvent(screenEvent)
        wrapper.setUserAttribute("a key", "a value", 1)
        wrapper.logEvent(commerceEvent)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logEvent(Mockito.any(MPEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logScreen(Mockito.any(MPEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logEvent(Mockito.any(CommerceEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())
        wrapper.replayEvents()
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logEvent(Mockito.any(MPEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logScreen(Mockito.any(MPEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logEvent(Mockito.any(CommerceEvent::class.java))
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"), Mockito.anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testReplayAndDisableQueue() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        wrapper.kitsLoaded = false
        wrapper.replayAndDisableQueue()
    }

    @Test
    @Throws(Exception::class)
    fun testQueueStringAttribute() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.queueAttributeSet("a key", "a value", 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertEquals(wrapper.attributeQueue.peek()?.value, "a value")
        Assert.assertEquals(
            wrapper.attributeQueue.peek()?.type?.toLong(),
            KitFrameworkWrapper.AttributeChange.SET_ATTRIBUTE.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testQueueNullAttribute() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.queueAttributeTag("a key", 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertNull(wrapper.attributeQueue.peek()?.value)
        Assert.assertEquals(
            wrapper.attributeQueue.peek()?.type?.toLong(),
            KitFrameworkWrapper.AttributeChange.TAG.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testQueueListAttribute() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.queueAttributeSet("a key", ArrayList<String>(), 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertEquals(wrapper.attributeQueue.peek()?.value, ArrayList<String>())
        Assert.assertEquals(
            wrapper.attributeQueue.peek()?.type?.toLong(),
            KitFrameworkWrapper.AttributeChange.SET_ATTRIBUTE.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testQueueAttributeRemoval() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.queueAttributeRemove("a key", 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertEquals(wrapper.attributeQueue.peek()?.value, null)
        Assert.assertEquals(
            wrapper.attributeQueue.peek()?.type?.toLong(),
            KitFrameworkWrapper.AttributeChange.REMOVE_ATTRIBUTE.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testQueueAttributeIncrement() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.queueAttributeIncrement("a key", 3, "3", 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertEquals(wrapper.attributeQueue.peek()?.value, "3")
        Assert.assertEquals(
            wrapper.attributeQueue.peek()?.type?.toLong(),
            KitFrameworkWrapper.AttributeChange.INCREMENT_ATTRIBUTE.toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testQueueEvent() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.eventQueue)
        wrapper.kitsLoaded = false
        val event = Mockito.mock(MPEvent::class.java)
        wrapper.queueEvent(event)
        Assert.assertEquals(wrapper.eventQueue.peek(), event)
        for (i in 0..49) {
            wrapper.queueEvent(event)
        }
        Assert.assertEquals(10, wrapper.eventQueue.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttribute() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.attributeQueue)
        wrapper.kitsLoaded = false
        wrapper.setUserAttribute("a key", "a value", 1)
        Assert.assertEquals(wrapper.attributeQueue.peek()?.key, "a key")
        Assert.assertEquals(wrapper.attributeQueue.peek()?.value, "a value")
        wrapper.kitsLoaded = true
        wrapper.setUserAttribute("a key", "a value", 1)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).setUserAttribute(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())
        wrapper.setUserAttribute("a key", "a value", 1)
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).setUserAttribute(Mockito.eq("a key"), Mockito.eq("a value"), Mockito.eq(1L))
    }

    @Test
    @Throws(Exception::class)
    fun testLogEvent() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.eventQueue)
        wrapper.kitsLoaded = false
        val event = Mockito.mock(MPEvent::class.java)
        wrapper.logEvent(event)
        Assert.assertEquals(wrapper.eventQueue.peek(), event)
        for (i in 0..49) {
            wrapper.logEvent(event)
        }
        Assert.assertEquals(10, wrapper.eventQueue.size.toLong())
        wrapper.kitsLoaded = true
        wrapper.logEvent(event)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logEvent(Mockito.any(MPEvent::class.java))
        wrapper.logEvent(event)
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logEvent(Mockito.any(MPEvent::class.java))
    }

    @Test
    @PrepareForTest(CommerceEvent::class)
    @Throws(Exception::class)
    fun testLogCommerceEvent() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.eventQueue)
        wrapper.kitsLoaded = false
        val event = Mockito.mock(CommerceEvent::class.java)
        wrapper.logEvent(event)
        Assert.assertEquals(wrapper.eventQueue.peek(), event)
        for (i in 0..49) {
            wrapper.logEvent(event)
        }
        Assert.assertEquals(10, wrapper.eventQueue.size.toLong())
        wrapper.kitsLoaded = true
        wrapper.logEvent(event)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logEvent(Mockito.any(CommerceEvent::class.java))
        wrapper.logEvent(event)
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logEvent(Mockito.any(CommerceEvent::class.java))
    }

    @Test
    @PrepareForTest(CommerceEvent::class)
    fun testLogBaseEvent() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.eventQueue)
        wrapper.kitsLoaded = false
        val event = Mockito.mock(BaseEvent::class.java)
        wrapper.logEvent(event)
        Assert.assertEquals(wrapper.eventQueue.peek(), event)
        for (i in 0..49) {
            wrapper.logEvent(event)
        }
        Assert.assertEquals(10, wrapper.eventQueue.size.toLong())
        wrapper.kitsLoaded = true
        wrapper.logEvent(event)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logEvent(Mockito.any(BaseEvent::class.java))
        wrapper.logEvent(event)
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logEvent(Mockito.any(BaseEvent::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testLogScreen() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.eventQueue)
        wrapper.kitsLoaded = false
        val event = Mockito.mock(MPEvent::class.java)
        Mockito.`when`(event.isScreenEvent).thenReturn(true)
        wrapper.logScreen(event)
        Assert.assertEquals(wrapper.eventQueue.peek(), event)
        for (i in 0..49) {
            wrapper.logScreen(event)
        }
        Assert.assertEquals(10, wrapper.eventQueue.size.toLong())
        wrapper.kitsLoaded = true
        wrapper.logScreen(event)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Mockito.verify(
            mockKitManager,
            Mockito.times(0)
        ).logScreen(Mockito.any(MPEvent::class.java))
        wrapper.logScreen(event)
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).logScreen(Mockito.any(MPEvent::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testIsKitActive() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertFalse(wrapper.isKitActive(0))
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        Assert.assertFalse(wrapper.isKitActive(0))
        Mockito.verify(
            mockKitManager,
            Mockito.times(1)
        ).isKitActive(Mockito.anyInt())
        Mockito.`when`(mockKitManager.isKitActive(Mockito.anyInt())).thenReturn(true)
        Assert.assertTrue(wrapper.isKitActive(0))
    }

    @Test
    @Throws(Exception::class)
    fun testGetSupportedKits() {
        val wrapper = KitFrameworkWrapper(
            Mockito.mock(
                Context::class.java
            ),
            Mockito.mock(ReportingManager::class.java),
            Mockito.mock(ConfigManager::class.java),
            Mockito.mock(AppStateManager::class.java),
            true,
            Mockito.mock(MParticleOptions::class.java)
        )
        Assert.assertNull(wrapper.supportedKits)
        val mockKitManager = Mockito.mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)
        val supportedKits: MutableSet<Int> = HashSet()
        supportedKits.add(3)
        Mockito.`when`(mockKitManager.supportedKits).thenReturn(supportedKits)
        Assert.assertEquals(wrapper.supportedKits, supportedKits)
    }

    @Test
    fun testCoreCallbacksImpl() {
        val randomUtils = RandomUtils()
        val ran = Random()
        val mockConfigManager = Mockito.mock(
            ConfigManager::class.java
        )
        val mockAppStateManager = Mockito.mock(
            AppStateManager::class.java
        )
        val mockActivity = Mockito.mock(
            Activity::class.java
        )
        val mockKitConfiguration = JSONArray()
        for (i in 0 until randomUtils.randomInt(1, 10)) {
            mockKitConfiguration.put(
                randomUtils.getAlphaNumericString(randomUtils.randomInt(1, 30))
            )
        }
        val mockLaunchUri = Mockito.mock(Uri::class.java)
        val mockPushInstanceId = randomUtils.getAlphaNumericString(15)
        val mockPushSenderId = randomUtils.getAlphaNumericString(10)
        val mockUserBucket = randomUtils.randomInt(-100, 100)
        val isBackground = ran.nextBoolean()
        val isEnabled = ran.nextBoolean()
        val isPushEnabled = ran.nextBoolean()
        val mockIntegrationAttributes1 = randomUtils.getRandomAttributes(4)
        val mockIntegrationAttributes2 = randomUtils.getRandomAttributes(5)
        Mockito.`when`(mockAppStateManager.launchUri).thenReturn(mockLaunchUri)
        Mockito.`when`(mockAppStateManager.currentActivity).thenReturn(WeakReference(mockActivity))
        Mockito.`when`(mockAppStateManager.isBackgrounded()).thenReturn(isBackground)
        Mockito.`when`(mockConfigManager.latestKitConfiguration).thenReturn(mockKitConfiguration)
        Mockito.`when`(mockConfigManager.pushInstanceId).thenReturn(mockPushInstanceId)
        Mockito.`when`(mockConfigManager.pushSenderId).thenReturn(mockPushSenderId)
        Mockito.`when`(mockConfigManager.userBucket).thenReturn(mockUserBucket)
        Mockito.`when`(mockConfigManager.isEnabled).thenReturn(isEnabled)
        Mockito.`when`(mockConfigManager.isPushEnabled).thenReturn(isPushEnabled)
        Mockito.`when`(mockConfigManager.getIntegrationAttributes(1))
            .thenReturn(mockIntegrationAttributes1)
        Mockito.`when`(mockConfigManager.getIntegrationAttributes(2))
            .thenReturn(mockIntegrationAttributes2)
        val coreCallbacks: CoreCallbacks = KitFrameworkWrapper.CoreCallbacksImpl(
            Mockito.mock(
                KitFrameworkWrapper::class.java
            ),
            mockConfigManager,
            mockAppStateManager
        )
        Assert.assertEquals(mockActivity, coreCallbacks.getCurrentActivity()?.get())
        Assert.assertEquals(mockKitConfiguration, coreCallbacks.getLatestKitConfiguration())
        Assert.assertEquals(mockLaunchUri, coreCallbacks.getLaunchUri())
        Assert.assertEquals(mockPushInstanceId, coreCallbacks.getPushInstanceId())
        Assert.assertEquals(mockPushSenderId, coreCallbacks.getPushSenderId())
        Assert.assertEquals(mockUserBucket.toLong(), coreCallbacks.getUserBucket().toLong())
        Assert.assertEquals(isBackground, coreCallbacks.isBackgrounded())
        Assert.assertEquals(isEnabled, coreCallbacks.isEnabled())
        Assert.assertEquals(isPushEnabled, coreCallbacks.isPushEnabled())
        Assert.assertEquals(mockIntegrationAttributes1, coreCallbacks.getIntegrationAttributes(1))
        Assert.assertEquals(mockIntegrationAttributes2, coreCallbacks.getIntegrationAttributes(2))
    }

    @Test
    fun testSetWrapperSdkVersion_noCalls() {
        val wrapper = KitFrameworkWrapper(
            mock(
                Context::class.java
            ),
            mock(ReportingManager::class.java),
            mock(ConfigManager::class.java),
            mock(AppStateManager::class.java),
            true,
            mock(MParticleOptions::class.java)
        )

        val mockKitManager = mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)

        verify(mockKitManager, times(0)).setWrapperSdkVersion(any())
    }

    @Test
    fun testSetWrapperSdkVersion_kitManagerSet_setWrapperVersionCalled() {
        val wrapper = KitFrameworkWrapper(
            mock(
                Context::class.java
            ),
            mock(ReportingManager::class.java),
            mock(ConfigManager::class.java),
            mock(AppStateManager::class.java),
            true,
            mock(MParticleOptions::class.java)
        )

        val mockKitManager = mock(KitManager::class.java)
        wrapper.setKitManager(mockKitManager)

        val expectedSdkVersion = WrapperSdkVersion(WrapperSdk.WrapperFlutter, "1.0.0")
        wrapper.setWrapperSdkVersion(expectedSdkVersion)

        verify(mockKitManager).setWrapperSdkVersion(expectedSdkVersion)
    }
}
