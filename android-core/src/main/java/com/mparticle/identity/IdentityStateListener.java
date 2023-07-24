package com.mparticle.identity;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The IdentityStateListener is a callback which will be invoked when either a new user is identified,
 * or the current user's "logged in" status changes. The "user" received in the onUserIdentified() implementation
 * is the new current user and should be equal, although not referentially. The "previousUser" parameter refers
 * to the the MParticleUser instance which was previously the current user, if there was one.
 */
public interface IdentityStateListener {
    void onUserIdentified(@NonNull MParticleUser user, @Nullable MParticleUser previousUser);
}