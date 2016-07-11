package com.mparticle.internal;


import android.content.SharedPreferences;

import com.mparticle.MParticle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigManagerTest {
    com.mparticle.mock.MockContext context;
    ConfigManager manager;
    private static final String sampleConfig = "{ \"dt\":\"ac\", \"id\":\"5b7b8073-852b-47c2-9b89-c4bc66e3bd55\", \"ct\":1428030730685, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[ \"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\" ], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"tri\" : { \"mm\" : [{ \"dt\" : \"x\", \"eh\" : true } ], \"evts\" : [1217787541, 2, 3] }, \"eks\":[ { \"id\":64, \"as\":{ \"clientId\":\"8FMBElARYl9ZtgwYIN5sZA==\", \"surveyId\":\"android_app\", \"sendAppVersion\":\"True\", \"rootUrl\":\"http://survey.foreseeresults.com/survey/display\" }, \"hs\":{ \"et\":{ \"57\":0, \"49\":0, \"55\":0, \"52\":0, \"53\":0, \"50\":0, \"56\":0, \"51\":0, \"54\":0, \"48\":0 }, \"ec\":{ \"609391310\":0, \"-1282670145\":0, \"2138942058\":0, \"-1262630649\":0, \"-877324321\":0, \"1700497048\":0, \"1611158813\":0, \"1900204162\":0, \"-998867355\":0, \"-1758179958\":0, \"-994832826\":0, \"1598473606\":0, \"-2106320589\":0 }, \"ea\":{ \"343635109\":0, \"1162787110\":0, \"-427055400\":0, \"-1285822129\":0, \"1699530232\":0 }, \"svec\":{ \"-725356351\":0, \"-1992427723\":0, \"751512662\":0, \"-118381281\":0, \"-171137512\":0, \"-2036479142\":0, \"-1338304551\":0, \"1003167705\":0, \"1046650497\":0, \"1919407518\":0, \"-1326325184\":0, \"480870493\":0, \"-1087232483\":0, \"-725540438\":0, \"-461793000\":0, \"1935019626\":0, \"76381608\":0, \"273797382\":0, \"-948909976\":0, \"-348193740\":0, \"-685370074\":0, \"-849874419\":0, \"2074021738\":0, \"-767572488\":0, \"-1091433459\":0, \"1671688881\":0, \"1304651793\":0, \"1299738196\":0, \"326063875\":0, \"296835202\":0, \"268236000\":0, \"1708308839\":0, \"101093345\":0, \"-652558691\":0, \"-1613021771\":0, \"1106318256\":0, \"-473874363\":0, \"-1267780435\":0, \"486732621\":0, \"1855792002\":0, \"-881258627\":0, \"698731249\":0, \"1510155838\":0, \"1119638805\":0, \"479337352\":0, \"1312099430\":0, \"1712783405\":0, \"-459721027\":0, \"-214402990\":0, \"617910950\":0, \"428901717\":0, \"-201124647\":0, \"940674176\":0, \"1632668193\":0, \"338835860\":0, \"879890181\":0, \"1667730064\":0 } } } ], \"lsv\":\"2.1.4\", \"pio\":30 }";
    private MParticle mockMp;
    private KitFrameworkWrapper kitManager;


    @Before
    public void setUp() throws Exception {
        context = new com.mparticle.mock.MockContext();
        manager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret");
        mockMp= Mockito.mock(MParticle.class);
        MParticle.setInstance(mockMp);
        kitManager = Mockito.mock(KitFrameworkWrapper.class);
        Mockito.when(mockMp.getKitManager()).thenReturn(kitManager);
        manager.updateConfig(new JSONObject(sampleConfig));
    }

    @Test
    public void testSaveConfigJson() throws Exception {
        manager.saveConfigJson(null);
        JSONObject json  = new JSONObject();
        json.put("test", "value");
        manager.saveConfigJson(json);
        JSONObject object = new JSONObject(manager.mPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertNotNull(object);
    }

    @Test
    public void testGetLatestKitConfiguration() throws Exception {
        JSONArray array = manager.getLatestKitConfiguration();
        JSONObject ekConfig = array.getJSONObject(0);
        assertEquals(64, ekConfig.getInt("id"));
    }

    @Test
    public void testUpdateConfig() throws Exception {
        assertEquals(5, ConfigManager.getPushKeys(context).length());
        manager.updateConfig(new JSONObject());
        JSONObject object = new JSONObject(manager.mPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertTrue(!object.keys().hasNext());
    }

    @Test
    public void testUpdateConfigWithBoolean() throws Exception {
        manager.updateConfig(new JSONObject(sampleConfig));
        manager.updateConfig(new JSONObject(), false);
        JSONObject object = new JSONObject(manager.mPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertTrue(object.keys().hasNext());
    }

    @Test
    public void testGetActiveModuleIds() throws Exception {
        Mockito.when(kitManager.getActiveModuleIds()).thenReturn("this is a test");
        assertEquals("this is a test", manager.getActiveModuleIds());
    }

    @Test
    public void testRestrictAAIDBasedOnLAT() throws Exception {
        ConfigManager testManager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret");
        assertTrue(testManager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", "false");
        testManager.updateConfig(config);
        assertFalse(testManager.getRestrictAAIDBasedOnLAT());
        config.put("rdlat", "true");
        testManager.updateConfig(config);
        assertTrue(testManager.getRestrictAAIDBasedOnLAT());
    }

    @Test
    public void testDelayedStart() throws Exception {
        final Boolean[] called = new Boolean[3];
     /*   MParticle.setInstance(new MockMParticle() {
            @Override
            public void setNetworkTrackingEnabled(boolean enabled) {
                called[1] = true;
            }

            @Override
            public MPMessagingAPI Messaging() {
                return new MPMessagingAPI(null, null) {
                    @Override
                    public void enablePushNotifications(String senderId) {
                        called[0] = true;
                    }
                };
            }
        });
        manager.delayedStart();
        if (manager.isPushEnabled()){
            assertTrue(called[0]);
        }
        if (manager.isNetworkPerformanceEnabled()){
            assertTrue(called[1]);
        }*/
    }

    @Test
    public void testGetTriggerMessageMatches() throws Exception {
        JSONArray triggerMessageMatches = manager.getTriggerMessageMatches();
        assertEquals(1, triggerMessageMatches.length());
    }

    @Test
    public void testGetInfluenceOpenTimeoutMillis() throws Exception {
        assertEquals(30*60*1000, manager.getInfluenceOpenTimeoutMillis());
    }



    @Test
    public void testSetAndGetLogUnhandledExceptions() throws Exception {
        assertFalse(manager.getLogUnhandledExceptions());
        manager.setLogUnhandledExceptions(true);
        assertTrue(manager.getLogUnhandledExceptions());
        JSONObject object = new JSONObject(sampleConfig);
        object.put(ConfigManager.KEY_UNHANDLED_EXCEPTIONS, "forcecatch");
        manager.updateConfig(object);
        manager.setLogUnhandledExceptions(false);
        assertTrue(manager.getLogUnhandledExceptions());
    }


    @Test
    public void testGetApiKey() throws Exception {
        assertEquals(manager.sLocalPrefs.mKey, manager.getApiKey());
    }

    @Test
    public void testGetApiSecret() throws Exception {
        assertEquals(manager.sLocalPrefs.mSecret, manager.getApiSecret());
    }

    @Test
    public void testUploadInterval() throws Exception {
        JSONObject object = new JSONObject(sampleConfig);

        assertEquals((1000 * manager.sLocalPrefs.uploadInterval), manager.getUploadInterval());
        object.put(ConfigManager.KEY_UPLOAD_INTERVAL, 110);
        manager.updateConfig(object);
        assertEquals(1000*110, manager.getUploadInterval());
    }

    @Test
    public void testGetEnvironment() throws Exception {
        assertEquals(MParticle.Environment.Production, manager.getEnvironment());
    }


    @Test
    public void testSessionTimeout() throws Exception {
        assertEquals(manager.sLocalPrefs.sessionTimeout * 1000, manager.getSessionTimeout());
        JSONObject object = new JSONObject(sampleConfig);
        object.put(ConfigManager.KEY_SESSION_TIMEOUT, 123);
        manager.updateConfig(object);
        assertEquals(123 * 1000, manager.getSessionTimeout());
    }

    @Test
    public void testPushSenderId() throws Exception {
        assertNull(manager.getPushSenderId());
        manager.setPushSenderId("sender_id_test");
        assertEquals("sender_id_test", manager.getPushSenderId());
    }

    @Test
    public void testPushSoundEnabled() throws Exception {
        assertEquals(AppConfig.DEFAULT_ENABLE_PUSH_SOUND, manager.isPushSoundEnabled());
        manager.setPushSoundEnabled(true);
        assertTrue(manager.isPushSoundEnabled());
        manager.setPushSoundEnabled(false);
        assertFalse(manager.isPushSoundEnabled());
    }

    @Test
    public void testPushVibrationEnabled() throws Exception {
        assertEquals(AppConfig.DEFAULT_ENABLE_PUSH_VIBRATION, manager.isPushVibrationEnabled());
        manager.setPushVibrationEnabled(true);
        assertTrue(manager.isPushVibrationEnabled());
        manager.setPushVibrationEnabled(false);
        assertFalse(manager.isPushVibrationEnabled());
    }

    @Test
    public void testIsEnabled() throws Exception {
        assertTrue(manager.isEnabled());
        manager.setOptOut(true);
        assertFalse(manager.isEnabled());
        JSONObject object = new JSONObject(sampleConfig);
        object.put(ConfigManager.KEY_OPT_OUT, true);
        manager.updateConfig(object);
        assertTrue(manager.isEnabled());
    }

    @Test
    public void testIsAutoTrackingEnabled() throws Exception {
        assertEquals(manager.sLocalPrefs.autoTrackingEnabled, manager.isAutoTrackingEnabled());
    }

    @Test
    public void testPushNotificationIcon() throws Exception {
        assertEquals(0, ConfigManager.getPushIcon(context));
        manager.setPushNotificationIcon(5);
        assertEquals(5, ConfigManager.getPushIcon(context));
    }

    @Test
    public void testSetPushNotificationTitle() throws Exception {
        assertEquals(0, ConfigManager.getPushTitle(context));
        manager.setPushNotificationTitle(4);
        assertEquals(4, ConfigManager.getPushTitle(context));
    }

    @Test
    public void testGetPushKeys() throws Exception {
        JSONArray pushKeys = ConfigManager.getPushKeys(context);
        String[] keys = {"mp_message", "com.urbanairship.push.ALERT", "alert", "a", "message"};
        List<String> list = Arrays.asList(keys);
        for (int i = 0; i < pushKeys.length(); i++){
            assertTrue(list.contains(pushKeys.getString(i)));
        }
    }

    @Test
    public void testGetBreadcrumbLimit() throws Exception {
        assertEquals(AppConfig.DEFAULT_BREADCRUMB_LIMIT, manager.getBreadcrumbLimit());
    }

    @Test
    public void testSetBreadcrumbLimit() throws Exception {
        manager.setBreadcrumbLimit(4343);
        assertEquals(4343, manager.getBreadcrumbLimit());
    }

    @Test
    public void testSetMpid() throws Exception {
        long mpid = System.currentTimeMillis();
        manager.setMpid(mpid);
        assertEquals(mpid, manager.getMpid());
    }

    @Test
    public void testGetAudienceTimeout() throws Exception {
        assertEquals(manager.sLocalPrefs.audienceTimeout, manager.getAudienceTimeout());
    }

    @Test
    public void testSetLogLevel() throws Exception {
        manager.setLogLevel(MParticle.LogLevel.ERROR);
        assertEquals(manager.sLocalPrefs.logLevel, MParticle.LogLevel.ERROR);
    }

    @Test
    public void testGetCurrentRampValue() throws Exception {
        assertEquals(-1, manager.getCurrentRampValue());
        JSONObject object = new JSONObject(sampleConfig);
        object.put(ConfigManager.KEY_RAMP, 43);
        manager.updateConfig(object);
        assertEquals(43, manager.getCurrentRampValue());
    }

    @Test
    public void testGetTriggerMessageHashes() throws Exception {
        JSONArray hashes = manager.getTriggerMessageHashes();
        for (int i = 0; i < hashes.length(); i++){
            int hash = hashes.getInt(i);
            assertTrue(hash == 1217787541 || hash == 2 || hash == 3);
        }
    }

    @Test
    public void testShouldTrigger() throws Exception {
        MPMessage message = new MPMessage.Builder(Constants.MessageType.COMMERCE_EVENT, new Session(), null).build();
        assertTrue(manager.shouldTrigger(message));
        message = new MPMessage.Builder(Constants.MessageType.PUSH_RECEIVED, new Session(), null).build();
        assertTrue(manager.shouldTrigger(message));
    }

    @Test
    public void testGetMpid() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(null, 0);
        long mpid = prefs.getLong(Constants.PrefKeys.Mpid, 0);
        assertTrue(mpid == 0);
        mpid = manager.getMpid();
        long storedMpid = prefs.getLong(Constants.PrefKeys.Mpid, 0);
        assertTrue(mpid != 0);
        assertTrue(storedMpid == mpid);
    }

    @Test
    public void testGetUserBucket() throws Exception {
        int bucket = manager.getUserBucket();
        assertTrue(bucket >= 0 && bucket <= 100);
    }

    @Test
    public void testRestrictLatNoConfig() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
    }

    @Test
    public void testRestrictLatFromConfig() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", false);
        manager.updateConfig(config);
        assertFalse(manager.getRestrictAAIDBasedOnLAT());
    }

    @Test
    public void testRestrictLatTrueToFalse() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", false);
        manager.updateConfig(config);
        assertFalse(manager.getRestrictAAIDBasedOnLAT());

    }

    @Test
    public void testRestrictLatFalseToTrue() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", false);
        manager.updateConfig(config);
        config.put("rdlat", "true");
        manager.updateConfig(config);
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
    }

    @Test
    public void testRestrictLatFalseToNoConfig() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", false);
        manager.updateConfig(config);
        config.remove("rdlat");
        manager.updateConfig(config);
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
    }

    @Test
    public void testRestrictLatTrueToNoConfig() throws Exception {
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
        JSONObject config = new JSONObject();
        config.put("rdlat", true);
        manager.updateConfig(config);
        config.remove("rdlat");
        manager.updateConfig(config);
        assertTrue(manager.getRestrictAAIDBasedOnLAT());
    }
}