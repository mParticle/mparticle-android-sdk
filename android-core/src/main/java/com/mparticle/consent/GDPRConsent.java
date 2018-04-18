package com.mparticle.consent;

import android.support.annotation.Nullable;

import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Record of consent under the GDPR.
 */
public final class GDPRConsent {
    private static final String SERIALIZED_KEY_CONSENTED = "consented";
    private static final String SERIALIZED_KEY_TIMESTAMP = "timestamp";
    private static final String SERIALIZED_KEY_DOCUMENT = "document";
    private static final String SERIALIZED_KEY_LOCATION = "location";
    private static final String SERIALIZED_KEY_HARDWARE_ID = "hardware_id";
    private boolean mConsented;
    private String mDocument;
    private Long mTimestamp;
    private String mLocation;
    private String mHardwareId;

    private GDPRConsent() {
    }

    private GDPRConsent(Builder builder) {
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

    public static Builder builder(boolean consented) {
        return new Builder(consented);
    }

    public static Builder withGDPRConsent(GDPRConsent gdprConsent) {
        return new Builder(gdprConsent);
    }

    static Builder withGDPRConsent(String gdprConsent) {
        return Builder.withString(gdprConsent);
    }

    public boolean isConsented() {
        return mConsented;
    }

    @Nullable
    public String getDocument() {
        return mDocument;
    }

    public Long getTimestamp() {
        return mTimestamp;
    }

    @Nullable
    public String getLocation() {
        return mLocation;
    }

    @Nullable
    public String getHardwareId() {
        return mHardwareId;
    }

    @Override
    public String toString() {
        JSONObject gdprConsentJsonObject = new JSONObject();
        try {
            gdprConsentJsonObject.put(SERIALIZED_KEY_CONSENTED, isConsented());
            if (getTimestamp() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_TIMESTAMP, getTimestamp());
            }
            if (getDocument() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_DOCUMENT, getDocument());
            }
            if (getLocation() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_LOCATION, getLocation());
            }
            if (getHardwareId() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_HARDWARE_ID, getHardwareId());
            }
        } catch (JSONException ignored) {

        }
        return gdprConsentJsonObject.toString();
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

        public Builder hardwareId(String hardwareId) {
            this.mHardwareId = hardwareId;
            return this;
        }

        public Builder location(String location) {
            this.mLocation = location;
            return this;
        }

        public Builder timestamp(Long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        public Builder document(String document) {
            this.mDocument = document;
            return this;
        }

        public Builder consented(boolean consented) {
            this.mConsented = consented;
            return this;
        }

        public GDPRConsent build() {
            return new GDPRConsent(this);
        }

        @Override
        public String toString() {
            return build().toString();
        }
    }
}
