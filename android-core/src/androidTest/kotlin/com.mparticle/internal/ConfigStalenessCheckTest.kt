package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigStalenessCheckTest : BaseCleanInstallEachTest() {
    val configManager: ConfigManager
        get() = MParticle.getInstance()?.Internal()?.configManager.assertNotNull()

    @Test
    fun testNeverStale() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        var latch = MPLatch(1)
        val options = MParticleOptions.builder(mContext)
            .addCredentials()
            .build()

        mServer.setupConfigResponse(config1.toString())

        MParticle.start(options)
        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toString(), configManager.config)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        mServer.setupConfigResponse(config2.toString())

        MParticle.start(options)
        latch = MPLatch(1)
        configManager.onNewConfig { latch.countDown() }

        // after restart, we should still have config1
        assertEquals(config1.toString(), configManager.config)
        assertEquals(etag, configManager.etag)
        assertEquals(lastModified, configManager.ifModified)
        assertEquals(timestamp, configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config2.toString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testExceedStalenessThreshold() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        var latch = MPLatch(1)
        val options = MParticleOptions.builder(mContext)
            .addCredentials()
            .configMaxAgeSeconds(1)
            .build()

        mServer.setupConfigResponse(config1.toString())

        MParticle.start(options)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toString(), MParticle.getInstance()?.Internal()?.configManager?.config)

        MParticle.setInstance(null)

        Thread.sleep(1010)

        mServer.setupConfigResponse(config2.toString())

        MParticle.start(options)
        latch = MPLatch(1)

        // after configMaxAge time has elapsed, config should be cleared after restart
        assertTrue(configManager.config?.isEmpty()!!)
        assertNull(configManager.etag)
        assertNull(configManager.ifModified)
        assertNull(configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        // after config has been fetched, we should see config2
        assertEquals(config2.toString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testDoesntExceedStalenessThreshold() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        var latch = MPLatch(1)
        val options = MParticleOptions.builder(mContext)
            .addCredentials()
            .configMaxAgeSeconds(100)
            .build()

        mServer.setupConfigResponse(config1.toString())

        MParticle.start(options)
        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toString(), configManager.config)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        mServer.setupConfigResponse(config2.toString())

        MParticle.start(options)
        latch = MPLatch(1)
        configManager.onNewConfig { latch.countDown() }

        // after restart, we should still have config1
        assertEquals(config1.toString(), configManager.config)
        assertEquals(etag, configManager.etag)
        assertEquals(lastModified, configManager.ifModified)
        assertEquals(timestamp, configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config2.toString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testAlwaysStale() {
        val config1 = randomJson(4)
        val config2 = randomJson(4)
        var latch = MPLatch(1)
        val options = MParticleOptions.builder(mContext)
            .addCredentials()
            .configMaxAgeSeconds(0)
            .build()

        mServer.setupConfigResponse(config1.toString())

        MParticle.start(options)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toString(), MParticle.getInstance()?.Internal()?.configManager?.config)

        MParticle.setInstance(null)

        mServer.setupConfigResponse(config2.toString())

        MParticle.start(options)
        latch = MPLatch(1)

        // directly after restart, config should be cleared
        assertTrue(configManager.config?.isEmpty()!!)
        assertNull(configManager.etag)
        assertNull(configManager.ifModified)
        assertNull(configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        // after config has been fetched, we should see config2
        assertEquals(config2.toString(), configManager.config)
    }

    private fun randomJson(size: Int) =
        (1..size)
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
