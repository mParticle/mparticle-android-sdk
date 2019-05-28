package com.mparticle.internal;

import android.content.SharedPreferences;

import com.mparticle.identity.AliasRequest;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;
import com.mparticle.networking.MPConnection;
import com.mparticle.networking.MPUrl;
import com.mparticle.networking.MParticleBaseClientImpl;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class MParticleApiClientImplTest {

    MParticleApiClientImpl client;
    private MPConnection mockConnection;
    private ConfigManager configManager;
    private MockSharedPreferences sharedPrefs;

    public void setup() throws Exception {
        configManager = Mockito.mock(ConfigManager.class);
        Mockito.when(configManager.getApiKey()).thenReturn("some api key");
        Mockito.when(configManager.getApiSecret()).thenReturn("some api secret");
        sharedPrefs = new MockSharedPreferences();
        client = new MParticleApiClientImpl(
                configManager,
                sharedPrefs,
                new MockContext()
        );
        client.mDeviceRampNumber = 50;
        client.setSupportedKitString("");
        MPUrl mockUrl = PowerMockito.mock(MPUrl.class);

        mockConnection = PowerMockito.mock(MPConnection.class);
        Mockito.when(mockUrl.openConnection()).thenReturn(mockConnection);
        Mockito.when(mockConnection.getURL()).thenReturn(mockUrl);
        Mockito.when(mockUrl.getFile()).thenReturn("/config");
        client.setConfigUrl(mockUrl);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testAddMessageSignature() throws Exception {
        setup();

        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        ArgumentCaptor<String> headerCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCapture = ArgumentCaptor.forClass(String.class);
        client.addMessageSignature(mockConnection, "this is a sample batch");
        Mockito.verify(mockConnection, Mockito.times(2)).setRequestProperty(headerCapture.capture(), headerValueCapture.capture());
        List<String> headerKeys = headerCapture.getAllValues();
        List<String> headerValues = headerValueCapture.getAllValues();

        int dateIndex = headerKeys.indexOf("Date");
        assertTrue(headerKeys.toString(), dateIndex >= 0);
        String dateValue = headerValues.get(dateIndex);
        assertNotNull(dateValue);
        assertTrue(dateValue.length() > 0);

        int signatureIndex = headerKeys.indexOf("x-mp-signature");
        assertTrue(headerValues.toString(), signatureIndex >= 0);
        String signatureValue = headerValues.get(signatureIndex);
        assertNotNull(signatureValue);
        assertTrue(signatureValue.length() > 0);
    }

    @Test
    public void testRampNumber() throws Exception {
        MockContext context = new MockContext();
        SharedPreferences sharedPreferences = new MockSharedPreferences();
        context.setSharedPreferences(sharedPreferences);

        int test = MPUtility.hashFnv1A(MPUtility.getRampUdid(context).getBytes())
                .mod(BigInteger.valueOf(100))
                .intValue();

        assertTrue("Ramp should be between 0 and 100: " + test, test >= 0 && test < 100);
        int test2 = MPUtility.hashFnv1A(MPUtility.getRampUdid(context).getBytes())
                .mod(BigInteger.valueOf(100))
                .intValue();
        assertTrue(test == test2);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testFetchConfigSuccess() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        Mockito.when(mockConnection.getResponseCode()).thenReturn(200);
        JSONObject response = new JSONObject();
        response.put("test", "value");
        Mockito.when(MPUtility.getJsonResponse(mockConnection)).thenReturn(response);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        client.fetchConfig();
        Mockito.verify(configManager).updateConfig(captor.capture());
        assertEquals(response, captor.getValue());
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testFetchConfigFailure() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        Mockito.when(mockConnection.getResponseCode()).thenReturn(400);
        Exception e = null;
        try {
            client.fetchConfig();
        } catch (MParticleApiClientImpl.MPConfigException cfe) {
            e = cfe;
        }
        assertNotNull(e);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testConfigDelay() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        Mockito.when(mockConnection.getResponseCode()).thenReturn(400);
        Exception e = null;
        try {
            client.fetchConfig();
        } catch (MParticleApiClientImpl.MPConfigException cfe) {
            e = cfe;
        }
        assertNotNull(e);
        e = null;
        try {
            client.fetchConfig();
        } catch (MParticleApiClientImpl.MPConfigException cfe) {
            e = cfe;
        }
        assertNull(e);
        e = null;
        try {
            client.fetchConfig(true);
        } catch (MParticleApiClientImpl.MPConfigException cfe) {
            e = cfe;
        }
        assertNotNull(e);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testCheckThrottleTime() throws Exception {
        setup();

        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            client.checkThrottleTime(endpoint);
            client.getRequestHandler().setNextRequestTime(endpoint, System.currentTimeMillis() + 1000);
            Exception e = null;
            try {
                client.checkThrottleTime(endpoint);
            } catch (MParticleApiClientImpl.MPThrottleException cfe) {
                e = cfe;
            }
            assertNotNull(e);
        }
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testMessageBatchWhileThrottled() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        try {
            client.sendMessageBatch("");
        }catch (Exception e) {
            if (e instanceof MParticleApiClientImpl.MPThrottleException) {
                throw e;
            }
        }
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.EVENTS, System.currentTimeMillis() + 1000);
        Exception e = null;
        try {
            client.sendMessageBatch("");
        } catch (MParticleApiClientImpl.MPThrottleException cfe) {
            e = cfe;
        }
        assertNotNull(e);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testConfigRequestWhileThrottled() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        //set all endpoints throttled, Config should not be listening to any throttle
        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            client.getRequestHandler().setNextRequestTime(endpoint, System.currentTimeMillis() + 1000);
        }
        Mockito.when(mockConnection.getResponseCode()).thenReturn(200);
        JSONObject response = new JSONObject();
        response.put("test", "value");
        Mockito.when(MPUtility.getJsonResponse(mockConnection)).thenReturn(response);
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        client.fetchConfig();
        Mockito.verify(configManager).updateConfig(captor.capture());
        assertEquals(response, captor.getValue());
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testAliasRequestWhileThrottled() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        try {
            client.sendAliasRequest("");
        } catch (Exception e) {
            if (e instanceof MParticleApiClientImpl.MPThrottleException) {
                throw e;
            }
        }
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.ALIAS, System.currentTimeMillis() + 1000);
        Exception e = null;
        try {
            client.sendAliasRequest("");
        } catch (MParticleApiClientImpl.MPThrottleException cfe) {
            e = cfe;
        }
        assertNotNull(e);
    }

    /**
     * make sure that both Events and Alias requests go through when the Other's requests
     * are throttled
     * @throws Exception
     */
    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class, MPUtility.class})
    public void testAliasEventsOnSeparateThrottles() throws Exception {
        setup();
        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.hmacSha256Encode(Mockito.anyString(), Mockito.anyString())).thenReturn("encoded");
        try {
            client.sendMessageBatch("");
            client.sendAliasRequest("");
        } catch (Exception e) {
            if (e instanceof MParticleApiClientImpl.MPThrottleException) {
                throw e;
            }
        }


        //make sure Events still works when Alias is throttled
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.ALIAS, System.currentTimeMillis() + 1000);
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.EVENTS, System.currentTimeMillis() - 1000);
        MParticleApiClientImpl.MPThrottleException ex = null;
        try {
            client.sendMessageBatch("");
        } catch (Exception e) {
            if (e instanceof MParticleApiClientImpl.MPThrottleException) {
                throw e;
            }
        }
        try {
            client.sendAliasRequest("");
        } catch (MParticleApiClientImpl.MPThrottleException e) {
            ex = e;
        }
        assertNotNull(ex);

        //make sure Alias still works when Events is throttled
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.ALIAS, System.currentTimeMillis() - 1000);
        client.getRequestHandler().setNextRequestTime(MParticleBaseClientImpl.Endpoint.EVENTS, System.currentTimeMillis() + 1000);
        ex = null;
        try {
            client.sendAliasRequest("");
        } catch (Exception e) {
            if (e instanceof MParticleApiClientImpl.MPThrottleException) {
                throw e;
            }
        }
        try {
            client.sendMessageBatch("");
        } catch (MParticleApiClientImpl.MPThrottleException e) {
            ex = e;
        }
        assertNotNull(ex);
    }

    @Test
    @PrepareForTest({URL.class, MParticleApiClientImpl.class})
    public void testSetNextAllowedRequestTime() throws Exception {
        setup();

        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {

            assertEquals(0, client.getNextRequestTime(endpoint));
            //need a delta to account for test timing variation
            double delta = 50;
            client.getRequestHandler().setNextAllowedRequestTime(null, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > System.currentTimeMillis() + client.DEFAULT_THROTTLE_MILLIS - delta);


            Mockito.when(mockConnection.getHeaderField(Mockito.anyString())).thenReturn(null);
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta);

            Mockito.when(mockConnection.getHeaderField("Retry-After")).thenReturn("");
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta);

            Mockito.when(mockConnection.getHeaderField("Retry-After")).thenReturn("-1000");
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > client.DEFAULT_THROTTLE_MILLIS + System.currentTimeMillis() - delta);

            Mockito.when(mockConnection.getHeaderField("Retry-After")).thenReturn("60");
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= 60 * 1000 + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > 60 * 1000 + System.currentTimeMillis() - delta);

            Mockito.when(mockConnection.getHeaderField("Retry-After")).thenReturn("");
            Mockito.when(mockConnection.getHeaderField("retry-after")).thenReturn("100");
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= 100 * 1000 + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > 100 * 1000 + System.currentTimeMillis() - 10);

            Mockito.when(mockConnection.getHeaderField("Retry-After")).thenReturn(Integer.toString(60 * 60 * 25));
            client.getRequestHandler().setNextRequestTime(endpoint, 0);
            assertEquals(0, client.getNextRequestTime(endpoint));
            client.getRequestHandler().setNextAllowedRequestTime(mockConnection, endpoint);
            assertTrue(client.getNextRequestTime(endpoint) <= client.MAX_THROTTLE_MILLIS + System.currentTimeMillis());
            assertTrue(client.getNextRequestTime(endpoint) > client.MAX_THROTTLE_MILLIS + System.currentTimeMillis() - delta);
        }
    }

}