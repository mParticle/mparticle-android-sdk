package com.mparticle.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ConfigStalenessCheckTest : BaseCleanInstallEachTest() {
    val configManager: ConfigManager
        get() =
            MParticle
                .getInstance()
                ?.Internal()
                ?.configManager
                .assertNotNull()

    @Test
    fun testNeverStale() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        val options =
            MParticleOptions
                .builder(mContext)
                .addCredentials()
                .build()

        startAndFetch(options, config1)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        startAndFetch(options, config2) {
            // after restart, we should still have config1
            assertEquals(config1.toString(), configManager.config)
            assertEquals(etag, configManager.etag)
            assertEquals(lastModified, configManager.ifModified)
            assertEquals(timestamp, configManager.configTimestamp)
        }
    }

    @Test
    fun testExceedStalenessThreshold() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        val options =
            MParticleOptions
                .builder(mContext)
                .addCredentials()
                .configMaxAgeSeconds(1)
                .build()

        startAndFetch(options, config1)

        MParticle.setInstance(null)
        // Staleness is measured with System.currentTimeMillis(), so this has to be real time.
        Thread.sleep(1010)

        startAndFetch(options, config2) {
            // after configMaxAge time has elapsed, config should be cleared after restart
            assertConfigCleared()
        }
    }

    @Test
    fun testDoesntExceedStalenessThreshold() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        val options =
            MParticleOptions
                .builder(mContext)
                .addCredentials()
                .configMaxAgeSeconds(100)
                .build()

        startAndFetch(options, config1)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        startAndFetch(options, config2) {
            // after restart, we should still have config1
            assertEquals(config1.toString(), configManager.config)
            assertEquals(etag, configManager.etag)
            assertEquals(lastModified, configManager.ifModified)
            assertEquals(timestamp, configManager.configTimestamp)
        }
    }

    @Test
    fun testAlwaysStale() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        val options =
            MParticleOptions
                .builder(mContext)
                .addCredentials()
                .configMaxAgeSeconds(0)
                .build()

        startAndFetch(options, config1)

        MParticle.setInstance(null)
        startAndFetch(options, config2) {
            // directly after restart, config should be cleared
            assertConfigCleared()
        }
    }

    /**
     * Starts the SDK with its config fetch held, runs [whileHeld] against the state start left, then lets the fetch
     * answer with [config] and waits until the SDK has applied it.
     */
    private fun startAndFetch(
        options: MParticleOptions,
        config: JSONObject,
        whileHeld: () -> Unit = {},
    ) {
        val release = CountDownLatch(1)
        mServer.setupConfigResponse(config.toString(), release)
        MParticle.start(options)
        val applied = MPLatch(1)
        configManager.onNewConfig { applied.countDown() }
        try {
            whileHeld()
        } finally {
            release.countDown()
        }
        applied.await()
        assertEquals(config.toString(), configManager.config)
    }

    private fun assertConfigCleared() {
        assertTrue(configManager.config?.isEmpty()!!)
        assertNull(configManager.etag)
        assertNull(configManager.ifModified)
        assertNull(configManager.configTimestamp)
    }

    private fun randomJson(size: Int) = (1..size)
        .map { mRandomUtils.getAlphaNumericString(4) to mRandomUtils.getAlphaNumericString(6) }
        .fold(JSONObject()) { init, attribute ->
            init.apply { put(attribute.first, attribute.second) }
        }

    fun <T> T?.assertNotNull(): T {
        assertNotNull(this)
        return this
    }

    fun MParticleOptions.Builder.addCredentials() = this.credentials("apiKey", "apiSecret")

    fun ConfigManager.onNewConfig(callback: () -> Unit) {
        addConfigUpdatedListener { configType, isNew ->
            if (isNew && configType == ConfigManager.ConfigType.CORE) {
                callback()
            }
        }
    }
}
