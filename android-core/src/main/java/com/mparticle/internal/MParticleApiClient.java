package com.mparticle.internal;

import com.mparticle.networking.MParticleBaseClient;

import org.json.JSONObject;

import java.io.IOException;

/**
 * API client interface primarily needed for mocking/testing.
 */
public interface MParticleApiClient extends MParticleBaseClient {
    void fetchConfig() throws IOException, MParticleApiClientImpl.MPConfigException;
    void fetchConfig(boolean force) throws IOException, MParticleApiClientImpl.MPConfigException;
    int sendMessageBatch(String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException;
    JSONObject fetchAudiences();
    boolean isThrottled();
    JSONObject getCookies();

}