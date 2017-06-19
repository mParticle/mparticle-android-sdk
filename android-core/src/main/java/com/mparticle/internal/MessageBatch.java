package com.mparticle.internal;

import android.content.SharedPreferences;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class MessageBatch extends JSONObject {

    private long messageLengthBytes;

    private MessageBatch() {
        super();
    }

    public static MessageBatch create(boolean history, ConfigManager configManager, SharedPreferences preferences, JSONObject cookies) throws JSONException {
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
        uploadMessage.put(Constants.MessageKey.MPID, configManager.getMpid());
        uploadMessage.put(Constants.MessageKey.SANDBOX, configManager.getEnvironment().equals(MParticle.Environment.Development));

        uploadMessage.put(Constants.MessageKey.LTV, MParticle.getInstance().Commerce().getCurrentUserLtv());
        String apiKey = configManager.getApiKey();

        if (history) {
            String deletedAttr = configManager.getUserConfig().getDeletedUserAttributes();
            if (deletedAttr != null) {
                uploadMessage.put(Constants.MessageKey.DELETED_USER_ATTRIBUTES, new JSONArray(deletedAttr));
                configManager.getUserConfig().deleteDeletedUserAttributes();
            }
        }

        if (MParticle.getInstance().ProductBags().getBags().size() > 0) {
            uploadMessage.put(Constants.MessageKey.PRODUCT_BAGS, new JSONObject(MParticle.getInstance().ProductBags().toString()));
        }

        uploadMessage.put(Constants.MessageKey.COOKIES, cookies);
        uploadMessage.put(Constants.MessageKey.PROVIDER_PERSISTENCE, configManager.getProviderPersistence());
        uploadMessage.put(Constants.MessageKey.INTEGRATION_ATTRIBUTES, configManager.getIntegrationAttributes());

        return uploadMessage;
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
