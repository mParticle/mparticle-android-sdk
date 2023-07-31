package com.mparticle.networking;

public interface MParticleBaseClient {
    BaseNetworkConnection getRequestHandler();

    void setRequestHandler(BaseNetworkConnection handler);
}
