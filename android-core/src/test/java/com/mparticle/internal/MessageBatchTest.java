package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.ProductBagApi;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONArray;
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
        Mockito.when(mockCommerce.getCurrentUserLtv()).thenReturn(new BigDecimal(0));
        Mockito.when(mockMp.Commerce()).thenReturn(mockCommerce);
        MParticle.setInstance(mockMp);
        ProductBagApi bags = new ProductBagApi(new MockContext());
        bags.addProduct("whatever", null);
        Mockito.when(mockMp.ProductBags()).thenReturn(bags);
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production, "some api key", "some api secret");
        MockSharedPreferences sharedPrefs = new MockSharedPreferences();
        boolean sessionHistory = true;
        MessageBatch batch = MessageBatch.create(new MockContext(), new JSONArray(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject(), new JSONObject());
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
        batch = MessageBatch.create(new MockContext(), new JSONArray(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject(), new JSONObject());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ai"));


        assertNotNull(batch.getJSONObject("pb"));

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

        bags.removeProductBag("whatever");
        batch = MessageBatch.create(new MockContext(), new JSONArray(), new JSONArray(), sessionHistory, new JSONObject(), new JSONObject(), manager, sharedPrefs, new JSONObject(), new JSONObject());
        assertFalse(batch.has("pb"));
    }
}