package com.mparticle

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.mparticle.api.events.toMPEvent
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.IdentityStateListener
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.MParticleJSInterface
import com.mparticle.internal.MessageManager
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.DTO
import com.mparticle.messages.IdentityResponseMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.TestingUtils
import com.mparticle.testing.Utils.setStrictMode
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import com.mparticle.testing.orThrow
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class MParticleITest : BaseStartedTest() {
    private val configResponse =
"""{"dt":"ac", "id":"fddf1f96-560e-41f6-8f9b-ddd070be0765", "ct":1434392412994, "dbg":false, "cue":"appdefined", "pmk":["mp_message", "com.urbanairship.push.ALERT", "alert", "a", "message"], "cnp":"appdefined", "soc":0, "oo":false, "eks":[], "pio":30 }"""
            .let {
                DTO.from<ConfigResponseMessage>(it)
            }
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
        assertTrue(MParticle.getInstance()!!.mAppStateManager.session.isActive)
        MParticle.getInstance()!!.optOut = true
        Assert.assertFalse(MParticle.getInstance()!!.mAppStateManager.session.isActive)
    }

    @Test
    fun testSetInstallReferrer() {
        MParticle.getInstance()!!.installReferrer = "foo install referrer"
        junit.framework.Assert.assertEquals(
            "foo install referrer",
            MParticle.getInstance()!!
                .installReferrer
        )
    }

    @Test
    fun testInstallReferrerUpdate() {
        val randomName: String = RandomUtils.getAlphaNumericString(RandomUtils.randomInt(4, 64))
        MParticle.getInstance()!!.installReferrer = randomName
        assertTrue(MParticle.getInstance()!!.installReferrer == randomName)
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
            object : KitFrameworkWrapper(context, null, null, null, true, null) {
                override fun installReferrerUpdated() {
                    called[1] = true
                }
            }

        // Test when the InstallReferrer is set directly on the InstallReferrerHelper.
        var installReferrer: String = RandomUtils.getAlphaNumericString(10)
        InstallReferrerHelper.setInstallReferrer(context, installReferrer)
        assertTrue(called[0])
        assertTrue(called[1])
        Arrays.fill(called, false)

        // Test when it is set through the MParticle object in the public API.
        installReferrer = RandomUtils.getAlphaNumericString(10)
        MParticle.getInstance()!!.installReferrer = installReferrer
        assertTrue(called[0])
        assertTrue(called[1])
        Arrays.fill(called, false)
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testRegisterWebView() {
        MParticle.setInstance(null)
        val token: String = RandomUtils.getAlphaNumericString(15)
        val setupConfigResponse = Server.endpoint(EndpointType.Config).nextResponse {
            SuccessResponse { responseObject = ConfigResponseMessage(workspaceToken = token) }
        }
        startMParticle()
        val jsInterfaces: MutableMap<String, Any> = HashMap()
        val latch = FailureLatch()
        Handler(Looper.getMainLooper()).post(
            Runnable {
                val webView: WebView = object : WebView(context) {
                    override fun addJavascriptInterface(`object`: Any, name: String) {
                        jsInterfaces[name] = `object`
                    }
                }
                MParticle.getInstance()!!.registerWebView(webView)
                assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + token + "_v2"] is MParticleJSInterface)
                val clientToken: String = RandomUtils.getAlphaNumericString(15)
                MParticle.getInstance()!!.registerWebView(webView, clientToken)
                assertTrue(jsInterfaces[MParticleJSInterface.INTERFACE_BASE_NAME + "_" + clientToken + "_v2"] is MParticleJSInterface)
                latch.countDown()
            }
        )
        latch.await()
        Assert.assertEquals(2, jsInterfaces.size.toLong())
    }

    private fun ensureSessionActive() {
        MParticle.getInstance().orThrow().apply {
            if (!isSessionActive) {
                logEvent(TestingUtils.randomMPEventRich.toMPEvent())
                assertTrue(isSessionActive)
            }
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetSync() {
        testReset { MParticle.reset(context) }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetAsync() {
        testReset {
            val latch: CountDownLatch = FailureLatch()
            MParticle.reset(
                context,
                object : MParticle.ResetListener {
                    override fun onReset() {
                        latch.countDown()
                    }
                }
            )
            try {
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetIdentitySync() {
        testResetIdentityCall { MParticle.reset(context) }
    }

    @OrchestratorOnly
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testResetIdentityAsync() {
        testResetIdentityCall {
            val latch: CountDownLatch = FailureLatch()
            MParticle.reset(
                context,
                object : MParticle.ResetListener {
                    override fun onReset() {
                        latch.countDown()
                    }
                }
            )
            try {
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @OrchestratorOnly
    @Test
    @Throws(InterruptedException::class)
    fun testResetConfigCall() {
        Server
            .endpoint(EndpointType.Config)
            .nextResponse {
                SuccessResponse { responseObject = configResponse }
            }
        MParticle.getInstance()!!.refreshConfiguration()
        MParticle.reset(context)
        // This sleep is here just to make sure
        Thread.sleep(100)
        assertSDKGone()
    }

    /**
     * Test that Identity calls in progress will exit gracefully, and not trigger any callbacks.
     */
    @Throws(InterruptedException::class)
    fun testResetIdentityCall(resetRunnable: Runnable) {
        val called = BooleanArray(2)
        val crashListener: IdentityStateListener = object : IdentityStateListener {
            override fun onUserIdentified(user: MParticleUser, previousUser: MParticleUser?) {
                assertTrue(called[0])
                throw IllegalStateException("Should not be getting callbacks after reset")
            }
        }
        Server.endpoint(EndpointType.Identity_Identify).nextResponse {
            SuccessResponse {
                responseObject = IdentityResponseMessage(mpid = Random.Default.nextLong())
            }
        }
        MParticle.getInstance()!!.Identity().addIdentityStateListener(crashListener)

        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { true }
            .after {
                MParticle.getInstance()!!
                    .Identity().identify(IdentityApiRequest.withEmptyUser().build())
            }
            .blockUntilFinished()
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
            MParticle.getInstance().orThrow().Internal().configManager.pushSenderId
        assertTrue(MParticle.getInstance().orThrow().Internal().configManager.isPushEnabled)
        Assert.assertEquals(senderId, fetchedSenderId)
        val otherSenderId = "senderIdLogPushRegistration"
        MParticle.getInstance()!!.logPushRegistration("instanceId", otherSenderId)
        fetchedSenderId = MParticle.getInstance().orThrow().Internal().configManager.pushSenderId
        Assert.assertEquals(otherSenderId, fetchedSenderId)
        MParticle.getInstance()!!.Messaging().disablePushNotifications()
        fetchedSenderId = MParticle.getInstance().orThrow().Internal().configManager.pushSenderId
        Assert.assertFalse(MParticle.getInstance().orThrow().Internal().configManager.isPushEnabled)
        TestCase.assertNull(fetchedSenderId)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLogPushRegistrationModifyMessages() {
        val pushRegistrationTest = PushRegistrationTest()
        pushRegistrationTest.localContext = context
        for (setPush in pushRegistrationTest.setPushes) {
            val oldRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            setPush.setPushRegistration(oldRegistration)
            val newPushRegistration = PushRegistration(
                RandomUtils.getAlphaNumericString(10),
                RandomUtils.getAlphaNumericString(15)
            )
            Server
                .endpoint(EndpointType.Identity_Modify)
                .assertNextRequest {
                    it.body.identityChanges?.let {
                        it.size == 1 &&
                            it[0].newValue == newPushRegistration.instanceId &&
                            it[0].oldValue == oldRegistration.instanceId &&
                            it[0].identityType == "push_token"
                    } ?: false
                }
                .after {
                    MParticle.getInstance()!!
                        .logPushRegistration(newPushRegistration.instanceId, newPushRegistration.senderId)
                }
                .blockUntilFinished()
        }
    }

    @Test
    fun testSetLocation() {
        val location = Location("")
        MParticle.getInstance()!!.setLocation(location)
        Assert.assertEquals(location, MParticle.getInstance()!!.mMessageManager.location)
        MParticle.getInstance()!!.setLocation(null)
        TestCase.assertNull(MParticle.getInstance()!!.mMessageManager.location)
    }

    @Throws(JSONException::class, InterruptedException::class)
    private fun testReset(resetRunnable: Runnable) {
        for (i in 0..9) {
            MParticle.getInstance()!!.logEvent(TestingUtils.randomMPEventRich.toMPEvent())
        }
        for (i in 0..9) {
            MParticle.getInstance().orThrow().Internal().configManager
                .setMpid(Random.Default.nextLong(), Random.Default.nextBoolean())
        }
        val databaseJson = mockingPlatforms.getDatabaseContents(listOf("messages"))
        assertTrue((databaseJson["messages"] as List<String>).size > 0)
        assertEquals(6, mockingPlatforms.getDatabaseContents().entries.size)
        assertTrue(
            10 < MParticle.getInstance().orThrow().Internal().configManager.mpids.size
        )

        // Set strict mode, so if we get any warning or error messages during the reset/restart phase,
        // it will throw an exception.
        setStrictMode(MParticle.LogLevel.WARNING)
        resetRunnable.run()
        assertSDKGone()

        // Restart the SDK, to the point where the initial Identity call returns, make sure there are no errors on startup.
        setStrictMode(
            MParticle.LogLevel.WARNING,
            "Failed to get MParticle instance, getInstance() called prior to start()."
        )
        beforeAll()
    }

    private fun assertSDKGone() {
        // Check post-reset state:
        // should be 2 entries in default SharedPreferences (the install boolean and the original install time)
        // and 0 other SharedPreferences tables.
        // Make sure the 2 entries in default SharedPreferences are the correct values.
        // 0 tables should exist.
        // Then we call DatabaseHelper.getInstance(Context).openDatabase, which should create the database,
        // and make sure it is created without an error message, and that all the tables are empty.
        val sharedPrefsDirectory: String =
            context.filesDir.path.replace("files", "shared_prefs/")
        val files = File(sharedPrefsDirectory).listFiles()
        for (file in files) {
            val sharedPreferenceName =
                file.path.replace(sharedPrefsDirectory, "").replace(".xml", "")
            if (sharedPreferenceName != "WebViewChromiumPrefs" && sharedPreferenceName != "com.mparticle.test_preferences") {
                junit.framework.Assert.fail(
                    """
    SharedPreference file failed to clear:
    ${getSharedPrefsContents(sharedPreferenceName)}
                    """.trimIndent()
                )
            }
        }
        assertEquals(0, context.databaseList().size)
        try {
            val databaseJson = mockingPlatforms.getDatabaseContents()
            databaseJson.entries.forEach { assertEquals(it.key, 0, (it.value as Collection<*>).size) }
        } catch (e: JSONException) {
            junit.framework.Assert.fail(e.message)
        }
    }

    private fun getSharedPrefsContents(name: String): String {
        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            """
     $name:
     ${JSONObject(prefs.all).toString(4)}
            """.trimIndent()
        } catch (e: JSONException) {
            "error printing SharedPrefs :/"
        }
    }
}
