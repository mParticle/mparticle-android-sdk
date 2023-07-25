package com.mparticle.identity;

import androidx.annotation.NonNull;

/**
 * to the {@link BaseIdentityTask} that is being returned from this method ){@link BaseIdentityTask#addSuccessListener(TaskSuccessListener)}. Within the
 * {@link IdentityApiResult} returned by the success listener, you can run the same code you do
 * in you {@link UserAliasHandler}, using the {@link MParticleUser}s returned by
 * {@link IdentityApiResult#getUser()} and {@link IdentityApiResult#getPreviousUser()} in place
 * of "newUser" and "previousUser" respectively
 *
 * Interface which will handle the transition between Users.
 */
@Deprecated
public interface UserAliasHandler {

    /**
     * A handler for when Users change. Any carry-over in state between an outgoing user and an incoming user, should take place here.
     *
     * @param previousUser the outgoing User
     * @param newUser      the incoming User
     */
    void onUserAlias(@NonNull MParticleUser previousUser, @NonNull MParticleUser newUser);
}
