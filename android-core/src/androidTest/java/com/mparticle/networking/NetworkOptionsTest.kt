package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.AccessUtils
import com.mparticle.testing.BaseTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import com.mparticle.utils.startMParticle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.MalformedURLException

class NetworkOptionsTest : BaseTest() {
    private var mpClient: MParticleBaseClientImpl? = null
    private var identityClient: MParticleBaseClientImpl? = null
    private val defaultUrls: MutableMap<MParticleBaseClientImpl.Endpoint, MPUrl> = HashMap()
    var apiKey: String = "key"
    @Before
    @Throws(InterruptedException::class, MalformedURLException::class)
    fun beforeClass() {
        startMParticle(MParticleOptions.builder(context).credentials(apiKey, "secret"))
        setClients()
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            defaultUrls[endpoint] = mpClient!!.getUrl(endpoint, endpoint.name)
        }
        MParticle.setInstance(null)
    }

    @After
    fun after() {
        MParticle.setInstance(null)
    }

    @Test
    @Throws(MalformedURLException::class, InterruptedException::class)
    fun testDefaultEndpoints() {
        MParticle.start(MParticleOptions.builder(context).credentials(apiKey, "s").build())
        setClients()
        Assert.assertEquals(
            NetworkOptionsManager.MP_URL,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_CONFIG_URL,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_URL,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_IDENTITY_URL,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).authority
        )
        var randIdentityPath: String = RandomUtils.getAlphaString(10)
        Assert.assertEquals(
            "/v1/$randIdentityPath",
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).path
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_URL,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_CONFIG_URL,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_URL,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_IDENTITY_URL,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).authority
        )
        randIdentityPath = RandomUtils.getAlphaString(10)
        Assert.assertEquals(
            "/v1/$randIdentityPath",
            identityClient!!.getUrl(
                MParticleBaseClientImpl.Endpoint.IDENTITY,
                randIdentityPath
            ).path
        )
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testRandomEndpoint() {
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
        setClients()
        Assert.assertEquals(
            audienceUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).authority
        )
        Assert.assertEquals(
            configUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).authority
        )
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            identityUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).authority
        )
        var randIdentityPath: String = RandomUtils.getAlphaString(10)
        Assert.assertEquals(
            "/v1/$randIdentityPath",
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).path
        )
        Assert.assertEquals(
            audienceUrl,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).authority
        )
        Assert.assertEquals(
            configUrl,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).authority
        )
        Assert.assertEquals(
            eventsUrl,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            identityUrl,
            identityClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).authority
        )
        randIdentityPath = RandomUtils.getAlphaString(10)
        Assert.assertEquals(
            "/v1/$randIdentityPath",
            identityClient!!.getUrl(
                MParticleBaseClientImpl.Endpoint.IDENTITY,
                randIdentityPath
            ).path
        )

        // test the that the Path is still the default one (make sure the overrideSubdirectory is not kicking in when it shouldn't)
        Assert.assertEquals(
            defaultUrls[MParticleBaseClientImpl.Endpoint.AUDIENCE]!!.path,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).path
        )
        Assert.assertEquals(
            defaultUrls[MParticleBaseClientImpl.Endpoint.CONFIG]!!.path,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).path
        )
        Assert.assertEquals(
            defaultUrls[MParticleBaseClientImpl.Endpoint.EVENTS]!!.path,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).path
        )
        Assert.assertEquals(
            defaultUrls[MParticleBaseClientImpl.Endpoint.IDENTITY]!!.path,
            mpClient!!.getUrl(
                MParticleBaseClientImpl.Endpoint.IDENTITY,
                MParticleBaseClientImpl.Endpoint.IDENTITY.name
            ).path
        )
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testOverrideSubdirectory() {
        val identityUrl: String = RandomUtils.getAlphaString(20)
        val configUrl: String = RandomUtils.getAlphaString(20)
        val audienceUrl: String = RandomUtils.getAlphaString(20)
        val eventsUrl: String = RandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(
                        DomainMapping.audienceMapping(audienceUrl, true)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.configMapping(configUrl, true)
                            .build()
                    )
                    .addDomainMapping(
                        DomainMapping.identityMapping(identityUrl, true)
                            .build()
                    )
                    .addDomainMapping(DomainMapping.eventsMapping(eventsUrl, true).build())
                    .build()
            )
            .build()
        MParticle.start(options)
        setClients()
        Assert.assertEquals(
            audienceUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).authority
        )
        Assert.assertEquals(
            configUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).authority
        )
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            identityUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).authority
        )
        val randIdentityPath: String = RandomUtils.getAlphaString(10)
        Assert.assertEquals(
            "/$randIdentityPath",
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).path
        )

        // test the that the Path is still the default one (make sure the overrideSubdirectory is not kicking in when it shouldn't)
        Assert.assertEquals(
            defaultUrls[MParticleBaseClientImpl.Endpoint.AUDIENCE]!!.path,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).path
        )
        var configPath = defaultUrls[MParticleBaseClientImpl.Endpoint.CONFIG]!!
            .path
        configPath = configPath.substring(configPath.indexOf(apiKey) - 1)
        Assert.assertEquals(
            configPath,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).path
        )
        var eventsPath = defaultUrls[MParticleBaseClientImpl.Endpoint.EVENTS]!!
            .path
        eventsPath = eventsPath.substring(eventsPath.indexOf(apiKey) - 1)
        Assert.assertEquals(
            eventsPath,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).path
        )
        var identityPath = defaultUrls[MParticleBaseClientImpl.Endpoint.IDENTITY]!!
            .path
        identityPath =
            identityPath.substring(identityPath.indexOf(MParticleBaseClientImpl.Endpoint.IDENTITY.name) - 1)
        Assert.assertEquals(
            identityPath,
            mpClient!!.getUrl(
                MParticleBaseClientImpl.Endpoint.IDENTITY,
                MParticleBaseClientImpl.Endpoint.IDENTITY.name
            ).path
        )
    }

    /**
     * make sure that when you have "Events" DomainMapping it will, by default, apply to "Alias" endpoint.
     * This was the behavior we rolled out before the "Alias" DomainMapping was introduced, we need
     * to make sure it still works
     */
    @Test
    @Throws(MalformedURLException::class)
    fun testEventsLegacyBehavior() {
        val eventsUrl: String = RandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                    .build()
            )
            .build()
        MParticle.start(options)
        setClients()
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.ALIAS).authority
        )
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testAliasOverrideEvents() {
        val eventsUrl: String = RandomUtils.getAlphaString(20)
        val aliasUrl: String = RandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                    .addDomainMapping(DomainMapping.aliasMapping(aliasUrl).build())
                    .build()
            )
            .build()
        MParticle.start(options)
        setClients()
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            aliasUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.ALIAS).authority
        )
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testEventsDoesntApplyAlias() {
        val eventsUrl: String = RandomUtils.getAlphaString(20)
        val options = MParticleOptions.builder(context)
            .credentials(apiKey, "secret")
            .networkOptions(
                NetworkOptions.builder()
                    .addDomainMapping(DomainMapping.eventsMapping(eventsUrl, false, true).build())
                    .build()
            )
            .build()
        MParticle.start(options)
        setClients()
        Assert.assertEquals(
            eventsUrl,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).authority
        )
        Assert.assertEquals(
            NetworkOptionsManager.MP_URL,
            mpClient!!.getUrl(MParticleBaseClientImpl.Endpoint.ALIAS).authority
        )
    }

    private fun setClients() {
        mpClient = AccessUtils.apiClient as MParticleBaseClientImpl?
        identityClient = com.mparticle.identity.AccessUtils.identityApiClient
    }
}
