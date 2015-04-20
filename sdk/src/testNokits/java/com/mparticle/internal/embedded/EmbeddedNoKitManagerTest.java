package com.mparticle.internal.embedded;


import android.app.Activity;
import android.location.Location;

import com.mparticle.internal.ConfigManager;
import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.TestConstants;
import com.mparticle.internal.MPUtility;
import com.mparticle.mock.MockContext;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddedNoKitManagerTest extends TestCase {

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
        assertEquals(0, providers.size());
        assertNull(providers.get(37));
        assertNull(providers.get(56));
        assertNull(providers.get(64));
        assertNull(providers.get(68));
        manager.updateKits(new JSONArray());
        assertEquals(0, providers.size());
    }



    public void testLogEvent() throws Exception {
        manager.logEvent(null);
        manager.logEvent(new MPEvent.Builder("test name", MParticle.EventType.Location).build());
    }

    public void testLogTransaction() throws Exception {
        manager.logTransaction(null);
        manager.logTransaction(new MPProduct.Builder("name", "sku").build());
    }

    public void testLogScreen() throws Exception {
        manager.logScreen(null, null);
        manager.logScreen("name", null);
        manager.logScreen("name", new Hashtable<String, String>());
    }

    public void testSetLocation() throws Exception {
        manager.setLocation(null);
        manager.setLocation(new Location("passive"));
    }

    public void testSetUserAttributes() throws Exception {
        manager.setUserAttributes(new JSONObject());
        manager.setUserAttributes(null);
    }

    public void testRemoveUserAttribute() throws Exception {
        manager.removeUserAttribute(null);
        manager.removeUserAttribute("");
    }

    public void testSetUserIdentity() throws Exception {
        manager.setUserIdentity("", null);
        manager.setUserIdentity("", MParticle.IdentityType.CustomerId);
        manager.setUserIdentity(null, MParticle.IdentityType.CustomerId);
    }

    public void testLogout() throws Exception {
        manager.logout();
    }

    public void testOnActivityStarted() throws Exception {
        manager.onActivityStarted(null, 0);
        manager.onActivityStarted(Mockito.mock(Activity.class), 238);
    }

    public void testIsEmbeddedKitUri() throws Exception {
        assertFalse(manager.isEmbeddedKitUri(null));
        assertFalse(manager.isEmbeddedKitUri("mparticle"));
    }

    public void testGetActiveModuleIds() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);

        String activeModuleIds = manager.getActiveModuleIds();
        assertTrue(MPUtility.isEmpty(activeModuleIds));
    }

    public void testGetSurveyUrl() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);
        assertNull(manager.getSurveyUrl(56, new JSONObject()));
    }

    public void testGetContext() throws Exception {
        assertNotNull(manager.getContext());
    }
}