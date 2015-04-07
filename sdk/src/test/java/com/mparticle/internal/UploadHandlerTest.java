package com.mparticle.internal;



import android.os.Message;

import com.mparticle.AppStateManager;
import com.mparticle.MockConfigManager;
import com.mparticle.MockContext;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class UploadHandlerTest {
    UploadHandler handler;

    @Before
    public void setUp() throws Exception {
        MParticleDatabase db = Mockito.mock(MParticleDatabase.class);
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
        for (int i = 400 ; i < 500; i++){
            assertTrue(handler.shouldDelete(i));
        }
        for (int i = 500 ; i < 600; i++){
            assertFalse(handler.shouldDelete(i));
        }

    }

    @Test
    public void testSetApiClient() throws Exception {

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

    @Test
    public void testPrepareUploads() throws Exception {

    }

    @Test
    public void testProcessUploads() throws Exception {

    }

    @Test
    public void testShouldDelete1() throws Exception {

    }

    @Test
    public void testProcessCommands() throws Exception {

    }

    @Test
    public void testCreateUploadMessage() throws Exception {

    }

    @Test
    public void testAddGCMHistory() throws Exception {

    }

    @Test
    public void testDbInsertUpload() throws Exception {

    }

    @Test
    public void testDbDeleteProcessedMessages() throws Exception {

    }

    @Test
    public void testDbMarkAsUploadedMessage() throws Exception {

    }

    @Test
    public void testDbDeleteUpload() throws Exception {

    }

    @Test
    public void testDbDeleteCommand() throws Exception {

    }

    @Test
    public void testDbInsertCommand() throws Exception {

    }

    @Test
    public void testSetApiClient1() throws Exception {

    }

    @Test
    public void testFetchSegments1() throws Exception {

    }
}