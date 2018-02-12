package com.mparticle.identity;

/**
 * A class for expressing the results of an IdentityApi request
 */
public final class IdentityApiResult {
    private final MParticleUser user;
    public IdentityApiResult(MParticleUser user) {
        this.user = user;
    }

    /**
     * Query the User which was returned by the IdentityApi request
     * @return the User
     */
    public MParticleUser getUser() {
        return user;
    }
}
