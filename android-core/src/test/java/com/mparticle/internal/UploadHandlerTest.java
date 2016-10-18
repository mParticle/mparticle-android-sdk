package com.mparticle.internal;



import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class UploadHandlerTest {
    UploadHandler handler;
    private SQLiteDatabase mockDatabase;
    private ConfigManager mConfigManager;

    @Before
    public void setUp() throws Exception {
        MParticleDatabase db = Mockito.mock(MParticleDatabase.class);
        MParticle.setInstance(Mockito.mock(MParticle.class));
        mockDatabase = Mockito.mock(SQLiteDatabase.class);
        Mockito.when(db.getWritableDatabase()).thenReturn(mockDatabase);
        AppStateManager stateManager = Mockito.mock(AppStateManager.class);
        mConfigManager = Mockito.mock(ConfigManager.class);
        Mockito.when(MParticle.getInstance().getConfigManager()).thenReturn(mConfigManager);
        handler = new UploadHandler(new MockContext(), mConfigManager, db, stateManager, Mockito.mock(MessageManager.class));
    }

    @Test
    public void testSetConnected() throws Exception {
        handler.isNetworkConnected = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(handler.isNetworkConnected);
                handler.setConnected(false);
            }
        }).start();
        Thread.sleep(1000);
        assertFalse(handler.isNetworkConnected);
        handler.isNetworkConnected = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(handler.isNetworkConnected);
            }
        }).start();

    }

    @Test
    public void testHandleMessage() throws Exception {
        Message message = Mockito.mock(Message.class);
        for (int i = 0; i < 30; i++){
            message.what = i;
            handler.handleMessage(message);
        }
    }

    @Test
    public void testShouldDelete() throws Exception {
        for (int i = 0 ; i < 202; i++){
            assertFalse(handler.shouldDelete(i));
        }
        assertTrue(handler.shouldDelete(202));
        for (int i = 203 ; i < 400; i++){
            assertFalse(handler.shouldDelete(i));
        }
        for (int i = 400 ; i < 429; i++){
            assertTrue(handler.shouldDelete(i));
        }
        assertFalse(handler.shouldDelete(429));
        for (int i = 430 ; i < 500; i++){
            assertTrue(handler.shouldDelete(i));
        }
        for (int i = 500 ; i < 600; i++){
            assertFalse(handler.shouldDelete(i));
        }

    }

    @Test
    public void testRampSampling() throws Exception {
        handler.handleMessage(null);
        MParticleApiClientImpl apiClient = Mockito.mock(MParticleApiClientImpl.class);
        MParticleApiClientImpl.MPRampException rampException = new MParticleApiClientImpl.MPRampException();
        Mockito.when(apiClient.sendMessageBatch(Mockito.anyString())).thenThrow(rampException);
        handler.setApiClient(apiClient);
        handler.uploadMessage(522, "");
        String[] params = {"522"};
        Mockito.verify(mockDatabase, Mockito.times(1)).delete(Mockito.anyString(), Mockito.anyString(), AdditionalMatchers.aryEq(params));
    }

    @Test
    public void testGetDeviceInfo() throws Exception {
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertNotNull(attributes);
    }

    @Test
    public void testGetAppInfo() throws Exception {
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertNotNull(attributes);
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDAllDefaults() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(true);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getGoogleAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AndroidAdIdInfo(AAID, false));
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertFalse(attributes.getBoolean("lat"));
        assertEquals(AAID, attributes.getString("gaid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATTrueRestrictTrue() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(true);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getGoogleAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AndroidAdIdInfo(AAID, true));
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertTrue(attributes.getBoolean("lat"));
        assertFalse(attributes.has("gaid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATTrueRestrictFalse() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(false);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getGoogleAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AndroidAdIdInfo(AAID, true));
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertTrue(attributes.getBoolean("lat"));
        assertEquals(AAID, attributes.getString("gaid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATFalseRestrictLatFalse() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(false);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getGoogleAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AndroidAdIdInfo(AAID, false));
        JSONObject attributes = new DeviceAttributes().getDeviceInfo(new MockContext());
        assertFalse(attributes.getBoolean("lat"));
        assertEquals(AAID, attributes.getString("gaid"));
    }

    @Test
    public void testDontUploadSessionHistory() throws Exception {
        handler.handleMessage(null);
        Mockito.when(mConfigManager.getIncludeSessionHistory()).thenReturn(false);
        Cursor mockCursor = Mockito.mock(Cursor.class);
        Mockito.when(mockCursor.moveToNext()).thenReturn(true, false);
        Mockito.when(mockCursor.getInt(Mockito.anyInt())).thenReturn(123);
        Mockito.when(mockCursor.getString(Mockito.anyInt())).thenReturn("cool message batch!");
        Mockito.when(mockDatabase.query(MParticleDatabase.UploadTable.TABLE_NAME, handler.uploadColumns,
                null, null, null, null, MParticleDatabase.UploadTable.CREATED_AT)).thenReturn(mockCursor);
        String[] whereArgs = {Long.toString(123)};
        handler.upload(true);
        Mockito.verify(mockDatabase).delete(Mockito.eq(MParticleDatabase.UploadTable.TABLE_NAME),
                Mockito.eq( "_id=?"), Mockito.eq(whereArgs));
    }

    @Test
    public void testUploadSessionHistory() throws Exception {
        handler.handleMessage(null);
        Cursor mockCursor = Mockito.mock(Cursor.class);
        Mockito.when(mockCursor.moveToNext()).thenReturn(true, false);
        Mockito.when(mockCursor.getInt(Mockito.anyInt())).thenReturn(123);
        Mockito.when(mockCursor.getString(Mockito.anyInt())).thenReturn("cool message batch!");
        Mockito.when(mockDatabase.query(MParticleDatabase.UploadTable.TABLE_NAME, handler.uploadColumns,
                null, null, null, null, MParticleDatabase.UploadTable.CREATED_AT)).thenReturn(mockCursor);
        MParticleApiClient mockApiClient = Mockito.mock(MParticleApiClient.class);
        handler.setApiClient(mockApiClient);
        Mockito.when(mConfigManager.getIncludeSessionHistory()).thenReturn(true);
        Mockito.when(mockCursor.moveToNext()).thenReturn(true, false);
        handler.upload(true);
        Mockito.verify(mockApiClient).sendMessageBatch(Mockito.eq("cool message batch!"));
    }
}