package com.mparticle.identity;

import com.mparticle.networking.MParticleBaseClient;

public interface MParticleIdentityClient extends MParticleBaseClient {
    IdentityHttpResponse login(IdentityApiRequest request) throws Exception;

    IdentityHttpResponse logout(IdentityApiRequest request) throws Exception;

    IdentityHttpResponse identify(IdentityApiRequest request) throws Exception;

    IdentityHttpResponse modify(IdentityApiRequest request) throws Exception;
}
