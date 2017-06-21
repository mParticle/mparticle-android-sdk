package com.mparticle.identity;

import com.mparticle.MParticle;

import java.util.HashMap;
import java.util.Map;

public final class IdentityApiRequest {

    public static Builder withEmptyUser() {
        return new IdentityApiRequest.Builder();
    }

    public static Builder withUser(MParticleUser currentUser) {
        return null;
    }

    public static class Builder {
        private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();

        public Builder(MParticleUser currentUser) {
            //TODO populate with current user
        }

        public Builder() {

        }

        public Builder email(String email) {
            return userIdentity(MParticle.IdentityType.Email, email);
        }

        public Builder customerId(String customerId) {
            return userIdentity(MParticle.IdentityType.CustomerId, customerId);
        }

        public Builder userIdentity(MParticle.IdentityType identityType, String identityValue) {
            //TODO
            return this;
        }

        public Builder userIdentities(Map<MParticle.IdentityType, String> userIdentities) {
            //TODO
            return this;
        }

        public IdentityApiRequest build() {
            //TODO
            return new IdentityApiRequest();
        }

        public Builder copyUserAttributes(boolean copyUserAttributes) {
            return null;
        }
    }
}