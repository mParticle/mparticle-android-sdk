package com.mparticle.consent;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    private Map<String, GDPRConsent> gdprConsentState = null;

    private ConsentState() {
    }

    private ConsentState(Builder builder) {
        gdprConsentState = builder.gdprConsentState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder withConsentState(ConsentState consentState) {
        return new Builder(consentState);
    }

    public static Builder withConsentState(String consentState) {
        return new Builder(consentState);
    }

    /**
     * When comparing consent values for duplication with string fields:
     * 1) casing doesn't matter. "foo" and "Foo" are the same;
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
     * Note that all purpose keys will be lower-cased and trimmed.
     *
     * @return returns an unmodifiable Map. Attempted mutation will
     * result in an <tt>UnsupportedOperationException</tt>.
     */
    @Nullable
    public Map<String, GDPRConsent> getGDPRConsentState() {
        return Collections.unmodifiableMap(gdprConsentState);
    }

    @Override
    public String toString() {
        JSONObject consentJsonObject = new JSONObject();
        try {
            JSONObject gdprConsentStateJsonObject = new JSONObject();
            consentJsonObject.put(SERIALIZED_GDPR_KEY, gdprConsentStateJsonObject);
            for (Map.Entry<String, GDPRConsent> entry : gdprConsentState.entrySet()) {
                gdprConsentStateJsonObject.put(entry.getKey(), entry.getValue().toString());
            }
        } catch (JSONException ignored) {

        }
        return consentJsonObject.toString();
    }

    public static class Builder {

        private Map<String, GDPRConsent> gdprConsentState = new HashMap<String, GDPRConsent>();

        public Builder() {

        }

        private Builder(ConsentState consentState) {
            setGDPRConsentState(consentState.getGDPRConsentState());
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
        public Builder addGDPRConsentState(@NonNull String purpose, @NonNull GDPRConsent consent) {
            String normalizedPurpose = ConsentState.canonicalizeForDeduplication(purpose);
            if (MPUtility.isEmpty(normalizedPurpose)) {
                Logger.error("Cannot set GDPR Consent with null or empty purpose.");
                return this;
            }
            if (consent == null) {
                Logger.error("Cannot set GDPR Consent with null or empty GDPRConsent object.");
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
        public void removeGDPRConsentState(@NonNull String purpose) {
            String normalizedPurpose = ConsentState.canonicalizeForDeduplication(purpose);
            if (MPUtility.isEmpty(normalizedPurpose)) {
                Logger.error("Cannot remove GDPR Consent with null or empty purpose");
                return;
            }
            if (gdprConsentState == null) {
                return;
            }
            gdprConsentState.remove(normalizedPurpose);
        }

        public ConsentState build() {
            return new ConsentState(this);
        }

        @Override
        public String toString() {
            return build().toString();
        }
    }
}