package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * class that represents observed changes in user state, can be used as a parameter in an Identity Request.
 * To instantiate an IdentityApiRequest, you must use a {@link IdentityApiRequest.Builder} which can be
 * retrieved by calling {@link IdentityApiRequest#withEmptyUser()} or {@link IdentityApiRequest#withUser(MParticleUser)}
 * if you would like to use a current user's Identities in the request
 *
 * @see IdentityApi#login(IdentityApiRequest)
 * @see IdentityApi#logout(IdentityApiRequest)
 * @see IdentityApi#identify(IdentityApiRequest)
 * @see IdentityApi#modify(IdentityApiRequest)
 *
 */
public final class IdentityApiRequest {
    private UserAliasHandler userAliasHandler = null;
    private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
    // for /modify requests
    private Map<MParticle.IdentityType, String> oldUserIdentities = new HashMap<MParticle.IdentityType, String>();
    private Map<String, String> otherOldIdentities = new HashMap<String, String>();
    private Map<String, String> otherNewIdentities = new HashMap<String, String>();
    Long mpid;

    private IdentityApiRequest(IdentityApiRequest.Builder builder) {
        if (builder.userIdentities != null){
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
     * instantiate an IdentityApiRequest.Builder() with no existing state (no UserIdentities)
     *
     *  @return an IdentityApiRequest.Builder
     *
     * @see IdentityApiRequest.Builder
     */
    public static Builder withEmptyUser() {
        return new IdentityApiRequest.Builder();
    }

    /**
     * instantiate an IdentityApiRequest.Builder() with an existing user's UserIdentities
     *
     * @param currentUser an MParticleUser
     *
     * @return an IdentityApiRequest.Builder
     *
     * @see IdentityApiRequest.Builder
     */
    public static Builder withUser(MParticleUser currentUser) {
        return new IdentityApiRequest.Builder(currentUser);
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return userIdentities;
    }

    public Map<MParticle.IdentityType, String> getOldIdentities() {
        return oldUserIdentities;
    }

    protected Map<String, String> getOtherOldIdentities() {
        return otherOldIdentities;
    }

    protected Map<String, String> getOtherNewIdentities() {
        return otherNewIdentities;
    }

    public UserAliasHandler getUserAliasHandler() {
        return userAliasHandler;
    }

    void setOldUserIdentities(Map<MParticle.IdentityType, String> identities) {
        this.oldUserIdentities = identities;
    }

    /**
     * a class used for constructing IdentityApiRequest
     */
    public static class Builder {
        private Long mpid;
        private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
        private Map<String, String> otherOldIdentities = new HashMap<String, String>();
        private Map<String, String> otherNewIdentities = new HashMap<String, String>();
        private UserAliasHandler userAliasHandler;

        protected Builder(MParticleUser currentUser) {
            if (currentUser != null) {
                userIdentities = currentUser.getUserIdentities();
                mpid = currentUser.getId();
            }
        }

        protected Builder() {

        }

        /**
         * set the IdentityType MParticle.IdentityType.Email
         *
         * @param email the email to be set
         *
         * @return the instance of the builder, for chaining calls
         */
        public Builder email(String email) {
            return userIdentity(MParticle.IdentityType.Email, email);
        }

        /**
         * set the IdentityType MParticle.IdentityType.CustomerId
         *
         * @param customerId the customerId to be set
         *
         * @return the instance of the builder, for chaining calls
         */
        public Builder customerId(String customerId) {
            return userIdentity(MParticle.IdentityType.CustomerId, customerId);
        }

        protected Builder pushToken(String newPushToken, String oldPushToken) {
            if (MPUtility.isEmpty(oldPushToken)) {
                oldPushToken = null;
            }
            otherOldIdentities.put("push_token", oldPushToken);
            otherNewIdentities.put("push_token", newPushToken);
            return this;
        }

        protected Builder googleAdId(String newGoogleAdId, String oldGoogleAdId) {
            otherOldIdentities.put("android_aaid", oldGoogleAdId);
            otherNewIdentities.put("android_aaid", newGoogleAdId);
            return this;
        }

        /**
         * set the value for the provided IdentityType key
         *
         * @param identityType the IdentityType to be set
         * @param identityValue the value the IdentityType should be set to
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see MParticle.IdentityType
         */
        public Builder userIdentity(MParticle.IdentityType identityType, String identityValue) {
            if (userIdentities.containsKey(identityType)) {
                Logger.warning("IdentityApiRequest already contains field with IdentityType of:" + identityType + ". It will be overwritten");
            }
            userIdentities.put(identityType, identityValue);
            return this;
        }

        /**
         * set IdentityTypes in bulk
         *
         * @param userIdentities the IdentityTypes to be set
         *
         * @return the instance of the builder, for chaining calls
         *
         * @see MParticle.IdentityType
         */
        public Builder userIdentities(Map<MParticle.IdentityType, String> userIdentities) {
            for (Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
                userIdentity(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * transform this class into an IdentityApiRequest which can be used with {@link IdentityApi} request
         *
         * @return an IdentityApiRequest
         */
        public IdentityApiRequest build() {
            return new IdentityApiRequest(this);
        }

        public Builder userAliasHandler(UserAliasHandler userAliasHandler) {
            this.userAliasHandler = userAliasHandler;
            return this;
        }
    }
}