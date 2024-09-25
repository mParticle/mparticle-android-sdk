package com.mparticle.internal;

import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_CONTEXT;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_ID;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_KEY;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_VERSION;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentInstance;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

public class MessageBatch extends JSONObject {
    private long messageLengthBytes;

    protected MessageBatch() {
        super();
    }

    public static MessageBatch create(boolean history, ConfigManager configManager, JSONObject cookies, BatchId batchId) throws JSONException {
        MessageBatch uploadMessage = new MessageBatch();
        if (BuildConfig.MP_DEBUG) {
            uploadMessage.put(Constants.MessageKey.ECHO, true);
        }
        uploadMessage.put(Constants.MessageKey.TYPE, Constants.MessageType.REQUEST_HEADER);
        uploadMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis());
        uploadMessage.put(Constants.MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);
        uploadMessage.put(Constants.MessageKey.OPT_OUT_HEADER, configManager.getOptedOut());
        uploadMessage.put(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL, configManager.getUploadInterval() / 1000);
        uploadMessage.put(Constants.MessageKey.MPARTICLE_CONFIG_VERSION, configManager.getEtag());
        uploadMessage.put(Constants.MessageKey.CONFIG_SESSION_TIMEOUT, configManager.getSessionTimeout() / 1000);
        uploadMessage.put(Constants.MessageKey.MPID, String.valueOf(batchId.getMpid()));
        uploadMessage.put(Constants.MessageKey.SANDBOX, configManager.getEnvironment().equals(MParticle.Environment.Development));
        uploadMessage.put(Constants.MessageKey.DEVICE_APPLICATION_STAMP, configManager.getDeviceApplicationStamp());

        if (history) {
            String deletedAttr = configManager.getUserStorage(batchId.getMpid()).getDeletedUserAttributes();
            if (deletedAttr != null) {
                uploadMessage.put(Constants.MessageKey.DELETED_USER_ATTRIBUTES, new JSONArray(deletedAttr));
                configManager.getUserStorage().deleteDeletedUserAttributes();
            }
        }

        uploadMessage.put(Constants.MessageKey.COOKIES, cookies);
        uploadMessage.put(Constants.MessageKey.PROVIDER_PERSISTENCE, configManager.getProviderPersistence());
        uploadMessage.put(Constants.MessageKey.INTEGRATION_ATTRIBUTES, configManager.getIntegrationAttributes());
        uploadMessage.addConsentState(configManager.getConsentState(batchId.getMpid()));
        uploadMessage.addDataplanContext(batchId.getDataplanId(), batchId.getDataplanVersion());
        return uploadMessage;
    }

    public void addConsentState(ConsentState consentState) {
        if (consentState != null) {
            try {
                JSONObject state = new JSONObject();
                this.put(Constants.MessageKey.CONSENT_STATE, state);


                Map<String, GDPRConsent> gdprState = consentState.getGDPRConsentState();
                if (gdprState != null) {
                    JSONObject gdpr = new JSONObject();
                    state.put(Constants.MessageKey.CONSENT_STATE_GDPR, gdpr);
                    for (Map.Entry<String, GDPRConsent> entry : gdprState.entrySet()) {
                        ConsentInstance consent = entry.getValue();
                        if (consent != null) {
                            addConsentStateJSON(gdpr, entry.getKey(), entry.getValue());
                        }
                    }
                }
                CCPAConsent ccpaConsent = consentState.getCCPAConsentState();
                if (ccpaConsent != null) {
                    JSONObject ccpa = new JSONObject();
                    state.put(Constants.MessageKey.CONSENT_STATE_CCPA, ccpa);
                    addConsentStateJSON(ccpa, Constants.MessageKey.CCPA_CONSENT_KEY, ccpaConsent);
                }
            } catch (JSONException ignored) {
            }
        }
    }

    public void addDataplanContext(String dataplanId, Integer dataplanVersion) throws JSONException {
        if (dataplanId != null) {
            JSONObject dataplan = new JSONObject();
            dataplan.put(DATA_PLAN_ID, dataplanId);
            if (dataplanVersion != null) {
                dataplan.put(DATA_PLAN_VERSION, dataplanVersion);
            }

            put(DATA_PLAN_CONTEXT, new JSONObject().put(DATA_PLAN_KEY, dataplan));
        }
    }

    public void addMessage(JSONObject message) {
        try {
            if (!has(Constants.MessageKey.MESSAGES)) {
                put(Constants.MessageKey.MESSAGES, new JSONArray());
            }
            getJSONArray(Constants.MessageKey.MESSAGES).put(message);
        } catch (JSONException ignored) {
        }
    }

    public void addReportingMessage(JSONObject reportingMessage) {
        try {
            if (!has(Constants.MessageKey.REPORTING)) {
                put(Constants.MessageKey.REPORTING, new JSONArray());
            }
            getJSONArray(Constants.MessageKey.REPORTING).put(reportingMessage);
        } catch (JSONException ignored) {
        }
    }

    public void setAppInfo(JSONObject appInfo) {
        try {
            put(Constants.MessageKey.APP_INFO, appInfo);
        } catch (JSONException ignored) {
        }
    }

    public void setDeviceInfo(JSONObject deviceInfo) {
        try {
            put(Constants.MessageKey.DEVICE_INFO, deviceInfo);
        } catch (JSONException ignored) {
        }
    }

    public JSONObject getAppInfo() {
        try {
            return getJSONObject(Constants.MessageKey.APP_INFO);
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONObject getDeviceInfo() {
        try {
            return getJSONObject(Constants.MessageKey.DEVICE_INFO);
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONArray getMessages() {
        try {
            return getJSONArray(Constants.MessageKey.MESSAGES);
        } catch (JSONException e) {
            return null;
        }
    }

    public void setIdentities(JSONArray identities) {
        try {
            put(Constants.MessageKey.USER_IDENTITIES, identities);
        } catch (JSONException ignored) {
        }
    }

    public void setUserAttributes(JSONObject userAttributes) {
        try {
            put(Constants.MessageKey.USER_ATTRIBUTES, userAttributes);
        } catch (JSONException ignored) {
        }
    }

    public long getMessageLengthBytes() {
        return messageLengthBytes;
    }

    public void incrementMessageLengthBytes(long bytes) {
        messageLengthBytes = messageLengthBytes + bytes;
    }

    private void addConsentStateJSON(JSONObject parentJSON, String key, ConsentInstance consentInstance) throws JSONException {
        JSONObject consentInstanceJSON = new JSONObject();
        parentJSON.put(key, consentInstanceJSON);
        consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_CONSENTED, consentInstance.isConsented());
        if (consentInstance.getDocument() != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_DOCUMENT, consentInstance.getDocument());
        }
        consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_TIMESTAMP, consentInstance.getTimestamp());
        if (consentInstance.getLocation() != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_LOCATION, consentInstance.getLocation());
        }
        if (consentInstance.getHardwareId() != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID, consentInstance.getHardwareId());
        }
    }
}
