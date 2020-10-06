package com.mparticle.internal;



import android.content.Context;
import android.database.Cursor;
import android.os.Message;
import androidx.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.SdkListener;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.AliasResponse;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.messages.MPAliasMessage;
import com.mparticle.mock.MockContext;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class UploadHandlerTest {
    UploadHandler handler;
    private ConfigManager mConfigManager;

    @Before
    public void setUp() throws Exception {
        MParticle.setInstance(new MockMParticle());
        AppStateManager stateManager = Mockito.mock(AppStateManager.class);
        mConfigManager = MParticle.getInstance().Internal().getConfigManager();
        handler = new UploadHandler(new MockContext(), mConfigManager, stateManager, Mockito.mock(MessageManager.class), Mockito.mock(MParticleDBManager.class));
    }

    @Test
    public void testSetConnected() throws Exception {
        handler.isNetworkConnected = true;
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(handler.isNetworkConnected);
                handler.setConnected(false);
                latch.countDown();
            }
        }).start();
        latch.await(1000, TimeUnit.MILLISECONDS);
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
            if (i != 200) {
                assertFalse(handler.shouldDelete(i));
            }
        }
        assertTrue(handler.shouldDelete(200));
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
        Mockito.when(handler.mParticleDBManager.deleteUpload(Mockito.anyInt())).thenReturn(1);
        handler.setApiClient(apiClient);
        handler.uploadMessage(522, "");
        String[] params = {"522"};
    }

    @Test
    public void testGetDeviceInfo() throws Exception {
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(new MockContext());
        assertNotNull(attributes);
    }

    @Test
    public void testGetAppInfo() throws Exception {
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(new MockContext());
        assertNotNull(attributes);
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDAllDefaults() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(true);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AdIdInfo(AAID, false, MPUtility.AdIdInfo.Advertiser.AMAZON));
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(new MockContext());
        assertFalse(attributes.getBoolean("lat"));
        assertEquals(AAID, attributes.getString("faid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATTrueRestrictTrue() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(true);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AdIdInfo(AAID, true, MPUtility.AdIdInfo.Advertiser.GOOGLE));
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(new MockContext());
        assertTrue(attributes.getBoolean("lat"));
        assertFalse(attributes.has("gaid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATTrueRestrictFalse() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(false);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AdIdInfo(AAID, true, MPUtility.AdIdInfo.Advertiser.AMAZON));
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.FIRE_OS).getDeviceInfo(new MockContext());
        assertTrue(attributes.getBoolean("lat"));
        assertEquals(AAID, attributes.getString("faid"));
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testGetAAIDLATFalseRestrictLatFalse() throws Exception {
        final String AAID = UUID.randomUUID().toString();
        Mockito.when(mConfigManager.getRestrictAAIDBasedOnLAT()).thenReturn(false);
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.getAdIdInfo(Mockito.any(Context.class))).thenReturn(new MPUtility.AdIdInfo(AAID, false, MPUtility.AdIdInfo.Advertiser.GOOGLE));
        JSONObject attributes = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(new MockContext());
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
        handler.upload(true);
    }

    @Test
    public void testUploadSessionHistory() throws Exception {
        handler.handleMessage(null);
        Cursor mockCursor = Mockito.mock(Cursor.class);
        Mockito.when(handler.mParticleDBManager.getReadyUploads())
                .thenReturn(new ArrayList<MParticleDBManager.ReadyUpload>(){
                    {
                        add(new MParticleDBManager.ReadyUpload(123, false, "a message batch"));
                    }
                });
        MParticleApiClient mockApiClient = Mockito.mock(MParticleApiClient.class);
        handler.setApiClient(mockApiClient);
        Mockito.when(mConfigManager.getIncludeSessionHistory()).thenReturn(true);
        Mockito.when(mockCursor.moveToNext()).thenReturn(true, false);
        handler.upload(true);
        Mockito.verify(mockApiClient).sendMessageBatch(Mockito.eq("a message batch"));
    }

    @Test
    public void testRetryLogic() throws IOException, MParticleApiClientImpl.MPThrottleException, JSONException, MParticleApiClientImpl.MPRampException {
        final AndroidUtils.Mutable<Integer> deleteId = new AndroidUtils.Mutable<Integer>(null);

        MParticleDBManager database = new MParticleDBManager(new MockContext()) {

            @Override
            public int deleteUpload(int id) {
                deleteId.value = id;
                return id;
            }
        };

        UploadHandler uploadHandler = new UploadHandler(new MockContext(),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                Mockito.mock(MessageManager.class),
                database) {
            @Override
            public boolean shouldDelete(int statusCode) {
                return false;
            }
        };

        MParticleApiClient mockApiClient = Mockito.mock(MParticleApiClient.class);
        Mockito.when(mockApiClient.sendAliasRequest(Mockito.any(String.class))).thenReturn(new MParticleApiClient.AliasNetworkResponse(0));
        uploadHandler.setApiClient(mockApiClient);

        assertNull(deleteId.value);

        AliasRequest aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();
        JSONObject request = new MPAliasMessage(aliasRequest, "das","apiKey");
        uploadHandler.uploadAliasRequest(1, request.toString());

        assertNull(deleteId.value);
    }

    @Test
    public void testDeleteLogic() throws IOException, MParticleApiClientImpl.MPThrottleException, JSONException, MParticleApiClientImpl.MPRampException {
        final AndroidUtils.Mutable<Integer> deletedUpload = new AndroidUtils.Mutable<Integer>(null);

        MParticleDBManager database = new MParticleDBManager(new MockContext()) {
            @Override
            public int deleteUpload(int id) {
                deletedUpload.value = id;
                return id;
            }
        };

        UploadHandler uploadHandler = new UploadHandler(new MockContext(),
                Mockito.mock(ConfigManager.class),
                Mockito.mock(AppStateManager.class),
                Mockito.mock(MessageManager.class),
                database) {
            @Override
            public boolean shouldDelete(int statusCode) {
                return true;
            }
        };

        MParticleApiClient mockApiClient = Mockito.mock(MParticleApiClient.class);
        Mockito.when(mockApiClient.sendAliasRequest(Mockito.any(String.class))).thenReturn(new MParticleApiClient.AliasNetworkResponse(0));
        uploadHandler.setApiClient(mockApiClient);


        assertNull(deletedUpload.value);

        AliasRequest aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();

        uploadHandler.uploadAliasRequest(1, new MPAliasMessage(aliasRequest, "das", "apiKey").toString());

        assertNotNull(deletedUpload.value);
    }

    @PrepareForTest({MPUtility.class})
    public void testAliasCallback() throws MParticleApiClientImpl.MPRampException, MParticleApiClientImpl.MPThrottleException, JSONException, IOException {
        RandomUtils ran = new RandomUtils();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.isAppDebuggable(Mockito.any(Context.class))).thenReturn(true);
        final AndroidUtils.Mutable<AliasResponse> capturedResponse = new AndroidUtils.Mutable<AliasResponse>(null);
        SdkListener sdkListener = new SdkListener() {
            @Override
            public void onAliasRequestFinished(@NonNull AliasResponse aliasResponse) {
                capturedResponse.value = aliasResponse;
            }
        };

        MParticle.addListener(new MockContext(), sdkListener);
        MParticleApiClient mockApiClient = Mockito.mock(MParticleApiClient.class);
        handler.setApiClient(mockApiClient);

        //test successful request
        Mockito.when(mockApiClient.sendAliasRequest(Mockito.any(String.class))).thenReturn(new MParticleApiClient.AliasNetworkResponse(202));

        assertNull(capturedResponse.value);

        AliasRequest aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();
        MPAliasMessage aliasRequestMessage = new MPAliasMessage(aliasRequest, "das", "apiKey");
        handler.uploadAliasRequest(1, aliasRequestMessage.toString());

        assertTrue(capturedResponse.value.isSuccessful());
        assertNull(capturedResponse.value.getErrorResponse());
        assertFalse(capturedResponse.value.willRetry());
        assertEquals(aliasRequest, capturedResponse.value.getRequest());
        assertEquals(202, capturedResponse.value.getResponseCode());
        assertEquals(aliasRequestMessage.getRequestId(), capturedResponse.value.getRequestId());
        capturedResponse.value = null;


        //test retry request
        Mockito.when(mockApiClient.sendAliasRequest(Mockito.any(String.class))).thenReturn(new MParticleApiClient.AliasNetworkResponse(429));

        assertNull(capturedResponse.value);

        aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();
        aliasRequestMessage = new MPAliasMessage(aliasRequest, "das", "apiKey");
        handler.uploadAliasRequest(2, aliasRequestMessage.toString());

        assertFalse(capturedResponse.value.isSuccessful());
        assertNull(capturedResponse.value.getErrorResponse());
        assertTrue(capturedResponse.value.willRetry());
        assertEquals(aliasRequest, capturedResponse.value.getRequest());
        assertEquals(429, capturedResponse.value.getResponseCode());
        assertEquals(aliasRequestMessage.getRequestId(), capturedResponse.value.getRequestId());
        capturedResponse.value = null;

        //test error message present
        String error = ran.getAlphaNumericString(20);
        Mockito.when(mockApiClient.sendAliasRequest(Mockito.any(String.class))).thenReturn(new MParticleApiClient.AliasNetworkResponse(400, error));

        assertNull(capturedResponse.value);

        aliasRequest = TestingUtils.getInstance().getRandomAliasRequest();
        aliasRequestMessage = new MPAliasMessage(aliasRequest, "das", "apiKey");
        handler.uploadAliasRequest(3, aliasRequestMessage.toString());

        assertFalse(capturedResponse.value.isSuccessful());
        assertEquals(capturedResponse.value.getErrorResponse(), error);
        assertFalse(capturedResponse.value.willRetry());
        assertEquals(aliasRequest, capturedResponse.value.getRequest());
        assertEquals(aliasRequestMessage.getRequestId(), capturedResponse.value.getRequestId());
    }
}