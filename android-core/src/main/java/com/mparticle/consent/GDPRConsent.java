package com.mparticle.consent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Record of consent under the GDPR.
 */
public final class GDPRConsent extends ConsentInstance {

    private GDPRConsent() {
    }

    private GDPRConsent(GDPRConsent.Builder builder) {
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
    public static GDPRConsent.Builder builder(boolean consented) {
        return new GDPRConsent.Builder(consented);
    }

    @NonNull
    public static GDPRConsent.Builder withGDPRConsent(@NonNull GDPRConsent gdprConsent) {
        return new GDPRConsent.Builder(gdprConsent);
    }

    static GDPRConsent.Builder withGDPRConsent(String gdprConsent) {
        return GDPRConsent.Builder.withString(gdprConsent);
    }

    public static class Builder {

        private Boolean mConsented = null;
        private String mDocument = null;
        private Long mTimestamp = null;
        private String mLocation = null;
        private String mHardwareId = null;

        private Builder(GDPRConsent gdprConsent) {
            this.consented(gdprConsent.isConsented())
                    .document(gdprConsent.getDocument())
                    .timestamp(gdprConsent.getTimestamp())
                    .location(gdprConsent.getLocation())
                    .hardwareId(gdprConsent.getHardwareId());
        }

        private Builder(boolean consented) {
            this.consented(consented);
        }

        private static GDPRConsent.Builder withString(String serializedGDPRConsent) {
            GDPRConsent.Builder builder = GDPRConsent.builder(false);
            if (MPUtility.isEmpty(serializedGDPRConsent)) {
                return builder;
            }
            try {
                JSONObject gdprConsentJsonObject = new JSONObject(serializedGDPRConsent);
                builder.consented(gdprConsentJsonObject.optBoolean(SERIALIZED_KEY_CONSENTED));
                if (gdprConsentJsonObject.has(SERIALIZED_KEY_TIMESTAMP)) {
                    builder.timestamp(gdprConsentJsonObject.optLong(SERIALIZED_KEY_TIMESTAMP));
                }
                builder.document(gdprConsentJsonObject.optString(SERIALIZED_KEY_DOCUMENT, null));
                builder.location(gdprConsentJsonObject.optString(SERIALIZED_KEY_LOCATION, null));
                builder.hardwareId(gdprConsentJsonObject.optString(SERIALIZED_KEY_HARDWARE_ID, null));
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
        public GDPRConsent build() {
            return new GDPRConsent(this);
        }

        @Override
        @NonNull
        public String toString() {
            return build().toString();
        }
    }
}
