package com.mparticle.networking

import android.net.Uri
import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.database.UploadSettings
import com.mparticle.testutils.BaseCleanInstallEachTest
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
            defaultUrls[endpoint] =
                baseClientImpl.getUrl(endpoint, endpoint.name, null, UploadSettings(apiKey, "secret", NetworkOptions.builder().build(), "", ""))
        }
        MParticle.setInstance(null)
    }

    @Test
    fun testUrlPrefixWithPodRedirection() {
        val prefix = "eu1"
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
        val url =
            baseClientImpl.getPodUrl(NetworkOptionsManager.MP_URL_PREFIX, prefix, true)
        assertEquals("${NetworkOptionsManager.MP_URL_PREFIX}.$prefix.mparticle.com", url)
    }

    @Test
    fun testUrlPrefixWithoutPodRedirection() {
        val prefix = "eu1"
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
        val url =
            baseClientImpl.getPodUrl(NetworkOptionsManager.MP_URL_PREFIX, prefix, false)
        assertEquals("${NetworkOptionsManager.MP_URL_PREFIX}.mparticle.com", url)
    }

    @Test
    fun testAllPrefixes() {
        val mConfigManager = ConfigManager(
            mContext,
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null,
            null
        )

        // Following are the fake APIs for testing purposes.
        val map = mapOf<String, String>(
            Pair("us1-1vc4gbp24cdtx6e31s58icnymzy83f1uf", "us1"),
            Pair("us2-v2p8lr3w2g90vtpaumbq21zy05cl50qm3", "us2"),
            Pair("eu1-bkabfno0b8zpv5bwi2zm2mfa1kfml19al", "eu1"),
            Pair("au1-iermuj83dbeoshm0n32f10feotclq6i4a", "au1"),
            Pair("st1-k77ivhkbbqf4ce0s3y12zpcthyn1ixfyu", "st1"),
            Pair("us3-w1y2y8yj8q58d5bx9u2dvtxzl4cpa7cuf", "us3"),
            Pair("kajsdhasdiuyaiudiashhadjhdasjk", "us1")
        )
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
        map.forEach { (key, value) ->
            mConfigManager.setCredentials(key, value)
            val prefix = mConfigManager.getPodPrefix(key)
            assertEquals(value, prefix)
            assertEquals(
                "${NetworkOptionsManager.MP_URL_PREFIX}.$prefix.mparticle.com",
                baseClientImpl.getPodUrl(
                    NetworkOptionsManager.MP_URL_PREFIX,
                    prefix,
                    true
                )
            )
            assertEquals(
                "${NetworkOptionsManager.MP_IDENTITY_URL_PREFIX}.$prefix.mparticle.com",
                baseClientImpl.getPodUrl(
                    NetworkOptionsManager.MP_IDENTITY_URL_PREFIX,
                    prefix,
                    true
                )
            )

            assertEquals(
                "${NetworkOptionsManager.MP_URL_PREFIX}.mparticle.com",
                baseClientImpl.getPodUrl(
                    NetworkOptionsManager.MP_URL_PREFIX,
                    prefix,
                    false
                )
            )
            assertEquals(
                "${NetworkOptionsManager.MP_IDENTITY_URL_PREFIX}.mparticle.com",
                baseClientImpl.getPodUrl(
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
        val uploadSettings = UploadSettings(apiKey, "secret", options.networkOptions, "", "")
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, null, uploadSettings)
            assertEquals(defaultUrls[endpoint].toString(), generatedUrl.defaultUrl.toString())
        }
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            val generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name, null, uploadSettings)
            Assert.assertNotEquals(defaultUrls[endpoint].toString(), generatedUrl.toString())
            Assert.assertFalse(generatedUrl === generatedUrl.defaultUrl)
            assertEquals(
                defaultUrls[endpoint].toString(),
                generatedUrl.defaultUrl.toString()
            )
        }
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val subdirectory = "/v2/"
        val uri = Uri.Builder()
            .scheme(BuildConfig.SCHEME)
            .encodedAuthority("nativesdks.us1.mparticle.com")
            .path("$subdirectory us1-foo/events")
            .build()
        val result = baseClientImpl.generateDefaultURL(
            false,
            uri,
            "nativesdks.mparticle.com",
            "v2/us1-akshd324uajbhg123OIASI/events"
        )
        assertEquals("https://nativesdks.mparticle.com/v2/us1-akshd324uajbhg123OIASI/events", result.toString())
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_defaultDomain_IS_Empty() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val subdirectory = "/v2/"
        val uri = Uri.Builder()
            .scheme(BuildConfig.SCHEME)
            .encodedAuthority("nativesdks.us1.mparticle.com")
            .path("$subdirectory eu1-fooapi/events")
            .build()
        val result = baseClientImpl.generateDefaultURL(
            false,
            uri,
            "",
            "v2/us1-asjdjasdgjhasgdjhas/events"
        )
        assertEquals("https://nativesdks.us1.mparticle.com/v2/us1-asjdjasdgjhasgdjhas/events", result.toString())
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_defaultDomain_IS_NULL() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val subdirectory = "/v2/"
        val uri = Uri.Builder()
            .scheme(BuildConfig.SCHEME)
            .encodedAuthority("nativesdks.us1.mparticle.com")
            .path("$subdirectory us1-foo/events")
            .build()
        val result = baseClientImpl.generateDefaultURL(
            false,
            uri,
            null,
            "v2/us1-asjdjasdgjhasgdjhas/events"
        )
        assertEquals("https://nativesdks.us1.mparticle.com/v2/us1-asjdjasdgjhasgdjhas/events", result.toString())
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_URL_IS_NULL() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val result = baseClientImpl.generateDefaultURL(
            false,
            null,
            "nativesdks.mparticle.com",
            "v2/us1-bee5781b649a7a40a592c2000bc892d0/events"
        )
        assertEquals(null, result)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_PATH_IS_NULL() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val subdirectory = "/v2/"
        val uri = Uri.Builder()
            .scheme(BuildConfig.SCHEME)
            .encodedAuthority("nativesdks.us1.mparticle.com")
            .path("$subdirectory us1-foo/events")
            .build()
        val result = baseClientImpl.generateDefaultURL(
            false,
            uri,
            "nativesdks.mparticle.com",
            null
        )
        assertEquals("https://nativesdks.mparticle.com/v2/%20us1-foo/events", result.toString())
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_defaultDomain_AND_URL_AND_PATH_ARE_NULL() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val result = baseClientImpl.generateDefaultURL(false, null, null, null)
        assertEquals(null, result)
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testGenerateDefaultURL_When_defaultDomain_FLAG_IS_TRUE() {
        val options = MParticleOptions.builder(mContext)
            .credentials(apiKey, "secret")
            .build()
        MParticle.start(options)
        val baseClientImpl = AccessUtils.getApiClient() as MParticleBaseClientImpl
        val subdirectory = "/v2/"
        val uri = Uri.Builder()
            .scheme(BuildConfig.SCHEME)
            .encodedAuthority("nativesdks.us1.mparticle.com")
            .path("$subdirectory us1-foo/events")
            .build()
        val result = baseClientImpl.generateDefaultURL(
            true,
            uri,
            "nativesdks.mparticle.com",
            "v2/us1-akshd324uajbhg123OIASI/events"
        )
        assertEquals(null, result)
    }
}
