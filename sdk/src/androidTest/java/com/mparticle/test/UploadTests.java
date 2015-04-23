package com.mparticle.test;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.IMPApiClient;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.internal.MessageBatch;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class UploadTests extends AndroidTestCase {

    private static final Object exceptionLock = new Object();
    private Exception exception = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MParticle.start(getContext());
        MParticle.getInstance().upload();
        MParticle.getInstance().setEnvironment(MParticle.Environment.Development);
    }

    public void setException(Exception e) {
        synchronized (exceptionLock) {
            exception = e;
        }
    }

    @Test
    public void testMessageBatchFormat() {
        try {
            MParticle.getInstance().setUserIdentity("some id", MParticle.IdentityType.CustomerId);
            MParticle.getInstance().setUserAttribute("some key", "some value");
            MPMessage message = new MPMessage.Builder("type", "id", null).build();
            JSONArray array = new JSONArray();
            array.put(message);
            JSONObject batch = MessageBatch.create(getContext(), array, false, new JSONObject(), new JSONObject(), MParticle.getInstance().internal().getConfigurationManager(), TestUtils.getSharedPrefs(getContext()), null);
            assertNotNull(batch);
            assertNotNull(batch.get(Constants.MessageKey.TYPE));
            assertNotNull(batch.get(Constants.MessageKey.ID));
            assertNotNull(batch.get(Constants.MessageKey.TIMESTAMP));
            assertNotNull(batch.get(Constants.MessageKey.MPARTICLE_VERSION));
            assertNotNull(batch.get(Constants.MessageKey.OPT_OUT_HEADER));
            assertNotNull(batch.get(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL));
            assertNotNull(batch.get(Constants.MessageKey.CONFIG_SESSION_TIMEOUT));
            assertNotNull(batch.get(Constants.MessageKey.APP_INFO));
            assertNotNull(batch.get(Constants.MessageKey.DEVICE_INFO));
            assertNotNull(batch.get(Constants.MessageKey.SANDBOX));
            assertNotNull(batch.get(Constants.MessageKey.LTV));
            assertNotNull(batch.get(Constants.MessageKey.USER_ATTRIBUTES));
            assertNotNull(batch.get(Constants.MessageKey.USER_IDENTITIES));
            assertNotNull(batch.get(Constants.MessageKey.MESSAGES));
        }catch (Exception e){
            fail(e.toString());
        }
    }

    @Test
    public void testPushIntegration(){

        final CountDownLatch signal = new CountDownLatch(1);
        Intent pushIntent = new Intent();
        Bundle extras = PushTests.getMpExtras(PushTests.MP_JSON);
        extras.putString("m_expy", Long.toString(System.currentTimeMillis() + 60000));
        pushIntent.putExtras(extras);

        MPCloudNotificationMessage message = null;
        try {
            message = (MPCloudNotificationMessage) AbstractCloudMessage.createMessage(pushIntent, null);
        } catch (AbstractCloudMessage.InvalidGcmMessageException e) {
            assertNotNull(e);
            return;
        }

        MParticle.getInstance().internal().getMessageManager().mUploadHandler.setApiClient(new IMPApiClient() {
            @Override
            public void fetchConfig() throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPConfigException {

            }

            @Override
            public HttpURLConnection sendMessageBatch(String message) throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPRampException {
                try {
                    Log.d("MParticle Unit Test", "inspecting message batch");
                    JSONObject requestJson = new JSONObject(message);
                    JSONArray messages = requestJson.getJSONArray("msgs");
                    String error = "Push message not found";
                    for (int i = 0; i < messages.length(); i++){
                        JSONObject object = messages.getJSONObject(i);
                        Log.v("MParticle Unit Test", object.toString(4));
                        if (object.get(Constants.MessageKey.TYPE).equals(Constants.MessageType.PUSH_RECEIVED)){
                            Log.d("MParticle Unit Test", "Push message found!");
                            setException(null);
                            error = null;
                            signal.countDown();
                            break;
                        }
                    }
                    if (error != null){
                        Log.d("MParticle Unit Test", "Push message NOT found!");
                        setException(new Exception(error));
                    }

                }catch (Exception e){
                    Log.d("MParticle Unit Test", "Failing!!! " + e.getMessage());
                    setException(e);
                }finally {

                }
                URL url = new URL("http://www.mparticle.com");
                MockHttpUrlConnection connection = new MockHttpUrlConnection(url);
                connection.setResponseCode(202);

                return connection;
            }

            @Override
            public HttpURLConnection sendCommand(String commandUrl, String method, String postData, String headers) {
                return null;
            }

            @Override
            public JSONObject fetchAudiences() {
                return null;
            }

            @Override
            public boolean isThrottled() {
                return false;
            }

            @Override
            public JSONObject getCookies() {
                return null;
            }
        });

        MParticle.getInstance().internal().logNotification(message, null, true, AppStateManager.APP_STATE_NOTRUNNING, AbstractCloudMessage.FLAG_RECEIVED);

        MParticle.getInstance().upload();
        try {
            signal.await(60, TimeUnit.SECONDS);
        }catch (Exception e){
            fail(e.toString());
        }
        if (exception != null){
            Log.d("MParticle Unit Test", "Exception not null - Failing!!!");
            fail(exception.toString());
        }


    }


}
