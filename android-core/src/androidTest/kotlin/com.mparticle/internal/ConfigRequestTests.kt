package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.KitConfigMessage
import com.mparticle.messages.toJsonString
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import com.mparticle.utils.startMParticle
import org.junit.Test
import kotlin.test.assertEquals

class ConfigRequestTests : BaseTest() {

    val simpleConfigWithKits = ConfigResponseMessage(
        id = "12345",
        kits = listOf(KitConfigMessage(1))
    )

    @Test
    fun testConfigRequestWithDataplanIdAndVersion() {
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                it.url.contains("plan_id=dpId") &&
                    it.url.contains("plan_version=101")
            }
            .after {
                MParticleOptions
                    .builder(context)
                    .credentials("key", "secret")
                    .dataplan("dpId", 101)
                    .let {
                        startMParticle(it)
                    }
            }
            .blockUntilFinished()

        // make sure it works on subsiquent requests
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                it.url.contains("plan_id=dpId") &&
                    it.url.contains("plan_version=101")
            }
            .after {
                AccessUtils.uploadHandler.mApiClient.fetchConfig(true)
            }
            .blockUntilFinished()
    }

    @Test
    fun testConfigRequestWithDataplanIdNoVersion() {
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                it.url.contains("plan_id") &&
                    !it.url.contains("plan_version")
            }
            .after {
                MParticleOptions
                    .builder(context)
                    .credentials("key", "secret")
                    .dataplan("dpId", null)
                    .let {
                        startMParticle(it)
                    }
            }
            .blockUntilFinished()

        // make sure it works on subsiquent requests
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                it.url.contains("plan_id") &&
                    !it.url.contains("plan_version")
            }
            .after {
                AccessUtils.uploadHandler.mApiClient.fetchConfig(true)
            }
            .blockUntilFinished()
    }

    @Test
    fun testConfigRequestWithDataplanVersionButNoId() {
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                !it.url.contains("plan_id") &&
                    !it.url.contains("plan_version")
            }
            .after {
                MParticleOptions
                    .builder(context)
                    .credentials("key", "secret")
                    .dataplan(null, 2)
                    .let {
                        startMParticle(it)
                    }
            }
            .blockUntilFinished()

        // make sure it works on subsiquent requests
        Server
            .endpoint(EndpointType.Config)
            .assertWillReceive {
                !it.url.contains("plan_id") &&
                    !it.url.contains("plan_version")
            }
            .after {
                AccessUtils.apiClient.fetchConfig(true)
            }
            .blockUntilFinished()
    }

    @Test
    fun testRemoteConfigApplied() {
        val latch = FailureLatch(count = 1)

        Server
            .endpoint(EndpointType.Config)
            .nextResponse { SuccessResponse(simpleConfigWithKits) }
            .assertWillReceive {
                true
            }
            .after {
                startMParticle(MParticleOptions.builder(context))
            }
            .blockUntilFinished()
        assertEquals(simpleConfigWithKits.kits?.joinToString(", ") { it.toJsonString() }.let { "[$it]" }, MParticle.getInstance()?.Internal()?.configManager?.latestKitConfiguration.toString())
    }
}
