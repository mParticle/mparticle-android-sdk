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
        MessageBatch batch = MessageBatch.create( sessionHistory, manager, sharedPrefs,new JSONObject(), manager.getMpid());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.get("ltv"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }
        sessionHistory = false;
        batch = MessageBatch.create( sessionHistory, manager, sharedPrefs,new JSONObject(), manager.getMpid());
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("pb"));
        assertNotNull(batch.get("ltv"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }

        bags.removeProductBag("whatever");
        batch = MessageBatch.create( sessionHistory, manager, sharedPrefs,new JSONObject(), manager.getMpid());
        assertFalse(batch.has("pb"));
    }
}