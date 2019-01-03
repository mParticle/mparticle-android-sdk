package com.mparticle.identity;

import android.support.annotation.NonNull;

/**
 * Interface which will handle the transition between Users
 */
public interface UserAliasHandler {

    /**
     * a handler for when Users change. Any carry-over in state between an outgoing user and an incoming user, should take place here
     * @param previousUser the outgoing User
     * @param newUser the incoming User
     */
    void onUserAlias(@NonNull MParticleUser previousUser, @NonNull MParticleUser newUser);
}
