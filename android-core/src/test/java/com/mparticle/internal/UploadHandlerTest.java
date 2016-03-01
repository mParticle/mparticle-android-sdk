package com.mparticle.internal;



import android.database.sqlite.SQLiteDatabase;
import android.os.Message;

import com.mparticle.mock.MockConfigManager;
import com.mparticle.mock.MockContext;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class UploadHandlerTest {
    UploadHandler handler;
    private SQLiteDatabase mockDatabase;

    @Before
    public void setUp() throws Exception {
        MParticleDatabase db = Mockito.mock(MParticleDatabase.class);
        mockDatabase = Mockito.mock(SQLiteDatabase.class);
        Mockito.when(db.getWritableDatabase()).thenReturn(mockDatabase);
        AppStateManager stateManager = Mockito.mock(AppStateManager.class);
        handler = new UploadHandler(new MockContext(), new MockConfigManager(), db, stateManager);
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
        JSONObject attributes = handler.getDeviceInfo();
        assertNotNull(attributes);
    }

    @Test
    public void testGetAppInfo() throws Exception {
        JSONObject attributes = handler.getAppInfo();
        assertNotNull(attributes);
    }

}