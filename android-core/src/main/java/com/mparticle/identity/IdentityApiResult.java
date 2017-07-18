package com.mparticle.identity;


public final class IdentityApiResult {
    private final MParticleUser user;
    public IdentityApiResult(MParticleUser user) {
        this.user = user;
    }
    public MParticleUser getUser() {
        return user;
    }
}
