package com.mparticle.identity;

interface UserAliasHandler {
    void onUserAlias(MParticleUser previousUser, MParticleUser newUser);
}
