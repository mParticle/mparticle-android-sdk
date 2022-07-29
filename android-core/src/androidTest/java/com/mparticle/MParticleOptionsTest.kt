package com.mparticle

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.test.rule.GrantPermissionRule
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.orThrow
import com.mparticle.testing.productionContext
import com.mparticle.utils.getInstallType
import com.mparticle.utils.startMParticle
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class MParticleOptionsTest : BaseTest() {
    @Before
    fun before() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
    }

    @Test
    @Throws(Exception::class)
    fun testCrashOnNoCredentials() {
        var thrown = false
        clearStoredPreferences()
        try {
            MParticleOptions.builder(context).build()
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(context).apply {
                apiKey = "key"
                build()
            }
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        clearStoredPreferences()
        thrown = false
        try {
            MParticleOptions.builder(context).apply {
                apiSecret = "secret"
                build()
            }
        } catch (ex: IllegalArgumentException) {
            thrown = true
        }
        Assert.assertTrue(thrown)
        try {
            MParticleOptions.builder(context).buildForInternalRestart()
        } catch (ex: IllegalArgumentException) {
            Assert.fail("MParticleOptions should build without credentials if the internal build function is used")
        }
        try {
            MParticleOptions.builder(productionContext).build()
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
        private get() = context.getSharedPreferences("mp_preferences", Context.MODE_PRIVATE)

    @Test
    @Throws(Exception::class)
    fun testSetCredentials() {
        val key = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()
        startMParticle(
            MParticleOptions.builder(productionContext)
                .credentials(key, secret)
        )
        assertEquals(MParticle.getInstance()?.Internal()?.configManager?.apiKey, key)
        assertEquals(
            MParticle.getInstance()?.Internal()?.configManager?.apiSecret,
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
        startMParticle(MParticleOptions.builder(context))
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())

        // test androidIdDisabled == true
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdDisabled(true)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        // test androidIdEnabled == false
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(false)
        )
        Assert.assertFalse(MParticle.isAndroidIdEnabled())
        Assert.assertTrue(MParticle.isAndroidIdDisabled())
        MParticle.setInstance(null)

        // test androidIdDisabled == false
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdDisabled(false)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())

        // test androidIdEnabled == true
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(true)
        )
        Assert.assertTrue(MParticle.isAndroidIdEnabled())
        Assert.assertFalse(MParticle.isAndroidIdDisabled())
    }

    @Test
    @Throws(Exception::class)
    fun testDevicePerformanceMetricsDisabled() {
//        startMParticle()
//        Assert.assertFalse(MParticle.getInstance().orThrow().isDevicePerformanceMetricsDisabled)
//        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .devicePerformanceMetricsDisabled(false)
        )
        Assert.assertFalse(MParticle.getInstance().orThrow().isDevicePerformanceMetricsDisabled)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .devicePerformanceMetricsDisabled(true)
        )
        Assert.assertTrue(MParticle.getInstance().orThrow().isDevicePerformanceMetricsDisabled)
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLogLevel() {
        startMParticle()
        assertEquals(Logger.DEFAULT_MIN_LOG_LEVEL, Logger.getMinLogLevel())
        startMParticle(
            MParticleOptions.builder(productionContext)
                .logLevel(MParticle.LogLevel.VERBOSE)
        )
        assertEquals(MParticle.LogLevel.VERBOSE, Logger.getMinLogLevel())
        startMParticle(
            MParticleOptions.builder(productionContext).logLevel(MParticle.LogLevel.ERROR)
        )
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
    }

    @Test
    @Throws(Exception::class)
    fun testEnvironment() {
        startMParticle()
        assertEquals(
            MParticle.getInstance().orThrow().environment,
            MParticle.Environment.Development
        )
        startMParticle(
            MParticleOptions.builder(productionContext)
                .environment(MParticle.Environment.Production)
        )
        assertEquals(MParticle.getInstance()?.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
        val productionContext = productionContext
        val debuggable: Boolean = MPUtility.isAppDebuggable(productionContext)
        Assert.assertFalse(debuggable)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .environment(MParticle.Environment.AutoDetect)
        )
        assertEquals(MParticle.getInstance()?.environment, MParticle.Environment.Production)
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testEnableUncaughtExceptionLogging() {
        val options = MParticleOptions.builder(productionContext)
            .credentials("key", "secret")
            .build()
        MParticle.start(options)
        Assert.assertFalse(
            MParticle.getInstance().orThrow().Internal().configManager.logUnhandledExceptions
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .enableUncaughtExceptionLogging(true)
        )
        Assert.assertTrue(
            MParticle.getInstance().orThrow().Internal().configManager.logUnhandledExceptions
        )
        startMParticle(
            MParticleOptions.builder(productionContext)
                .enableUncaughtExceptionLogging(false)
        )
        Assert.assertFalse(
            MParticle.getInstance().orThrow().Internal().configManager.logUnhandledExceptions
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionTimeout() {
        startMParticle()
        assertEquals(
            60000,
            MParticle.getInstance().orThrow().Internal().configManager.sessionTimeout.toLong()
        )
        startMParticle(
            MParticleOptions.builder(productionContext)
                .sessionTimeout(-123)
        )
        assertEquals(
            60000,
            MParticle.getInstance().orThrow().Internal().configManager.sessionTimeout.toLong()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .sessionTimeout(123)
        )
        assertEquals(MParticle.Environment.Production, MParticle.getInstance()?.environment)
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.sessionTimeout.toLong(),
            123000
        )

        // make sure it resets if the session timeout is not specified
        startMParticle()
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.sessionTimeout.toLong(),
            60000
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testInstallType() {
        startMParticle()
        assertEquals(
            getInstallType(MParticle.getInstance().orThrow().mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .installType(MParticle.InstallType.KnownInstall)
        )
        assertEquals(
            getInstallType(MParticle.getInstance().orThrow().mMessageManager),
            MParticle.InstallType.KnownInstall
        )
        startMParticle(
            MParticleOptions.builder(productionContext)
                .installType(MParticle.InstallType.KnownUpgrade)
        )
        assertEquals(
            getInstallType(MParticle.getInstance().orThrow().mMessageManager),
            MParticle.InstallType.KnownUpgrade
        )
        startMParticle(
            MParticleOptions.builder(productionContext)
                .installType(MParticle.InstallType.AutoDetect)
        )
        assertEquals(
            getInstallType(MParticle.getInstance().orThrow().mMessageManager),
            MParticle.InstallType.AutoDetect
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadInterval() {
        // default upload interval for production
        startMParticle()
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.uploadInterval, 10000
        )
        MParticle.setInstance(null)

        // default upload interval for production
        startMParticle(MParticleOptions.builder(productionContext))
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.uploadInterval, 600000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .uploadInterval(123)
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.uploadInterval, 123000
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .uploadInterval(-123)
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.uploadInterval, 600000
        )
        MParticle.setInstance(null)
    }

    @Test
    @Throws(Exception::class)
    fun testAttributionListener() {
        startMParticle()
        Assert.assertNull(MParticle.getInstance().orThrow().attributionListener)
        startMParticle(
            MParticleOptions.builder(context)
                .attributionListener(object : AttributionListener {
                    override fun onResult(result: AttributionResult) {}
                    override fun onError(error: AttributionError) {}
                })
        )
        Assert.assertNotNull(MParticle.getInstance().orThrow().attributionListener)
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(context)
                .attributionListener(null)
        )
        Assert.assertNull(MParticle.getInstance().orThrow().attributionListener)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemTest() {
        val called: Mutable<Boolean> = Mutable<Boolean>(false)
        val latch: CountDownLatch = FailureLatch()
        startMParticle(
            MParticleOptions.builder(context)
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
        )
        Server
            .endpoint(EndpointType.Events)
            .assertNextRequest {
                "FireTV" == it.body.deviceInfo?.platform
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
                    upload()
                }
            }
            .blockUntilFinished()
    }

    @Test
    @Throws(InterruptedException::class)
    fun setOperatingSystemDefault() {
        startMParticle(MParticleOptions.builder(context))
        Server
            .endpoint(EndpointType.Events)
            .assertNextRequest {
                "Android" == it.body.deviceInfo?.platform
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("event name", MParticle.EventType.Location).build())
                    upload()
                }
            }
            .blockUntilFinished()
    }

    @Rule
    @JvmField
    var mRuntimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @Test
    @Throws(InterruptedException::class)
    fun testLocationTracking() {
        startMParticle(
            MParticleOptions.builder(context)
                .locationTrackingDisabled()
        )
        Assert.assertFalse(MParticle.getInstance().orThrow().isLocationTrackingEnabled)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle(
            MParticleOptions.builder(context)
                .locationTrackingEnabled("passive", 100, 20)
        )
        Assert.assertTrue(MParticle.getInstance().orThrow().isLocationTrackingEnabled)
        MParticle.setInstance(null)
        Assert.assertNull(MParticle.getInstance())
        startMParticle()
        Assert.assertFalse(MParticle.getInstance().orThrow().isLocationTrackingEnabled)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testTimeout() {
        startMParticle(
            MParticleOptions.builder(productionContext)
                .credentials("this", "that")
                .identityConnectionTimeout(-123)
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.identityConnectionTimeout
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.connectionTimeout.toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .identityConnectionTimeout(0)
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.identityConnectionTimeout
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.connectionTimeout.toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(
            MParticleOptions.builder(productionContext)
                .identityConnectionTimeout(123)
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.identityConnectionTimeout
                .toLong(),
            123000
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.connectionTimeout.toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        MParticle.setInstance(null)
        startMParticle(MParticleOptions.builder(productionContext))
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.identityConnectionTimeout
                .toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
        assertEquals(
            MParticle.getInstance().orThrow().Internal().configManager.connectionTimeout.toLong(),
            (ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000).toLong()
        )
    }

    @Test
    fun testNetworkOptions() {
        val options = MParticleOptions.builder(productionContext)
            .credentials("key", "secret")
            .build()
        Assert.assertTrue(
            com.mparticle.networking.AccessUtils.equals(
                options.networkOptions,
                com.mparticle.networking.NetworkOptionsManager.defaultNetworkOptions()
            )
        )
    }

    @Test
    fun testConfigStaleness() {
        // nothing set, should return null
        var options = MParticleOptions.builder(context)
            .credentials("key", "secret")
            .build()
        Assert.assertNull(options.configMaxAge)

        // 0 should return 0
        options = MParticleOptions.builder(context)
            .credentials("key", "secret")
            .configMaxAgeSeconds(0)
            .build()
        assertEquals(0, options.configMaxAge.toLong())

        // positive number should return positive number
        val testValue: Int = Math.abs(Random.Default.nextInt())
        options = MParticleOptions.builder(context)
            .credentials("key", "secret")
            .configMaxAgeSeconds(testValue)
            .build()
        assertEquals(testValue.toLong(), options.configMaxAge.toLong())

        // negative number should get thrown out and return null
        options = MParticleOptions.builder(context)
            .credentials("key", "secret")
            .configMaxAgeSeconds(-5)
            .build()
        Assert.assertNull(options.configMaxAge)
    }

    @Test
    fun testAndroidIdLogMessage() {
        val infoLogs = mutableListOf<String?>()
        Logger.setLogHandler(object : Logger.DefaultLogHandler() {
            override fun log(priority: MParticle.LogLevel, error: Throwable?, messages: String?) {
                super.log(priority, error, messages)
                if (priority == MParticle.LogLevel.INFO) {
                    infoLogs.add(messages)
                }
            }
        })
        MParticleOptions.builder(context)
            .credentials("this", "that")
            .androidIdDisabled(true)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on MParticleOptions settings"))
        infoLogs.clear()
        MParticleOptions.builder(context)
            .credentials("this", "that")
            .androidIdDisabled(false)
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will be collected based on MParticleOptions settings"))
        infoLogs.clear()

        // test default
        MParticleOptions.builder(context)
            .credentials("this", "that")
            .build()
        Assert.assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on default settings"))
        infoLogs.clear()
    }
}
