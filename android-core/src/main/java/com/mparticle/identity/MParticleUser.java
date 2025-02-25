package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListenerType;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.AudienceTask;
import com.mparticle.consent.ConsentState;

import java.util.Map;

public interface MParticleUser {

    /**
     * query the MPID of the User
     *
     * @return the mpid
     */
    @NonNull
    long getId();

    /**
     * query the attributes of the User
     *
     * @return the User's attributes
     */
    @NonNull
    @WorkerThread
    Map<String, Object> getUserAttributes();

    /**
     * query the attributes of the User asynchronously
     *
     * @param listener a callback for querying User's attributes
     * @return
     */
    @Nullable
    Map<String, Object> getUserAttributes(@Nullable final UserAttributeListenerType listener);

    /**
     * assign attributes to the User in bulk
     *
     * @param userAttributes the attributes to be set
     * @return whether the attributes were successfully set
     */
    boolean setUserAttributes(@NonNull Map<String, Object> userAttributes);

    /**
     * query the Identities of the User
     *
     * @return the User's Identities
     */
    @NonNull
    Map<MParticle.IdentityType, String> getUserIdentities();

    /**
     * set a single attribute for the user
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return whether the attributes were successfully set
     */
    boolean setUserAttribute(@NonNull String key, @NonNull Object value);

    /**
     * set a single attribute for the user whose value is an Object, not just a String
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return whether the attributes were successfully set
     */
    boolean setUserAttributeList(@NonNull String key, @NonNull Object value);

    /**
     * increment an attribute for the user
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return whether the attributes were successfully set
     */
    boolean incrementUserAttribute(@NonNull String key, Number value);

    /**
     * remove an attribute for the user
     *
     * @param key the key of the attribute which is to be removed
     * @return whether the attributes were successfully removed
     */
    boolean removeUserAttribute(@NonNull String key);

    /**
     * set a tag for a User. A tag is represented by a key and a value of "null"
     *
     * @param tag the tag to be set for the user
     * @return whether the tag was successfully set
     */
    boolean setUserTag(@NonNull String tag);

    /**
     * Query the ConsentState of this user
     */
    @NonNull
    ConsentState getConsentState();

    /**
     * Set the ConsentState for this user
     */
    void setConsentState(@Nullable ConsentState state);

    /**
     * Query the "Logged In" status for this user. A user is considered Logged In based on the presence of one or more {@link com.mparticle.MParticle.IdentityType}, such as IdentityType.CustomerId, defined by a workspace's IDSync strategy.
     *
     * @return whether the user is "Logged In"
     */
    boolean isLoggedIn();

    /**
     * The timestamp representing the first time this user was observed on this device
     *
     * @return
     */
    long getFirstSeenTime();

    /**
     * The timestamp representing the last time this user was the "currentUser" on this device
     *
     * @return the time, in milliseconds
     */
    long getLastSeenTime();

    /**
     * Get a list of the audiences that this given MPID is currently within.
     *
     *  @return an AudienceTask<AudienceResponse> which provides an asynchronous result
     */
     AudienceTask<AudienceResponse> getUserAudiences();
}