package com.mparticle.internal.embedded;


import com.mparticle.ConfigManager;
import com.mparticle.TestConstants;
import com.mparticle.mock.MockContext;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class EmbeddedKitManagerTest extends TestCase {

    private EmbeddedKitManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new EmbeddedKitManager(new MockContext());
        assertNotNull(manager.providers);

    }

    public void testUpdateKits() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        manager.updateKits(null);
        assertNotNull(manager.providers);
        manager.updateKits(new JSONArray());
        assertNotNull(manager.providers);
        manager.updateKits(configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS));
        ConcurrentHashMap<Integer, EmbeddedProvider> providers = manager.providers;
        assertEquals(4, providers.size());
        assertNotNull(providers.get(37));
        assertNotNull(providers.get(56));
        assertNotNull(providers.get(64));
        assertNotNull(providers.get(68));
        manager.updateKits(new JSONArray());
        assertEquals(0, providers.size());
    }

    public void testLogEvent() throws Exception {

    }

    public void testLogTransaction() throws Exception {

    }

    public void testLogScreen() throws Exception {

    }

    public void testSetLocation() throws Exception {

    }

    public void testSetUserAttributes() throws Exception {

    }

    public void testRemoveUserAttribute() throws Exception {

    }

    public void testSetUserIdentity() throws Exception {

    }

    public void testLogout() throws Exception {

    }

    public void testOnActivityStarted() throws Exception {

    }

    public void testIsEmbeddedKitUri() throws Exception {

    }

    public void testGetActiveModuleIds() throws Exception {

    }

    public void testGetSurveyUrl() throws Exception {

    }

    public void testGetContext() throws Exception {

    }

    public void testGetConfigurationManager() throws Exception {

    }

    public void testGetAppStateManager() throws Exception {

    }

    public void testSetConfigManager() throws Exception {

    }

    public void testSetAppStateManager() throws Exception {

    }
}