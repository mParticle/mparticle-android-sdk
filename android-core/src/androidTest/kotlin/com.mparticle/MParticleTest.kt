package com.mparticle

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityStateListener
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.MParticleJSInterface
import com.mparticle.internal.MessageManager
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.internal.database.services.UploadService
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer.JSONMatch
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import com.mparticle.testutils.TestingUtils
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import kotlin.test.assertTrue

class MParticleTest : BaseCleanStartedEachTest() {
    private val configResponse =
        "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[] }, \"pio\":30 }"

    @Test
    fun testEnsureSessionActive() {
        MParticle.getInstance()!!.mAppStateManager.ensureActiveSession()
        ensureSessionActive()
    }

    @Test
    fun testEnsureSessionActiveAtStart() {
        Assert.assertFalse(MParticle.getInstance()!!.isSessionActive)
    }

    @Test
    fun testSessionEndsOnOptOut() {
        MParticle.getInstance()!!.mAppStateManager.ensureActiveSession()
        Assert.assertTrue(MParticle.getInstance()!!.mAppStateManager.session.isActive)
        MParticle.getInstance()!!.optOut = true
        Assert.assertFalse(MParticle.getInstance()!!.mAppStateManager.session.isActive)
    }

    @Test
    fun testSetIntervalValue() {
        restartWithOptions(baseMParticleOptionBuilder.uploadInterval(20))
        val mp = MParticle.getInstance()
        Assert.assertEquals(20.toMillis(), mp?.mConfigManager?.uploadInterval)
        mp?.setUpdateInterval(30)
        Assert.assertEquals(30.toMillis(), mp?.mConfigManager?.uploadInterval)
    }

    @Test
    fun testSetIntervalValueInvalidNEgativeValue() {
        restartWithOptions(baseMParticleOptionBuilder.uploadInterval(20))
        val mp = MParticle.getInstance()
        Assert.assertEquals(20.toMillis(), mp?.mConfigManager?.uploadInterval)
        mp?.setUpdateInterval(-1)
        Assert.assertEquals(20.toMillis(), mp?.mConfigManager?.uploadInterval)
    }

    @Test
    fun testSetIntervalValueInvalidZeroValue() {
        restartWithOptions(baseMParticleOptionBuilder.uploadInterval(20))
        val mp = MParticle.getInstance()
        Assert.assertEquals(20.toMillis(), mp?.mConfigManager?.uploadInterval)
        mp?.setUpdateInterval(0)
        Assert.assertEquals(20.toMillis(), mp?.mConfigManager?.uploadInterval)
    }

    private fun Int.toMillis() = this * 1000L

    @Test
    fun testSetInstallReferrer() {
        MParticle.getInstance()!!.installReferrer = "foo install referrer"
        Assert.assertEquals("foo install referrer", MParticle.getInstance()!!.installReferrer)
    }

    @Test
    fun testInstallReferrerUpdate() {
        val randomName = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(4, 64))
        MParticle.getInstance()!!.installReferrer = randomName
        Assert.assertTrue(MParticle.getInstance()!!.installReferrer == randomName)
    }

    /**
     * These tests are to make sure that we are not missing any instances of the InstallReferrer
     * being set at any of the entry points, without the corresponding installReferrerUpdated() calls
     * being made.
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCalledUpdateInstallReferrer() {
        val called = BooleanArray(2)
        MParticle.getInstance()!!.mMessageManager = object : MessageManager() {
            override fun installReferrerUpdated() {
                called[0] = true
            }
        }
        MParticle.getInstance()!!.mKitManager =
            object : KitFrameworkWrapper(mContext, null, null, null, true, null) {
                override fun installReferrerUpdated() {
                    called[1] = true
                }
            }

        // Test when the InstallReferrer is set directly on the InstallReferrerHelper.
        var installReferrer = mRandomUtils.getAlphaNumericString(10)
        InstallReferrerHelper.setInstallReferrer(mContext, installReferrer)
        Assert.assertTrue(called[0])
        Assert.assertTrue(called[1])
        Arrays.fill(called, false)

        // Test when it is set through the MParticle object in the public API.
        installReferrer = mRandomUtils.getAlphaNumericString(10)
        MParticle.getInstance()!!.installReferrer = installReferrer
        Assert.assertTrue(called[0])
        Assert.assertTrue(called[1])
        Arrays.fill(called, false)

        // Just a sanity check, if Context is null, it should not set mark the InstallReferrer as updated.
        installReferrer = mRandomUtils.getAlphaNumericString(10)
        // InstallReferrerHelper.setInstallReferrer(null, installReferrer) //function does nothing when context is null
        Assert.assertFalse(called[0])
        Assert.assertFalse(called[1])
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testRegisterWebView() {
        MParticle.setInstance(null)
        val token = mRandomUtils.getAlphaNumericString(15)
        mServer.setupConfigResponse(
            JSONObject().put(ConfigManager.WORKSPACE_TOKEN, token).toString()
        )
        startMParticle()
        val jsInterfaces: MutableMap<String, Any> = HashMap()
        val latch = MPLatch(1)
        Handler(Looper.getMainLooper()).post {
            val webView: WebView = object : WebView(mContext) {
                override fun addJavascriptInterface(`object`: Any, name: String) {
                    jsInterfaces[name] = `object`
                }
            }
            MParticle.getInstance()!!.registerWebView(webView)
            Assert.assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + token + "_v2"] is MParticleJSInterface)
            val clientToken = mRandomUtils.getAlphaNumericString(15)
            MParticle.getInstance()!!.registerWebView(webView, clientToken)
            Assert.assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + clientToken + "_v2"] is MParticleJSInterface)
            latch.countDown()
        }
        latch.await()
        Assert.assertEquals(2, jsInterfaces.size.toLong())
    }

    private fun ensureSessionActive() {
        if (!MParticle.getInstance()!!.isSessionActive) {
            MParticle.getInstance()!!.logEvent(TestingUtils.getInstance().randomMPEventSimple)
            Assert.assertTrue(MParticle.getInstance()!!.isSessionActive)
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetIdentitySync() {
        testResetIdentityCall { MParticle.reset(mContext) }
    }

//    @OrchestratorOnly
//    @Test
//    @Throws(JSONException::class, InterruptedException::class)
//    fun testResetIdentityAsync() {
//        testResetIdentityCall {
//            val latch: CountDownLatch = MPLatch(1)
//            MParticle.reset(mContext) { latch.countDown() }
//            try {
//                latch.await()
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//        }
//    }

    @OrchestratorOnly
    @Test
    @Throws(InterruptedException::class)
    fun testResetConfigCall() {
        mServer.setupConfigResponse(configResponse, 100)
        MParticle.getInstance()!!.refreshConfiguration()
        MParticle.reset(mContext)
        // This sleep is here just to
        Thread.sleep(100)
        assertSDKGone()
    }

    /**
     * Test that Identity calls in progress will exit gracefully, and not trigger any callbacks.
     */
    @Throws(InterruptedException::class)
    fun testResetIdentityCall(resetRunnable: Runnable) {
        val called = BooleanArray(2)
        val crashListener = IdentityStateListener { user, previousUser ->
            Assert.assertTrue(called[0])
            throw IllegalStateException("Should not be getting callbacks after reset")
        }
        mServer.setupHappyIdentify(ran.nextLong(), 100)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(crashListener)
        MParticle.getInstance()!!.Identity().identify(IdentityApiRequest.withEmptyUser().build())
        called[0] = true
        mServer.waitForVerify(Matcher(mServer.Endpoints().identifyUrl))
        resetRunnable.run()
        assertSDKGone()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPushEnabledApi() {
        val senderId = "senderId"
        startMParticle()
        MParticle.getInstance()!!.Messaging().enablePushNotifications(senderId)
        var fetchedSenderId: String? =
            MParticle.getInstance()!!.mInternal.getConfigManager().getPushSenderId()
        Assert.assertTrue(
            MParticle.getInstance()!!.mInternal.getConfigManager().isPushEnabled() ?: false
        )
        Assert.assertEquals(senderId, fetchedSenderId)
        val otherSenderId = "senderIdLogPushRegistration"
        MParticle.getInstance()!!.logPushRegistration("instanceId", otherSenderId)
        fetchedSenderId = MParticle.getInstance()!!.mInternal.getConfigManager().getPushSenderId()
        Assert.assertEquals(otherSenderId, fetchedSenderId)
        MParticle.getInstance()!!.Messaging().disablePushNotifications()
        fetchedSenderId = MParticle.getInstance()!!.mInternal.getConfigManager().getPushSenderId()
        Assert.assertFalse(
            MParticle.getInstance()!!.mInternal.getConfigManager().isPushEnabled() ?: false
        )
        Assert.assertNull(fetchedSenderId)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogPushRegistrationModifyMessages() {
        val pushRegistrationTest = PushRegistrationTest().setServer(mServer)
        pushRegistrationTest.setContext(mContext)
        for (setPush in pushRegistrationTest.setPushes) {
            val oldRegistration = PushRegistration(
                mRandomUtils.getAlphaNumericString(10),
                mRandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(oldRegistration)
            val newPushRegistration = PushRegistration(
                mRandomUtils.getAlphaNumericString(10),
                mRandomUtils.getAlphaNumericString(15)
            )
            val latch: CountDownLatch = MPLatch(1)
            val received = AndroidUtils.Mutable(false)
            mServer.waitForVerify(
                Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(
                    JSONMatch { jsonObject ->
                        if (jsonObject.has("identity_changes")) {
                            try {
                                val identityChanges = jsonObject.getJSONArray("identity_changes")
                                Assert.assertEquals(1, identityChanges.length().toLong())
                                val identityChange = identityChanges.getJSONObject(0)
                                val failureMessage =
                                    "When " + oldRegistration + " set with: " + setPush.name

                                // This is a wierd case. We might be setting the old pushRegistration with "logPushRegistration()",
                                // which will kick of its own modify request. We want to ignore this if this is the case.
                                if (identityChange.getString("new_value") == oldRegistration.instanceId) {
                                    return@JSONMatch false
                                }
                                Assert.assertEquals(
                                    failureMessage,
                                    oldRegistration.instanceId,
                                    identityChange.getString("old_value")
                                )
                                Assert.assertEquals(
                                    failureMessage,
                                    newPushRegistration.instanceId,
                                    identityChange.getString("new_value")
                                )
                                Assert.assertEquals(
                                    failureMessage,
                                    "push_token",
                                    identityChange.getString("identity_type")
                                )
                            } catch (jse: JSONException) {
                                jse.toString()
                            }
                            return@JSONMatch true
                        }
                        false
                    }
                )
            ) {
                received.value = true
                latch.countDown()
            }
            MParticle.getInstance()!!
                .logPushRegistration(newPushRegistration.instanceId, newPushRegistration.senderId)
            latch.await()
        }
    }

    @Test
    fun testWrapperSdkVersionWithoutBeingSet() {
        val instance = MParticle.getInstance()!!
        Assert.assertNotNull(instance.wrapperSdkVersion)
        Assert.assertEquals(WrapperSdk.WrapperNone, instance.wrapperSdkVersion.sdk)
        Assert.assertNull(instance.wrapperSdkVersion.version)
    }

    @Test
    fun testSetLocation() {
        val location = Location("")
        MParticle.getInstance()!!.setLocation(location)
        Assert.assertEquals(location, MParticle.getInstance()!!.mMessageManager.location)
        MParticle.getInstance()!!.setLocation(null)
        Assert.assertNull(MParticle.getInstance()!!.mMessageManager.location)
    }

    @Test
    fun testEnableLocationTracking() {
        val location = Location("")
        val mp = MParticle.getInstance()
        mp!!.enableLocationTracking(LocationManager.NETWORK_PROVIDER, 30 * 1000, 1000)
        mp!!.setLocation(location)
        Assert.assertEquals(location, mp!!.mMessageManager.location)
        Assert.assertNotNull(mp.mMessageManager.location)
    }

    @Test
    fun testEnableLocationTrackingAndDisableLocationTracking() {
        val location = Location("")
        val mp = MParticle.getInstance()
        mp!!.enableLocationTracking(LocationManager.NETWORK_PROVIDER, 30 * 1000, 1000)
        mp!!.setLocation(location)
        Assert.assertEquals(location, mp!!.mMessageManager.location)
        mp.disableLocationTracking()
        mp.setLocation(null)
        Assert.assertNull(mp.mMessageManager.location)
    }

    @Test
    fun testSwitchWorkspacesCredentials() {
        MParticle.setInstance(null)

        val optionsBuilder = MParticleOptions.builder(mContext)
        optionsBuilder.apiKey = "apiKey1"
        optionsBuilder.apiSecret = "apiSecret1"
        val options = optionsBuilder.build()

        MParticle.start(options)
        val instance1 = MParticle.getInstance()

        Assert.assertEquals("apiKey1", instance1!!.mConfigManager.apiKey)
        Assert.assertEquals("apiSecret1", instance1.mConfigManager.apiSecret)

        val switchOptionsBuilder = MParticleOptions.builder(mContext)
        switchOptionsBuilder.apiKey = "apiKey2"
        switchOptionsBuilder.apiSecret = "apiSecret2"
        val switchOptions = switchOptionsBuilder.build()

        MParticle.switchWorkspace(switchOptions)

        Thread.sleep(5000)
        val instance2 = MParticle.getInstance()

        Assert.assertEquals("apiKey2", instance2!!.mConfigManager.apiKey)
        Assert.assertEquals("apiSecret2", instance2.mConfigManager.apiSecret)
    }

    @Test
    fun testSwitchWorkspacesUploadSettings() {
        MParticle.setInstance(null)

        val optionsBuilder = MParticleOptions.builder(mContext)
        optionsBuilder.apiKey = "apiKey1"
        optionsBuilder.apiSecret = "apiSecret1"
        val options = optionsBuilder.build()

        MParticle.start(options)
        val instance1 = MParticle.getInstance()

        val eventsUrl1 = mServer.Endpoints().eventsUrl
        Assert.assertEquals(true, eventsUrl1.path.contains(options.apiKey))

        Assert.assertEquals("apiKey1", instance1!!.mConfigManager.apiKey)
        Assert.assertEquals("apiSecret1", instance1.mConfigManager.apiSecret)
        val event1 = MPEvent.Builder("event 1").build()
        instance1.logEvent(event1)

        val readyUploads1 = UploadService.getReadyUploads(instance1.mDatabaseManager.database)
        Assert.assertEquals(0, readyUploads1.count())

        val switchOptionsBuilder = MParticleOptions.builder(mContext)
        switchOptionsBuilder.apiKey = "apiKey2"
        switchOptionsBuilder.apiSecret = "apiSecret2"
        val switchOptions = switchOptionsBuilder.build()

        MParticle.switchWorkspace(switchOptions)

        Thread.sleep(5000)

        val instance2 = MParticle.getInstance()

        val eventsUrl2 = mServer.Endpoints().eventsUrl
        Assert.assertTrue(eventsUrl2.path.contains(switchOptions.apiKey))

        val event2 = MPEvent.Builder("event 2").build()
        instance2!!.logEvent(event2)

        // TODO: Improvements to mock server to add more comprehensive testing on this - https://go.mparticle.com/work/SQDSDKS-6840
    }

    @Throws(JSONException::class, InterruptedException::class)
    private fun testReset(resetRunnable: Runnable) {
        for (i in 0..9) {
            MParticle.getInstance()!!.logEvent(TestingUtils.getInstance().randomMPEventRich)
        }
        for (i in 0..9) {
            MParticle.getInstance()!!.mInternal.getConfigManager()
                .setMpid(ran.nextLong(), ran.nextBoolean())
        }
        val databaseJson = getDatabaseContents(listOf("messages"))
        Assert.assertTrue(databaseJson.getJSONArray("messages").length() > 0)
        Assert.assertEquals(6, allTables.size.toLong())
        Assert.assertTrue(
            10 < (MParticle.getInstance()!!.mInternal.getConfigManager().getMpids()?.size ?: 0)
        )

        // Set strict mode, so if we get any warning or error messages during the reset/restart phase,
        // it will throw an exception.
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING)
        resetRunnable.run()
        assertSDKGone()

        // Restart the SDK, to the point where the initial Identity call returns, make sure there are no errors on startup.
        TestingUtils.setStrictMode(
            MParticle.LogLevel.WARNING,
            "Failed to get MParticle instance, getInstance() called prior to start()."
        )
        beforeBase()
    }

    private fun assertSDKGone() {
        // Check post-reset state:
        // should be 2 entries in default SharedPreferences (the install boolean and the original install time)
        // and 0 other SharedPreferences tables.
        // Make sure the 2 entries in default SharedPreferences are the correct values.
        // 0 tables should exist.
        // Then we call DatabaseHelper.getInstance(Context).openDatabase, which should create the database,
        // and make sure it is created without an error message, and that all the tables are empty.
        val sharedPrefsDirectory = mContext.filesDir.path.replace("files", "shared_prefs/")
        val files = File(sharedPrefsDirectory).listFiles()
        files?.iterator()?.forEach { file ->
            val sharedPreferenceName =
                file.path.replace(sharedPrefsDirectory, "").replace(".xml", "")
            if (sharedPreferenceName != "WebViewChromiumPrefs" && sharedPreferenceName != "com.mparticle.test_preferences") {
                Assert.fail(
                    """
    SharedPreference file failed to clear:
    ${getSharedPrefsContents(sharedPreferenceName)}
                    """.trimIndent()
                )
            }
        }
        Assert.assertEquals(0, mContext.databaseList().size.toLong())
        try {
            val databaseJson = databaseContents
            val keys = databaseJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                Assert.assertEquals(key, 0, databaseJson.getJSONArray(key).length().toLong())
            }
        } catch (e: JSONException) {
            Assert.fail(e.message)
        }
    }

    private fun getSharedPrefsContents(name: String): String {
        return try {
            val prefs = mContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            """
     $name:
     ${JSONObject(prefs.all).toString(4)}
            """.trimIndent()
        } catch (e: JSONException) {
            "error printing SharedPrefs :/"
        }
    }
}
