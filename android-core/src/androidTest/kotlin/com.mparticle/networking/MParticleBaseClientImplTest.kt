package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.NetworkUtilities
import com.mparticle.internal.AccessUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    fun testUrlPrefixWithPodRedirection() {
        val prefix = "eu1"
        val url =
            NetworkUtilities.getUrlWithPrefix(NetworkOptionsManager.MP_URL_PREFIX, prefix, true)
        assertEquals("${NetworkOptionsManager.MP_URL_PREFIX}.$prefix.mparticle.com", url)
    }

    @Test
    fun testUrlPrefixWithoutPodRedirection() {
        val prefix = "eu1"
        val url =
            NetworkUtilities.getUrlWithPrefix(NetworkOptionsManager.MP_URL_PREFIX, prefix, false)
        assertEquals("${NetworkOptionsManager.MP_URL_PREFIX}.mparticle.com", url)
    }

    @Test
    fun testAllPrefixes() {
        val map = mapOf<String, String>(
            Pair("us1-1vc4gbp24cdtx6e31s58icnymzy83f1uf", "us1"),
            Pair("us2-v2p8lr3w2g90vtpaumbq21zy05cl50qm3", "us2"),
            Pair("eu1-bkabfno0b8zpv5bwi2zm2mfa1kfml19al", "eu1"),
            Pair("au1-iermuj83dbeoshm0n32f10feotclq6i4a", "au1"),
            Pair("st1-k77ivhkbbqf4ce0s3y12zpcthyn1ixfyu", "st1"),
            Pair("us3-w1y2y8yj8q58d5bx9u2dvtxzl4cpa7cuf", "us3")
        )
        map.forEach { key, value ->
            val prefix = NetworkUtilities.getPodPrefix(key) ?: ""
            assertEquals(value, prefix)
            assertEquals(
                "${NetworkOptionsManager.MP_URL_PREFIX}.$prefix.mparticle.com",
                NetworkUtilities.getUrlWithPrefix(NetworkOptionsManager.MP_URL_PREFIX, prefix, true)
            )
            assertEquals(
                "${NetworkOptionsManager.MP_IDENTITY_URL_PREFIX}.$prefix.mparticle.com",
                NetworkUtilities.getUrlWithPrefix(
                    NetworkOptionsManager.MP_IDENTITY_URL_PREFIX,
                    prefix,
                    true
                )
            )

            assertEquals(
                "${NetworkOptionsManager.MP_URL_PREFIX}.mparticle.com",
                NetworkUtilities.getUrlWithPrefix(
                    NetworkOptionsManager.MP_URL_PREFIX,
                    prefix,
                    false
                )
            )
            assertEquals(
                "${NetworkOptionsManager.MP_IDENTITY_URL_PREFIX}.mparticle.com",
                NetworkUtilities.getUrlWithPrefix(
                    NetworkOptionsManager.MP_IDENTITY_URL_PREFIX,
                    prefix,
                    false
                )
            )
        }
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
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, true)
            assertEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
            TestCase.assertTrue(generatedUrl === generatedUrl.defaultUrl)
        }
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, false)
            Assert.assertNotEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
            Assert.assertFalse(generatedUrl === generatedUrl.defaultUrl)
            assertEquals(
                defaultUrls[endpoint].toString(),
                generatedUrl.defaultUrl.toString()
            )
        }
    }
}
