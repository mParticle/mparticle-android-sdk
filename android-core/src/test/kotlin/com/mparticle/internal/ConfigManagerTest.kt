package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.internal.KitManager.KitStatus
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.mock.MockContext
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.RandomUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Random
import kotlin.math.abs

class ConfigManagerTest {
    lateinit var context: MockContext
    lateinit var manager: ConfigManager
    lateinit var mockMp: MParticle
    private val ran = Random()
    private val randomUtils = RandomUtils()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = MockContext()
        manager = ConfigManager(
            context,
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null,
            null
        )
        mockMp = MockMParticle()
        MParticle.setInstance(mockMp)
        manager.updateConfig(JSONObject(sampleConfig))
    }

    @Test
    fun testInitialization() {
        manager = ConfigManager(
            context,
            MParticle.Environment.Production,
            "key1",
            "secret1",
            null,
            null,
            null,
            null,
            null,
            null
        )
        Assert.assertEquals("key1", manager.apiKey)
        Assert.assertEquals("secret1", manager.apiSecret)
        Assert.assertEquals(MParticle.Environment.Production, ConfigManager.getEnvironment())

        // should persist when we start configmanager without any arguments
        manager = ConfigManager(context)
        Assert.assertEquals("key1", manager.apiKey)
        Assert.assertEquals("secret1", manager.apiSecret)
        Assert.assertEquals(MParticle.Environment.Production, ConfigManager.getEnvironment())

        // updates key/secret if one is non-null
        manager = ConfigManager(
            context,
            MParticle.Environment.Development,
            "key2",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        Assert.assertEquals("key2", manager.apiKey)
        Assert.assertNull(manager.apiSecret)
        Assert.assertEquals(MParticle.Environment.Development, ConfigManager.getEnvironment())
    }

    @Test
    @Throws(Exception::class)
    fun testSaveConfigJson() {
        manager.saveConfigJson(null)
        val json = JSONObject()
        json.put("test", "value")
        manager.saveConfigJson(json)
        val `object` =
            ConfigManager.sPreferences.getString(ConfigManager.CONFIG_JSON, null)
                ?.let { JSONObject(it) }
        Assert.assertNotNull(`object`)
    }

    @Test
    @Throws(Exception::class)
    fun testGetLatestKitConfiguration() {
        val array = manager.latestKitConfiguration
        val ekConfig = array?.getJSONObject(0)
        Assert.assertEquals(64, ekConfig?.getInt("id"))
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateConfig() {
        Assert.assertEquals(5, ConfigManager.getPushKeys(context).length().toLong())
        manager.updateConfig(JSONObject())
        val `object` =
            ConfigManager.sPreferences.getString(ConfigManager.CONFIG_JSON, null)
                ?.let { JSONObject(it) }
        if (`object` != null) {
            Assert.assertTrue(!`object`.keys().hasNext())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateConfigWithReload() {
        manager.updateConfig(JSONObject(sampleConfig))
        manager.reloadCoreConfig(JSONObject())
        val `object` =
            ConfigManager.sPreferences.getString(ConfigManager.CONFIG_JSON, null)
                ?.let { JSONObject(it) }
        `object`?.keys()?.hasNext()?.let { Assert.assertTrue(it) }
    }

    @Test
    @Throws(Exception::class)
    fun testGetActiveModuleIds() {
        val kitStatusMap: MutableMap<Int, KitStatus> = HashMap()
        kitStatusMap[1] = KitStatus.STOPPED
        kitStatusMap[2] = KitStatus.ACTIVE
        kitStatusMap[3] = KitStatus.NOT_CONFIGURED
        kitStatusMap[5] = KitStatus.STOPPED
        kitStatusMap[4] = KitStatus.ACTIVE
        kitStatusMap[6] = KitStatus.NOT_CONFIGURED
        Mockito.`when`(
            MParticle.getInstance()?.Internal()?.kitManager?.kitStatus
        )
            .thenReturn(kitStatusMap)
        Assert.assertEquals("1,2,4,5", manager.activeModuleIds)
        Mockito.`when`(
            MParticle.getInstance()?.Internal()?.kitManager?.kitStatus
        )
            .thenReturn(HashMap())
        Assert.assertEquals("", manager.activeModuleIds)
    }

    @Test
    @Throws(Exception::class)
    fun testDelayedStart() {
        /* val called = arrayOfNulls<Boolean>(3)
            MParticle.setInstance(new MockMParticle() {
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
    @Throws(Exception::class)
    fun testGetTriggerMessageMatches() {
        val triggerMessageMatches = manager.triggerMessageMatches
        Assert.assertEquals(1, triggerMessageMatches.length().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetInfluenceOpenTimeoutMillis() {
        Assert.assertEquals((30 * 60 * 1000).toLong(), manager.influenceOpenTimeoutMillis)
    }

    @Test
    @Throws(Exception::class)
    fun testSetAndGetLogUnhandledExceptions() {
        Assert.assertFalse(manager.logUnhandledExceptions)
        manager.logUnhandledExceptions = true
        Assert.assertTrue(manager.logUnhandledExceptions)
        val `object` = JSONObject(sampleConfig)
        `object`.put(ConfigManager.KEY_UNHANDLED_EXCEPTIONS, "forcecatch")
        manager.updateConfig(`object`)
        manager.logUnhandledExceptions = false
        Assert.assertTrue(manager.logUnhandledExceptions)
    }

    @Test
    @Throws(Exception::class)
    fun testGetApiKey() {
        manager.setCredentials("some key", "some key")
        Assert.assertEquals("some key", manager.apiKey)
    }

    @Test
    @Throws(Exception::class)
    fun testGetApiSecret() {
        manager.setCredentials("some key", "some secret")
        Assert.assertEquals("some secret", manager.apiSecret)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadInterval() {
        val `object` = JSONObject(sampleConfig)
        manager.setUploadInterval(987)
        Assert.assertEquals((1000 * 987).toLong(), manager.uploadInterval)
        `object`.put(ConfigManager.KEY_UPLOAD_INTERVAL, 110)
        manager.updateConfig(`object`)
        Assert.assertEquals((1000 * 110).toLong(), manager.uploadInterval)
    }

    @Test
    @Throws(Exception::class)
    fun testGetEnvironment() {
        Assert.assertEquals(MParticle.Environment.Production, ConfigManager.getEnvironment())
    }

    @Test
    @Throws(Exception::class)
    fun testSessionTimeout() {
        manager.sessionTimeout = 123
        Assert.assertEquals((123 * 1000).toLong(), manager.sessionTimeout.toLong())
        val `object` = JSONObject(sampleConfig)
        `object`.put(ConfigManager.KEY_SESSION_TIMEOUT, 123)
        manager.updateConfig(`object`)
        Assert.assertEquals((123 * 1000).toLong(), manager.sessionTimeout.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testPushSenderId() {
        Assert.assertNull(manager.pushSenderId)
        manager.pushSenderId = "sender_id_test"
        Assert.assertEquals("sender_id_test", manager.pushSenderId)
    }

    @Test
    @Throws(Exception::class)
    fun testIsEnabled() {
        Assert.assertTrue(manager.isEnabled)
        manager.setOptOut(true)
        Assert.assertFalse(manager.isEnabled)
        val `object` = JSONObject(sampleConfig)
        `object`.put(ConfigManager.KEY_OPT_OUT, true)
        manager.updateConfig(`object`)
        Assert.assertTrue(manager.isEnabled)
    }

    @Test
    @Throws(Exception::class)
    fun testPushNotificationIcon() {
        Assert.assertEquals(0, ConfigManager.getPushIcon(context).toLong())
        manager.setPushNotificationIcon(5)
        Assert.assertEquals(5, ConfigManager.getPushIcon(context).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSetPushNotificationTitle() {
        Assert.assertEquals(0, ConfigManager.getPushTitle(context).toLong())
        manager.setPushNotificationTitle(4)
        Assert.assertEquals(4, ConfigManager.getPushTitle(context).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetPushKeys() {
        val pushKeys = ConfigManager.getPushKeys(context)
        val keys = arrayOf("mp_message", "com.urbanairship.push.ALERT", "alert", "a", "message")
        val list = listOf(*keys)
        for (i in 0 until pushKeys.length()) {
            Assert.assertTrue(list.contains(pushKeys.getString(i)))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetBreadcrumbLimit() {
        Assert.assertEquals(
            UserStorage.DEFAULT_BREADCRUMB_LIMIT.toLong(),
            ConfigManager.getBreadcrumbLimit(context).toLong()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetBreadcrumbLimit() {
        manager.setBreadcrumbLimit(4343)
        Assert.assertEquals(4343, ConfigManager.getBreadcrumbLimit(context).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSetMpid() {
        val mpid = System.currentTimeMillis()
        manager.setMpid(mpid, ran.nextBoolean())
        Assert.assertEquals(mpid, manager.mpid)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCurrentRampValue() {
        Assert.assertEquals(-1, manager.currentRampValue.toLong())
        val `object` = JSONObject(sampleConfig)
        `object`.put(ConfigManager.KEY_RAMP, 43)
        manager.updateConfig(`object`)
        Assert.assertEquals(43, manager.currentRampValue.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetTriggerMessageHashes() {
        val hashes = manager.triggerMessageHashes
        for (i in 0 until hashes.length()) {
            val hash = hashes.getInt(i)
            Assert.assertTrue(hash == 1217787541 || hash == 2 || hash == 3)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testShouldTrigger() {
        var message = BaseMPMessage.Builder(Constants.MessageType.COMMERCE_EVENT)
            .build(InternalSession(), null, 1)
        Assert.assertTrue(manager.shouldTrigger(message))
        message = BaseMPMessage.Builder(Constants.MessageType.PUSH_RECEIVED)
            .build(InternalSession(), null, 1)
        Assert.assertTrue(manager.shouldTrigger(message))
    }

    @Test
    @Throws(Exception::class)
    fun testGetMpid() {
        val prefs = context.getSharedPreferences(null, 0)
        // Since getMpId() is called in the ConfigManager constructor, reset it here.
        prefs.edit().remove(Constants.PrefKeys.MPID).apply()
        var mpid = prefs.getLong(Constants.PrefKeys.MPID, 0)
        Assert.assertTrue(mpid == 0L)
        mpid = manager.mpid
        val storedMpid = prefs.getLong(Constants.PrefKeys.MPID, 0)
        // Changed this from != 0, since as of IdentityAPI changes, we do not want to generate MPIDs
        // client side.
        Assert.assertTrue(mpid == 0L)
        Assert.assertTrue(storedMpid == mpid)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserBucket() {
        val bucket = manager.userBucket
        Assert.assertTrue(bucket >= 0 && bucket <= 100)
    }

    @Test
    @Throws(Exception::class)
    fun testSetNullIntegrationAttributes() {
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        manager.setIntegrationAttributes(1, null)
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        ConfigManager.sPreferences.edit()
            .putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"test-value\"}}").apply()
        Assert.assertTrue(ConfigManager.sPreferences.contains(ATTRIBUTES))
        manager.setIntegrationAttributes(1, null)
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
    }

    @Test
    @Throws(Exception::class)
    fun testSetEmptyIntegrationAttributes() {
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        val attributes: Map<String, String> = HashMap()
        manager.setIntegrationAttributes(1, attributes)
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        ConfigManager.sPreferences.edit()
            .putString(ATTRIBUTES, "{\"1\":{\"test-key\":\"test-value\"}}").apply()
        Assert.assertTrue(ConfigManager.sPreferences.contains(ATTRIBUTES))
        manager.setIntegrationAttributes(1, attributes)
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
    }

    @Test
    @Throws(Exception::class)
    fun testSetNonEmptyIntegrationAttributes() {
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        val attributes: MutableMap<String, String> = HashMap()
        attributes["test-key"] = "value 2"
        manager.setIntegrationAttributes(1, attributes)
        attributes["test-key"] = "value 3"
        manager.setIntegrationAttributes(12, attributes)
        Assert.assertEquals(
            "{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}",
            ConfigManager.sPreferences.getString(
                ATTRIBUTES,
                null
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetKitIntegrationAttributes() {
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        Assert.assertEquals(0, manager.getIntegrationAttributes(1).size.toLong())
        ConfigManager.sPreferences.edit().putString(
            ATTRIBUTES,
            "{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}"
        ).apply()
        var attributes = manager.getIntegrationAttributes(1)
        Assert.assertEquals(1, attributes.size.toLong())
        Assert.assertEquals("value 2", attributes["test-key"])
        attributes = manager.getIntegrationAttributes(12)
        Assert.assertEquals(1, attributes.size.toLong())
        Assert.assertEquals("value 3", attributes["test-key"])
        ConfigManager.sPreferences.edit().remove(ATTRIBUTES).apply()
        Assert.assertEquals(0, manager.getIntegrationAttributes(1).size.toLong())
        Assert.assertEquals(0, manager.getIntegrationAttributes(12).size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllIntegrationAttributes() {
        Assert.assertFalse(ConfigManager.sPreferences.contains(ATTRIBUTES))
        Assert.assertNull(manager.integrationAttributes)
        ConfigManager.sPreferences.edit().putString(
            ATTRIBUTES,
            "{\"1\":{\"test-key\":\"value 2\"},\"12\":{\"test-key\":\"value 3\"}}"
        ).apply()
        val attributes = manager.integrationAttributes
        Assert.assertEquals(2, attributes.length().toLong())
        Assert.assertEquals("value 2", attributes.getJSONObject("1")["test-key"])
        Assert.assertEquals("value 3", attributes.getJSONObject("12")["test-key"])
        ConfigManager.sPreferences.edit().remove(ATTRIBUTES).apply()
        Assert.assertNull(manager.integrationAttributes)
    }

    @Test
    @Throws(Exception::class)
    fun testSaveUserIdentityJson() {
        manager.saveUserIdentityJson(JSONArray())
        Assert.assertEquals(0, manager.userIdentityJson.length().toLong())
        val identity =
            JSONObject("{ \"n\": 7, \"i\": \"email value 1\", \"dfs\": 1473869816521, \"f\": true }")
        val identities = JSONArray()
        identities.put(identity)
        manager.saveUserIdentityJson(identities)
        Assert.assertEquals(1, manager.userIdentityJson.length().toLong())
        Assert.assertEquals(
            1473869816521L,
            manager.userIdentityJson.getJSONObject(0).getLong("dfs")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserIdentityJsonFixup() {
        manager.saveUserIdentityJson(JSONArray())
        val identity = JSONObject("{ \"n\": 7, \"i\": \"email value 1\" }")
        val identities = JSONArray()
        identities.put(identity)
        manager.saveUserIdentityJson(identities)
        Assert.assertEquals(1, manager.userIdentityJson.length().toLong())
        Assert.assertEquals(0, manager.userIdentityJson.getJSONObject(0).getLong("dfs"))
    }

    @Test
    @Throws(Exception::class)
    fun testMarkIdentitiesAsSeen() {
        var identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 2, \"i\": \" value 2\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 3, \"i\": \" value 3\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 4, \"i\": \" value 4\", \"dfs\": 1473869816521, \"f\": true }"))
        manager.saveUserIdentityJson(identities)
        Assert.assertNull(manager.markIdentitiesAsSeen(JSONArray()))
        val seenIdentities = manager.markIdentitiesAsSeen(identities)
        Assert.assertNotEquals(seenIdentities, identities)
        for (i in 0 until seenIdentities.length()) {
            Assert.assertFalse(seenIdentities.getJSONObject(i).getBoolean("f"))
        }
        identities = JSONArray()
        identities.put(JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 2, \"i\": \" value 2\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 3, \"i\": \" value 3\", \"dfs\": 1473869816521, \"f\": true }"))
        identities.put(JSONObject("{ \"n\": 4, \"i\": \" value 4\", \"dfs\": 1473869816521, \"f\": false }"))
        manager.saveUserIdentityJson(identities)
        Assert.assertNotNull(manager.userIdentityJson)
        Assert.assertEquals(4, manager.userIdentityJson.length().toLong())
        val newIdentities = JSONArray()
        newIdentities.put(JSONObject("{ \"n\": 1, \"i\": \" value 1\", \"dfs\": 1473869816521, \"f\": true }"))
        val updatedIdentities = manager.markIdentitiesAsSeen(newIdentities)
        Assert.assertEquals(4, updatedIdentities.length().toLong())
        for (i in 0 until updatedIdentities.length()) {
            when (updatedIdentities.getJSONObject(i).getInt("n")) {
                1, 4 -> Assert.assertFalse(updatedIdentities.getJSONObject(i).getBoolean("f"))
                else -> Assert.assertTrue(updatedIdentities.getJSONObject(i).getBoolean("f"))
            }
        }
    }

    @Test
    fun testSetMpidCallback() {
        ConfigManager.addMpIdChangeListener { mpid, previousMpid -> callbackResult.value = mpid }
        val ran = Random()
        val mpid1 = ran.nextLong()
        val mpid2 = ran.nextLong()
        manager.setMpid(mpid1, false)
        Assert.assertEquals(mpid1, getCallbackResult())
        manager.setMpid(mpid1, false)
        Assert.assertNull(getCallbackResult())
        manager.setMpid(mpid1, true)
        Assert.assertEquals(mpid1, getCallbackResult())
        manager.setMpid(mpid1, true)
        Assert.assertNull(getCallbackResult())
        manager.setMpid(mpid1, false)
        Assert.assertEquals(mpid1, getCallbackResult())
        manager.setMpid(mpid1, false)
        Assert.assertNull(getCallbackResult())
        manager.setMpid(mpid2, true)
        Assert.assertEquals(mpid2, getCallbackResult())
        manager.setMpid(mpid1, true)
        Assert.assertEquals(mpid1, getCallbackResult())
    }

    @Test
    fun testPushInstanceIdBackground() {
        Assert.assertNull(manager.pushInstanceIdBackground)
        Assert.assertNull(manager.pushInstanceId)
        manager.setPushRegistrationInBackground(PushRegistration("instanceId", "senderId"))
        Assert.assertNotNull(manager.pushInstanceIdBackground)
        Assert.assertEquals("", manager.pushInstanceIdBackground)
        Assert.assertEquals(manager.pushInstanceId, "instanceId")
        manager.setPushRegistrationInBackground(PushRegistration("instanceId2", "senderId2"))
        Assert.assertEquals("instanceId", manager.pushInstanceIdBackground)
        Assert.assertEquals("instanceId2", manager.pushInstanceId)
        manager.clearPushRegistrationBackground()
        Assert.assertNull(manager.pushInstanceIdBackground)
        Assert.assertEquals("instanceId2", manager.pushInstanceId)
    }

    @Test
    @Throws(JSONException::class)
    fun testMaxAliasWindow() {
        // test default value
        Assert.assertEquals(90, manager.aliasMaxWindow.toLong())

        // test set via config
        val maxWindow = ran.nextInt()
        val jsonObject = JSONObject()
            .put(ConfigManager.ALIAS_MAX_WINDOW, maxWindow)
        manager.updateConfig(jsonObject)
        Assert.assertEquals(maxWindow.toLong(), manager.aliasMaxWindow.toLong())
    }

    private val callbackResult = AndroidUtils.Mutable<Long?>(null)
    private fun getCallbackResult(): Long? {
        val result = callbackResult.value
        callbackResult.value = null
        return result
    }

    @Test
    @Throws(JSONException::class)
    fun testETag() {
        manager = ConfigManager(
            context,
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null,
            null
        )
        val newEtag = RandomUtils().getAlphaString(24)
        // test default value
        Assert.assertNull(manager.etag)

        // test set via config
        manager.updateConfig(JSONObject(), newEtag, null)
        Assert.assertEquals(newEtag, manager.etag)
    }

    @Test
    @Throws(JSONException::class)
    fun testLastModified() {
        manager = ConfigManager(
            context,
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null,
            null
        )
        val lastModified = abs(ran.nextLong()).toString()

        // test default value
        Assert.assertNull(manager.ifModified)

        // test set via config
        manager.updateConfig(JSONObject(), null, lastModified)
        Assert.assertEquals(lastModified, manager.ifModified)
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun testConfigTimestamp() {
        ConfigManager.clear()

        // test default value
        Assert.assertNull(manager.configTimestamp)

        // test set via config, make sure it is after previous timestamp
        val startTime = System.currentTimeMillis()
        manager.updateConfig(JSONObject(), null, null)
        val endTime = System.currentTimeMillis()
        val setTimestamp = manager.configTimestamp
        Assert.assertNotNull(setTimestamp)
        if (setTimestamp != null) {
            Assert.assertTrue(setTimestamp >= startTime)
        }
        if (setTimestamp != null) {
            Assert.assertTrue(setTimestamp <= endTime)
        }

        // test that it stays consistent
        Thread.sleep(10)
        Assert.assertEquals(setTimestamp, manager.configTimestamp)
    }

    @Test
    @Throws(JSONException::class)
    fun testGetConfig() {
        ConfigManager.clear()
        val newConfigJson = JSONObject()
        val configSize = abs(ran.nextInt() % 15)
        for (i in 0 until configSize) {
            newConfigJson.put(
                randomUtils.getAlphaNumericString(8),
                randomUtils.getAlphaNumericString(12)
            )
        }

        // test defaults
        Assert.assertTrue(manager.config?.isEmpty()!!)
        Assert.assertNull(manager.etag)
        Assert.assertNull(manager.ifModified)
        Assert.assertNull(manager.configTimestamp)

        // test reload() does not set config
        manager.reloadCoreConfig(newConfigJson)
        Assert.assertTrue(manager.config?.isEmpty()!!)
        Assert.assertNull(manager.etag)
        Assert.assertNull(manager.ifModified)
        Assert.assertNull(manager.configTimestamp)

        // test update DOES set config
        manager.updateConfig(newConfigJson, "my ETag", "12345")
        Assert.assertEquals(newConfigJson.toString(), manager.config)
        Assert.assertEquals("my ETag", manager.etag)
        Assert.assertEquals("12345", manager.ifModified)
        Assert.assertNotNull(manager.configTimestamp)
    }

    companion object {
        private const val sampleConfig =
            "{ \"dt\":\"ac\", \"id\":\"5b7b8073-852b-47c2-9b89-c4bc66e3bd55\", \"ct\":1428030730685, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[ \"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\" ], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"tri\" : { \"mm\" : [{ \"dt\" : \"x\", \"eh\" : true } ], \"evts\" : [1217787541, 2, 3] }, \"eks\":[ { \"id\":64, \"as\":{ \"clientId\":\"8FMBElARYl9ZtgwYIN5sZA==\", \"surveyId\":\"android_app\", \"sendAppVersion\":\"True\", \"rootUrl\":\"http://survey.foreseeresults.com/survey/display\" }, \"hs\":{ \"et\":{ \"57\":0, \"49\":0, \"55\":0, \"52\":0, \"53\":0, \"50\":0, \"56\":0, \"51\":0, \"54\":0, \"48\":0 }, \"ec\":{ \"609391310\":0, \"-1282670145\":0, \"2138942058\":0, \"-1262630649\":0, \"-877324321\":0, \"1700497048\":0, \"1611158813\":0, \"1900204162\":0, \"-998867355\":0, \"-1758179958\":0, \"-994832826\":0, \"1598473606\":0, \"-2106320589\":0 }, \"ea\":{ \"343635109\":0, \"1162787110\":0, \"-427055400\":0, \"-1285822129\":0, \"1699530232\":0 }, \"svec\":{ \"-725356351\":0, \"-1992427723\":0, \"751512662\":0, \"-118381281\":0, \"-171137512\":0, \"-2036479142\":0, \"-1338304551\":0, \"1003167705\":0, \"1046650497\":0, \"1919407518\":0, \"-1326325184\":0, \"480870493\":0, \"-1087232483\":0, \"-725540438\":0, \"-461793000\":0, \"1935019626\":0, \"76381608\":0, \"273797382\":0, \"-948909976\":0, \"-348193740\":0, \"-685370074\":0, \"-849874419\":0, \"2074021738\":0, \"-767572488\":0, \"-1091433459\":0, \"1671688881\":0, \"1304651793\":0, \"1299738196\":0, \"326063875\":0, \"296835202\":0, \"268236000\":0, \"1708308839\":0, \"101093345\":0, \"-652558691\":0, \"-1613021771\":0, \"1106318256\":0, \"-473874363\":0, \"-1267780435\":0, \"486732621\":0, \"1855792002\":0, \"-881258627\":0, \"698731249\":0, \"1510155838\":0, \"1119638805\":0, \"479337352\":0, \"1312099430\":0, \"1712783405\":0, \"-459721027\":0, \"-214402990\":0, \"617910950\":0, \"428901717\":0, \"-201124647\":0, \"940674176\":0, \"1632668193\":0, \"338835860\":0, \"879890181\":0, \"1667730064\":0 } } } ], \"lsv\":\"2.1.4\", \"pio\":30 }"
        var ATTRIBUTES = "mp::integrationattributes"
    }
}
