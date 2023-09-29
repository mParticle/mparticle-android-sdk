package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.MalformedURLException

class MParticleBaseClientImplTest : BaseCleanInstallEachTest() {
    var defaultUrls = HashMap<MParticleBaseClientImpl.Endpoint, MPUrl>()
    var apiKey = mRandomUtils.getAlphaString(10)

    @Before
    @Throws(InterruptedException::class, MalformedURLException::class)
    fun before() {
        startMParticle(MParticleOptions.builder(mContext).credentials(apiKey, "secret"))
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            defaultUrls[endpoint] = baseClientImpl.getUrl(endpoint, endpoint.name, false)
        }
        MParticle.setInstance(null)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGetUrlForceDefaultOption() {
        val identityUrl = mRandomUtils.getAlphaString(20)
        val configUrl = mRandomUtils.getAlphaString(20)
        val audienceUrl = mRandomUtils.getAlphaString(20)
        val eventsUrl = mRandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(mContext)
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
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
//        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
//            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, true)
//            Assert.assertEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
//            TestCase.assertTrue(generatedUrl === generatedUrl.defaultUrl)
//        }
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
