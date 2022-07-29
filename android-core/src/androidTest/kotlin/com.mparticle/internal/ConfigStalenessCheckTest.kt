package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.toJsonString
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigStalenessCheckTest : BaseTest() {
    val configManager: ConfigManager
        get() = MParticle.getInstance()?.Internal()?.configManager.assertNotNull()

    @Test
    fun testNeverStale() {
        val config1 = randomConfig()
        val config2 = randomConfig()
        var latch = FailureLatch()
        val options = MParticleOptions.builder(context)
            .addCredentials()
            .build()

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config1) }

        MParticle.start(options)
        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toJsonString(), configManager.config)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config2) }

        MParticle.start(options)
        latch = FailureLatch()
        configManager.onNewConfig { latch.countDown() }

        // after restart, we should still have config1
        assertEquals(config1.toJsonString(), configManager.config)
        assertEquals(etag, configManager.etag)
        assertEquals(lastModified, configManager.ifModified)
        assertEquals(timestamp, configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config2.toJsonString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testExceedStalenessThreshold() {
        val config1 = randomConfig()
        val config2 = randomConfig()
        var latch = FailureLatch()
        val options = MParticleOptions.builder(context)
            .addCredentials()
            .configMaxAgeSeconds(1)
            .build()

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config1) }

        MParticle.start(options)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toJsonString(), MParticle.getInstance()?.Internal()?.configManager?.config)

        MParticle.setInstance(null)

        Thread.sleep(1010)

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config2) }

        MParticle.start(options)
        latch = FailureLatch()

        // after configMaxAge time has elapsed, config should be cleared after restart
        assertNull(configManager.config)
        assertNull(configManager.etag)
        assertNull(configManager.ifModified)
        assertNull(configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        // after config has been fetched, we should see config2
        assertEquals(config2.toJsonString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testDoesntExceedStalenessThreshold() {
        val config1 = randomConfig()
        val config2 = randomConfig()
        var latch = FailureLatch()
        val options = MParticleOptions.builder(context)
            .addCredentials()
            .configMaxAgeSeconds(100)
            .build()

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config1) }

        MParticle.start(options)
        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toJsonString(), configManager.config)
        val etag = configManager.etag
        val lastModified = configManager.ifModified
        val timestamp = configManager.configTimestamp

        MParticle.setInstance(null)
        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config2) }

        MParticle.start(options)
        latch = FailureLatch()
        configManager.onNewConfig { latch.countDown() }

        // after restart, we should still have config1
        assertEquals(config1.toJsonString(), configManager.config)
        assertEquals(etag, configManager.etag)
        assertEquals(lastModified, configManager.ifModified)
        assertEquals(timestamp, configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config2.toJsonString(), MParticle.getInstance()?.Internal()?.configManager?.config)
    }

    @Test
    fun testAlwaysStale() {
        val config1 = randomConfig()
        val config2 = randomConfig()
        var latch = FailureLatch()
        val options = MParticleOptions.builder(context)
            .addCredentials()
            .configMaxAgeSeconds(0)
            .build()

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config1) }

        MParticle.start(options)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        assertEquals(config1.toJsonString(), MParticle.getInstance()?.Internal()?.configManager?.config)

        MParticle.setInstance(null)

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(config2) }

        MParticle.start(options)
        latch = FailureLatch()

        // directly after restart, config should be cleared
        assertNull(configManager.config)
        assertNull(configManager.etag)
        assertNull(configManager.ifModified)
        assertNull(configManager.configTimestamp)

        configManager.onNewConfig { latch.countDown() }
        latch.await()

        // after config has been fetched, we should see config2
        assertEquals(config2.toJsonString(), configManager.config)
    }

    private fun randomConfig() = ConfigResponseMessage(id = Random.Default.nextInt().toString())

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
