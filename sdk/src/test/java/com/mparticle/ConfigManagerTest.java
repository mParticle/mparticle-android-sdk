package com.mparticle;

import android.test.mock.*;

import com.mparticle.internal.embedded.EmbeddedKitManager;
import com.mparticle.messaging.MPMessagingAPI;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigManagerTest {


    MockContext context;
    ConfigManager manager;
    private static final String sampleConfig = "{\"dt\":\"ac\",\"id\":\"5b7b8073-852b-47c2-9b89-c4bc66e3bd55\",\"ct\":1428030730685,\"dbg\":false,\"cue\":\"appdefined\",\"pmk\":[\"mp_message\",\"com.urbanairship.push.ALERT\",\"alert\",\"a\",\"message\"],\"cnp\":\"appdefined\",\"soc\":0,\"oo\":false,\"eks\":[{\"id\":64,\"as\":{\"clientId\":\"8FMBElARYl9ZtgwYIN5sZA==\",\"surveyId\":\"android_app\",\"sendAppVersion\":\"True\",\"rootUrl\":\"http://survey.foreseeresults.com/survey/display\"},\"hs\":{\"et\":{\"57\":0,\"49\":0,\"55\":0,\"52\":0,\"53\":0,\"50\":0,\"56\":0,\"51\":0,\"54\":0,\"48\":0},\"ec\":{\"609391310\":0,\"-1282670145\":0,\"2138942058\":0,\"-1262630649\":0,\"-877324321\":0,\"1700497048\":0,\"1611158813\":0,\"1900204162\":0,\"-998867355\":0,\"-1758179958\":0,\"-994832826\":0,\"1598473606\":0,\"-2106320589\":0},\"ea\":{\"343635109\":0,\"1162787110\":0,\"-427055400\":0,\"-1285822129\":0,\"1699530232\":0},\"svec\":{\"-725356351\":0,\"-1992427723\":0,\"751512662\":0,\"-118381281\":0,\"-171137512\":0,\"-2036479142\":0,\"-1338304551\":0,\"1003167705\":0,\"1046650497\":0,\"1919407518\":0,\"-1326325184\":0,\"480870493\":0,\"-1087232483\":0,\"-725540438\":0,\"-461793000\":0,\"1935019626\":0,\"76381608\":0,\"273797382\":0,\"-948909976\":0,\"-348193740\":0,\"-685370074\":0,\"-849874419\":0,\"2074021738\":0,\"-767572488\":0,\"-1091433459\":0,\"1671688881\":0,\"1304651793\":0,\"1299738196\":0,\"326063875\":0,\"296835202\":0,\"268236000\":0,\"1708308839\":0,\"101093345\":0,\"-652558691\":0,\"-1613021771\":0,\"1106318256\":0,\"-473874363\":0,\"-1267780435\":0,\"486732621\":0,\"1855792002\":0,\"-881258627\":0,\"698731249\":0,\"1510155838\":0,\"1119638805\":0,\"479337352\":0,\"1312099430\":0,\"1712783405\":0,\"-459721027\":0,\"-214402990\":0,\"617910950\":0,\"428901717\":0,\"-201124647\":0,\"940674176\":0,\"1632668193\":0,\"338835860\":0,\"879890181\":0,\"1667730064\":0}}}],\"lsv\":\"2.1.4\",\"pio\":30}";
    private EmbeddedKitManager ekManager;

    @Before
    public void setUp() throws Exception {
        context = new MockContext();
        manager = new ConfigManager(context, MParticle.Environment.AutoDetect);
        ekManager = new EmbeddedKitManager(new MockContext());
        manager.setEmbeddedKitManager(ekManager);
        manager.updateConfig(new JSONObject(sampleConfig));
        MockMParticle mp = new MockMParticle();
        MParticle.setInstance(mp);
    }


    @Test
    public void testSetEmbeddedKitManager() throws Exception {
        EmbeddedKitManager ekManager = new EmbeddedKitManager(new MockContext());
        manager.setEmbeddedKitManager(ekManager);
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
    public void testRestore() throws Exception {
        manager.restore();
        JSONObject object = new JSONObject(manager.mPreferences.getString(ConfigManager.CONFIG_JSON, null));
        assertNotNull(object);
        assertTrue(ConfigManager.getPushKeys(context).length() == 5);
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
        assertEquals(ekManager.getActiveModuleIds(), manager.getActiveModuleIds());
    }

    @Test
    public void testDelayedStart() throws Exception {
        boolean enabled = manager.isPushEnabled();
        final Boolean[] called = new Boolean[3];
        MParticle.setInstance(new MockMParticle(){

            @Override
            public MPMessagingAPI Messaging() {
                return new MPMessagingAPI(null, null){
                    @Override
                    public void enablePushNotifications(String senderId) {
                        called[0] = true;
                    }
                };
            }
        });
        manager.delayedStart();
        if (enabled){
            assertTrue(called[0]);
        }
    }

    @Test
    public void testGetTriggerMessageMatches() throws Exception {

    }

    @Test
    public void testGetInfluenceOpenTimeoutMillis() throws Exception {

    }

    @Test
    public void testEnableUncaughtExceptionLogging() throws Exception {

    }

    @Test
    public void testDisableUncaughtExceptionLogging() throws Exception {

    }

    @Test
    public void testGetLogUnhandledExceptions() throws Exception {

    }

    @Test
    public void testSetLogUnhandledExceptions() throws Exception {

    }

    @Test
    public void testGetApiKey() throws Exception {

    }

    @Test
    public void testGetApiSecret() throws Exception {

    }

    @Test
    public void testGetUploadInterval() throws Exception {

    }

    @Test
    public void testGetEnvironment() throws Exception {

    }

    @Test
    public void testSetUploadInterval() throws Exception {

    }

    @Test
    public void testGetSessionTimeout() throws Exception {

    }

    @Test
    public void testSetSessionTimeout() throws Exception {

    }

    @Test
    public void testIsPushEnabled() throws Exception {

    }

    @Test
    public void testGetPushSenderId() throws Exception {

    }

    @Test
    public void testSetPushSenderId() throws Exception {

    }

    @Test
    public void testLog() throws Exception {

    }

    @Test
    public void testLog1() throws Exception {

    }

    @Test
    public void testGetLicenseKey() throws Exception {

    }

    @Test
    public void testIsLicensingEnabled() throws Exception {

    }

    @Test
    public void testSetPushSoundEnabled() throws Exception {

    }

    @Test
    public void testSetPushVibrationEnabled() throws Exception {

    }

    @Test
    public void testSetPushRegistrationId() throws Exception {

    }

    @Test
    public void testIsEnabled() throws Exception {

    }

    @Test
    public void testSetOptOut() throws Exception {

    }

    @Test
    public void testGetOptedOut() throws Exception {

    }

    @Test
    public void testIsAutoTrackingEnabled() throws Exception {

    }

    @Test
    public void testIsPushSoundEnabled() throws Exception {

    }

    @Test
    public void testIsPushVibrationEnabled() throws Exception {

    }

    @Test
    public void testSetPushNotificationIcon() throws Exception {

    }

    @Test
    public void testSetPushNotificationTitle() throws Exception {

    }

    @Test
    public void testGetPushKeys() throws Exception {

    }

    @Test
    public void testGetPushTitle() throws Exception {

    }

    @Test
    public void testGetPushIcon() throws Exception {

    }

    @Test
    public void testGetBreadcrumbLimit() throws Exception {

    }

    @Test
    public void testSetBreadcrumbLimit() throws Exception {

    }

    @Test
    public void testGetProviderPersistence() throws Exception {

    }

    @Test
    public void testIsNetworkPerformanceEnabled() throws Exception {

    }

    @Test
    public void testSetNetworkingEnabled() throws Exception {

    }

    @Test
    public void testSetMpid() throws Exception {

    }

    @Test
    public void testGetMpid() throws Exception {

    }

    @Test
    public void testGetAudienceTimeout() throws Exception {

    }

    @Test
    public void testSetLogLevel() throws Exception {

    }

    @Test
    public void testGetCurrentRampValue() throws Exception {

    }

    @Test
    public void testGetTriggerMessageHashes() throws Exception {

    }

    @Test
    public void testShouldTrigger() throws Exception {

    }
}