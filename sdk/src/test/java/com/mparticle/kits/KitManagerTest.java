package com.mparticle.kits;


import android.app.Activity;
import android.location.Location;
import android.net.Uri;

import com.mparticle.internal.ConfigManager;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.TestConstants;
import com.mparticle.mock.MockContext;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.Mockito;


import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KitManagerTest extends TestCase {

    private KitManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        manager = new KitManager(new MockContext());
        assertNotNull(manager.providers);
        KitFactory factory = Mockito.mock(KitFactory.class);
        Mockito.when(factory.isSupported(Mockito.anyInt())).thenReturn(true);
        AbstractKit mockProvider = Mockito.mock(AbstractKit.class);
        Mockito.when(mockProvider.isRunning()).thenReturn(true);
        Mockito.when(mockProvider.parseConfig(Mockito.any(JSONObject.class))).thenReturn(mockProvider);
        Mockito.when(factory.createInstance(Mockito.anyInt())).thenReturn(mockProvider);
        MParticle.setInstance(Mockito.mock(MParticle.class));
        manager.ekFactory = factory;
    }

    public void testUpdateKits() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        manager.updateKits(null);
        assertNotNull(manager.providers);
        manager.updateKits(new JSONArray());
        assertNotNull(manager.providers);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        assertNotNull(array);
        manager.updateKits(array);
        ConcurrentHashMap<Integer, AbstractKit> providers = manager.providers;
        assertEquals(array.length(), providers.size());
        assertNotNull(providers.get(37));
        assertNotNull(providers.get(56));
        assertNotNull(providers.get(64));
        assertNotNull(providers.get(68));
        manager.updateKits(new JSONArray());
        assertEquals(0, providers.size());
        manager.updateKits(configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS));
        assertEquals(4, providers.size());
    }

    public void testLogEvent() throws Exception {
        manager.logEvent(new MPEvent.Builder("test name", MParticle.EventType.Location).build());
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
        assertFalse(manager.isKitUrl(null));
        assertFalse(manager.isKitUrl("mparticle"));
    }

    public void testGetActiveModuleIds() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);

        String activeModuleIds = manager.getActiveModuleIds();
        String[] ids = activeModuleIds.split(",");
        String[] testIds = {"56", "64", "37", "68"};
        List<String> idList = Arrays.asList(testIds);
        for (String id : ids){
            assertTrue(idList.contains(id.trim()));
        }

    }

    public void testGetSurveyUrl() throws Exception {
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);
        ISurveyProvider mockForesee = Mockito.mock(ForeseeKit.class);
        Uri uri = Mockito.mock(Uri.class);
        Mockito.when(mockForesee.getSurveyUrl(Mockito.any(JSONObject.class))).thenReturn(uri);
        manager.providers.put(MParticle.ServiceProviders.FORESEE_ID, (AbstractKit) mockForesee);
        assertNull(manager.getSurveyUrl(56, new JSONObject()));
        assertTrue(manager.getSurveyUrl(MParticle.ServiceProviders.FORESEE_ID, new JSONObject()) == uri);
    }

    public void testGetContext() throws Exception {
        assertNotNull(manager.getContext());
    }
}