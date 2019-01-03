package com.mparticle.identity;


import android.support.annotation.NonNull;

/**
 * The IdentityStateListener is a callback which will be invoked when either a new user is identified,
 * or the current user's "logged in" status changes. The user received in the onUserIdentified() implementation
 * is the new current user and should be equal, although not referentially
 */
public interface IdentityStateListener {
    void onUserIdentified(@NonNull MParticleUser user);
}