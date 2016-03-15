package com.mparticle.internal;

import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class MParticleApiClientImplTest {

    MParticleApiClientImpl client;
    private HttpURLConnection mockConnection;
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
        URL mockUrl = PowerMockito.mock(URL.class);

        mockConnection = PowerMockito.mock(HttpURLConnection.class);
        Mockito.when(mockUrl.openConnection()).thenReturn(mockConnection);
        Mockito.when(mockConnection.getURL()).thenReturn(mockUrl);
        Mockito.when(mockUrl.getFile()).thenReturn("/config");
        client.setConfigUrl(mockUrl);
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

}