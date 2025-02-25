package com.mparticle.internal

import android.location.Location
import android.os.Looper
import android.os.Message
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.messages.MPAliasMessage
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockSharedPreferences
import com.mparticle.mock.utils.RandomUtils
import com.mparticle.testutils.TestingUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Random
import java.util.concurrent.atomic.AtomicLong

@RunWith(PowerMockRunner::class)
@PrepareForTest(Looper::class)
class MessageManagerTest {
    private lateinit var context: MockContext
    private lateinit var configManager: ConfigManager
    private lateinit var appStateManager: AppStateManager
    private lateinit var manager: MessageManager
    private lateinit var messageHandler: MessageHandler
    private lateinit var uploadHandler: UploadHandler
    private val defaultId = 1L

    @Before
    fun setup() {
        MParticle.setInstance(MockMParticle())
        context = MockContext()
        configManager = Mockito.mock(ConfigManager::class.java)
        Mockito.`when`(configManager.apiKey).thenReturn("123456789")
        Mockito.`when`(configManager.userStorage)
            .thenReturn(UserStorage.create(context, Random().nextInt().toLong()))
        Mockito.`when`(MParticle.getInstance()?.Internal()?.configManager?.mpid)
            .thenReturn(defaultId)
        Mockito.`when`(configManager.mpid).thenReturn(defaultId)
        // Prepare and mock the Looper class
        PowerMockito.mockStatic(Looper::class.java)
        val looper: Looper = Mockito.mock(Looper::class.java)
        Mockito.`when`(Looper.getMainLooper()).thenReturn(looper)
        appStateManager = AppStateManager(context, true)
        messageHandler = Mockito.mock(MessageHandler::class.java)
        uploadHandler = Mockito.mock(UploadHandler::class.java)
        manager = MessageManager(
            context,
            configManager,
            MParticle.InstallType.AutoDetect,
            appStateManager,
            Mockito.mock(MParticleDBManager::class.java),
            messageHandler,
            uploadHandler
        )
        Mockito.`when`(messageHandler.obtainMessage(Mockito.anyInt(), Mockito.any())).thenReturn(
            Message()
        )
        Mockito.`when`(messageHandler.obtainMessage(Mockito.anyInt())).thenReturn(Message())
    }

    @Test
    @PrepareForTest(MessageManager::class, MPUtility::class, Looper::class)
    @Throws(Exception::class)
    fun testGetStateInfo() {
        PowerMockito.mockStatic(MPUtility::class.java, Answers.RETURNS_MOCKS.get())
        val stateInfo = MessageManager.getStateInfo()
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_AVAILABLE_MEMORY))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_TOTAL_MEMORY))
        Assert.assertNotNull(stateInfo.getDouble(MessageKey.STATE_INFO_BATTERY_LVL))
        Assert.assertNotNull(stateInfo.getDouble(MessageKey.STATE_INFO_TIME_SINCE_START))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_AVAILABLE_DISK))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_AVAILABLE_EXT_DISK))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_APP_MEMORY_USAGE))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_APP_MEMORY_AVAIL))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_APP_MEMORY_MAX))
        Assert.assertNotNull(stateInfo.getString(MessageKey.STATE_INFO_DATA_CONNECTION))
        Assert.assertNotNull(stateInfo.getInt(MessageKey.STATE_INFO_ORIENTATION))
        Assert.assertNotNull(stateInfo.getInt(MessageKey.STATE_INFO_BAR_ORIENTATION))
        Assert.assertNotNull(stateInfo.getBoolean(MessageKey.STATE_INFO_MEMORY_LOW))
        Assert.assertNotNull(stateInfo.getBoolean(MessageKey.STATE_INFO_GPS))
        Assert.assertNotNull(stateInfo.getLong(MessageKey.STATE_INFO_MEMORY_THRESHOLD))
        Assert.assertNotNull(stateInfo.getInt(MessageKey.STATE_INFO_NETWORK_TYPE))
    }

    @Test
    @PrepareForTest(MessageManager::class, MPUtility::class, Looper::class)
    @Throws(Exception::class)
    fun testGetTotalMemory() {
        PowerMockito.mockStatic(MPUtility::class.java, Answers.RETURNS_MOCKS.get())
        val prefs = context.getSharedPreferences(null, 0)
        val memory = MPUtility.getTotalMemory(context)
        Assert.assertEquals(-1, prefs.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1))
        val newMemory = MessageManager.getTotalMemory()
        Assert.assertEquals(memory, newMemory)
        Assert.assertEquals(memory, prefs.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1234))
    }

    @Test
    @PrepareForTest(MessageManager::class, MPUtility::class, Looper::class)
    @Throws(Exception::class)
    fun testGetSystemMemoryThreshold() {
        PowerMockito.mockStatic(MPUtility::class.java, Answers.RETURNS_MOCKS.get())
        val prefs = context.getSharedPreferences(null, 0)
        val memory = MPUtility.getSystemMemoryThreshold(context)
        Assert.assertEquals(-1, prefs.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1))
        val newMemory = MessageManager.getSystemMemoryThreshold()
        Assert.assertEquals(memory, newMemory)
        Assert.assertEquals(
            memory,
            prefs.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1234)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCreateFirstRunMessage() {
        appStateManager.session.start(context)
        val firstRun = manager.createFirstRunMessage()
        Assert.assertEquals(Constants.MessageType.FIRST_RUN, firstRun.messageType)
        Assert.assertEquals(firstRun.sessionId, appStateManager.session.mSessionID)
        Assert.assertEquals(appStateManager.session.mSessionStartTime, firstRun.timestamp)
    }

    @Test
    @Throws(Exception::class)
    fun testStartSession() {
        val prefs = context.getSharedPreferences(null, 0) as MockSharedPreferences
        var sessionStart = manager.startSession(appStateManager.session)
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
        Assert.assertNotNull(sessionStart)
        Assert.assertEquals(Constants.MessageType.SESSION_START, sessionStart.messageType)
        Assert.assertFalse(sessionStart.has(MessageKey.PREVIOUS_SESSION_LENGTH))
        Assert.assertNull(sessionStart.optString(MessageKey.PREVIOUS_SESSION_ID, null))
        Assert.assertFalse(sessionStart.has(MessageKey.PREVIOUS_SESSION_START))
        Assert.assertEquals(
            appStateManager.session.mSessionID,
            configManager.userStorage.getPreviousSessionId(null)
        )
        Assert.assertFalse(
            prefs.getBoolean(
                Constants.PrefKeys.FIRSTRUN_MESSAGE + configManager.apiKey,
                true
            )
        )
        configManager.userStorage.setPreviousSessionForeground(42000)
        configManager.userStorage.setPreviousSessionStart(24000)
        prefs.commit()
        sessionStart = manager.startSession(appStateManager.session)
        Assert.assertNotNull(sessionStart)
        Assert.assertEquals(Constants.MessageType.SESSION_START, sessionStart.messageType)
        Assert.assertEquals(42, sessionStart.getLong(MessageKey.PREVIOUS_SESSION_LENGTH))
        Assert.assertEquals(24000, sessionStart.getLong(MessageKey.PREVIOUS_SESSION_START))
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            messageHandler.obtainMessage(MessageHandler.END_ORPHAN_SESSIONS, configManager.mpid)
        )
    }

    @Test
    fun testIncrementSessionCounter() {
        var count = configManager.userStorage.getCurrentSessionCounter(-5)
        Assert.assertEquals(-5, count.toLong())
        for (i in 0..9) {
            configManager.userStorage.incrementSessionCounter()
        }
        count = configManager.userStorage.getCurrentSessionCounter(-5)
        Assert.assertEquals(10, count.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateSessionEnd() {
        manager.updateSessionEnd(null)
        val session = InternalSession().start(context)
        val currentTime = System.currentTimeMillis()
        session.mSessionStartTime = currentTime - 10000
        val stoppedTime = AtomicLong(currentTime - 5000)
        session.updateBackgroundTime(stoppedTime, currentTime)
        session.mLastEventTime = currentTime
        manager.updateSessionEnd(session)
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
        val time = configManager.userStorage.getPreviousSessionForegound(-1)
        Assert.assertEquals(5000, time)
    }

    @Test
    @Throws(Exception::class)
    fun testEndSession() {
        val session = appStateManager.session.start(context)
        Assert.assertNotEquals(Constants.NO_SESSION_ID, session.mSessionID)
        manager.endSession(session)
    }

    @Test
    @Throws(Exception::class)
    fun testLogEvent() {
        appStateManager.session.start(context)
        manager.logEvent(null, "test screen name")
        manager.logEvent(null, null)
        val info: MutableMap<String, String?> = HashMap(1)
        info["test key"] = "test value"
        val event = MPEvent.Builder("test event name", MParticle.EventType.Location).duration(100.0)
            .addCustomFlag("flag 1", "value 1")
            .addCustomFlag("flag 1", "value 2").addCustomFlag("flag 2", "value 3")
            .customAttributes(info).build()
        val message = manager.logEvent(event, "test screen name")
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.EVENT, message.messageType)
        Assert.assertEquals(appStateManager.session.mSessionID, message.sessionId)
        Assert.assertEquals(
            message.getLong(MessageKey.EVENT_START_TIME),
            appStateManager.session.mLastEventTime
        )
        event.length?.let { lenght ->
            Assert.assertEquals(
                message.getDouble(MessageKey.EVENT_DURATION),
                lenght,
                2.0
            )
        }
        val attrs = message.getJSONObject(MessageKey.ATTRIBUTES)
        Assert.assertNotNull(attrs)
        Assert.assertEquals("test value", attrs.getString("test key"))
        Assert.assertEquals("test event name", message.name)
        Assert.assertEquals(message[MessageKey.EVENT_TYPE], MParticle.EventType.Location)
        Assert.assertEquals("test screen name", message.getString(MessageKey.CURRENT_ACTIVITY))
        Assert.assertEquals(
            1,
            context.getSharedPreferences("name", 0).getInt(Constants.PrefKeys.EVENT_COUNTER, -1)
                .toLong()
        )
        for (i in 0..99) {
            manager.logEvent(event, "test screen name")
        }
        val flags = message.getJSONObject("flags")
        val flag1 = flags.getJSONArray("flag 1")
        Assert.assertEquals(flag1.length().toLong(), 2)
        Assert.assertEquals(flag1[0], "value 1")
        Assert.assertEquals(flag1[1], "value 2")
        val flag2 = flags.getJSONArray("flag 2")
        Assert.assertEquals(flag2.length().toLong(), 1)
        Assert.assertEquals(flag2[0], "value 3")
        Assert.assertEquals(
            101,
            context.getSharedPreferences("name", 0).getInt(Constants.PrefKeys.EVENT_COUNTER, -1)
                .toLong()
        )
        Mockito.verify(messageHandler, Mockito.times(101)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLogCommerceEventWithNullUser() {
        val event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("foo", "bar", 10.0).build())
                .build()
        val message = manager.logEvent(event)
        Assert.assertNotNull(message)

        // Mockito defaults to an Answer of 1
        Assert.assertEquals(1L, message.mpId)
    }

    @Test
    @Throws(Exception::class)
    fun testLogScreen() {
        manager.logScreen(MPEvent.Builder("screen name").build(), true)
        manager.logScreen(null, true)
        appStateManager.session.start(context)
        val info: MutableMap<String, String?> = HashMap()
        info["test key"] = "test value"
        var message = manager.logScreen(
            MPEvent.Builder("screen name").addCustomFlag("flag 1", "value 1")
                .addCustomFlag("flag 1", "value 2").addCustomFlag("flag 2", "value 3")
                .customAttributes(info).build(),
            true
        )
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.SCREEN_VIEW, message.messageType)
        Assert.assertEquals(appStateManager.session.mSessionID, message.sessionId)
        Assert.assertEquals(message.getDouble(MessageKey.EVENT_DURATION), 0.0, 2.0)
        Assert.assertEquals(message.getString(MessageKey.SCREEN_STARTED), "activity_started")
        Assert.assertEquals(
            message.getLong(MessageKey.EVENT_START_TIME),
            appStateManager.session.mLastEventTime
        )
        val flags = message.getJSONObject("flags")
        val flag1 = flags.getJSONArray("flag 1")
        Assert.assertEquals(flag1.length().toLong(), 2)
        Assert.assertEquals(flag1[0], "value 1")
        Assert.assertEquals(flag1[1], "value 2")
        val flag2 = flags.getJSONArray("flag 2")
        Assert.assertEquals(flag2.length().toLong(), 1)
        Assert.assertEquals(flag2[0], "value 3")
        val attrs = message.getJSONObject(MessageKey.ATTRIBUTES)
        Assert.assertNotNull(attrs)
        Assert.assertEquals("test value", attrs.getString("test key"))
        Assert.assertEquals("screen name", message.name)
        message = manager.logScreen(
            MPEvent.Builder("screen name 2").customAttributes(info).build(),
            false
        )
        Assert.assertEquals(message.getString(MessageKey.SCREEN_STARTED), "activity_stopped")
        Mockito.verify(messageHandler, Mockito.times(3)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLogBreadcrumb() {
        appStateManager.session.start(context)
        manager.logBreadcrumb(null)
        val message = manager.logBreadcrumb("test crumb")
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.BREADCRUMB, message.messageType)
        Assert.assertEquals(appStateManager.session.mLastEventTime, message.timestamp)
        Assert.assertEquals(appStateManager.session.mSessionID, message.sessionId)
        Assert.assertEquals(
            message.getLong(MessageKey.EVENT_START_TIME),
            appStateManager.session.mLastEventTime
        )
        Assert.assertEquals(
            message.getInt(MessageKey.BREADCRUMB_SESSION_COUNTER).toLong(),
            configManager.userStorage.currentSessionCounter.toLong()
        )
        Assert.assertEquals(message.getString(MessageKey.BREADCRUMB_LABEL), "test crumb")
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOptOut() {
        appStateManager.session.start(context)
        val optOutTime = System.currentTimeMillis()
        val message = manager.optOut(optOutTime, true)
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.OPT_OUT, message.messageType)
        Assert.assertTrue(message.getBoolean(MessageKey.OPT_OUT_STATUS))
        Assert.assertEquals(message.getLong(MessageKey.TIMESTAMP), optOutTime)
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
        val message2 = manager.optOut(optOutTime, false)
        Assert.assertFalse(message2.getBoolean(MessageKey.OPT_OUT_STATUS))
    }

    @Test
    @Throws(Exception::class)
    fun testLogErrorEvent() {
        appStateManager.session.start(context)
        val errorMessage = "message"
        val t = Throwable("test")
        val attrs = JSONObject()
        attrs.put("test key", "test value")
        manager.logErrorEvent(null, null, null)
        manager.logErrorEvent(null, null, null, false)
        manager.logErrorEvent(null, null, null, true)
        manager.logErrorEvent(errorMessage, null, null, true)
        manager.logErrorEvent(null, t, null, true)
        var message = manager.logErrorEvent(errorMessage, t, attrs, true)
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.ERROR, message.messageType)
        Assert.assertEquals(appStateManager.session.mLastEventTime, message.timestamp)
        Assert.assertEquals(message.getString(MessageKey.ERROR_MESSAGE), t.message)
        Assert.assertEquals(message.getString(MessageKey.ERROR_SEVERITY), "error")
        Assert.assertEquals(message.getString(MessageKey.ERROR_CLASS), t.javaClass.canonicalName)
        var stringWriter = StringWriter()
        t.printStackTrace(PrintWriter(stringWriter))
        Assert.assertEquals(
            message.getString(MessageKey.ERROR_STACK_TRACE),
            stringWriter.toString()
        )
        Assert.assertEquals(
            message.getInt(MessageKey.ERROR_SESSION_COUNT).toLong(),
            configManager.userStorage.currentSessionCounter.toLong()
        )
        Mockito.verify(messageHandler, Mockito.times(3)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
        message = manager.logErrorEvent(errorMessage, t, attrs, false)
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.ERROR, message.messageType)
        Assert.assertEquals(appStateManager.session.mLastEventTime, message.timestamp)
        Assert.assertEquals(message.getString(MessageKey.ERROR_MESSAGE), t.message)
        Assert.assertEquals(message.getString(MessageKey.ERROR_SEVERITY), "fatal")
        Assert.assertEquals(message.getString(MessageKey.ERROR_CLASS), t.javaClass.canonicalName)
        stringWriter = StringWriter()
        t.printStackTrace(PrintWriter(stringWriter))
        Assert.assertEquals(
            message.getString(MessageKey.ERROR_STACK_TRACE),
            stringWriter.toString()
        )
        Assert.assertEquals(
            message.getInt(MessageKey.ERROR_SESSION_COUNT).toLong(),
            configManager.userStorage.currentSessionCounter.toLong()
        )
        Mockito.verify(messageHandler, Mockito.times(4)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
        message = manager.logErrorEvent(errorMessage, t, attrs)
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.ERROR, message.messageType)
        Assert.assertEquals(appStateManager.session.mLastEventTime, message.timestamp)
        Assert.assertEquals(message.getString(MessageKey.ERROR_MESSAGE), t.message)
        Assert.assertEquals(message.getString(MessageKey.ERROR_SEVERITY), "error")
        Assert.assertEquals(message.getString(MessageKey.ERROR_CLASS), t.javaClass.canonicalName)
        stringWriter = StringWriter()
        t.printStackTrace(PrintWriter(stringWriter))
        Assert.assertEquals(
            message.getString(MessageKey.ERROR_STACK_TRACE),
            stringWriter.toString()
        )
        Assert.assertEquals(
            message.getInt(MessageKey.ERROR_SESSION_COUNT).toLong(),
            configManager.userStorage.currentSessionCounter.toLong()
        )
        Mockito.verify(messageHandler, Mockito.times(5)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLogNetworkPerformanceEvent() {
        appStateManager.session.start(context)
        var message = manager.logNetworkPerformanceEvent(0, null, null, 0, 0, 0, null)
        Assert.assertNull(message)
        message = manager.logNetworkPerformanceEvent(
            1,
            "GET",
            "someurl",
            12,
            123,
            1234,
            "request string"
        )
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.NETWORK_PERFORMNACE, message.messageType)
        Assert.assertEquals(message.getString(MessageKey.NPE_METHOD), "GET")
        Assert.assertEquals(message.getString(MessageKey.NPE_URL), "someurl")
        Assert.assertEquals(message.getLong(MessageKey.NPE_LENGTH), 12)
        Assert.assertEquals(message.getLong(MessageKey.NPE_SENT), 123)
        Assert.assertEquals(message.getLong(MessageKey.NPE_REC), 1234)
        Assert.assertEquals(message.getString(MessageKey.NPE_POST_DATA), "request string")
        Assert.assertEquals(message.getLong(MessageKey.TIMESTAMP), 1)
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetPushRegistrationId() {
        appStateManager.session.start(context)
        var message = manager.setPushRegistrationId(null, true)
        Assert.assertNull(message)
        message = manager.setPushRegistrationId("", true)
        Assert.assertNull(message)
        message = manager.setPushRegistrationId("coolgcmregid", true)
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.PUSH_REGISTRATION, message.messageType)
        Assert.assertEquals(message.getString(MessageKey.PUSH_TOKEN), "coolgcmregid")
        Assert.assertEquals(message.getString(MessageKey.PUSH_TOKEN_TYPE), "google")
        Assert.assertEquals(message.getBoolean(MessageKey.PUSH_REGISTER_FLAG), true)
        message = manager.setPushRegistrationId("coolgcmregid", false)
        Assert.assertEquals(message.getBoolean(MessageKey.PUSH_REGISTER_FLAG), false)
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetSessionAttributes() {
        manager.setSessionAttributes()
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testStartUploadLoop() {
        manager.startUploadLoop()
        Mockito.verify(uploadHandler, Mockito.times(1))
            .removeMessages(UploadHandler.UPLOAD_MESSAGES, configManager.mpid)
        Mockito.verify(uploadHandler, Mockito.times(1)).sendMessageDelayed(
            uploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, configManager.mpid),
            Constants.INITIAL_UPLOAD_DELAY
        )
    }

    @Test
    @Throws(Exception::class)
    fun testDoUpload() {
        manager.doUpload()
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetLocation() {
        val location = Mockito.mock(
            Location::class.java
        )
        manager.location = location
        Assert.assertEquals(location, manager.location)
    }

    @Test
    @Throws(Exception::class)
    fun testLogStateTransition() {
        appStateManager.session.start(context)
        manager.mInstallType = MParticle.InstallType.KnownInstall
        var message = manager.logStateTransition(null, null, null, null, null, 0, 0, 0)
        Assert.assertNull(message)
        message = manager.logStateTransition("", null, null, null, null, 0, 0, 0)
        Assert.assertNull(message)
        val currentActivity = "some activity name"
        val launchUri = "some uri"
        val launchExtras = "some extras"
        val launchSourcePackage = "com.some.package"
        message = manager.logStateTransition(
            Constants.StateTransitionType.STATE_TRANS_INIT,
            currentActivity,
            launchUri,
            launchExtras,
            launchSourcePackage,
            42,
            24,
            123
        )
        Assert.assertNotNull(message)
        Assert.assertEquals(Constants.MessageType.APP_STATE_TRANSITION, message.messageType)
        Assert.assertEquals(
            message.getString(MessageKey.STATE_TRANSITION_TYPE),
            Constants.StateTransitionType.STATE_TRANS_INIT
        )
        Assert.assertEquals(message.sessionId, appStateManager.session.mSessionID)
        Assert.assertEquals(message.getString(MessageKey.CURRENT_ACTIVITY), currentActivity)
        Assert.assertEquals(message.getString(MessageKey.ST_LAUNCH_REFERRER), launchUri)
        Assert.assertEquals(message.getString(MessageKey.ST_LAUNCH_PARAMS), launchExtras)
        Assert.assertEquals(
            message.getString(MessageKey.ST_LAUNCH_SOURCE_PACKAGE),
            launchSourcePackage
        )
        Assert.assertEquals(message.getLong(MessageKey.ST_LAUNCH_PRV_FORE_TIME), 42)
        Assert.assertEquals(message.getLong(MessageKey.ST_LAUNCH_TIME_SUSPENDED), 24)
        Assert.assertEquals(message.getLong(MessageKey.ST_INTERRUPTIONS), 123)
        Assert.assertEquals(message.getBoolean(MessageKey.APP_INIT_UPGRADE), false)
        Assert.assertEquals(message.getBoolean(MessageKey.APP_INIT_FIRST_RUN), true)
        Mockito.verify(messageHandler, Mockito.times(1))?.sendMessage(
            Mockito.any(
                Message::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChange() {
        var message = manager.logUserAttributeChangeMessage(
            "this is a key",
            "this is the new value",
            "this is the old value",
            false,
            false,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals("this is the new value", message.getString("nv"))
        Assert.assertEquals("this is the old value", message.getString("ov"))
        Assert.assertEquals(false, message.getBoolean("d"))
        Assert.assertEquals(false, message.getBoolean("na"))
        val newValue: MutableList<String> = ArrayList()
        newValue.add("this is a new value")
        newValue.add("this is another new value")
        val oldValue: MutableList<String> = ArrayList()
        oldValue.add("this is an old value")
        oldValue.add("this is another old value")
        message = manager.logUserAttributeChangeMessage(
            "this is a key",
            newValue,
            oldValue,
            false,
            true,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals(2, message.getJSONArray("nv").length().toLong())
        Assert.assertEquals(2, message.getJSONArray("ov").length().toLong())
        Assert.assertEquals("this is an old value", message.getJSONArray("ov")[0])
        Assert.assertEquals("this is a new value", message.getJSONArray("nv")[0])
        Assert.assertEquals(false, message.getBoolean("d"))
        Assert.assertEquals(true, message.getBoolean("na"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeNewAttribute() {
        val message = manager.logUserAttributeChangeMessage(
            "this is a key",
            "this is the new value",
            null,
            false,
            false,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals("this is the new value", message.getString("nv"))
        Assert.assertEquals(JSONObject.NULL, message["ov"])
        Assert.assertEquals(false, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeNewTag() {
        val message =
            manager.logUserAttributeChangeMessage("this is a key", null, null, false, false, 0, 1)
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals(JSONObject.NULL, message["ov"])
        Assert.assertEquals(JSONObject.NULL, message["nv"])
        Assert.assertEquals(false, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeTagToAttribute() {
        val message = manager.logUserAttributeChangeMessage(
            "this is a key",
            "this is the new value",
            null,
            false,
            false,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals(JSONObject.NULL, message["ov"])
        Assert.assertEquals("this is the new value", message["nv"])
        Assert.assertEquals(false, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeStringToList() {
        val newValue: MutableList<String> = ArrayList()
        newValue.add("this is a new value")
        newValue.add("this is another new value")
        val message = manager.logUserAttributeChangeMessage(
            "this is a key",
            newValue,
            "this is the old value",
            false,
            false,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals(2, message.getJSONArray("nv").length().toLong())
        Assert.assertEquals("this is a new value", message.getJSONArray("nv")[0])
        Assert.assertEquals("this is the old value", message["ov"])
        Assert.assertEquals(false, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeRemoveTag() {
        val message =
            manager.logUserAttributeChangeMessage("this is a key", null, null, true, false, 0, 1)
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals(JSONObject.NULL, message["ov"])
        Assert.assertEquals(JSONObject.NULL, message["nv"])
        Assert.assertEquals(true, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserAttributeChangeRemoveAttribute() {
        val message = manager.logUserAttributeChangeMessage(
            "this is a key",
            null,
            "this is the old value",
            true,
            false,
            0,
            1
        )
        Assert.assertEquals("this is a key", message.getString("n"))
        Assert.assertEquals("this is the old value", message["ov"])
        Assert.assertEquals(JSONObject.NULL, message["nv"])
        Assert.assertEquals(true, message.getBoolean("d"))
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserIdentityChangeMessage() {
        val oldIdentity = JSONObject()
        val newIdentity = JSONObject()
        val identities = JSONArray()
        val message =
            manager.logUserIdentityChangeMessage(newIdentity, oldIdentity, identities, defaultId)
        Assert.assertEquals(message["oi"], oldIdentity)
        Assert.assertEquals(message["ni"], newIdentity)
        Assert.assertEquals(message["ui"], identities)
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserIdentityChangeMessageNewIdentity() {
        val newIdentity = JSONObject()
        val identities = JSONArray()
        val message =
            manager.logUserIdentityChangeMessage(newIdentity, null, identities, defaultId)
        Assert.assertEquals(message["oi"], JSONObject.NULL)
        Assert.assertEquals(message["ni"], newIdentity)
        Assert.assertEquals(message["ui"], identities)
    }

    @Test
    @Throws(Exception::class)
    fun testLogUserIdentityChangeMessageRemoveIdentity() {
        val oldIdentity = JSONObject()
        val identities = JSONArray()
        val message =
            manager.logUserIdentityChangeMessage(null, oldIdentity, identities, defaultId)
        Assert.assertEquals(message["ni"], JSONObject.NULL)
        Assert.assertEquals(message["oi"], oldIdentity)
        Assert.assertEquals(message["ui"], identities)
    }

    @Test
    fun testToListMan() {
        val map = RandomUtils.getInstance().getRandomAttributes(10)
        val listMap = MessageManager().toStringListMap(map)
        Assert.assertEquals(map.size.toLong(), listMap.size.toLong())
        for ((key, value) in map) {
            Assert.assertEquals(1, listMap[key]?.size)
            Assert.assertEquals(value, listMap[key]?.get(0))
        }
        Assert.assertEquals(null, MessageManager().toStringListMap(null))
    }

    @Test
    @Throws(JSONException::class)
    fun testAliasRequestSerialization() {
        val request = TestingUtils.getInstance().randomAliasRequest
        val aliasMessage = MPAliasMessage(request, "das", "apiKey")
        Assert.assertEquals(request, aliasMessage.aliasRequest)
    }
}
