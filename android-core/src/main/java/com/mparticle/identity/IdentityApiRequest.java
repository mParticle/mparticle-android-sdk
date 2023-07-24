package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents observed changes in user state, can be used as a parameter in an Identity Request.
 * To instantiate an IdentityApiRequest, you must use a {@link IdentityApiRequest.Builder} which can be
 * retrieved by calling {@link IdentityApiRequest#withEmptyUser()} or {@link IdentityApiRequest#withUser(MParticleUser)}
 * if you would like to use a current user's Identities in the request.
 *
 * @see IdentityApi#login(IdentityApiRequest)
 * @see IdentityApi#logout(IdentityApiRequest)
 * @see IdentityApi#identify(IdentityApiRequest)
 * @see IdentityApi#modify(IdentityApiRequest)
 */
public final class IdentityApiRequest {
    private UserAliasHandler userAliasHandler = null;
    private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
    // for /modify requests
    private Map<String, String> otherOldIdentities = new HashMap<String, String>();
    private Map<String, String> otherNewIdentities = new HashMap<String, String>();
    Long mpid;

    private IdentityApiRequest(IdentityApiRequest.Builder builder) {
        if (builder.userIdentities != null) {
            this.userIdentities = builder.userIdentities;
        }
        if (builder.userAliasHandler != null) {
            this.userAliasHandler = builder.userAliasHandler;
        }
        if (builder.otherOldIdentities.size() == builder.otherNewIdentities.size()) {
            this.otherNewIdentities = builder.otherNewIdentities;
            this.otherOldIdentities = builder.otherOldIdentities;
        }
        if (builder.mpid != null) {
            this.mpid = builder.mpid;
        }
    }

    /**
     * Instantiate an IdentityApiRequest.Builder() with no existing state (no UserIdentities).
     *
     * @return an IdentityApiRequest.Builder
     * @see IdentityApiRequest.Builder
     */
    @NonNull
    public static Builder withEmptyUser() {
        return new IdentityApiRequest.Builder();
    }

    /**
     * instantiate an IdentityApiRequest.Builder() with an existing user's UserIdentities.
     *
     * @param currentUser an MParticleUser
     * @return an IdentityApiRequest.Builder
     * @see IdentityApiRequest.Builder
     */
    @NonNull
    public static Builder withUser(@Nullable MParticleUser currentUser) {
        return new IdentityApiRequest.Builder(currentUser);
    }

    @NonNull
    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return userIdentities;
    }

    @NonNull
    protected Map<String, String> getOtherOldIdentities() {
        return otherOldIdentities;
    }

    @NonNull
    protected Map<String, String> getOtherNewIdentities() {
        return otherNewIdentities;
    }

    @Nullable
    public UserAliasHandler getUserAliasHandler() {
        return userAliasHandler;
    }

    /**
     * A class used for constructing IdentityApiRequest.
     */
    public static class Builder {
        private Long mpid;
        private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
        private Map<String, String> otherOldIdentities = new HashMap<String, String>();
        private Map<String, String> otherNewIdentities = new HashMap<String, String>();
        private UserAliasHandler userAliasHandler;

        protected Builder(@Nullable MParticleUser currentUser) {
            if (currentUser != null) {
                userIdentities = currentUser.getUserIdentities();
                mpid = currentUser.getId();
            }
        }

        protected Builder() {

        }

        /**
         * Set the IdentityType MParticle.IdentityType.Email.
         *
         * @param email the email to be set
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder email(@Nullable String email) {
            return userIdentity(MParticle.IdentityType.Email, email);
        }

        /**
         * Set the IdentityType MParticle.IdentityType.CustomerId.
         *
         * @param customerId the customerId to be set
         * @return the instance of the builder, for chaining calls
         */
        @NonNull
        public Builder customerId(@Nullable String customerId) {
            return userIdentity(MParticle.IdentityType.CustomerId, customerId);
        }

        @NonNull
        protected Builder pushToken(@Nullable String newPushToken, @Nullable String oldPushToken) {
            if (MPUtility.isEmpty(oldPushToken)) {
                oldPushToken = null;
            }
            if (MPUtility.isEmpty(newPushToken)) {
                newPushToken = null;
            }
            otherOldIdentities.put("push_token", oldPushToken);
            otherNewIdentities.put("push_token", newPushToken);
            return this;
        }

        @NonNull
        protected Builder googleAdId(@Nullable String newGoogleAdId, @Nullable String oldGoogleAdId) {
            otherOldIdentities.put("android_aaid", oldGoogleAdId);
            otherNewIdentities.put("android_aaid", newGoogleAdId);
            return this;
        }

        /**
         * Set the value for the provided IdentityType key.
         *
         * @param identityType  the IdentityType to be set
         * @param identityValue the value the IdentityType should be set to
         * @return the instance of the builder, for chaining calls
         * @see MParticle.IdentityType
         */
        @NonNull
        public Builder userIdentity(@NonNull MParticle.IdentityType identityType, @Nullable String identityValue) {
            if (userIdentities.containsKey(identityType)) {
                Logger.warning("IdentityApiRequest already contains field with IdentityType of:" + identityType + ". It will be overwritten");
            }
            userIdentities.put(identityType, identityValue);
            return this;
        }

        /**
         * Set IdentityTypes in bulk.
         *
         * @param userIdentities the IdentityTypes to be set
         * @return the instance of the builder, for chaining calls
         * @see MParticle.IdentityType
         */
        @NonNull
        public Builder userIdentities(@NonNull Map<MParticle.IdentityType, String> userIdentities) {
            for (Map.Entry<MParticle.IdentityType, String> entry : userIdentities.entrySet()) {
                userIdentity(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Transform this class into an IdentityApiRequest which can be used with {@link IdentityApi} request.
         *
         * @return an IdentityApiRequest
         */
        @NonNull
        public IdentityApiRequest build() {
            return new IdentityApiRequest(this);
        }

        /**
         * Deprecated. To easily migrate your existing code, add a success listener
         * to the {@link BaseIdentityTask} that is being returned from this method ){@link BaseIdentityTask#addSuccessListener(TaskSuccessListener)}. Within the
         * {@link IdentityApiResult} returned by the success listener, you can run the same code you do
         * in you {@link UserAliasHandler}, using the {@link MParticleUser}s returned by
         * {@link IdentityApiResult#getUser()} and {@link IdentityApiResult#getPreviousUser()} in place
         * of "newUser" and "previousUser" respectively
         *
         * @param userAliasHandler
         * @return
         */
        @Deprecated
        @NonNull
        public Builder userAliasHandler(@Nullable UserAliasHandler userAliasHandler) {
            this.userAliasHandler = userAliasHandler;
            return this;
        }
    }
}