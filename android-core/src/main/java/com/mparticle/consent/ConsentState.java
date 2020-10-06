package com.mparticle.consent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * ConsentState represents the set of purposes and regulations for which a user
 * has consented for data collection.
 */
public final class ConsentState {

    private final static String SERIALIZED_GDPR_KEY = "GDPR";
    private final static String SERIALIZED_CCPA_KEY = "CCPA";

    private Map<String, GDPRConsent> gdprConsentState = null;
    private CCPAConsent ccpaConsentState;

    private ConsentState() {
    }

    private ConsentState(Builder builder) {
        gdprConsentState = builder.gdprConsentState;
        ccpaConsentState = builder.ccpaConsent;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    public static Builder withConsentState(@NonNull ConsentState consentState) {
        return new Builder(consentState);
    }

    @NonNull
    public static Builder withConsentState(@NonNull String consentState) {
        return new Builder(consentState);
    }

    /**
     * When comparing consent values for duplication with string fields:
     * 1) case doesn't matter. "foo" and "Foo" are the same;
     * 2) null, empty, and whitespace are all the same - nothing;
     * 3) leading or training whitespace is ignored. "foo   ", "    foo", and "foo" are the same;
     */
    private static String canonicalizeForDeduplication(String source) {
        if (MPUtility.isEmpty(source)) {
            return null;
        }

        return source.toLowerCase(Locale.US).trim();
    }

    /**
     * Retrieve the current GDPR consent state for this user.
     * <p>
     * Note that all purpose keys will be lower-case and trimmed.
     *
     * @return returns an unmodifiable Map. Attempted mutation will
     * result in an <code>UnsupportedOperationException</code>.
     */
    @NonNull
    public Map<String, GDPRConsent> getGDPRConsentState() {
        return Collections.unmodifiableMap(gdprConsentState);
    }

    @Nullable
    public CCPAConsent getCCPAConsentState() {
        return ccpaConsentState;
    }

    @Override
    @NonNull
    public String toString() {
        JSONObject consentJsonObject = new JSONObject();
        try {
            JSONObject gdprConsentStateJsonObject = new JSONObject();
            consentJsonObject.put(SERIALIZED_GDPR_KEY, gdprConsentStateJsonObject);
            for (Map.Entry<String, GDPRConsent> entry : gdprConsentState.entrySet()) {
                gdprConsentStateJsonObject.put(entry.getKey(), entry.getValue().toString());
            }
            if (ccpaConsentState != null) {
                consentJsonObject.put(SERIALIZED_CCPA_KEY, ccpaConsentState.toString());
            }
        } catch (JSONException ignored) {

        }
        return consentJsonObject.toString();
    }

    public static class Builder {

        private Map<String, GDPRConsent> gdprConsentState = new HashMap<String, GDPRConsent>();
        private CCPAConsent ccpaConsent = null;

        public Builder() {

        }

        private Builder(ConsentState consentState) {
            setGDPRConsentState(consentState.getGDPRConsentState());
            setCCPAConsentState(consentState.getCCPAConsentState());
        }

        private Builder(String serializedConsent) {
            if (MPUtility.isEmpty(serializedConsent)) {
                return;
            }
            try {
                JSONObject jsonConsent = new JSONObject(serializedConsent);
                if (jsonConsent.has(SERIALIZED_GDPR_KEY)) {
                    JSONObject gdprConsentState = jsonConsent.getJSONObject(SERIALIZED_GDPR_KEY);
                    for (Iterator<String> it = gdprConsentState.keys(); it.hasNext(); ) {
                        String key = it.next();
                        this.addGDPRConsentState(key, GDPRConsent.withGDPRConsent(gdprConsentState.getString(key)).build());
                    }
                }
                if (jsonConsent.has(SERIALIZED_CCPA_KEY)) {
                    String ccpaConsentString = jsonConsent.getString(SERIALIZED_CCPA_KEY);
                    setCCPAConsentState(CCPAConsent.withCCPAConsent(ccpaConsentString).build());
                }
            } catch (JSONException ignored) {

            }
        }

        /**
         * Set/replace the entire GDPR consent state of this builder.
         * <p>
         * Note that all purpose keys will be lower-cased and trimmed.
         *
         * @param consentState
         */
        @NonNull
        public Builder setGDPRConsentState(@Nullable Map<String, GDPRConsent> consentState) {
            if (consentState == null) {
                gdprConsentState = new HashMap<String, GDPRConsent>();
                return this;
            }
            Map<String, GDPRConsent> consentStateCopy = new HashMap<String, GDPRConsent>(consentState);
            gdprConsentState = new HashMap<String, GDPRConsent>();
            for (Map.Entry<String, GDPRConsent> entry : consentStateCopy.entrySet()) {
                this.addGDPRConsentState(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Add or override a single GDPR consent state for this builder.
         * <p>
         * Note that all purpose keys will be lower-cased and trimmed.
         *
         * @param purpose
         * @param consent
         */
        @NonNull
        public Builder addGDPRConsentState(@NonNull String purpose, @NonNull GDPRConsent consent) {
            String normalizedPurpose = ConsentState.canonicalizeForDeduplication(purpose);
            if (MPUtility.isEmpty(normalizedPurpose)) {
                Logger.error("Cannot set GDPR Consent with null or empty purpose.");
                return this;
            }
            if (gdprConsentState == null) {
                gdprConsentState = new HashMap<String, GDPRConsent>();
            }
            gdprConsentState.put(normalizedPurpose, consent);
            return this;
        }

        /**
         * Remove a single GDPR consent state for this builder.
         * <p>
         * Note that all purpose keys will be lower-cased and trimmed.
         *
         * @param purpose
         */
        @NonNull
        public Builder removeGDPRConsentState(@NonNull String purpose) {
            String normalizedPurpose = ConsentState.canonicalizeForDeduplication(purpose);
            if (MPUtility.isEmpty(normalizedPurpose)) {
                Logger.error("Cannot remove GDPR Consent with null or empty purpose");
                return this;
            }
            if (gdprConsentState == null) {
                return this;
            }
            gdprConsentState.remove(normalizedPurpose);
            return this;
        }

        @Deprecated
        @NonNull
        public Builder setCCPAConsent(@NonNull CCPAConsent ccpaConsent) {
            return setCCPAConsentState(ccpaConsent);
        }

        @NonNull
        public Builder setCCPAConsentState(@NonNull CCPAConsent ccpaConsent) {
            this.ccpaConsent = ccpaConsent;
            return this;
        }

        @Deprecated
        @NonNull
        public Builder removeCCPAConsent() {
            return removeCCPAConsentState();
        }

        @NonNull
        public Builder removeCCPAConsentState() {
            ccpaConsent = null;
            return this;
        }

        @NonNull
        public ConsentState build() {
            return new ConsentState(this);
        }

        @Override
        @NonNull
        public String toString() {
            return build().toString();
        }
    }
}