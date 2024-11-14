package com.mparticle.internal

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.mock.MockApplication
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockSharedPreferences
import com.mparticle.testutils.AndroidUtils
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class)
class AppStateManagerTest {
    lateinit var manager: AppStateManager
    private var mockContext: MockApplication? = null
    private val activity = Mockito.mock(
        Activity::class.java
    )
    private var prefs: MockSharedPreferences? = null
    private var messageManager: MessageManager? = null

    @Before
    fun setup() {
        val context = MockContext()
        mockContext = context.applicationContext as MockApplication
        // Prepare and mock the Looper class
        PowerMockito.mockStatic(Looper::class.java)
        val looper: Looper = Mockito.mock(Looper::class.java)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)
        manager = AppStateManager(mockContext!!, true)
        prefs = mockContext?.getSharedPreferences(null, 0) as MockSharedPreferences
        val configManager = Mockito.mock(ConfigManager::class.java)
        manager.setConfigManager(configManager)
        Mockito.`when`(configManager.isEnabled).thenReturn(true)
        messageManager = Mockito.mock(MessageManager::class.java)
        manager.setMessageManager(messageManager)
        val mp: MParticle = MockMParticle()
        MParticle.setInstance(mp)
        manager.delayedBackgroundCheckHandler = Mockito.mock(Handler::class.java)
        AppStateManager.mInitialized = false
    }

    @Test
    @Throws(Exception::class)
    fun testInit() {
        manager.init(10)
        Assert.assertNull(mockContext?.mCallbacks)
        manager.init(14)
        Assert.assertNotNull(mockContext?.mCallbacks)
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityStarted() {
        Assert.assertEquals(true, manager.isBackgrounded())
        manager.onActivityStarted(activity)
        Mockito.verify(MParticle.getInstance()!!.Internal().kitManager, Mockito.times(1))
            .onActivityStarted(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResumed() {
        Assert.assertEquals(true, manager.isBackgrounded())
        manager.onActivityResumed(activity)
        Assert.assertTrue(AppStateManager.mInitialized)
        Assert.assertEquals(false, manager.isBackgrounded())
        manager.onActivityResumed(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testIntentParameterParsing() {
        var mockIntent = Mockito.mock(Intent::class.java)
        Mockito.`when`(mockIntent.dataString).thenReturn("this is data string 1")
        var mockCallingActivity = Mockito.mock(
            ComponentName::class.java
        )
        Mockito.`when`(mockCallingActivity.packageName).thenReturn("package name 1")
        Mockito.`when`(activity.callingActivity).thenReturn(mockCallingActivity)
        Mockito.`when`(activity.intent).thenReturn(mockIntent)
        manager.onActivityResumed(activity)
        Mockito.verify(messageManager, Mockito.times(1))
            ?.logStateTransition(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("this is data string 1"),
                Mockito.isNull(String::class.java),
                Mockito.eq("package name 1"),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyInt()
            )
        mockIntent = Mockito.mock(Intent::class.java)
        Mockito.`when`(mockIntent.dataString).thenReturn("this is data string 2")
        mockCallingActivity = Mockito.mock(ComponentName::class.java)
        Mockito.`when`(mockCallingActivity.packageName).thenReturn("package name 2")
        Mockito.`when`(activity.callingActivity).thenReturn(mockCallingActivity)
        Mockito.`when`(activity.intent).thenReturn(mockIntent)
        manager.onActivityPaused(activity)
        Thread.sleep(1000)
        manager.onActivityResumed(activity)
        Mockito.verify(messageManager, Mockito.times(1))
            ?.logStateTransition(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("this is data string 2"),
                Mockito.isNull(String::class.java),
                Mockito.eq("package name 2"),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyInt()
            )
    }

    /**
     * This tests what happens if we're started in something other than the launch Activity.
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testSecondActivityStart() = runTest(StandardTestDispatcher()) {
        manager.onActivityPaused(activity)
        Thread.sleep(1000)
        Assert.assertEquals(true, manager.isBackgrounded())
        manager.onActivityResumed(activity)
        val activity2 = Mockito.mock(
            Activity::class.java
        )
        val activity3 = Mockito.mock(
            Activity::class.java
        )
        manager.onActivityPaused(activity2)
        manager.onActivityPaused(activity3)
        Thread.sleep(1000)
        Assert.assertEquals(false, manager.isBackgrounded())
        manager.onActivityPaused(activity)
        Thread.sleep(1000)
        Assert.assertEquals(true, manager.isBackgrounded())
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityPaused() {
        manager.onActivityResumed(activity)
        Assert.assertEquals(false, manager.isBackgrounded())
        manager.onActivityPaused(activity)
        Thread.sleep(1000)
        Assert.assertEquals(true, manager.isBackgrounded())
        Assert.assertTrue(AppStateManager.mInitialized)
        Assert.assertTrue(manager.mLastStoppedTime.get() > 0)
        manager.onActivityResumed(activity)
        Assert.assertTrue(
            manager.session.backgroundTime.toString() + " ms",
            manager.session.backgroundTime in 1000..1199
        )
    }

    @Test
    @Throws(Exception::class)
    fun testEndSession() {
        manager.startSession()
        manager.endSession()
        Assert.assertTrue(manager.session.mSessionID == "NO-SESSION")
    }

    @Test
    @Throws(Exception::class)
    fun testStartSession() {
        val session = manager.session
        Assert.assertTrue(session.mSessionID == "NO-SESSION")
        manager.startSession()
        Assert.assertNotEquals(manager.session.mSessionID, session.mSessionID)
    }

    @Test
    fun testShouldEndSession() {
        val isTimedOut = AndroidUtils.Mutable(false)
        val isBackground = AndroidUtils.Mutable(false)
        val session = AndroidUtils.Mutable<InternalSession?>(object : InternalSession() {
            override fun isTimedOut(sessionTimeout: Int): Boolean {
                return isTimedOut.value
            }
        })
        session.value?.mSessionStartTime = 1
        manager = object : AppStateManager(MockContext(), true) {
            override fun isBackgrounded(): Boolean {
                return isBackground.value
            }

            override fun fetchSession(): InternalSession {
                return session.value!!
            }
        }
        manager.session = session.value!!
        val configManager = Mockito.mock(ConfigManager::class.java)
        manager.setConfigManager(configManager)
        Mockito.`when`(MParticle.getInstance()?.Media()?.audioPlaying).thenReturn(false)
        isTimedOut.value = true
        isBackground.value = true
        Assert.assertTrue(manager.shouldEndSession())
        isTimedOut.value = false
        isBackground.value = true
        Assert.assertFalse(manager.shouldEndSession())
        isTimedOut.value = true
        isBackground.value = false
        Assert.assertFalse(manager.shouldEndSession())
        isTimedOut.value = true
        isBackground.value = true
        Assert.assertTrue(manager.shouldEndSession())
        Mockito.`when`(MParticle.getInstance()!!.Media().audioPlaying).thenReturn(true)
        Assert.assertFalse(manager.shouldEndSession())
        MParticle.setInstance(null)
        // Make sure it doesn't crash.
        Assert.assertTrue(manager.shouldEndSession())
    }
}
