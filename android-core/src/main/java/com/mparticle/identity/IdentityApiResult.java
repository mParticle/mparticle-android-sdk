package com.mparticle.identity;

import android.support.annotation.NonNull;

/**
 * A class for expressing the results of an IdentityApi request
 */
public final class IdentityApiResult {
    private final MParticleUser user;
    public IdentityApiResult(@NonNull MParticleUser user) {
        this.user = user;
    }

    /**
     * Query the User which was returned by the IdentityApi request
     * @return the User
     */
    @NonNull
    public MParticleUser getUser() {
        return user;
    }
}
