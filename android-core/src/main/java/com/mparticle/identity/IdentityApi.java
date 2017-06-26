package com.mparticle.identity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.MParticleTask;

public final class IdentityApi {

    @Nullable
    public MParticleUser getCurrentUser() {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public void addIdentityStateListener(IdentityStateListener listener) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public void removeIdentityStateListener(IdentityStateListener listener) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    public MParticleTask<IdentityApiResult> logout(IdentityApiRequest logoutRequest) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    public MParticleTask<IdentityApiResult> login(@Nullable IdentityApiRequest loginRequest) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public MParticleTask<Void> modify(@NonNull IdentityApiRequest updateRequest) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public MParticleTask<IdentityApiResult> identify(IdentityApiRequest identifyRequest) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }
}