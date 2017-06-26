package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;

import java.util.HashMap;
import java.util.Map;

public final class IdentityApiRequest {
    private Map<MParticle.IdentityType, String> userIdentities;

    private IdentityApiRequest(IdentityApiRequest.Builder builder) {
        this.userIdentities = builder.userIdentities;
    }

    public static Builder withEmptyUser() {
        return new IdentityApiRequest.Builder();
    }

    public static Builder withUser(MParticleUser currentUser) {
        return new IdentityApiRequest.Builder(currentUser);
    }

    public static class Builder {
        private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();

        public Builder(MParticleUser currentUser) {
            userIdentities = currentUser.getUserIdentities();
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
            if (userIdentities.containsKey(identityType)) {
                Logger.warning("IdentityApiRequest already contains field with IdentityType of:" + identityType + ". It will be overwritten");
            }
            userIdentities.put(identityType, identityValue);
            return this;
        }

        public Builder userIdentities(Map<MParticle.IdentityType, String> userIdentities) {
            for (Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
                userIdentity(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public IdentityApiRequest build() {
            return new IdentityApiRequest(this);
        }

        public Builder copyUserAttributes(boolean copyUserAttributes) {
            return null;
        }
    }
}