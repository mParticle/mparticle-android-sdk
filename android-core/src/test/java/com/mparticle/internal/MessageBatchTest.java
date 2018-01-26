package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class MessageBatchTest {


    @Test
    public void testCreate() throws Exception {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        CommerceApi mockCommerce = Mockito.mock(CommerceApi.class);
        Mockito.when(mockMp.Commerce()).thenReturn(mockCommerce);
        MParticle.setInstance(mockMp);
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production, "some api key", "some api secret");
        MockSharedPreferences sharedPrefs = new MockSharedPreferences();
        boolean sessionHistory = true;
        MessageBatch batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), manager.getMpid());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }
        sessionHistory = false;
        batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), manager.getMpid());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }

        batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), manager.getMpid());
        assertFalse(batch.has("pb"));
    }
}