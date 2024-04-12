package com.mparticle.identity;

public class AudienceApiResult {
    private final MParticleUser user;

    public AudienceApiResult(MParticleUser user) {
        this.user = user;
    }


    public MParticleUser getUser() {
        return user;
    }
}
