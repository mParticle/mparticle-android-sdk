package com.mparticle.identity;

import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.identity.MParticleUserDelegate;
import com.mparticle.internal.dto.MParticleUserDTO;

public interface MParticleIdentityClient {

    MParticleUserDTO login(IdentityApiRequest request) throws Exception;
    MParticleUserDTO logout(IdentityApiRequest request) throws Exception;
    MParticleUserDTO identify(IdentityApiRequest request) throws Exception;
    Boolean modify(IdentityApiRequest request) throws Exception;
}
