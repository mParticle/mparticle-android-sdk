package com.mparticle.consent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Record of consent under the CCPA.
 */
public class CCPAConsent extends ConsentInstance {

    private CCPAConsent() {
    }

    private CCPAConsent(Builder builder) {
        if (builder.mTimestamp == null) {
            mTimestamp = System.currentTimeMillis();
        } else {
            mTimestamp = builder.mTimestamp;
        }
        this.mConsented = builder.mConsented;
        this.mDocument = builder.mDocument;
        this.mLocation = builder.mLocation;
        this.mHardwareId = builder.mHardwareId;
    }

    @NonNull
    public static Builder builder(boolean consented) {
        return new Builder(consented);
    }

    @NonNull
    public static Builder withCCPAConsent(@NonNull CCPAConsent ccpaConsent) {
        return new Builder(ccpaConsent);
    }

    static Builder withCCPAConsent(String ccpaConsent) {
        return Builder.withString(ccpaConsent);
    }

    public static class Builder {

        private Boolean mConsented = null;
        private String mDocument = null;
        private Long mTimestamp = null;
        private String mLocation = null;
        private String mHardwareId = null;

        private Builder(CCPAConsent ccpaConsent) {
            this.consented(ccpaConsent.isConsented())
                    .document(ccpaConsent.getDocument())
                    .timestamp(ccpaConsent.getTimestamp())
                    .location(ccpaConsent.getLocation())
                    .hardwareId(ccpaConsent.getHardwareId());
        }

        private Builder(boolean consented) {
            this.consented(consented);
        }

        private static Builder withString(String serializedCCPAConsent) {
            Builder builder = CCPAConsent.builder(false);
            if (MPUtility.isEmpty(serializedCCPAConsent)) {
                return builder;
            }
            try {
                JSONObject ccpaConsentJsonObject = new JSONObject(serializedCCPAConsent);
                builder.consented(ccpaConsentJsonObject.optBoolean(SERIALIZED_KEY_CONSENTED));
                if (ccpaConsentJsonObject.has(SERIALIZED_KEY_TIMESTAMP)) {
                    builder.timestamp(ccpaConsentJsonObject.optLong(SERIALIZED_KEY_TIMESTAMP));
                }
                builder.document(ccpaConsentJsonObject.optString(SERIALIZED_KEY_DOCUMENT, null));
                builder.location(ccpaConsentJsonObject.optString(SERIALIZED_KEY_LOCATION, null));
                builder.hardwareId(ccpaConsentJsonObject.optString(SERIALIZED_KEY_HARDWARE_ID, null));
            } catch (JSONException ignored) {

            }

            return builder;
        }

        @NonNull
        public Builder hardwareId(@Nullable String hardwareId) {
            this.mHardwareId = hardwareId;
            return this;
        }

        @NonNull
        public Builder location(@Nullable String location) {
            this.mLocation = location;
            return this;
        }

        @NonNull
        public Builder timestamp(@Nullable Long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        @NonNull
        public Builder document(@Nullable String document) {
            this.mDocument = document;
            return this;
        }

        @NonNull
        public Builder consented(boolean consented) {
            this.mConsented = consented;
            return this;
        }

        @NonNull
        public CCPAConsent build() {
            return new CCPAConsent(this);
        }

        @Override
        @NonNull
        public String toString() {
            return build().toString();
        }
    }
}
