package com.mparticle.internal;

import com.mparticle.ConfigManager;
import com.mparticle.MParticle;
import com.mparticle.MockContext;
import com.mparticle.MockSharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageBatchTest {

    @Test
    public void testCreate() throws Exception {
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production);
        MockSharedPreferences sharedPrefs = new MockSharedPreferences();
        boolean sessionHistory = true;
        MessageBatch batch = MessageBatch.create(new MockContext(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject());
        assertNotNull(batch.getString(Constants.MessageKey.TYPE));
        assertNotNull(batch.getString(Constants.MessageKey.ID));
        assertNotNull(batch.getDouble(Constants.MessageKey.TIMESTAMP));
        assertNotNull(batch.getString(Constants.MessageKey.MPARTICLE_VERSION));
        assertNotNull(batch.getBoolean(Constants.MessageKey.OPT_OUT_HEADER));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_SESSION_TIMEOUT));
        assertNotNull(batch.getJSONObject(Constants.MessageKey.APP_INFO));

        JSONObject deviceInfo = batch.getJSONObject(Constants.MessageKey.DEVICE_INFO);
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getBoolean(Constants.MessageKey.PUSH_SOUND_ENABLED));
        assertNotNull(deviceInfo.getBoolean(Constants.MessageKey.PUSH_VIBRATION_ENABLED));
        assertNotNull(batch.get(Constants.MessageKey.LTV));
        assertNotNull(batch.getJSONArray(Constants.MessageKey.HISTORY)); //history batch
        assertNotNull(batch.getJSONObject(Constants.MessageKey.COOKIES));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject(Constants.MessageKey.PROVIDER_PERSISTENCE));
        }
        sessionHistory = false;
        batch = MessageBatch.create(new MockContext(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject());
        assertNotNull(batch.getString(Constants.MessageKey.TYPE));
        assertNotNull(batch.getString(Constants.MessageKey.ID));
        assertNotNull(batch.getDouble(Constants.MessageKey.TIMESTAMP));
        assertNotNull(batch.getString(Constants.MessageKey.MPARTICLE_VERSION));
        assertNotNull(batch.getBoolean(Constants.MessageKey.OPT_OUT_HEADER));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_SESSION_TIMEOUT));
        assertNotNull(batch.getJSONObject(Constants.MessageKey.APP_INFO));

        deviceInfo = batch.getJSONObject(Constants.MessageKey.DEVICE_INFO);
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getBoolean(Constants.MessageKey.PUSH_SOUND_ENABLED));
        assertNotNull(deviceInfo.getBoolean(Constants.MessageKey.PUSH_VIBRATION_ENABLED));
        assertNotNull(batch.get(Constants.MessageKey.LTV));
        assertNotNull(batch.getJSONArray(Constants.MessageKey.MESSAGES)); //history batch
        assertNotNull(batch.getJSONObject(Constants.MessageKey.COOKIES));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject(Constants.MessageKey.PROVIDER_PERSISTENCE));
        }


    }
}