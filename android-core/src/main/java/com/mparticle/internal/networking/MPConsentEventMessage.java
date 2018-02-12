package com.mparticle.internal.networking;

import android.location.Location;

import com.mparticle.ConsentEvent;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.Session;

import org.json.JSONException;

public class MPConsentEventMessage extends BaseMPMessage {

    protected MPConsentEventMessage () {}

    protected MPConsentEventMessage(Builder builder) throws JSONException {
        super(builder);
        addConsentEvent(this, builder.mConsentEvent);
    }

    private void addConsentEvent(BaseMPMessage message, ConsentEvent consentEvent) {
        try {
            message.put(Constants.MessageKey.Consent.CONSENTED, consentEvent.hasConsent() ? "true" : "false")
                    .put(Constants.MessageKey.TIMESTAMP, String.valueOf(consentEvent.getTimestamp()));
            if (consentEvent.getConsentCategory() != null) {
                message.put(Constants.MessageKey.Consent.CATEGORY, getConsentCategoryString(consentEvent.getConsentCategory()));
            }
            if (consentEvent.getConsentLocation() != null) {
                message.put(Constants.MessageKey.Consent.LOCATION, consentEvent.getConsentLocation());
            }
            if (consentEvent.getDocument() != null) {
                message.put(Constants.MessageKey.Consent.DOCUMENT, consentEvent.getDocument());
            }
            if (consentEvent.getRegulation() != null) {
                message.put(Constants.MessageKey.Consent.REGULATION, getRegulationString(consentEvent.getRegulation()));
            }
            if (consentEvent.getCustomAttributes() != null) {
                message.put(Constants.MessageKey.Consent.ATTRIBUTES, MPUtility.mapToJson(consentEvent.getCustomAttributes()));
            }
            if (consentEvent.getHardwareId() != null) {
                message.put(Constants.MessageKey.Consent.HARDWARE_ID, consentEvent.getHardwareId());
            }
            if (consentEvent.getPurpose() != null) {
                message.put(Constants.MessageKey.Consent.PURPOSE, consentEvent.getPurpose());
            }
        } catch (JSONException ignore) {
            Logger.warning("Failed to create mParticle consent message");
        }
    }

    protected String getConsentCategoryString(ConsentEvent.ConsentCategory consentCategory) {
        if (consentCategory != null) {
            switch (consentCategory) {
                case LOCATION:
                    return "location";
                case PARENTAL:
                    return "parental";
                case PROCESSING:
                    return "processing";
                case SENSITIVE_DATA:
                    return "sensitive_data";
                    default:
                        return "unknown";
            }
        }
        return null;
    }

    protected String getRegulationString(ConsentEvent.Regulation regulation) {
        if (regulation != null) {
            switch (regulation) {
                case GDPR:
                    return "gdpr";
                case UNKNOWN:
                    return "unknown";
            }
        }
        return null;
    }


    public static class Builder extends BaseMPMessageBuilder {
        ConsentEvent mConsentEvent;

        public Builder(ConsentEvent event, Session session, Location location, long mpId) {
            super(Constants.MessageType.CONSENT_EVENT, session, location, mpId);
            mConsentEvent = event;
        }

        @Override
        public BaseMPMessage build() throws JSONException {
            return new MPConsentEventMessage(this);
        }
    }
}
