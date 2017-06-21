package com.mparticle;

import android.support.annotation.NonNull;

import java.util.HashMap;

class MParticleOptions {

    private MParticle.InstallType installType;
    private MParticle.Environment environment;
    private String apiKey;
    private String apiSecret;

    private MParticleOptions(){}

    public MParticleOptions(Builder builder) {
        //TODO
    }

    public static MParticleOptions.Builder builder() {
        return new Builder();
    }

    public MParticle.InstallType getInstallType() {
        return installType;
    }

    public MParticle.Environment getEnvironment() {
        return environment;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public static class Builder {

        public Builder credentials(@NonNull String apiKey, @NonNull String apiSecret) {
            //TODO
            return null;
        }

        public Builder installType(@NonNull MParticle.InstallType installType) {
            //TODO
            return null;
        }

        public Builder environment(@NonNull MParticle.Environment environment) {
            //TODO
            return null;
        }

        public Builder userIdentities(@NonNull HashMap<MParticle.IdentityType, String> initialIdentities) {
            //TODO
            return null;
        }
        public MParticleOptions build() {
            return new MParticleOptions(this);
        }
    }

}
