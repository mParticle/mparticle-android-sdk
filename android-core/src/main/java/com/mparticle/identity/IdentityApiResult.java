package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class for expressing the results of an IdentityApi request
 */
public final class IdentityApiResult {
    private final MParticleUser user;
    private final MParticleUser previousUser;

    public IdentityApiResult(@NonNull MParticleUser user, @Nullable MParticleUser previousUser) {
        this.user = user;
        this.previousUser = previousUser;
    }

    /**
     * Query the User which was returned by the IdentityApi request
     *
     * @return the User
     */
    @NonNull
    public MParticleUser getUser() {
        return user;
    }

    /**
     * The User which is being replaced, if the IdentityApi call this instance is describing, resulted
     * in a new {@link IdentityApi#getCurrentUser()}, otherwise 'null'
     *
     * @return
     */
    @Nullable
    public MParticleUser getPreviousUser() {
        return previousUser;
    }
}
