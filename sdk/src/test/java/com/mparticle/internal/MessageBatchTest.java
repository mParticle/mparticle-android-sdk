package com.mparticle.internal;

import com.mparticle.ConfigManager;
import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

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
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ai"));

        JSONObject deviceInfo = batch.getJSONObject("di");
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getBoolean("se"));
        assertNotNull(deviceInfo.getBoolean("ve"));
        assertNotNull(batch.get("ltv"));
        assertNotNull(batch.getJSONArray("sh")); //history batch
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }
        sessionHistory = false;
        batch = MessageBatch.create(new MockContext(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ai"));

        deviceInfo = batch.getJSONObject("di");
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getBoolean("se"));
        assertNotNull(deviceInfo.getBoolean("ve"));
        assertNotNull(batch.get("ltv"));
        assertNotNull(batch.getJSONArray("msgs")); //history batch
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }


    }
}