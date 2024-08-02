package com.mparticle

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mparticle.MParticleOptions.BatchCreationListener
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.Logger.DefaultLogHandler
import com.mparticle.internal.MPUtility
import com.mparticle.networking.Matcher
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseAbstractTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch

class MParticleOptionsTest : BaseAbstractTest() {
    lateinit var mContext: Context
    private lateinit var mProductionContext: Context

    @Before
    fun before() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        mContext = InstrumentationRegistry.getInstrumentation().context
        mProductionContext = AndroidUtils.getProductionContext(mContext)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
    }

    @Test
    @Throws(Exception::class)
    fun testCrashOnNoCredentials() {
        var thrown = false
        clearStoredPreferences()
        try {
            MParticleOptions.builder(mContext).build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(mContext)
                .credentials("", "")
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(mContext)
                .credentials("key", "")
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(mContext)
                .credentials("", "key")
                .build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        setStoredPreference("key", "secret")
        try {
            MParticleOptions.builder(mContext).buildForInternalRestart()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials if the internal build function is used")
        }
        try {
            MParticleOptions.builder(mProductionContext).build()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials in a Production environment")
        }
        try {
            MParticleOptions.builder(mProductionContext)
                .credentials("", "")
                .build()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials in a Production environment")
        }
    }

    private fun clearStoredPreferences() {
        credentialsPreferences
            .edit()
            .remove(Constants.PrefKeys.API_KEY)
            .remove(Constants.PrefKeys.API_SECRET)
            .commit()
    }

    private fun setStoredPreference(apiKey: String, apiSecret: String) {
        credentialsPreferences
            .edit()
            .putString(Constants.PrefKeys.API_KEY, apiKey)
            .putString(Constants.PrefKeys.API_SECRET, apiSecret)
            .commit()
    }

    private val credentialsPreferences: SharedPreferences
        get() = mContext.getSharedPreferences("mp_preferences", Context.MODE_PRIVATE)

    @Test
    @Throws(Exception::class)
    fun testSetCredentials() {
        val key = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .credentials(key, secret)
        )
        Assert.assertEquals(MParticle.getInstance()?.mInternal?.configManager?.apiKey, key)
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.apiSecret,
            secret
        )
    }

    @Test
    @Throws(Exception::class)
    fun testAndroidIdDisabled() {
        // test defaults
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)
        startMParticle(MParticleOptions.builder(mContext))
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())

        // test androidIdDisabled == true
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdDisabled(true)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        // test androidIdEnabled == false
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdEnabled(false)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        // test androidIdDisabled == false
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdDisabled(false)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())

        // test androidIdEnabled == true
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdEnabled(true)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())
    }

    @Test
    @Throws(Exception::class)
    fun testDevicePerformanceMetricsDisabled() {
        startMParticle()
        MParticle.getInstance()?.let { Assert.assertFalse(it.isDevicePerformanceMetricsDisabled) }
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .devicePerformanceMetricsDisabled(false)
        )
        MParticle.getInstance()?.let { Assert.assertFalse(it.isDevicePerformanceMetricsDisabled) }
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .devicePerformanceMetricsDisabled(true)
        )
        MParticle.getInstance()?.let { Assert.assertTrue(it.isDevicePerformanceMetricsDisabled) }
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLogLevel() {
        startMParticle()
        Assert.assertEquals(Logger.getMinLogLevel(), Logger.DEFAULT_MIN_LOG_LEVEL)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .logLevel(MParticle.LogLevel.VERBOSE)
        )
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.VERBOSE)
        startMParticle(
            MParticleOptions.builder(mProductionContext).logLevel(MParticle.LogLevel.ERROR)
        )
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
    }

    @Test
    @Throws(Exception::class)
    fun testEnvironment() {
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance()?.environment,
            MParticle.Environment.Development
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .environment(MParticle.Environment.Production)
        )
        Assert.assertEquals(MParticle.getInstance()?.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
        val productionContext = mProductionContext
        val debuggable = MPUtility.isAppDebuggable(productionContext)
        Assert.assertFalse(debuggable)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .environment(MParticle.Environment.AutoDetect)
        )
        Assert.assertEquals(MParticle.getInstance()?.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testEnableUncaughtExceptionLogging() {
        val options = MParticleOptions.builder(mProductionContext)
            .credentials("key", "secret")
            .build()
        MParticle.start(options)
        MParticle.getInstance()?.mInternal?.configManager?.let {
            Assert.assertFalse(
                it.logUnhandledExceptions
            )
        }
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(true)
        )
        MParticle.getInstance()?.mInternal?.configManager?.logUnhandledExceptions?.let {
            Assert.assertTrue(
                it
            )
        }
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(false)
        )
        MParticle.getInstance()?.mInternal?.configManager?.let {
            Assert.assertFalse(
                it.logUnhandledExceptions
            )
        }
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionTimeout() {
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.sessionTimeout,
            60000
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .sessionTimeout(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.sessionTimeout,
            60000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .sessionTimeout(123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.sessionTimeout,
            123000
        )

        // make sure it resets if the session timeout is not specified
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.sessionTimeout,
            60000
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallType() {
        startMParticle()
        Assert.assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()?.mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.KnownInstall)
        )
        Assert.assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()?.mMessageManager),
            MParticle.InstallType.KnownInstall
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.KnownUpgrade)
        )
        Assert.assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()?.mMessageManager),
            MParticle.InstallType.KnownUpgrade
        )
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.AutoDetect)
        )
        Assert.assertEquals(
            AccessUtils.getInstallType(MParticle.getInstance()?.mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadInterval() {
        // default upload interval for production
        startMParticle()
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.uploadInterval,
            10000L
        )
        MParticle.setInstance(null)

        // default upload interval for production
        startMParticle(MParticleOptions.builder(mProductionContext))
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.uploadInterval,
            600000L
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .uploadInterval(123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.uploadInterval,
            123000L
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .uploadInterval(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.uploadInterval,
            600000L
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testAttributionListener() {
        startMParticle()
        Assert.assertNull(MParticle.getInstance()?.attributionListener)
        startMParticle(
            MParticleOptions.builder(mContext)
                .attributionListener(object : AttributionListener {
                    override fun onResult(result: AttributionResult) {}
                    override fun onError(error: AttributionError) {}
                })
        )
        Assert.assertNotNull(MParticle.getInstance()?.attributionListener)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mContext)
                .attributionListener(null)
        )
        Assert.assertNull(MParticle.getInstance()?.attributionListener)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemTest() {
        val called = AndroidUtils.Mutable(false)
        val latch: CountDownLatch = MPLatch(1)
        startMParticle(
            MParticleOptions.builder(mContext)
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
        )
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl)) { request ->
            Assert.assertEquals("FireTV", request.bodyJson.optJSONObject("di")?.optString("dp"))
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()
            ?.logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
        MParticle.getInstance()?.upload()
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemDefault() {
        val called = AndroidUtils.Mutable(false)
        val latch1: CountDownLatch = MPLatch(1)
        startMParticle(MParticleOptions.builder(mContext))
        mServer.waitForVerify(Matcher(mServer.Endpoints().eventsUrl)) { request ->
            Assert.assertEquals("Android", request.bodyJson.optJSONObject("di")?.optString("dp"))
            called.value = true
            latch1.countDown()
        }
        MParticle.getInstance()
            ?.logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
        MParticle.getInstance()?.upload()
        latch1.await()
        Assert.assertTrue(called.value)
    }

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    @Throws(InterruptedException::class)
    fun testLocationTracking() {
        startMParticle(
            MParticleOptions.builder(mContext)
                .locationTrackingDisabled()
        )
        MParticle.getInstance()?.let { Assert.assertFalse(it.isLocationTrackingEnabled) }
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(
            MParticleOptions.builder(mContext)
                .locationTrackingEnabled("passive", 100, 20)
        )
        MParticle.getInstance()?.let { Assert.assertTrue(it.isLocationTrackingEnabled) }
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        MParticle.getInstance()?.let { Assert.assertFalse(it.isLocationTrackingEnabled) }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTimeout() {
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(-123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.identityConnectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.connectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(0)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.identityConnectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.connectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(123)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.identityConnectionTimeout,
            123000
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.connectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        MParticle.setInstance(null)
        startMParticle(MParticleOptions.builder(mProductionContext))
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.identityConnectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
        Assert.assertEquals(
            MParticle.getInstance()?.mInternal?.configManager?.connectionTimeout,
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000)
        )
    }

    @Test
    fun testNetworkOptions() {
        val options = MParticleOptions.builder(mProductionContext)
            .credentials("key", "secret")
            .build()
        Assert.assertTrue(
            com.mparticle.networking.AccessUtils.equals(
                options.networkOptions,
                com.mparticle.networking.AccessUtils.defaultNetworkOptions
            )
        )
    }

    @Test
    fun testConfigStaleness() {
        // nothing set, should return null
        var options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .build()
        Assert.assertNull(options.configMaxAge)

        // 0 should return 0
        options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configMaxAgeSeconds(0)
            .build()
        Assert.assertEquals(0, options.configMaxAge)

        // positive number should return positive number
        val testValue = Math.abs(ran.nextInt())
        options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configMaxAgeSeconds(testValue)
            .build()
        Assert.assertEquals(testValue, options.configMaxAge)

        // negative number should get thrown out and return null
        options = MParticleOptions.builder(mContext)
            .credentials("key", "secret")
            .configMaxAgeSeconds(-5)
            .build()
        Assert.assertNull(options.configMaxAge)
    }

    @Test
    fun testAndroidIdLogMessage() {
        val infoLogs = ArrayList<String?>()
        Logger.setLogHandler(object : DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable?, messages: String) {
                super.log(priority, error, messages)
                if (priority == MParticle.LogLevel.INFO) {
                    infoLogs.add(messages)
                }
            }
        })
        MParticleOptions.builder(mContext)
            .credentials("this", "that")
            .androidIdDisabled(true)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on MParticleOptions settings"))
        infoLogs.clear()
        MParticleOptions.builder(mContext)
            .credentials("this", "that")
            .androidIdDisabled(false)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will be collected based on MParticleOptions settings"))
        infoLogs.clear()

        // test default
        MParticleOptions.builder(mContext)
            .credentials("this", "that")
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on default settings"))
        infoLogs.clear()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBatchCreationCallback() {
        val listener = BatchCreationListener { batch -> batch }
        var options = MParticleOptions.builder(mProductionContext)
            .batchCreationListener(listener)
            .credentials("this", "that")
            .build()
        Assert.assertEquals(listener, options.batchCreationListener)
        options = MParticleOptions.builder(mProductionContext)
            .credentials("this", "that")
            .batchCreationListener(listener)
            .batchCreationListener(null)
            .build()
        Assert.assertNull(options.batchCreationListener)
        options = MParticleOptions.builder(mProductionContext)
            .credentials("this", "that")
            .build()
        Assert.assertNull(options.batchCreationListener)
    }
}
