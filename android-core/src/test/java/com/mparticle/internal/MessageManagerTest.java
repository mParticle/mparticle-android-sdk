package com.mparticle.internal;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Message;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;
import com.mparticle.mock.utils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static com.mparticle.internal.Constants.MessageKey.TIMESTAMP;
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

    private long defaultId = 1L;

    @Before
    public void setup() {
        MParticle.setInstance(new MockMParticle());
        context = new MockContext();
        configManager = Mockito.mock(ConfigManager.class);
        Mockito.when(configManager.getApiKey()).thenReturn("123456789");
        Mockito.when(configManager.getUserStorage()).thenReturn(UserStorage.create(context, new Random().nextInt()));
        Mockito.when(MParticle.getInstance().Internal().getConfigManager().getMpid()).thenReturn(defaultId);
        Mockito.when(configManager.getMpid()).thenReturn(defaultId);
        appStateManager = new AppStateManager(context, true);
        messageHandler = Mockito.mock(MessageHandler.class);
        uploadHandler = Mockito.mock(UploadHandler.class);
        manager = new MessageManager(context,
                configManager,
                MParticle.InstallType.AutoDetect, appStateManager,
                Mockito.mock(MParticleDBManager.class),
                messageHandler,
                uploadHandler);
        Mockito.when(messageHandler.obtainMessage(Mockito.anyInt(), Mockito.any())).thenReturn(new Message());
        Mockito.when(messageHandler.obtainMessage(Mockito.anyInt())).thenReturn(new Message());
    }

    @Test
    @PrepareForTest({MessageManager.class, MPUtility.class})
    public void testGetStateInfo() throws Exception {
        PowerMockito.mockStatic(MPUtility.class, Answers.RETURNS_MOCKS.get());
        JSONObject stateInfo = manager.getStateInfo();
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
        appStateManager.getSession().start(context);
        MessageManager.BaseMPMessage firstRun = manager.createFirstRunMessage();
        assertEquals(Constants.MessageType.FIRST_RUN, firstRun.getMessageType());
        assertEquals(firstRun.getSessionId(), appStateManager.getSession().mSessionID);
        assertEquals(appStateManager.getSession().mSessionStartTime, firstRun.getTimestamp());
    }

    @Test
    public void testStartSession() throws Exception {
        MockSharedPreferences prefs = (MockSharedPreferences) context.getSharedPreferences(null, 0);
        MessageManager.BaseMPMessage sessionStart = manager.startSession(appStateManager.getSession());
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
        assertNotNull(sessionStart);
        assertEquals(Constants.MessageType.SESSION_START, sessionStart.getMessageType());
        assertFalse(sessionStart.has(Constants.MessageKey.PREVIOUS_SESSION_LENGTH));
        assertNull(sessionStart.optString(Constants.MessageKey.PREVIOUS_SESSION_ID, null));
        assertFalse(sessionStart.has(Constants.MessageKey.PREVIOUS_SESSION_START));
        assertEquals(appStateManager.getSession().mSessionID, configManager.getUserStorage().getPreviousSessionId(null));
        assertFalse(prefs.getBoolean(Constants.PrefKeys.FIRSTRUN_MESSAGE + configManager.getApiKey(), true));
        configManager.getUserStorage().setPreviousSessionForeground(42000);
        configManager.getUserStorage().setPreviousSessionStart(24000);
        prefs.commit();
        sessionStart = manager.startSession(appStateManager.getSession());
        assertNotNull(sessionStart);
        assertEquals(Constants.MessageType.SESSION_START, sessionStart.getMessageType());
        assertEquals(42, sessionStart.getLong(Constants.MessageKey.PREVIOUS_SESSION_LENGTH));
        assertEquals(24000, sessionStart.getLong(Constants.MessageKey.PREVIOUS_SESSION_START));
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(messageHandler.obtainMessage(MessageHandler.END_ORPHAN_SESSIONS, configManager.getMpid()));
    }

    @Test
    public void testIncrementSessionCounter(){
        int count = configManager.getUserStorage().getCurrentSessionCounter(-5);
        assertEquals(-5, count);
        for (int i = 0; i < 10; i++){
            configManager.getUserStorage().incrementSessionCounter();
        }
        count = configManager.getUserStorage().getCurrentSessionCounter(-5);
        assertEquals(10, count);
    }

    @Test
    public void testUpdateSessionEnd() throws Exception {
        manager.updateSessionEnd(null);
        InternalSession session = new InternalSession().start(context);
        long currentTime = System.currentTimeMillis();
        session.mSessionStartTime = currentTime - 10000;
        AtomicLong stoppedTime = new AtomicLong(currentTime - 5000);
        session.updateBackgroundTime(stoppedTime, currentTime);
        session.mLastEventTime = currentTime;
        manager.updateSessionEnd(session);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
        long time = configManager.getUserStorage().getPreviousSessionForegound(-1);
        assertEquals(5000, time);
    }

    @Test
    public void testEndSession() throws Exception {
        InternalSession session = appStateManager.getSession().start(context);
        assertNotEquals(Constants.NO_SESSION_ID, session.mSessionID);
        manager.endSession(session);
    }

    @Test
    public void testLogEvent() throws Exception {
        appStateManager.getSession().start(context);
        manager.logEvent(null, "test screen name");
        manager.logEvent(null, null);
        Map<String, String> info = new HashMap<String, String>(1);
        info.put("test key", "test value");
        MPEvent event = new MPEvent.Builder("test event name", MParticle.EventType.Location).duration(100).addCustomFlag("flag 1", "value 1")
                .addCustomFlag("flag 1", "value 2").addCustomFlag("flag 2", "value 3").info(info).build();
        MessageManager.BaseMPMessage message = manager.logEvent(event, "test screen name");
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
        JSONObject flags = message.getJSONObject("flags");
        JSONArray flag1 = flags.getJSONArray("flag 1");
        assertEquals(flag1.length(), 2);
        assertEquals(flag1.get(0), "value 1");
        assertEquals(flag1.get(1), "value 2");
        JSONArray flag2 = flags.getJSONArray("flag 2");
        assertEquals(flag2.length(), 1);
        assertEquals(flag2.get(0), "value 3");
        assertEquals(101, context.getSharedPreferences("name", 0).getInt(Constants.PrefKeys.EVENT_COUNTER, -1));
        Mockito.verify(messageHandler, Mockito.times(101)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogCommerceEventWithNullUser() throws Exception {
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("foo", "bar", 10).build()).build();
        MessageManager.BaseMPMessage message = manager.logEvent(event);
        assertNotNull(message);

        //Mockito defaults to an Answer of 1
        assertEquals(1L, message.getMpId());
    }

    @Test
    public void testLogScreen() throws Exception {
        manager.logScreen(new MPEvent.Builder("screen name").build(), true);
        manager.logScreen(null, true);
        appStateManager.getSession().start(context);
        Map<String, String> info = new HashMap<String, String>();
        info.put("test key", "test value");
        MessageManager.BaseMPMessage message = manager.logScreen(new MPEvent.Builder("screen name").addCustomFlag("flag 1", "value 1")
                .addCustomFlag("flag 1", "value 2").addCustomFlag("flag 2", "value 3").info(info).build(), true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.SCREEN_VIEW, message.getMessageType());
        assertEquals(appStateManager.getSession().mSessionID, message.getSessionId());
        assertEquals(message.getDouble(Constants.MessageKey.EVENT_DURATION), 0, 2);
        assertEquals(message.getString(Constants.MessageKey.SCREEN_STARTED), "activity_started");
        assertEquals(message.getLong(Constants.MessageKey.EVENT_START_TIME), appStateManager.getSession().mLastEventTime);
        JSONObject flags = message.getJSONObject("flags");
        JSONArray flag1 = flags.getJSONArray("flag 1");
        assertEquals(flag1.length(), 2);
        assertEquals(flag1.get(0), "value 1");
        assertEquals(flag1.get(1), "value 2");
        JSONArray flag2 = flags.getJSONArray("flag 2");
        assertEquals(flag2.length(), 1);
        assertEquals(flag2.get(0), "value 3");
        JSONObject attrs = message.getJSONObject(Constants.MessageKey.ATTRIBUTES);
        assertNotNull(attrs);
        assertEquals("test value", attrs.getString("test key"));
        assertEquals("screen name", message.getName());
        message = manager.logScreen(new MPEvent.Builder("screen name 2").info(info).build(), false);
        assertEquals(message.getString(Constants.MessageKey.SCREEN_STARTED), "activity_stopped");
        Mockito.verify(messageHandler, Mockito.times(3)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogBreadcrumb() throws Exception {
        appStateManager.getSession().start(context);
        manager.logBreadcrumb(null);
        MessageManager.BaseMPMessage message = manager.logBreadcrumb("test crumb");
        assertNotNull(message);
        assertEquals(Constants.MessageType.BREADCRUMB, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(appStateManager.getSession().mSessionID, message.getSessionId());
        assertEquals(message.getLong(Constants.MessageKey.EVENT_START_TIME), appStateManager.getSession().mLastEventTime);
        assertEquals(message.getInt(Constants.MessageKey.BREADCRUMB_SESSION_COUNTER), configManager.getUserStorage().getCurrentSessionCounter());
        assertEquals(message.getString(Constants.MessageKey.BREADCRUMB_LABEL), "test crumb");
        Mockito.verify(messageHandler, Mockito.times(2)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testOptOut() throws Exception {
        appStateManager.getSession().start(context);
        long optOutTime = System.currentTimeMillis();
        MessageManager.BaseMPMessage message = manager.optOut(optOutTime, true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.OPT_OUT, message.getMessageType());
        assertTrue(message.getBoolean(Constants.MessageKey.OPT_OUT_STATUS));
        assertEquals(message.getLong(TIMESTAMP), optOutTime);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
        MessageManager.BaseMPMessage message2 = manager.optOut(optOutTime, false);
        assertFalse(message2.getBoolean(Constants.MessageKey.OPT_OUT_STATUS));
    }

    @Test
    public void testLogErrorEvent() throws Exception {
        appStateManager.getSession().start(context);
        String errorMessage = "message";
        Throwable t = new Throwable("test");
        JSONObject attrs = new JSONObject();
        attrs.put("test key", "test value");
        manager.logErrorEvent(null, null, null);
        manager.logErrorEvent(null, null, null, false);
        manager.logErrorEvent(null, null, null, true);
        manager.logErrorEvent(errorMessage, null, null, true);
        manager.logErrorEvent(null, t, null, true);
        MessageManager.BaseMPMessage message = manager.logErrorEvent(errorMessage, t, attrs, true);
        assertNotNull(message);
        assertEquals(Constants.MessageType.ERROR, message.getMessageType());
        assertEquals(appStateManager.getSession().mLastEventTime, message.getTimestamp());
        assertEquals(message.getString(Constants.MessageKey.ERROR_MESSAGE), t.getMessage());
        assertEquals(message.getString(Constants.MessageKey.ERROR_SEVERITY), "error");
        assertEquals(message.getString(Constants.MessageKey.ERROR_CLASS), t.getClass().getCanonicalName());
        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        assertEquals(message.getString(Constants.MessageKey.ERROR_STACK_TRACE), stringWriter.toString());
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), configManager.getUserStorage().getCurrentSessionCounter());
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
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), configManager.getUserStorage().getCurrentSessionCounter());
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
        assertEquals(message.getInt(Constants.MessageKey.ERROR_SESSION_COUNT), configManager.getUserStorage().getCurrentSessionCounter());
        Mockito.verify(messageHandler, Mockito.times(5)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testLogNetworkPerformanceEvent() throws Exception {
        appStateManager.getSession().start(context);
        MessageManager.BaseMPMessage message = manager.logNetworkPerformanceEvent(0, null, null, 0, 0, 0, null);
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
        assertEquals(message.getLong(TIMESTAMP), 1);
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testSetPushRegistrationId() throws Exception {
        appStateManager.getSession().start(context);
        MessageManager.BaseMPMessage message = manager.setPushRegistrationId(null, true);
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
        Mockito.verify(uploadHandler, Mockito.times(1)).removeMessages(UploadHandler.UPLOAD_MESSAGES, configManager.getMpid());
        Mockito.verify(uploadHandler, Mockito.times(1)).sendMessageDelayed(uploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, configManager.getMpid()), Constants.INITIAL_UPLOAD_DELAY);
    }

    @Test
    public void testDoUpload() throws Exception {
        manager.doUpload();
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));
    }

    @Test
    public void testSetLocation() throws Exception {
        Location location = Mockito.mock(Location.class);
        manager.setLocation(location);
        assertEquals(location, manager.getLocation());
    }

    @Test
    public void testLogStateTransition() throws Exception {
        appStateManager.getSession().start(context);
        manager.mInstallType = MParticle.InstallType.KnownInstall;
        MessageManager.BaseMPMessage message = manager.logStateTransition(null, null, null, null, null, 0, 0, 0);
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
        Mockito.verify(messageHandler, Mockito.times(1)).sendMessage(Mockito.any(Message.class));

    }

    @Test
    public void testLogUserAttributeChange() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", "this is the new value", "this is the old value", false, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals("this is the new value", message.getString("nv"));
        assertEquals("this is the old value", message.getString("ov"));
        assertEquals(false, message.getBoolean("d"));
        assertEquals(false, message.getBoolean("na"));

        List<String> newValue = new ArrayList<String>();
        newValue.add("this is a new value");
        newValue.add("this is another new value");

        List<String> oldValue = new ArrayList<String>();
        oldValue.add("this is an old value");
        oldValue.add("this is another old value");

        message = manager.logUserAttributeChangeMessage("this is a key", newValue, oldValue, false, true, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals(2, message.getJSONArray("nv").length());
        assertEquals(2, message.getJSONArray("ov").length());
        assertEquals("this is an old value", message.getJSONArray("ov").get(0));
        assertEquals("this is a new value", message.getJSONArray("nv").get(0));
        assertEquals(false, message.getBoolean("d"));
        assertEquals(true, message.getBoolean("na"));
    }

    @Test
    public void testLogUserAttributeChangeNewAttribute() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", "this is the new value", null, false, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals("this is the new value", message.getString("nv"));
        assertEquals(JSONObject.NULL, message.get("ov"));
        assertEquals(false, message.getBoolean("d"));
    }

    @Test
    public void testLogUserAttributeChangeNewTag() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", null, null, false, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals(JSONObject.NULL, message.get("ov"));
        assertEquals(JSONObject.NULL, message.get("nv"));
        assertEquals(false, message.getBoolean("d"));
    }

    @Test
    public void testLogUserAttributeChangeTagToAttribute() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", "this is the new value", null, false, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals(JSONObject.NULL, message.get("ov"));
        assertEquals("this is the new value", message.get("nv"));
        assertEquals(false, message.getBoolean("d"));
    }

    @Test
    public void testLogUserAttributeChangeStringToList() throws Exception {
        List<String> newValue = new ArrayList<String>();
        newValue.add("this is a new value");
        newValue.add("this is another new value");
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", newValue, "this is the old value", false, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals(2, message.getJSONArray("nv").length());
        assertEquals("this is a new value", message.getJSONArray("nv").get(0));
        assertEquals("this is the old value", message.get("ov"));
        assertEquals(false, message.getBoolean("d"));
    }

    @Test
    public void testLogUserAttributeChangeRemoveTag() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", null, null, true, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals(JSONObject.NULL, message.get("ov"));
        assertEquals(JSONObject.NULL, message.get("nv"));
        assertEquals(true, message.getBoolean("d"));
    }

    @Test
    public void testLogUserAttributeChangeRemoveAttribute() throws Exception {
        MessageManager.BaseMPMessage message = manager.logUserAttributeChangeMessage("this is a key", null, "this is the old value", true, false, 0, 1);
        assertEquals("this is a key", message.getString("n"));
        assertEquals("this is the old value", message.get("ov"));
        assertEquals(JSONObject.NULL, message.get("nv"));
        assertEquals(true, message.getBoolean("d"));
    }

    @Test
    public void testLogUserIdentityChangeMessage() throws Exception {
        JSONObject oldIdentity = new JSONObject();
        JSONObject newIdentity = new JSONObject();
        JSONArray identities = new JSONArray();
        MessageManager.BaseMPMessage message = manager.logUserIdentityChangeMessage(newIdentity, oldIdentity, identities, defaultId);
        assertEquals(message.get("oi"), oldIdentity);
        assertEquals(message.get("ni"), newIdentity);
        assertEquals(message.get("ui"), identities);
    }

    @Test
    public void testLogUserIdentityChangeMessageNewIdentity() throws Exception {
        JSONObject newIdentity = new JSONObject();
        JSONArray identities = new JSONArray();
        MessageManager.BaseMPMessage message = manager.logUserIdentityChangeMessage(newIdentity, null, identities, defaultId);
        assertEquals(message.get("oi"), JSONObject.NULL);
        assertEquals(message.get("ni"), newIdentity);
        assertEquals(message.get("ui"), identities);
    }

    @Test
    public void testLogUserIdentityChangeMessageRemoveIdentity() throws Exception {
        JSONObject oldIdentity = new JSONObject();
        JSONArray identities = new JSONArray();
        MessageManager.BaseMPMessage message = manager.logUserIdentityChangeMessage(null, oldIdentity, identities, defaultId);
        assertEquals(message.get("ni"), JSONObject.NULL);
        assertEquals(message.get("oi"), oldIdentity);
        assertEquals(message.get("ui"), identities);
    }

    @Test
    public void testToListMan() {
        Map<String, String> map = RandomUtils.getInstance().getRandomAttributes(10);
        Map<String, List<String>> listMap = new MessageManager().toStringListMap(map);
        assertEquals(map.size(), listMap.size());
        for (Map.Entry<String, String> entry: map.entrySet()) {
            assertEquals(1, listMap.get(entry.getKey()).size());
            assertEquals(entry.getValue(), listMap.get(entry.getKey()).get(0));
        }

        assertEquals(null, new MessageManager().toStringListMap(null));
    }

    @Test
    public void testAliasRequestSerialization() throws JSONException {
        AliasRequest request = TestingUtils.getInstance().getRandomAliasRequest();
        MessageManager.MPAliasMessage aliasMessage = new MessageManager.MPAliasMessage(request, "das", "apiKey");
        assertEquals(request, aliasMessage.getAliasRequest());
    }
}