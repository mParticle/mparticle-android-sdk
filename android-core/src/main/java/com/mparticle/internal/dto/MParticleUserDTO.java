package com.mparticle.internal.dto;

import com.mparticle.MParticle;
import com.mparticle.identity.MParticleUser;

import java.util.HashMap;
import java.util.Map;

public class MParticleUserDTO {
    private long mpId;
    private Map<MParticle.IdentityType, String> identities;
    private Map<String, Object> userAttributes = new HashMap<String, Object>();

    private MParticleUserDTO() {}

    public MParticleUserDTO(long mpId, Map<MParticle.IdentityType, String> identities) {
        this(mpId, identities, null);
    }

    public MParticleUserDTO(long mpId, Map<MParticle.IdentityType, String> identities, Map<String, Object> userAttributes) {
        this.mpId = mpId;
        this.identities = identities;
        this.userAttributes = userAttributes;
    }

    public boolean hasError() {
        return false;
    }

    public Error getError() {
        return null;
    }

    public long getMpId() {
        return mpId;
    }

    public Map<MParticle.IdentityType, String> getIdentities() {
        return identities;
    }

    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    public static class Error extends MParticleUserDTO {
        private String error;

        public Error(String error) {
            this.error = error;
        }

        public String getErrorString() {
            return error;
        }

        @Override
        public boolean hasError() {
            return true;
        }

        @Override
        public Error getError() {
            return this;
        }
    }
}
