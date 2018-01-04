package com.mparticle.identity;

public interface UserAliasHandler {
    void onUserAlias(MParticleUser previousUser, MParticleUser newUser);
}
