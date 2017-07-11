package com.mparticle.kits;


import android.app.Activity;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockKitConfiguration;
import com.mparticle.mock.MockKitIntegrationFactory;
import com.mparticle.mock.MockKitManagerImpl;
import com.mparticle.mock.MockMParticle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class KitManagerTest  {

    private KitManagerImpl manager;

    @Before
    public void setUp() throws Exception {
        manager = new MockKitManagerImpl(new MockContext(), null, Mockito.mock(ConfigManager.class), null);
        assertNotNull(manager.providers);
        MockKitIntegrationFactory mockKitFactory = new MockKitIntegrationFactory();
        manager.setKitFactory(mockKitFactory);
        MParticle mockMp = new MockMParticle();
        //Mockito.when(mockMp.getKitManager()).thenReturn(Mockito.mock(KitFrameworkWrapper.class));
        MParticle.setInstance(mockMp);
    }

    @Test
    @PrepareForTest({Looper.class})
    public void testUpdateKits() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper looper = PowerMockito.mock(Looper.class);
        Mockito.when(Looper.myLooper()).thenReturn(looper);
        Mockito.when(Looper.getMainLooper()).thenReturn(looper);
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        manager.updateKits(null);
        assertNotNull(manager.providers);
        manager.updateKits(new JSONArray());
        assertNotNull(manager.providers);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        assertNotNull(array);
        manager.updateKits(array);
        ConcurrentHashMap<Integer, KitIntegration> providers = manager.providers;
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

    @Test
    public void testLogEvent() throws Exception {
        manager.logEvent(new MPEvent.Builder("test name", MParticle.EventType.Location).build());
    }

    @Test
    public void testLogScreen() throws Exception {
        manager.logScreen(null);
        manager.logScreen(new MPEvent.Builder("name").build());
    }

    @Test
    public void testSetLocation() throws Exception {
        manager.setLocation(null);
        manager.setLocation(new Location("passive"));
    }

    @Test
    public void testSetUserAttribute() throws Exception {
        manager.setUserAttribute("key", "value");
        manager.setUserAttribute("key", null);
        manager.setUserAttribute(null, null);
    }

    @Test
    public void testRemoveUserAttribute() throws Exception {
        manager.removeUserAttribute(null);
        manager.removeUserAttribute("");
    }

    @Test
    public void testSetUserIdentity() throws Exception {
        manager.setUserIdentity("", null);
        manager.setUserIdentity("", MParticle.IdentityType.CustomerId);
        manager.setUserIdentity(null, MParticle.IdentityType.CustomerId);
    }

    @Test
    public void testLogout() throws Exception {
        manager.logout();
    }

    @Test
    public void testOnActivityStarted() throws Exception {
        manager.onActivityStarted(null);
        manager.onActivityStarted(Mockito.mock(Activity.class));
    }

    @Test
    @PrepareForTest({Looper.class})
    public void testGetActiveModuleIds() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper looper = PowerMockito.mock(Looper.class);
        Mockito.when(Looper.myLooper()).thenReturn(looper);
        Mockito.when(Looper.getMainLooper()).thenReturn(looper);
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);

        String activeModuleIds = manager.getActiveModuleIds();
        String[] ids = activeModuleIds.split(",");
        String[] testIds = {"56", "64", "37", "68"};
        List<String> idList = Arrays.asList(testIds);
        for (String id : ids){
            assertTrue(activeModuleIds, idList.contains(id.trim()));
        }
    }

    @Test
    @PrepareForTest({Looper.class})
    public void testGetSurveyUrl() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper looper = PowerMockito.mock(Looper.class);
        Mockito.when(Looper.myLooper()).thenReturn(looper);
        Mockito.when(Looper.getMainLooper()).thenReturn(looper);
        JSONObject configJson = new JSONObject(TestConstants.SAMPLE_EK_CONFIG);
        JSONArray array = configJson.optJSONArray(ConfigManager.KEY_EMBEDDED_KITS);
        manager.updateKits(array);
        KitIntegration mockForesee = Mockito.mock(KitIntegration.class);
        JSONObject config = new JSONObject();
        config.put(KitConfiguration.KEY_ID, 100);
        KitConfiguration mockConfig = new MockKitConfiguration().parseConfiguration(config);
        Mockito.when(mockForesee.getConfiguration()).thenReturn(mockConfig);
        Uri uri = Mockito.mock(Uri.class);
        Mockito.when(mockForesee.getSurveyUrl(Mockito.any(Map.class), Mockito.any(Map.class))).thenReturn(uri);
        manager.providers.put(MParticle.ServiceProviders.FORESEE_ID, (KitIntegration) mockForesee);
        assertNull(manager.getSurveyUrl(56, new HashMap<String, String>(), new HashMap<String, List<String>>()));
        assertTrue(manager.getSurveyUrl(MParticle.ServiceProviders.FORESEE_ID, new HashMap<String, String>(), new HashMap<String, List<String>>()) == uri);
    }

    @Test
    public void testGetContext() throws Exception {
        assertNotNull(manager.getContext());
    }
}