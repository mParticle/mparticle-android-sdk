package com.mparticle.internal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * API client interface primarily needed for mocking/testing.
 */
public interface IMPApiClient {

    void fetchConfig()  throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPConfigException;
    int sendMessageBatch(String message) throws IOException, MParticleApiClient.MPThrottleException, MParticleApiClient.MPRampException;
    HttpURLConnection sendCommand(String commandUrl, String method, String postData, String headers) throws IOException, JSONException;
    JSONObject fetchAudiences();
    boolean isThrottled();
    JSONObject getCookies();
}
