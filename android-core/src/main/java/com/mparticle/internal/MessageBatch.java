package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MParticle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.UUID;

public class MessageBatch extends JSONObject {

    public MessageBatch() {
        super();
    }

    public static MessageBatch create(Context context, JSONArray messagesArray, JSONArray reportingMessagesArray, boolean history, JSONObject appInfo, JSONObject deviceInfo, ConfigManager configManager, SharedPreferences preferences, JSONObject cookies, JSONObject userAttributes) throws JSONException {
        MessageBatch uploadMessage = new MessageBatch();

        uploadMessage.put(Constants.MessageKey.TYPE, Constants.MessageType.REQUEST_HEADER);
        uploadMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis());
        uploadMessage.put(Constants.MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);
        uploadMessage.put(Constants.MessageKey.OPT_OUT_HEADER, configManager.getOptedOut());
        uploadMessage.put(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL, configManager.getUploadInterval()/1000);
        uploadMessage.put(Constants.MessageKey.CONFIG_SESSION_TIMEOUT, configManager.getSessionTimeout()/1000);
        uploadMessage.put(Constants.MessageKey.MPID, configManager.getMpid());

        uploadMessage.put(Constants.MessageKey.APP_INFO, appInfo);
        // if there is notification key then include it
        PushRegistrationHelper.PushRegistration registration = PushRegistrationHelper.getLatestPushRegistration(context);
        if (registration != null && !MPUtility.isEmpty(registration.instanceId)) {
            deviceInfo.put(Constants.MessageKey.PUSH_TOKEN, registration.instanceId);
            deviceInfo.put(Constants.MessageKey.PUSH_TOKEN_TYPE, Constants.GOOGLE_GCM);
        } else {
            deviceInfo.remove(Constants.MessageKey.PUSH_TOKEN);
            deviceInfo.remove(Constants.MessageKey.PUSH_TOKEN_TYPE);
        }

        deviceInfo.put(Constants.MessageKey.PUSH_SOUND_ENABLED, configManager.isPushSoundEnabled());
        deviceInfo.put(Constants.MessageKey.PUSH_VIBRATION_ENABLED, configManager.isPushVibrationEnabled());

        uploadMessage.put(Constants.MessageKey.DEVICE_INFO, deviceInfo);
        uploadMessage.put(Constants.MessageKey.SANDBOX, configManager.getEnvironment().equals(MParticle.Environment.Development));

        uploadMessage.put(Constants.MessageKey.LTV, MParticle.getInstance().Commerce().getCurrentUserLtv());
        String apiKey = configManager.getApiKey();

        if (userAttributes != null) {
            uploadMessage.put(Constants.MessageKey.USER_ATTRIBUTES, userAttributes);
        }

        if (history) {
            String deletedAttr = preferences.getString(Constants.PrefKeys.DELETED_USER_ATTRS + apiKey, null);
            if (deletedAttr != null) {
                uploadMessage.put(Constants.MessageKey.DELETED_USER_ATTRIBUTES, new JSONArray(deletedAttr));
                preferences.edit().remove(Constants.PrefKeys.DELETED_USER_ATTRS + apiKey).apply();
            }
        }

        if (MParticle.getInstance().ProductBags().getBags().size() > 0) {
            uploadMessage.put(Constants.MessageKey.PRODUCT_BAGS, new JSONObject(MParticle.getInstance().ProductBags().toString()));
        }

        String userIds = preferences.getString(Constants.PrefKeys.USER_IDENTITIES + apiKey, null);
        if (null != userIds) {
            JSONArray identities = new JSONArray(userIds);
            boolean changeMade = false;
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).getBoolean(Constants.MessageKey.IDENTITY_FIRST_SEEN)){
                    identities.getJSONObject(i).put(Constants.MessageKey.IDENTITY_FIRST_SEEN, false);
                    changeMade = true;
                }
            }
            if (changeMade) {
                uploadMessage.put(Constants.MessageKey.USER_IDENTITIES, new JSONArray(userIds));
                preferences.edit().putString(Constants.PrefKeys.USER_IDENTITIES + apiKey, identities.toString()).apply();
            }else{
                uploadMessage.put(Constants.MessageKey.USER_IDENTITIES, identities);
            }
        }

        uploadMessage.put(history ? Constants.MessageKey.HISTORY : Constants.MessageKey.MESSAGES, messagesArray);
        if (reportingMessagesArray != null && reportingMessagesArray.length() > 0) {
            uploadMessage.put(Constants.MessageKey.REPORTING, reportingMessagesArray);
        }
        uploadMessage.put(Constants.MessageKey.COOKIES, cookies);
        uploadMessage.put(Constants.MessageKey.PROVIDER_PERSISTENCE, configManager.getProviderPersistence());

        return uploadMessage;
    }
}
