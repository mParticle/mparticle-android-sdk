package com.mparticle.internal;

import android.content.SharedPreferences;
import android.os.Message;

import com.mparticle.AppStateManager;
import com.mparticle.ConfigManager;
import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class MessageManagerTest {

    private MockContext context;
    private ConfigManager configManager;
    private AppStateManager appStateManager;
    private MessageManager manager;
    private Session session;
    private MessageHandler messageHandler;
    private UploadHandler uploadHandler;

    @Before
    public void setup() throws Exception {
        context = new MockContext();
        configManager = Mockito.mock(ConfigManager.class);
        appStateManager = Mockito.mock(AppStateManager.class);
        messageHandler = Mockito.mock(MessageHandler.class);
        uploadHandler = Mockito.mock(UploadHandler.class);
        manager = new MessageManager(context,
                configManager,
                MParticle.InstallType.AutoDetect, appStateManager,
                messageHandler,
                uploadHandler);
        session = new Session().start();
        assertTrue(session.mSessionID.length() > 0);
        Mockito.when(appStateManager.getSession()).thenReturn(session);

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
        MPMessage firstRun = manager.createFirstRunMessage();
        assertEquals(Constants.MessageType.FIRST_RUN, firstRun.getMessageType());
        assertEquals(firstRun.getSessionId(), session.mSessionID);
        assertEquals(session.mSessionStartTime, firstRun.getTimestamp());
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
        assertEquals(session.mSessionID, prefs.getString(Constants.PrefKeys.PREVIOUS_SESSION_ID, null));
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
/*
    @Test
    public void testUpdateSessionEnd() throws Exception {

    }

    @Test
    public void testEndSession() throws Exception {

    }

    @Test
    public void testLogEvent() throws Exception {

    }

    @Test
    public void testLogScreen() throws Exception {

    }

    @Test
    public void testLogBreadcrumb() throws Exception {

    }

    @Test
    public void testOptOut() throws Exception {

    }

    @Test
    public void testLogErrorEvent() throws Exception {

    }

    @Test
    public void testLogErrorEvent1() throws Exception {

    }

    @Test
    public void testLogNetworkPerformanceEvent() throws Exception {

    }

    @Test
    public void testSetPushRegistrationId() throws Exception {

    }

    @Test
    public void testSetSessionAttributes() throws Exception {

    }

    @Test
    public void testStartUploadLoop() throws Exception {

    }

    @Test
    public void testDoUpload() throws Exception {

    }

    @Test
    public void testSetLocation() throws Exception {

    }

    @Test
    public void testLogStateTransition() throws Exception {

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

    }*/
}