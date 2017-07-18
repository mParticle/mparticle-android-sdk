package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.internal.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class IdentityApiRequest {
    private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
    private Map<String, String> otherOldIdentities = new HashMap<String, String>();
    private Map<String, String> otherNewIdentities = new HashMap<String, String>();
    private boolean isCopyUserAttributes = false;

    private IdentityApiRequest(IdentityApiRequest.Builder builder) {
        if (builder.userIdentities != null){
            this.userIdentities = builder.userIdentities;
        }
        if (builder.isCopyUserAttributes != null) {
            this.isCopyUserAttributes = builder.isCopyUserAttributes;
        }
        if (builder.otherOldIdentities.size() == builder.otherNewIdentities.size()) {
            this.otherNewIdentities = builder.otherNewIdentities;
            this.otherOldIdentities = builder.otherOldIdentities;
        }
    }

    public static Builder withEmptyUser() {
        return new IdentityApiRequest.Builder();
    }

    public static Builder withUser(MParticleUser currentUser) {
        return new IdentityApiRequest.Builder(currentUser);
    }

    public boolean shouldCopyUserAttributes() {
        return isCopyUserAttributes;
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return userIdentities;
    }

    protected Map<String, String> getOtherOldIdentities() {
        return otherOldIdentities;
    }

    protected Map<String, String> getOtherNewIdentities() {
        return otherNewIdentities;
    }

    public static class Builder {
        private Map<MParticle.IdentityType, String> userIdentities = new HashMap<MParticle.IdentityType, String>();
        private Map<String, String> otherOldIdentities = new HashMap<String, String>();
        private Map<String, String> otherNewIdentities = new HashMap<String, String>();
        private Boolean isCopyUserAttributes = null;

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

        protected Builder pushToken(String newPushToken, String oldPushToken) {
            otherOldIdentities.put("push_token", oldPushToken);
            otherNewIdentities.put("push_token", newPushToken);
            return this;
        }

        protected Builder googleAdId(String newGoogleAdId, String oldGoogleAdId) {
            otherOldIdentities.put("android_aaid", oldGoogleAdId);
            otherNewIdentities.put("android_aaid", newGoogleAdId);
            return this;
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
            this.isCopyUserAttributes = copyUserAttributes;
            return this;
        }
    }
}