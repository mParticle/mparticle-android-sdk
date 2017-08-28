package com.mparticle.internal.networking;

public interface MParticleBaseClient {
    BaseNetworkConnection getRequestHandler();
    void setRequestHandler(BaseNetworkConnection handler);
}
