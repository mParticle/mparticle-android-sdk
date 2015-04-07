package com.mparticle.internal;



import com.mparticle.AppStateManager;
import com.mparticle.MockConfigManager;
import com.mparticle.MockContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


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

    }

    @Test
    public void testHandleMessage() throws Exception {

    }

    @Test
    public void testShouldDelete() throws Exception {

    }

    @Test
    public void testSetApiClient() throws Exception {

    }

    @Test
    public void testFetchSegments() throws Exception {

    }

    @Test
    public void testHandleMessage1() throws Exception {

    }

    @Test
    public void testGetDeviceInfo() throws Exception {

    }

    @Test
    public void testGetAppInfo() throws Exception {

    }

    @Test
    public void testSetConnected1() throws Exception {

    }

    @Test
    public void testHandleMessage2() throws Exception {

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