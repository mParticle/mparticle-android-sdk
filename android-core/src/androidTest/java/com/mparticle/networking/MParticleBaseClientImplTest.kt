package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtils
import com.mparticle.testing.BaseTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import com.mparticle.utils.startMParticle
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.MalformedURLException

class MParticleBaseClientImplTest : BaseTest() {
    var defaultUrls: MutableMap<MParticleBaseClientImpl.Endpoint, MPUrl> = HashMap()
    var apiKey: String = "key"
    @Before
    @Throws(InterruptedException::class, MalformedURLException::class)
    fun before() {
        startMParticle(MParticleOptions.builder(context).credentials(apiKey, "secret"))
        val baseClientImpl = AccessUtils.apiClient as MParticleBaseClientImpl
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            defaultUrls[endpoint] = baseClientImpl.getUrl(endpoint, endpoint.name)
        }
        MParticle.setInstance(null)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGetUrlForceDefaultOption() {
        val identityUrl: String = RandomUtils.getAlphaString(20)
        val configUrl: String = RandomUtils.getAlphaString(20)
        val audienceUrl: String = RandomUtils.getAlphaString(20)
        val eventsUrl: String = RandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(
                        DomainMapping.audienceMapping(audienceUrl)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.configMapping(configUrl)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.identityMapping(identityUrl)
                            .build()
                    )
                    .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                    .build()
            )
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.apiClient as MParticleBaseClientImpl
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, true)
            Assert.assertEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
            TestCase.assertTrue(generatedUrl === generatedUrl.defaultUrl)
        }
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, false)
            Assert.assertNotEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
            Assert.assertFalse(generatedUrl === generatedUrl.defaultUrl)
            Assert.assertEquals(
                defaultUrls[endpoint].toString(),
                generatedUrl.defaultUrl.toString()
            )
        }
    }
}
