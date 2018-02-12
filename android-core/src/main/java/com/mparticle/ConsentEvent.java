package com.mparticle;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class ConsentEvent {
    private boolean mConsented;
    private Regulation mRegulation;
    private Long mTimestamp;
    private String mDocument;
    private String mConsentLocation;
    private String mHardwareId;
    private String mPurpose;
    private ConsentCategory mConsentCategory;
    private Map<String, String> mCustomAttributes;

    private ConsentEvent(@NonNull Builder builder) {
        this.mConsented = builder.consented;
        if (builder.regulation != null) {
            this.mRegulation = builder.regulation;
        }
        if (builder.timestamp > 0) {
            this.mTimestamp = builder.timestamp;
        } else {
            this.mTimestamp = System.currentTimeMillis();
        }
        if (builder.document != null) {
            this.mDocument = builder.document;
        }
        if (builder.consentLocation != null) {
            this.mConsentLocation = builder.consentLocation;
        }
        if (builder.consentCategory != null) {
            this.mConsentCategory = builder.consentCategory;
        }
        if (builder.hardwareId != null) {
            this.mHardwareId = builder.hardwareId;
        }
        if (builder.purpose != null) {
            this.mPurpose = builder.purpose;
        }
        if (builder.customAttributes != null) {
            this.mCustomAttributes = new HashMap<String, String>(builder.customAttributes);
        }
    }

    public static Builder builder(boolean consented) {
        return new Builder(consented);
    }

    public boolean hasConsent() {
        return mConsented;
    }

    public Regulation getRegulation() {
        return mRegulation;
    }

    public Long getTimestamp() {
        return mTimestamp;
    }

    public String getDocument() {
        return mDocument;
    }

    public String getConsentLocation() {
        return mConsentLocation;
    }

    public ConsentCategory getConsentCategory() {
        return mConsentCategory;
    }

    public String getHardwareId() {
        return mHardwareId;
    }

    public String getPurpose() {
        return mPurpose;
    }

    public Map<String, String> getCustomAttributes() {
        return mCustomAttributes;
    }

    public static class Builder {
        private boolean consented;
        private Regulation regulation;
        private long timestamp;
        private String document;
        private String consentLocation;
        private String hardwareId;
        private String purpose;
        private ConsentCategory consentCategory;
        private Map<String, String> customAttributes;

        private Builder(boolean consented) {
            this.consented = consented;
        }

        public Builder regulation(Regulation regulation) {
            this.regulation = regulation;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder document(String document) {
            this.document = document;
            return this;
        }

        public Builder consentLocation(String consentLocation) {
            this.consentLocation = consentLocation;
            return this;
        }

        public Builder consentCategory(ConsentCategory consentCategory) {
            this.consentCategory = consentCategory;
            return this;
        }

        public Builder hardwareId(String hardwareId) {
            this.hardwareId = hardwareId;
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder customAttributes(Map<String, String> customAttributes) {
            this.customAttributes = customAttributes;
            return this;
        }

        public Builder customAttribute(String key, String value) {
            if (customAttributes == null) {
                customAttributes = new HashMap<String, String>();
            }
            customAttributes.put(key, value);
            return this;
        }

        public ConsentEvent build() {
            return new ConsentEvent(this);
        }
    }

    public enum ConsentCategory {
        PARENTAL,
        PROCESSING,
        LOCATION,
        SENSITIVE_DATA;
    }

    public enum Regulation {
        UNKNOWN,
        GDPR;
    }
}
