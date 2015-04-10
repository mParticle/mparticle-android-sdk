package com.mparticle.internal;

import com.mparticle.ConfigManager;
import com.mparticle.MParticle;
import com.mparticle.MockContext;
import com.mparticle.MockSharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.junit.Assert.*;

/**
 * Created by sdozor on 4/9/15.
 */
public class MessageBatchTest {

    public void testCreate() throws Exception {
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production);
        MockSharedPreferences sharedPrefs = new MockSharedPreferences();
        MessageBatch batch = MessageBatch.create(new MockContext(), new JSONArray(), true, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject());
        assertNotNull(batch.getString(Constants.MessageKey.TYPE));
        assertNotNull(batch.getString(Constants.MessageKey.ID));
        assertNotNull(batch.getDouble(Constants.MessageKey.TIMESTAMP));
        assertNotNull(batch.getString(Constants.MessageKey.MPARTICLE_VERSION));
        assertNotNull(batch.getBoolean(Constants.MessageKey.OPT_OUT_HEADER));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL));
        assertNotNull(batch.getDouble(Constants.MessageKey.CONFIG_SESSION_TIMEOUT));
        assertNotNull(batch.getJSONObject(Constants.MessageKey.APP_INFO));

    }
}