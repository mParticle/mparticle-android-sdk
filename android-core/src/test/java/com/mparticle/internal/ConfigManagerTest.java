package com.mparticle.internal;


import android.content.SharedPreferences;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.RandomUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigManagerTest {
    com.mparticle.mock.MockContext context;
    ConfigManager manager;
    private static final String sampleConfig = "{ \"dt\":\"ac\", \"id\":\"5b7b8073-852b-47c2-9b89-c4bc66e3bd55\", \"ct\":1428030730685, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[ \"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\" ], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"tri\" : { \"mm\" : [{ \"dt\" : \"x\", \"eh\" : true } ], \"evts\" : [1217787541, 2, 3] }, \"eks\":[ { \"id\":64, \"as\":{ \"clientId\":\"8FMBElARYl9ZtgwYIN5sZA==\", \"surveyId\":\"android_app\", \"sendAppVersion\":\"True\", \"rootUrl\":\"http://survey.foreseeresults.com/survey/display\" }, \"hs\":{ \"et\":{ \"57\":0, \"49\":0, \"55\":0, \"52\":0, \"53\":0, \"50\":0, \"56\":0, \"51\":0, \"54\":0, \"48\":0 }, \"ec\":{ \"609391310\":0, \"-1282670145\":0, \"2138942058\":0, \"-1262630649\":0, \"-877324321\":0, \"1700497048\":0, \"1611158813\":0, \"1900204162\":0, \"-998867355\":0, \"-1758179958\":0, \"-994832826\":0, \"1598473606\":0, \"-2106320589\":0 }, \"ea\":{ \"343635109\":0, \"1162787110\":0, \"-427055400\":0, \"-1285822129\":0, \"1699530232\":0 }, \"svec\":{ \"-725356351\":0, \"-1992427723\":0, \"751512662\":0, \"-118381281\":0, \"-171137512\":0, \"-2036479142\":0, \"-1338304551\":0, \"1003167705\":0, \"1046650497\":0, \"1919407518\":0, \"-1326325184\":0, \"480870493\":0, \"-1087232483\":0, \"-725540438\":0, \"-461793000\":0, \"1935019626\":0, \"76381608\":0, \"273797382\":0, \"-948909976\":0, \"-348193740\":0, \"-685370074\":0, \"-849874419\":0, \"2074021738\":0, \"-767572488\":0, \"-1091433459\":0, \"1671688881\":0, \"1304651793\":0, \"1299738196\":0, \"326063875\":0, \"296835202\":0, \"268236000\":0, \"1708308839\":0, \"101093345\":0, \"-652558691\":0, \"-1613021771\":0, \"1106318256\":0, \"-473874363\":0, \"-1267780435\":0, \"486732621\":0, \"1855792002\":0, \"-881258627\":0, \"698731249\":0, \"1510155838\":0, \"1119638805\":0, \"479337352\":0, \"1312099430\":0, \"1712783405\":0, \"-459721027\":0, \"-214402990\":0, \"617910950\":0, \"428901717\":0, \"-201124647\":0, \"940674176\":0, \"1632668193\":0, \"338835860\":0, \"879890181\":0, \"1667730064\":0 } } } ], \"lsv\":\"2.1.4\", \"pio\":30 }";
    private MParticle mockMp;
    private Random ran = new Random();
    private RandomUtils randomUtils = new RandomUtils();


    @Before
    public void setUp() throws Exception {
        context = new com.mparticle.mock.MockContext();
        manager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
        mockMp = new MockMParticle();
        MParticle.setInstance(mockMp);
        manager.updateConfig(new JSONObject(sampleConfig));
    }

    @Test
    public void testSaveConfigJson() throws Exception {
        manager.saveConfigJson(null, null, null);
        JSONObject json = new JSONObject();
        json.put("test", "value");
        manager.saveConfigJson(json, null, null);
        JSONObject object = new JSONObject(manager.sPreferences.getString(ConfigManager.CONFIG_JSON, null));
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
        JSONObject object = new JSONObject(manager.sPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertTrue(!object.keys().hasNext());
    }

    @Test
    public void testUpdateConfigWithReload() throws Exception {
        manager.updateConfig(new JSONObject(sampleConfig));
        manager.reloadConfig(new JSONObject());
        JSONObject object = new JSONObject(manager.sPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertTrue(object.keys().hasNext());
    }

    @Test
    public void testGetActiveModuleIds() throws Exception {
        Mockito.when(MParticle.getInstance().Internal().getKitManager().getActiveModuleIds())
                .thenReturn("this is a test");
        assertEquals("this is a test", manager.getActiveModuleIds());
    }

    @Test
    public void testRestrictAAIDBasedOnLAT() throws Exception {
        ConfigManager testManager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
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
        assertEquals(30 * 60 * 1000, manager.getInfluenceOpenTimeoutMillis());
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
        assertEquals(manager.mLocalPrefs.mKey, manager.getApiKey());
    }

    @Test
    public void testGetApiSecret() throws Exception {
        assertEquals(manager.mLocalPrefs.mSecret, manager.getApiSecret());
    }

    @Test
    public void testUploadInterval() throws Exception {
        JSONObject object = new JSONObject(sampleConfig);

        assertEquals((1000 * manager.mLocalPrefs.uploadInterval), manager.getUploadInterval());
        object.put(ConfigManager.KEY_UPLOAD_INTERVAL, 110);
        manager.updateConfig(object);
        assertEquals(1000 * 110, manager.getUploadInterval());
    }

    @Test
    public void testUploadIntervalNoOverridenDelayedInit() {
        manager.setUploadInterval(123);
        assertEquals(123 * 1000, manager.getUploadInterval());
        manager.delayedStart();
        assertEquals(123 * 1000, manager.getUploadInterval());
    }

    @Test
    public void testGetEnvironment() throws Exception {
        assertEquals(MParticle.Environment.Production, manager.getEnvironment());
    }


    @Test
    public void testSessionTimeout() throws Exception {
        assertEquals(manager.mLocalPrefs.sessionTimeout * 1000, manager.getSessionTimeout());
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
        assertEquals(manager.mLocalPrefs.autoTrackingEnabled, manager.isAutoTrackingEnabled());
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
        for (int i = 0; i < pushKeys.length(); i++) {
            assertTrue(list.contains(pushKeys.getString(i)));
        }
    }

    @Test
    public void testGetBreadcrumbLimit() throws Exception {
        assertEquals(UserStorage.DEFAULT_BREADCRUMB_LIMIT, manager.getBreadcrumbLimit(context));
    }

    @Test
    public void testSetBreadcrumbLimit() throws Exception {
        manager.setBreadcrumbLimit(4343);
        assertEquals(4343, manager.getBreadcrumbLimit(context));
    }

    @Test
    public void testSetMpid() throws Exception {
        long mpid = System.currentTimeMillis();
        manager.setMpid(mpid, ran.nextBoolean());
        assertEquals(mpid, manager.getMpid());
    }

    @Test
    public void testGetAudienceTimeout() throws Exception {
        assertEquals(manager.mLocalPrefs.audienceTimeout, manager.getAudienceTimeout());
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
        for (int i = 0; i < hashes.length(); i++) {
            int hash = hashes.getInt(i);
            assertTrue(hash == 1217787541 || hash == 2 || hash == 3);
        }
    }

    @Test
    public void testShouldTrigger() throws Exception {
        BaseMPMessage message = new BaseMPMessage.Builder(Constants.MessageType.COMMERCE_EVENT).build(new InternalSession(), null, 1);
        assertTrue(manager.shouldTrigger(message));
        message = new BaseMPMessage.Builder(Constants.MessageType.PUSH_RECEIVED).build(new InternalSession(), null, 1);
        assertTrue(manager.shouldTrigger(message));
    }

    @Test
    public void testGetMpid() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(null, 0);
        //Since getMpId() is called in the ConfigManager constructor, reset it here.
        prefs.edit().remove(Constants.PrefKeys.MPID).apply();
        long mpid = prefs.getLong(Constants.PrefKeys.MPID, 0);
        assertTrue(mpid == 0);
        mpid = manager.getMpid();
        long storedMpid = prefs.getLong(Constants.PrefKeys.MPID, 0);
        //Changed this from != 0, since as of IdentityAPI changes, we do not want to generate MPIDs
        //client side.
        assertTrue(mpid == 0);
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

    static String ATTRIBUTES = "mp::integrationattributes";

    @Test
    public void testSetNullIntegrationAttributes() throws Exception {
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        manager.setIntegrationAttributes(1, null);
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        manager.sPreferences.edit().putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"test-value\"}}").apply();
        assertTrue(manager.sPreferences.contains(ATTRIBUTES));
        manager.setIntegrationAttributes(1, null);
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
    }

    @Test
    public void testSetEmptyIntegrationAttributes() throws Exception {
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        Map<String, String> attributes = new HashMap<String, String>();
        manager.setIntegrationAttributes(1, attributes);
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        manager.sPreferences.edit().putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"test-value\"}}").apply();
        assertTrue(manager.sPreferences.contains(ATTRIBUTES));
        manager.setIntegrationAttributes(1, attributes);
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
    }

    @Test
    public void testSetNonEmptyIntegrationAttributes() throws Exception {
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("test-key", "value 2");
        manager.setIntegrationAttributes(1, attributes);
        attributes.put("test-key", "value 3");
        manager.setIntegrationAttributes(12, attributes);
        assertEquals("{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}", manager.sPreferences.getString(ATTRIBUTES, null));
    }

    @Test
    public void testGetKitIntegrationAttributes() throws Exception {
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        assertEquals(0, manager.getIntegrationAttributes(1).size());
        manager.sPreferences.edit().putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}").apply();
        Map<String, String> attributes = manager.getIntegrationAttributes(1);
        assertEquals(1, attributes.size());
        assertEquals("value 2", attributes.get("test-key"));
        attributes = manager.getIntegrationAttributes(12);
        assertEquals(1, attributes.size());
        assertEquals("value 3", attributes.get("test-key"));
        manager.sPreferences.edit().remove(ATTRIBUTES).apply();
        assertEquals(0, manager.getIntegrationAttributes(1).size());
        assertEquals(0, manager.getIntegrationAttributes(12).size());
    }

    @Test
    public void testGetAllIntegrationAttributes() throws Exception {
        assertFalse(manager.sPreferences.contains(ATTRIBUTES));
        assertNull(manager.getIntegrationAttributes());
        manager.sPreferences.edit().putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}").apply();
        JSONObject attributes = manager.getIntegrationAttributes();
        assertEquals(2, attributes.length());
        assertEquals("value 2", attributes.getJSONObject("1").get("test-key"));
        assertEquals("value 3", attributes.getJSONObject("12").get("test-key"));
        manager.sPreferences.edit().remove(ATTRIBUTES).apply();
        assertNull(manager.getIntegrationAttributes());
    }

    @Test
    public void testDefaultIncludeSessionHistory() throws Exception {
        assertTrue(manager.getIncludeSessionHistory());
    }

    @Test
    public void testIncludeSessionHistoryUpdateFromServer() throws Exception {
        assertTrue(manager.getIncludeSessionHistory());
        JSONObject config = new JSONObject();
        config.put("inhd", false);
        manager.updateConfig(config);
        assertFalse(manager.getIncludeSessionHistory());
        config.put("inhd", true);
        manager.updateConfig(config);
        assertTrue(manager.getIncludeSessionHistory());
        config.put("inhd", "false");
        manager.updateConfig(config);
        assertFalse(manager.getIncludeSessionHistory());
    }

    @Test
    public void testSaveUserIdentityJson() throws Exception {
        manager.saveUserIdentityJson(new JSONArray());
        assertEquals(0, manager.getUserIdentityJson().length());
        JSONObject identity = new JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }");
        JSONArray identities = new JSONArray();
        identities.put(identity);
        manager.saveUserIdentityJson(identities);
        assertEquals(1, manager.getUserIdentityJson().length());
        assertEquals(1473869816521L, manager.getUserIdentityJson().getJSONObject(0).getLong("dfs"));
    }

    @Test
    public void testGetUserIdentityJsonFixup() throws Exception {
        manager.saveUserIdentityJson(new JSONArray());
        JSONObject identity = new JSONObject("{ \"n\": 7, \"i\": \"email value 1\" }");
        JSONArray identities = new JSONArray();
        identities.put(identity);
        manager.saveUserIdentityJson(identities);
        assertEquals(1, manager.getUserIdentityJson().length());
        assertEquals(0, manager.getUserIdentityJson().getJSONObject(0).getLong("dfs"));
    }

    @Test
    public void testMarkIdentitiesAsSeen() throws Exception {
        JSONArray identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 2, \"i\": \" value 2\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 3, \"i\": \" value 3\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 4, \"i\": \" value 4\", \"dfs\": 1473869816521, \"f\": true }"));
        manager.saveUserIdentityJson(identities);
        assertNull(manager.markIdentitiesAsSeen(new JSONArray()));
        JSONArray seenIdentities = manager.markIdentitiesAsSeen(identities);
        assertNotEquals(seenIdentities, identities);
        for (int i = 0; i < seenIdentities.length(); i++) {
            assertFalse(seenIdentities.getJSONObject(i).getBoolean("f"));
        }

        identities = new JSONArray();
        identities.put(new JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 2, \"i\": \" value 2\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 3, \"i\": \" value 3\", \"dfs\": 1473869816521, \"f\": true }"));
        identities.put(new JSONObject("{ \"n\": 4, \"i\": \" value 4\", \"dfs\": 1473869816521, \"f\": false }"));

        manager.saveUserIdentityJson(identities);
        assertNotNull(manager.getUserIdentityJson());
        assertEquals(4, manager.getUserIdentityJson().length());
        JSONArray newIdentities = new JSONArray();
        newIdentities.put(new JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"));
        JSONArray updatedIdentities = manager.markIdentitiesAsSeen(newIdentities);
        assertEquals(4, updatedIdentities.length());
        for (int i = 0; i< updatedIdentities.length(); i++) {
            int identity = updatedIdentities.getJSONObject(i).getInt("n");
            switch (identity) {
                case 1:
                case 4:
                    assertFalse(updatedIdentities.getJSONObject(i).getBoolean("f"));
                    break;
                default:
                    assertTrue(updatedIdentities.getJSONObject(i).getBoolean("f"));
            }
        }

    }

    @Test
    public void testSetMpidCallback() {

        ConfigManager.addMpIdChangeListener(new IdentityApi.MpIdChangeListener() {
            @Override
            public void onMpIdChanged(long mpid, long previousMpid) {
                callbackResult.value = mpid;
            }
        });
        Random ran = new Random();
        Long mpid1 = ran.nextLong();
        Long mpid2 = ran.nextLong();

        manager.setMpid(mpid1, false);
        assertEquals(mpid1, getCallbackResult());

        manager.setMpid(mpid1, false);
        assertNull(getCallbackResult());

        manager.setMpid(mpid1, true);
        assertEquals(mpid1, getCallbackResult());

        manager.setMpid(mpid1, true);
        assertNull(getCallbackResult());

        manager.setMpid(mpid1, false);
        assertEquals(mpid1, getCallbackResult());

        manager.setMpid(mpid1, false);
        assertNull(getCallbackResult());

        manager.setMpid(mpid2, true);
        assertEquals(mpid2, getCallbackResult());

        manager.setMpid(mpid1, true);
        assertEquals(mpid1, getCallbackResult());
    }

    @Test
    public void testPushInstanceIdBackground() {
        assertNull(manager.getPushInstanceIdBackground());
        assertNull(manager.getPushInstanceId());
        manager.setPushRegistrationInBackground(new PushRegistrationHelper.PushRegistration("instanceId", "senderId"));
        assertNotNull(manager.getPushInstanceIdBackground());
        assertEquals("", manager.getPushInstanceIdBackground());
        assertEquals(manager.getPushInstanceId(), "instanceId");
        manager.setPushRegistrationInBackground(new PushRegistrationHelper.PushRegistration("instanceId2", "senderId2"));
        assertEquals("instanceId", manager.getPushInstanceIdBackground());
        assertEquals("instanceId2", manager.getPushInstanceId());
        manager.clearPushRegistrationBackground();
        assertNull(manager.getPushInstanceIdBackground());
        assertEquals("instanceId2", manager.getPushInstanceId());
    }

    @Test
    public void testMaxAliasWindow() throws JSONException {
        //test default value
        assertEquals(90, manager.getAliasMaxWindow());

        //test set via config
        int maxWindow = ran.nextInt();
        JSONObject jsonObject = new JSONObject()
                .put(ConfigManager.ALIAS_MAX_WINDOW, maxWindow);
        manager.updateConfig(jsonObject);

        assertEquals(maxWindow, manager.getAliasMaxWindow());
    }

    private AndroidUtils.Mutable<Long> callbackResult = new AndroidUtils.Mutable<Long>(null);
    private Long getCallbackResult() {
        Long result = callbackResult.value;
        callbackResult.value = null;
        return result;
    }

    @Test
    public void testETag() throws JSONException {
        manager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
        String newEtag = new RandomUtils().getAlphaString(24);
        //test default value
        assertNull(manager.getEtag());

        //test set via config
        manager.updateConfig(new JSONObject(), newEtag, null);
        assertEquals(newEtag, manager.getEtag());
    }

    @Test
    public void testLastModified() throws JSONException {
        manager = new ConfigManager(context, MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
        String lastModified = String.valueOf(Math.abs(ran.nextLong()));

        //test default value
        assertNull(manager.getIfModified());

        //test set via config
        manager.updateConfig(new JSONObject(), null, lastModified);
        assertEquals(lastModified, manager.getIfModified());
    }

    @Test
    public void testConfigTimestamp() throws InterruptedException, JSONException {
        ConfigManager.clear();

        //test default value
        assertNull(manager.getConfigTimestamp());

        //test set via config, make sure it is after previous timestamp
        Long startTime = System.currentTimeMillis();
        manager.updateConfig(new JSONObject(), null, null);
        Long endTime = System.currentTimeMillis();

        Long setTimestamp = manager.getConfigTimestamp();
        assertNotNull(setTimestamp);
        assertTrue(setTimestamp >= startTime);
        assertTrue(setTimestamp <= endTime);

        //test that it stays consistant
        Thread.sleep(10);
        assertEquals(setTimestamp, manager.getConfigTimestamp());
    }

    @Test
    public void testGetConfig() throws JSONException {
        ConfigManager.clear();
        
        JSONObject newConfigJson = new JSONObject();
        int configSize = Math.abs(ran.nextInt() % 15);
        for (int i = 0; i < configSize; i++) {
            newConfigJson.put(randomUtils.getAlphaNumericString(8), randomUtils.getAlphaNumericString(12));
        }

        //test defaults
        assertNull(manager.getConfig());
        assertNull(manager.getEtag());
        assertNull(manager.getIfModified());
        assertNull(manager.getConfigTimestamp());

        //test reload() does not set config
        manager.reloadConfig(newConfigJson);
        assertNull(manager.getConfig());
        assertNull(manager.getEtag());
        assertNull(manager.getIfModified());
        assertNull(manager.getConfigTimestamp());

        //test update DOES set config
        manager.updateConfig(newConfigJson,"my ETag", "12345");
        assertEquals(newConfigJson.toString(), manager.getConfig());
        assertEquals("my ETag", manager.getEtag());
        assertEquals("12345", manager.getIfModified());
        assertNotNull(manager.getConfigTimestamp());
    }
}