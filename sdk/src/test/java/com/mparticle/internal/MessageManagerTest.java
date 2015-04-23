package com.mparticle.internal;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Message;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class MessageManagerTest {

    private MockContext context;
    private ConfigManager configManager;
    private AppStateManager appStateManager;
    private MessageManager manager;
    private MessageHandler messageHandler;
    private UploadHandler uploadHandler;

    @Before
    public void setup() throws Exception {
        context = new MockContext();
        configManager = Mockito.mock(ConfigManager.class);
        appStateManager = new AppStateManager(context, true);
        messageHandler = Mockito.mock(MessageHandler.class);
        uploadHandler = Mockito.mock(UploadHandler.class);
        manager = new MessageManager(context,
                configManager,
                MParticle.InstallType.AutoDetect, appStateManager,
                messageHandler,
                uploadHandler);
    }

    @Test
    @PrepareForTest({MessageManager.class, MPUtility.class})
    public void testGetStateInfo() throws Exception {
        PowerMockito.mockStatic(MPUtility.class, Answers.RETURNS_MOCKS.get());
        JSONObject stateInfo = manager.getStateInfo();
        assertNotNull(stateInfo.getString(Constants.MessageKey.STATE_INFO_CPU));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_AVAILABLE_MEMORY));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_TOTAL_MEMORY));
        assertNotNull(stateInfo.getDouble(Constants.MessageKey.STATE_INFO_BATTERY_LVL));
        assertNotNull(stateInfo.getDouble(Constants.MessageKey.STATE_INFO_TIME_SINCE_START));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_AVAILABLE_DISK));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_AVAILABLE_EXT_DISK));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_APP_MEMORY_USAGE));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_APP_MEMORY_AVAIL));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_APP_MEMORY_MAX));
        assertNotNull(stateInfo.getString(Constants.MessageKey.STATE_INFO_DATA_CONNECTION));
        assertNotNull(stateInfo.getInt(Constants.MessageKey.STATE_INFO_ORIENTATION));
        assertNotNull(stateInfo.getInt(Constants.MessageKey.STATE_INFO_BAR_ORIENTATION));
        assertNotNull(stateInfo.getBoolean(Constants.MessageKey.STATE_INFO_MEMORY_LOW));
        assertNotNull(stateInfo.getBoolean(Constants.MessageKey.STATE_INFO_GPS));
        assertNotNull(stateInfo.getLong(Constants.MessageKey.STATE_INFO_MEMORY_THRESHOLD));
        assertNotNull(stateInfo.getInt(Constants.MessageKey.STATE_INFO_NETWORK_TYPE));
    }

    @Test
    @PrepareForTest({MessageManager.class, MPUtility.class})
    public void testGetTotalMemory() throws Exception {
        PowerMockito.mockStatic(MPUtility.class, Answers.RETURNS_MOCKS.get());
        SharedPreferences prefs = context.getSharedPreferences(null, 0);
        long memory = MPUtility.getTotalMemory(context);
        assertEquals(-1, prefs.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1));
        long newMemory = manager.getTotalMemory();
        assertEquals(memory, newMemory);
        assertEquals(memory, prefs.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1234));
    }

    @Test
    @PrepareForTest({MessageManager.class, MPUtility.class})
    public void testGetSystemMemoryThreshold() throws Exception {
        PowerMockito.mockStatic(MPUtility.class, Answers.RETURNS_MOCKS.get());
        SharedPreferences prefs = context.getSharedPreferences(null, 0);
        long memory = MPUtility.getSystemMemoryThreshold(context);
        assertEquals(-1, prefs.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1));
        long newMemory = manager.getSystemMemoryThreshold();
        assertEquals(memory, newMemory);
        assertEquals(memory, prefs.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1234));
    }

    @Test
    public void testCreateFirstRunMessage() throws Exception {
        appStateManager.getSession().start();
        MPMessage firstRun = manager.createFirstRunMessage();
        assertEquals(Constants.MessageType.FIRST_RUN, firstRun.getMessageType());
        assertEquals(firstRun.getSessionId(), appStateManager.getSession().mSessionID);
        assertEquals(appStateManager.getSession().mSessionStartTime, firstRun.getTimestamp());
    }

    @Test
    public void testStartSession() throws Exception {
        MockSharedPreferences prefs = (MockSharedPreferences) context.getSharedPreferences(null, 0);
        MPMessage sessionStart = manager.startSession();
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
        assertNotNull(sessionStart);
        assertEquals(Constants.MessageType.SESSION_START, sessionStart.getMessageType());
        assertFalse(sessionStart.has(Constants.MessageKey.PREVIOUS_SESSION_LENGTH));
        assertNull(sessionStart.optString(Constants.MessageKey.PREVIOUS_SESSION_ID, null));
        assertFalse(sessionStart.has(Constants.MessageKey.PREVIOUS_SESSION_START));
        assertEquals(appStateManager.getSession().mSessionID, prefs.getString(Constants.PrefKeys.PREVIOUS_SESSION_ID, null));
        assertFalse(prefs.getBoolean(Constants.PrefKeys.FIRSTRUN + configManager.getApiKey(), true));
        prefs.putLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, 42000);
        prefs.putLong(Constants.PrefKeys.PREVIOUS_SESSION_START, 24000);
        prefs.commit();
        sessionStart = manager.startSession();
        assertNotNull(sessionStart);
        assertEquals(Constants.MessageType.SESSION_START, sessionStart.getMessageType());
        assertEquals(42, sessionStart.getLong(Constants.MessageKey.PREVIOUS_SESSION_LENGTH));
        assertEquals(24000, sessionStart.getLong(Constants.MessageKey.PREVIOUS_SESSION_START));
        Mockito.verify(messageHandler, Mockito.times(1)).sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
    }

    @Test
    public void testIncrementSessionCounter(){
        int count = context.getSharedPreferences(null, 0).getInt(Constants.PrefKeys.SESSION_COUNTER, -5);
        assertEquals(-5, count);
        for (int i = 0; i < 10; i++){
            manager.incrementSessionCounter();
        }
        count = context.getSharedPreferences(null, 0).getInt(Constants.PrefKeys.SESSION_COUNTER, -5);
        assertEquals(10, count);
    }

    @Test
    public void testUpdateSessionEnd() throws Exception {
        manager.updateSessionEnd(null);
        Session session = new Session().start();
        long currentTime = System.currentTimeMillis();
        session.mSessionStartTime = currentTime - 10000;
        AtomicLong stoppedTime = new AtomicLong(currentTime - 5000);
        session.updateBackgroundTime(stoppedTime, currentTime);
        session.mLastEventTime = currentTime;
        manager.updateSessionEnd(session);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
        long time = context.getSharedPreferences(null, 0).getLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, -1);
        assertEquals(5000, time);
    }

    @Test
    public void testEndSession() throws Exception {
        Session session = appStateManager.getSession().start();
        assertNotEquals(Constants.NO_SESSION_ID, session.mSessionID);
        manager.endSession(session);
    }

    @Test
    public void testLogEvent() throws Exception {
        appStateManager.getSession().start();
        manager.logEvent(null, "test screen name");
        manager.logEvent(null, null);
        Map<String, String> info = new HashMap<String, String>(1);
        info.put("test key", "test value");
        MPEvent event = new MPEvent.Builder("test event name", MParticle.EventType.Location).duration(100).info(info).build();
        MPMessage message = manager.logEvent(event, "test screen name");
        assertNotNull(message);
        assertEquals(Constants.MessageType.EVENT, message.getMessageType());
        assertEquals(appStateManager.getSession().mSessionID, message.getSessionId());
        assertEquals(message.getLong(Constants.MessageKey.EVENT_START_TIME), appStateManager.getSession().mLastEventTime);
        assertEquals(message.getDouble(Constants.MessageKey.EVENT_DURATION), event.getLength().doubleValue(), 2);
        JSONObject attrs = message.getJSONObject(Constants.MessageKey.ATTRIBUTES);
        assertNotNull(attrs);
        assertEquals("test value", attrs.getString("test key"));
        assertEquals("test event name", message.getName());
        assertEquals(message.get(Constants.MessageKey.EVENT_TYPE), MParticle.EventType.Location);
        assertEquals("test screen name", message.getString(Constants.MessageKey.CURRENT_ACTIVITY));
        assertEquals(1, context.getSharedPreferences("name", 0).getInt(Constants.PrefKeys.EVENT_COUNTER, -1));
        for (int i = 0; i < 100; i++){
            manager.logEvent(event, "test screen name");
        }
        assertEquals(101, context.getSharedPreferences("name", 0).getInt(Constants.PrefKeys.EVENT_COUNTER, -1));
        Mockito.verify(messageHandler, Mockito.times(101)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogScreen() throws Exception {
        manager.logScreen("screen name", null, true);
        manager.logScreen(null, null, true);
        appStateManager.getSession().start();
        JSONObject info = new JSONObject();
        info.put("test key", "test value");
        MPMessage message = manager.logScreen("screen name", info, true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.SCREEN_VIEW, message.getMessageType());
        assertEquals(appStateManager.getSession().mSessionID, message.getSessionId());
        assertEquals(message.getDouble(Constants.MessageKey.EVENT_DURATION), 0, 2);
        assertEquals(message.getString(Constants.MessageKey.SCREEN_STARTED), "activity_started");
        assertEquals(message.getLong(Constants.MessageKey.EVENT_START_TIME), appStateManager.getSession().mLastEventTime);
        JSONObject attrs = message.getJSONObject(Constants.MessageKey.ATTRIBUTES);
        assertNotNull(attrs);
        assertEquals("test value", attrs.getString("test key"));
        assertEquals("screen name", message.getName());
        message = manager.logScreen("screen name 2", info, false);
        assertEquals(message.getString(Constants.MessageKey.SCREEN_STARTED), "activity_stopped");
        Mockito.verify(messageHandler, Mockito.times(3)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogBreadcrumb() throws Exception {
        appStateManager.getSession().start();
        manager.logBreadcrumb(null);
        MPMessage message = manager.logBreadcrumb("test crumb");
        assertNotNull(message);
        assertEquals(Constants.MessageType.BREADCRUMB, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(appStateManager.getSession().mSessionID, message.getSessionId());
        assertEquals(message.getLong(Constants.MessageKey.EVENT_START_TIME), appStateManager.getSession().mLastEventTime);
        assertEquals(message.getInt(Constants.MessageKey.BREADCRUMB_SESSION_COUNTER), manager.getCurrentSessionCounter());
        assertEquals(message.getString(Constants.MessageKey.BREADCRUMB_LABEL), "test crumb");
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testOptOut() throws Exception {
        appStateManager.getSession().start();
        long optOutTime = System.currentTimeMillis();
        MPMessage message = manager.optOut(optOutTime, true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.OPT_OUT, message.getMessageType());
        assertTrue(message.getBoolean(Constants.MessageKey.OPT_OUT_STATUS));
        assertEquals(message.getLong(Constants.MessageKey.TIMESTAMP), optOutTime);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
        MPMessage message2 = manager.optOut(optOutTime, false);
        assertFalse(message2.getBoolean(Constants.MessageKey.OPT_OUT_STATUS));
    }

    @Test
    public void testLogErrorEvent() throws Exception {
        appStateManager.getSession().start();
        String errorMessage = "message";
        Throwable t = new Throwable("test");
        JSONObject attrs = new JSONObject();
        attrs.put("test key", "test value");
        manager.logErrorEvent(null, null, null);
        manager.logErrorEvent(null, null, null, false);
        manager.logErrorEvent(null, null, null, true);
        manager.logErrorEvent(errorMessage, null, null, true);
        manager.logErrorEvent(null, t, null, true);
        MPMessage message = manager.logErrorEvent(errorMessage, t, attrs, true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.ERROR, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(message.getString(Constants.MessageKey.ERROR_MESSAGE), t.getMessage());
        assertEquals(message.getString(Constants.MessageKey.ERROR_SEVERITY), "error");
        assertEquals(message.getString(Constants.MessageKey.ERROR_CLASS), t.getClass().getCanonicalName());
        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        assertEquals(message.getString(Constants.MessageKey.ERROR_STACK_TRACE), stringWriter.toString());
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), manager.getCurrentSessionCounter());
        Mockito.verify(messageHandler, Mockito.times(3)).sendMessage(Mockito.any(Message.class));

        message = manager.logErrorEvent(errorMessage, t, attrs, false);
        assertNotNull(message);
        assertEquals(Constants.MessageType.ERROR, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(message.getString(Constants.MessageKey.ERROR_MESSAGE), t.getMessage());
        assertEquals(message.getString(Constants.MessageKey.ERROR_SEVERITY), "fatal");
        assertEquals(message.getString(Constants.MessageKey.ERROR_CLASS), t.getClass().getCanonicalName());
        stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        assertEquals(message.getString(Constants.MessageKey.ERROR_STACK_TRACE), stringWriter.toString());
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), manager.getCurrentSessionCounter());
        Mockito.verify(messageHandler, Mockito.times(4)).sendMessage(Mockito.any(Message.class));

        message = manager.logErrorEvent(errorMessage, t, attrs);
        assertNotNull(message);
        assertEquals(Constants.MessageType.ERROR, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(message.getString(Constants.MessageKey.ERROR_MESSAGE), t.getMessage());
        assertEquals(message.getString(Constants.MessageKey.ERROR_SEVERITY), "error");
        assertEquals(message.getString(Constants.MessageKey.ERROR_CLASS), t.getClass().getCanonicalName());
        stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        assertEquals(message.getString(Constants.MessageKey.ERROR_STACK_TRACE), stringWriter.toString());
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), manager.getCurrentSessionCounter());
        Mockito.verify(messageHandler, Mockito.times(5)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogNetworkPerformanceEvent() throws Exception {
        appStateManager.getSession().start();
        MPMessage message = manager.logNetworkPerformanceEvent(0, null, null, 0, 0, 0, null);
        assertNull(message);
        message = manager.logNetworkPerformanceEvent(1, "GET", "someurl", 12, 123, 1234, "request string");
        assertNotNull(message);
        assertEquals(Constants.MessageType.NETWORK_PERFORMNACE, message.getMessageType());
        assertEquals(message.getString(Constants.MessageKey.NPE_METHOD), "GET");
        assertEquals(message.getString(Constants.MessageKey.NPE_URL), "someurl");
        assertEquals(message.getLong(Constants.MessageKey.NPE_LENGTH), 12);
        assertEquals(message.getLong(Constants.MessageKey.NPE_SENT), 123);
        assertEquals(message.getLong(Constants.MessageKey.NPE_REC), 1234);
        assertEquals(message.getString(Constants.MessageKey.NPE_POST_DATA), "request string");
        assertEquals(message.getLong(Constants.MessageKey.TIMESTAMP), 1);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testSetPushRegistrationId() throws Exception {
        appStateManager.getSession().start();
        MPMessage message = manager.setPushRegistrationId(null, true);
        assertNull(message);
        message = manager.setPushRegistrationId("", true);
        assertNull(message);
        message = manager.setPushRegistrationId("coolgcmregid", true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.PUSH_REGISTRATION, message.getMessageType());
        assertEquals(message.getString(Constants.MessageKey.PUSH_TOKEN), "coolgcmregid");
        assertEquals(message.getString(Constants.MessageKey.PUSH_TOKEN_TYPE), "google");
        assertEquals(message.getBoolean(Constants.MessageKey.PUSH_REGISTER_FLAG), true);
        message = manager.setPushRegistrationId("coolgcmregid", false);
        assertEquals(message.getBoolean(Constants.MessageKey.PUSH_REGISTER_FLAG), false);
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testSetSessionAttributes() throws Exception {
        manager.setSessionAttributes();
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testStartUploadLoop() throws Exception {
        manager.startUploadLoop();
        Mockito.verify(uploadHandler, Mockito.times(1)).removeMessages(UploadHandler.UPLOAD_MESSAGES);
        Mockito.verify(uploadHandler, Mockito.times(1)).sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
    }

    @Test
    public void testDoUpload() throws Exception {
        manager.doUpload();
        Mockito.verify(uploadHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testSetLocation() throws Exception {
        Location location = Mockito.mock(Location.class);
        manager.setLocation(location);
        assertEquals(location, manager.getLocation());
    }

    @Test
    public void testLogStateTransition() throws Exception {
        appStateManager.getSession().start();
        manager.mInstallType = MParticle.InstallType.KnownInstall;
        MPMessage message = manager.logStateTransition(null, null, null, null, null, 0, 0, 0);
        assertNull(message);
        message = manager.logStateTransition("", null, null, null, null, 0, 0, 0);
        assertNull(message);
        String currentActivity = "some activity name";
        String launchUri = "some uri";
        String launchExtras = "some extras";
        String launchSourcePackage = "com.some.package";
        message = manager.logStateTransition(Constants.StateTransitionType.STATE_TRANS_INIT, currentActivity, launchUri, launchExtras, launchSourcePackage, 42, 24, 123);
        assertNotNull(message);
        assertEquals(Constants.MessageType.APP_STATE_TRANSITION, message.getMessageType());
        assertEquals(message.getString(Constants.MessageKey.STATE_TRANSITION_TYPE), Constants.StateTransitionType.STATE_TRANS_INIT);
        assertEquals(message.getSessionId(), appStateManager.getSession().mSessionID);
        assertEquals(message.getString(Constants.MessageKey.CURRENT_ACTIVITY), currentActivity);
        assertEquals(message.getString(Constants.MessageKey.ST_LAUNCH_REFERRER), launchUri);
        assertEquals(message.getString(Constants.MessageKey.ST_LAUNCH_PARAMS), launchExtras);
        assertEquals(message.getString(Constants.MessageKey.ST_LAUNCH_SOURCE_PACKAGE), launchSourcePackage);
        assertEquals(message.getLong(Constants.MessageKey.ST_LAUNCH_PRV_FORE_TIME), 42);
        assertEquals(message.getLong(Constants.MessageKey.ST_LAUNCH_TIME_SUSPENDED), 24);
        assertEquals(message.getLong(Constants.MessageKey.ST_INTERRUPTIONS), 123);
        assertEquals(message.getBoolean(Constants.MessageKey.APP_INIT_UPGRADE), false);
        assertEquals(message.getBoolean(Constants.MessageKey.APP_INIT_FIRST_RUN), true);
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));

    }

    @Test
    public void testLogNotification() throws Exception {

    }

    @Test
    public void testLogNotification1() throws Exception {

    }

    @Test
    public void testLogProfileAction() throws Exception {

    }

    @Test
    public void testCreateMessageSessionEnd() throws Exception {

    }

    @Test
    public void testGetApiKey() throws Exception {

    }

    @Test
    public void testDelayedStart() throws Exception {

    }

    @Test
    public void testEndUploadLoop() throws Exception {

    }

    @Test
    public void testCheckForTrigger() throws Exception {

    }

    @Test
    public void testRefreshConfiguration() throws Exception {

    }

    @Test
    public void testInitConfigDelayed() throws Exception {

    }

    @Test
    public void testSaveGcmMessage() throws Exception {

    }

    @Test
    public void testSaveGcmMessage1() throws Exception {

    }

    @Test
    public void testSetDataConnection() throws Exception {

    }
}