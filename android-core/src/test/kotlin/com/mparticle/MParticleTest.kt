package com.mparticle

import android.graphics.Typeface
import android.os.Looper
import android.os.SystemClock
import android.webkit.WebView
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.AppStateManager
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.KitsLoadedCallback
import com.mparticle.internal.MParticleJSInterface
import com.mparticle.internal.MessageManager
import com.mparticle.media.MPMediaAPI
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.mock.MockContext
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.RandomUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class, SystemClock::class)
class MParticleTest {
    private lateinit var executor: ExecutorService

    @Before
    fun setup() {
        PowerMockito.mockStatic(Looper::class.java)
        val looper: Looper = Mockito.mock(Looper::class.java)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)

        // Mock SystemClock's static method
        PowerMockito.mockStatic(SystemClock::class.java)
        Mockito.`when`(SystemClock.elapsedRealtime()).thenReturn(123456789L)
        executor = Executors.newSingleThreadExecutor()
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttribute() {
        val mp: MParticle = InnerMockMParticle()
        val mockSession = Mockito.mock(
            InternalSession::class.java
        )
        Mockito.`when`(mp.mInternal.appStateManager.session)
            .thenReturn(mockSession)
        Mockito.`when`(mp.mInternal.configManager.isEnabled).thenReturn(true)
        Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(1L)
        MParticle.setInstance(mp)
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        Assert.assertFalse(mp.Identity().currentUser!!.setUserAttribute("", "test"))
        Assert.assertFalse(mp.Identity().currentUser!!.setUserAttribute("", ""))
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttribute(String(CharArray(257)), "")
        )
        val listCaptor = ArgumentCaptor.forClass(
            MutableList::class.java
        )
        val stringCaptor = ArgumentCaptor.forClass(
            String::class.java
        )
        val longCaptor = ArgumentCaptor.forClass(
            Long::class.java
        )
        val legalString = String(CharArray(Constants.LIMIT_ATTR_KEY))
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute(legalString, "")
        )
        Mockito.verify(mp.mMessageManager, Mockito.times(1))
            .setUserAttribute(legalString, "", 1, false)
        val integerList: MutableList<Int> = LinkedList()
        integerList.add(203948)
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute("test2", integerList)
        )
        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(
            stringCaptor.capture(),
            listCaptor.capture(),
            longCaptor.capture(),
            ArgumentMatchers.eq(false)
        )
        Assert.assertTrue(stringCaptor.value == "test2")
        var capturedStringList = listCaptor.value
        Assert.assertTrue(capturedStringList.size == 1)
        Assert.assertTrue(capturedStringList[0] == "203948")
        Assert.assertTrue(longCaptor.value == 1L)
        val longStringList: MutableList<String> = ArrayList()
        for (i in 0 until Constants.LIMIT_ATTR_VALUE) {
            longStringList.add("a")
        }
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute("test3", longStringList)
        )
        Mockito.verify(mp.mMessageManager, Mockito.times(3)).setUserAttribute(
            stringCaptor.capture(),
            listCaptor.capture(),
            longCaptor.capture(),
            ArgumentMatchers.eq(false)
        )
        Assert.assertTrue(stringCaptor.value == "test3")
        Assert.assertTrue(longCaptor.value == 1L)
        capturedStringList = listCaptor.value
        Assert.assertTrue(capturedStringList == longStringList)
        longStringList.add("too much!")
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttribute("test", longStringList)
        )
        val stringList: MutableList<String> = LinkedList()
        stringList.add(String(CharArray(Constants.LIMIT_ATTR_VALUE)))
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute("test", stringList)
        )
        stringList.add(String(CharArray(Constants.LIMIT_ATTR_VALUE + 1)))
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttribute("test", stringList)
        )
        Assert.assertTrue(mp.Identity().currentUser!!.setUserAttribute("test", ""))
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute(
                "test",
                String(
                    CharArray(4096)
                )
            )
        )
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttribute("test", String(CharArray(4097)))
        )
        Assert.assertTrue(mp.Identity().currentUser!!.setUserAttribute("test", 1212))
    }

    @Test
    fun testSettingValidSdkWrapper() {
        val mp: MParticle = InnerMockMParticle()
        mp.setWrapperSdk(WrapperSdk.WrapperFlutter, "test")
        with(mp.wrapperSdkVersion) {
            Assert.assertEquals("test", this.version)
            Assert.assertEquals(WrapperSdk.WrapperFlutter, this.sdk)
        }
    }

    @Test
    fun testNoSettingWrapperWithEmptyVersion() {
        val mp: MParticle = InnerMockMParticle()
        mp.setWrapperSdk(WrapperSdk.WrapperFlutter, "")
        with(mp.wrapperSdkVersion) {
            Assert.assertNull(this.version)
            Assert.assertEquals(WrapperSdk.WrapperNone, this.sdk)
        }
    }

    @Test
    fun testNotSeetingSdkWrapperSecondTime() {
        val mp: MParticle = InnerMockMParticle()
        mp.setWrapperSdk(WrapperSdk.WrapperFlutter, "test")
        with(mp.wrapperSdkVersion) {
            Assert.assertEquals("test", this.version)
            Assert.assertEquals(WrapperSdk.WrapperFlutter, this.sdk)
        }
        mp.setWrapperSdk(WrapperSdk.WrapperXamarin, "test2")
        with(mp.wrapperSdkVersion) {
            Assert.assertEquals("test", this.version)
            Assert.assertEquals(WrapperSdk.WrapperFlutter, this.sdk)
        }
    }

    @Test
    fun testGettingSdkWrapperWithoutSettingValues() {
        val mp: MParticle = InnerMockMParticle()
        with(mp.wrapperSdkVersion) {
            Assert.assertNotNull(this)
            Assert.assertNull(this.version)
            Assert.assertEquals(WrapperSdk.WrapperNone, this.sdk)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserAttributeList() {
        val mp: MParticle = InnerMockMParticle()
        val mockSession = Mockito.mock(
            InternalSession::class.java
        )
        Mockito.`when`(mp.mInternal.appStateManager.session).thenReturn(mockSession)
        Mockito.`when`(mp.mInternal.configManager.isEnabled).thenReturn(true)
        Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(2L)
        MParticle.setInstance(mp)
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttributeList("", "")
        )
        Assert.assertFalse(
            mp.Identity().currentUser!!
                .setUserAttributeList(String(CharArray(257)), "")
        )
        val listCaptor = ArgumentCaptor.forClass(
            MutableList::class.java
        )
        val stringCaptor = ArgumentCaptor.forClass(
            String::class.java
        )
        val longCaptor = ArgumentCaptor.forClass(
            Long::class.java
        )
        val integerList: MutableList<Int> = LinkedList()
        integerList.add(203948)
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttribute("test2", integerList)
        )
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).setUserAttribute(
            stringCaptor.capture(),
            listCaptor.capture(),
            longCaptor.capture(),
            ArgumentMatchers.eq(false)
        )
        Assert.assertTrue(stringCaptor.value == "test2")
        var capturedStringList = listCaptor.value
        Assert.assertTrue(capturedStringList.size == 1)
        Assert.assertTrue(capturedStringList[0] == "203948")
        Assert.assertTrue(longCaptor.value == 2L)
        val longStringList: MutableList<String> = ArrayList()
        for (i in 0 until Constants.LIMIT_ATTR_VALUE) {
            longStringList.add("a")
        }
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttributeList("test3", longStringList)
        )
        Mockito.verify(mp.mMessageManager, Mockito.times(2)).setUserAttribute(
            stringCaptor.capture(),
            listCaptor.capture(),
            longCaptor.capture(),
            ArgumentMatchers.eq(false)
        )
        Assert.assertTrue(stringCaptor.value == "test3")
        capturedStringList = listCaptor.value
        Assert.assertTrue(capturedStringList == longStringList)
        longStringList.add("too much!")
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttributeList("test", longStringList)
        )
        Assert.assertTrue(longCaptor.value == 2L)
        val stringList: MutableList<String> = LinkedList()
        stringList.add(String(CharArray(Constants.LIMIT_ATTR_VALUE)))
        Assert.assertTrue(
            mp.Identity().currentUser!!.setUserAttributeList("test", stringList)
        )
        stringList.add(String(CharArray(Constants.LIMIT_ATTR_VALUE + 1)))
        Assert.assertFalse(
            mp.Identity().currentUser!!.setUserAttributeList("test", stringList)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIncrementUserAttribute() {
        MParticle.setInstance(InnerMockMParticle())
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        val mp = MParticle.getInstance()
        if (mp != null) {
            Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(12L)
            Assert.assertTrue(
                mp.Identity().currentUser!!.incrementUserAttribute("test", 3)
            )
            Mockito.verify(mp.mMessageManager, Mockito.times(1))
                .incrementUserAttribute("test", 3, 12)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserTag() {
        val mp: MParticle = InnerMockMParticle()
        Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(1L)
        val mockSession = Mockito.mock(
            InternalSession::class.java
        )
        Mockito.`when`(mp.mInternal.appStateManager.session).thenReturn(mockSession)
        Mockito.`when`(mp.mInternal.configManager.isEnabled).thenReturn(true)
        MParticle.setInstance(mp)
        Assert.assertFalse(mp.Identity().currentUser!!.setUserTag(""))
        Assert.assertFalse(mp.Identity().currentUser!!.setUserTag(""))
        Assert.assertTrue(mp.Identity().currentUser!!.setUserTag("blah"))
        Mockito.verify(mp.mMessageManager, Mockito.times(1))
            .setUserAttribute("blah", null, 1, false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserAttributes() {
        MParticle.setInstance(InnerMockMParticle())
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        val mp = MParticle.getInstance()
        if (mp != null) {
            Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(1L)
        }
        mp!!.Identity().currentUser!!.userAttributes
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserAttributeLists() {
        MParticle.setInstance(InnerMockMParticle())
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        val mp = MParticle.getInstance()
        if (mp != null) {
            Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(1L)
        }
        mp!!.Identity().currentUser!!.userAttributes
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllUserAttributes() {
        MParticle.setInstance(InnerMockMParticle())
        MParticle.start(MParticleOptions.builder(MockContext()).build())
        val mp = MParticle.getInstance()
        if (mp != null) {
            Mockito.`when`(mp.mInternal.configManager.mpid).thenReturn(1L)
        }
        mp!!.Identity().currentUser!!.userAttributes
        Mockito.verify(mp.mMessageManager, Mockito.times(1)).getUserAttributes(null, 1L)
    }

    @Test
    @Throws(Exception::class)
    fun testAttributeListener() {
        MParticle.setInstance(InnerMockMParticle())
    }

    @Test
    @Throws(Exception::class)
    fun testSetGetImei() {
        MParticle.setDeviceImei(null)
        Assert.assertNull(MParticle.getDeviceImei())
        MParticle.setDeviceImei("foo imei")
        Assert.assertEquals("foo imei", MParticle.getDeviceImei())
        MParticle.setDeviceImei(null)
        Assert.assertNull(MParticle.getDeviceImei())
    }

    @Test
    fun testAddWebView() {
        val mp: MParticle = InnerMockMParticle()
        MParticle.setInstance(mp)
        val ran = RandomUtils()
        val values = arrayOf(
            "",
            "123",
            ran.getAlphaNumericString(5),
            ran.getAlphaNumericString(20),
            ran.getAlphaNumericString(100)
        )

        // test that we apply the token stored in the ConfigManager
        for (value in values) {
            Mockito.`when`(mp.mInternal.configManager.workspaceToken)
                .thenReturn(value)
            val called = AndroidUtils.Mutable(false)
            val webView: WebView = object : WebView(MockContext()) {
                override fun addJavascriptInterface(`object`: Any, name: String) {
                    Assert.assertEquals(
                        MParticleJSInterface.INTERFACE_BASE_NAME + "_" + value + "_v2",
                        name
                    )
                    called.value = true
                }
            }
            mp.registerWebView(webView)
            Assert.assertTrue(called.value)
        }

        // Test that we override the token stored in the ConfigManager, if the Client provides a token.
        for (value in values) {
            Mockito.`when`(mp.mInternal.configManager.workspaceToken)
                .thenReturn(value)
            val called = AndroidUtils.Mutable(false)
            val webView: WebView = object : WebView(MockContext()) {
                override fun addJavascriptInterface(`object`: Any, name: String) {
                    Assert.assertEquals(
                        MParticleJSInterface.INTERFACE_BASE_NAME + "_" + "hardcode" + "_v2",
                        name
                    )
                    called.value = true
                }
            }
            mp.registerWebView(webView, "hardcode")
            Assert.assertTrue(called.value)
        }
    }

    @Test
    fun testDeferPushRegistrationModifyRequest() {
        val instance: MParticle = InnerMockMParticle()
        instance.mIdentityApi = Mockito.mock(IdentityApi::class.java)
        Mockito.`when`(instance.Identity().currentUser).thenReturn(null)
        Mockito.`when`(
            instance.Identity().modify(
                Mockito.any(
                    IdentityApiRequest::class.java
                )
            )
        ).thenThrow(RuntimeException("Unexpected Modify Request"))
        MParticle.setInstance(instance)
        MParticle.getInstance()!!.logPushRegistration("instanceId", "senderId")
        Mockito.`when`(instance.Identity().currentUser).thenReturn(
            Mockito.mock(
                MParticleUser::class.java
            )
        )
        var ex: Exception? = null
        try {
            MParticle.getInstance()!!.logPushRegistration("instanceId", "senderId")
        } catch (e: Exception) {
            ex = e
        }
        Assert.assertEquals("Unexpected Modify Request", ex!!.message)
    }

    @Test
    fun testLogBaseEvent() {
        var instance: MParticle = InnerMockMParticle()
        Mockito.`when`(instance.mConfigManager.isEnabled).thenReturn(true)
        instance.logEvent(Mockito.mock(BaseEvent::class.java))
        Mockito.verify(instance.mKitManager, Mockito.times(1)).logEvent(
            Mockito.any(
                BaseEvent::class.java
            )
        )
        instance = InnerMockMParticle()
        Mockito.`when`(instance.mConfigManager.isEnabled).thenReturn(false)
        instance.logEvent(Mockito.mock(BaseEvent::class.java))
        instance.logEvent(Mockito.mock(MPEvent::class.java))
        Mockito.verify(instance.mKitManager, Mockito.times(0)).logEvent(
            Mockito.any(
                BaseEvent::class.java
            )
        )
    }

    @Test
    fun testIdentityTypeParsing() {
        for (identityType in MParticle.IdentityType.values()) {
            Assert.assertEquals(identityType, MParticle.IdentityType.parseInt(identityType.value))
        }
    }

    @Test
    fun testSelectPlacements_withFullParams_whenEnabled() {
        var instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled).thenReturn(true)

        val attributes = mutableMapOf<String, String>()
        attributes["key"] = "value"

        val placeholders: Map<String, WeakReference<RoktEmbeddedView>> = HashMap()
        val fonts: Map<String, WeakReference<Typeface>> = HashMap()

        val config = RoktConfig.Builder().colorMode(RoktConfig.ColorMode.DARK).build()

        val callbacks = object : MParticle.MpRoktEventCallback {
            override fun onLoad() {
                println("View loaded")
            }

            override fun onUnload(reason: MParticle.UnloadReasons) {
                println("View unloaded due to: $reason")
            }

            override fun onShouldShowLoadingIndicator() {
                println("Show loading indicator")
            }

            override fun onShouldHideLoadingIndicator() {
                println("Hide loading indicator")
            }
        }
        instance.rokt!!.selectPlacements("testView", attributes, callbacks, placeholders, fonts, config)

        verify(instance.mKitManager)?.execute("testView", attributes, callbacks, placeholders, fonts, config)
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenEnabled() {
        var instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(true)

        val attributes = mutableMapOf<String, String>()
        attributes.put("a", "b")

        instance.rokt.selectPlacements("basicView", attributes)

        verify(instance.mKitManager).execute("basicView", attributes, null, null, null, null)
    }

    @Test
    fun testSelectPlacements_withBasicParams_whenDisabled() {
        var instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(false)

        instance.rokt.selectPlacements("basicView", HashMap())

        verify(instance.mKitManager, never()).execute(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun testRoktSetWrapperSdk_whenEnabled() {
        val instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(true)

        val expectedSdk = WrapperSdk.WrapperFlutter
        val expectedVersion = "1.0.0"

        instance.setWrapperSdk(expectedSdk, expectedVersion)

        verify(instance.mKitManager).setWrapperSdkVersion(WrapperSdkVersion(expectedSdk, expectedVersion))
    }

    @Test
    fun testRoktSetWrapperSdk_whenDisabled_kitManagerNotCalled() {
        val instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(false)

        instance.rokt.selectPlacements("basicView", HashMap())

        verify(instance.mKitManager, never()).setWrapperSdkVersion(any())
    }

    @Test
    fun testReportConversion_withBasicParams_whenEnabled() {
        var instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(true)

        val attributes = mutableMapOf<String, String>()
        attributes.put("a", "b")

        instance.rokt.purchaseFinalized("132", "1111", true)

        verify(instance.mKitManager).purchaseFinalized("132", "1111", true)
    }

    @Test
    fun testReportConversion_withBasicParams_whenDisabled() {
        var instance: MParticle = InnerMockMParticle()
        MParticle.setInstance(instance)
        `when`(instance.mConfigManager.isEnabled()).thenReturn(false)

        instance.rokt.purchaseFinalized("132", "1111", true)

        verify(instance.mKitManager, never()).purchaseFinalized("132", "1111", true)
    }

    inner class InnerMockMParticle : MParticle() {
        init {
            mConfigManager = ConfigManager(MockContext())
            mKitManager = Mockito.mock(KitFrameworkWrapper::class.java)
            val realAppStateManager = AppStateManager(MockContext())
            mAppStateManager = Mockito.spy(realAppStateManager)
            mConfigManager = Mockito.mock(ConfigManager::class.java)
            mKitManager = Mockito.mock(KitFrameworkWrapper::class.java)
            mMessageManager = Mockito.mock(MessageManager::class.java)
            mMessaging = Mockito.mock(MPMessagingAPI::class.java)
            mMedia = Mockito.mock(MPMediaAPI::class.java)
            mIdentityApi = IdentityApi(
                MockContext(),
                mAppStateManager,
                mMessageManager,
                mInternal.configManager,
                mKitManager,
                OperatingSystem.ANDROID
            )
            Mockito.`when`(mKitManager.updateKits(Mockito.any())).thenReturn(KitsLoadedCallback())
            val event = MPEvent.Builder("this")
                .customAttributes(HashMap<String, String?>())
                .build()
            val attributes = event.customAttributes
        }
    }
}
