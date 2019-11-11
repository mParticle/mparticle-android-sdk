package com.mparticle.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_CONTEXT;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_ID;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_KEY;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_VERSION;

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
        uploadMessage.put(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL, configManager.getUploadInterval()/1000);
        uploadMessage.put(Constants.MessageKey.CONFIG_SESSION_TIMEOUT, configManager.getSessionTimeout()/1000);
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
                        GDPRConsent consent = entry.getValue();
                        if (consent != null) {
                            JSONObject gdprConsent = new JSONObject();
                            gdpr.put(entry.getKey(), gdprConsent);
                            gdprConsent.put(Constants.MessageKey.CONSENT_STATE_GDPR_CONSENTED, entry.getValue().isConsented());
                            if (entry.getValue().getDocument() != null) {
                                gdprConsent.put(Constants.MessageKey.CONSENT_STATE_GDPR_DOCUMENT, entry.getValue().getDocument());
                            }
                            if (entry.getValue().getTimestamp() != null) {
                                gdprConsent.put(Constants.MessageKey.CONSENT_STATE_GDPR_TIMESTAMP, entry.getValue().getTimestamp());
                            }
                            if (entry.getValue().getLocation() != null) {
                                gdprConsent.put(Constants.MessageKey.CONSENT_STATE_GDPR_LOCATION, entry.getValue().getLocation());
                            }
                            if (entry.getValue().getHardwareId() != null) {
                                gdprConsent.put(Constants.MessageKey.CONSENT_STATE_GDPR_HARDWARE_ID, entry.getValue().getHardwareId());
                            }
                        }
                    }
                }


            } catch (JSONException ignored) { }
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

    public void addSessionHistoryMessage(JSONObject message) {
        try {
            if (!has(Constants.MessageKey.HISTORY)) {
                put(Constants.MessageKey.HISTORY, new JSONArray());
            }
            getJSONArray(Constants.MessageKey.HISTORY).put(message);
        } catch (JSONException e) {
        }
    }

    public void addMessage(JSONObject message) {
        try {
            if (!has(Constants.MessageKey.MESSAGES)) {
                put(Constants.MessageKey.MESSAGES, new JSONArray());
            }
            getJSONArray(Constants.MessageKey.MESSAGES).put(message);
        } catch (JSONException e) {
        }
    }

    public void addReportingMessage(JSONObject reportingMessage) {
        try {
            if (!has(Constants.MessageKey.REPORTING)) {
                put(Constants.MessageKey.REPORTING, new JSONArray());
            }
            getJSONArray(Constants.MessageKey.REPORTING).put(reportingMessage);
        } catch (JSONException e) {
        }
    }

    public void setAppInfo(JSONObject appInfo) {
        try {
            put(Constants.MessageKey.APP_INFO, appInfo);
        } catch (JSONException e) {

        }
    }

    public void setDeviceInfo(JSONObject deviceInfo) {
        try {
            put(Constants.MessageKey.DEVICE_INFO, deviceInfo);
        } catch (JSONException e) {

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

    public JSONArray getSessionHistoryMessages() {
        try {
            return getJSONArray(Constants.MessageKey.HISTORY);
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
        } catch (JSONException e) {

        }
    }

    public void setUserAttributes(JSONObject userAttributes) {
        try {
            put(Constants.MessageKey.USER_ATTRIBUTES, userAttributes);
        } catch (JSONException e) {

        }
    }

    public long getMessageLengthBytes() {
        return messageLengthBytes;
    }

    public void incrementMessageLengthBytes(long bytes) {
        messageLengthBytes = messageLengthBytes + bytes;
    }
}
