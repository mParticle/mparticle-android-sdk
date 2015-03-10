package com.mparticle.internal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * API client interface primarily needed for mocking/testing.
 */
public interface IMPApiClient {

    public void fetchConfig()  throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPConfigException;
    public HttpURLConnection sendMessageBatch(String message) throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPRampException;
    public HttpURLConnection sendCommand(String commandUrl, String method, String postData, String headers) throws IOException, JSONException;
    public JSONObject fetchAudiences();
}
