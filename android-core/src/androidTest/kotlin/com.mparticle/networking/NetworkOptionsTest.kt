package com.mparticle.networking;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.ALIAS;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.AUDIENCE;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.CONFIG;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.EVENTS;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.IDENTITY;
import static org.junit.Assert.assertEquals;

public class NetworkOptionsTest extends BaseCleanInstallEachTest {
    private MParticleBaseClientImpl mpClient;
    private MParticleBaseClientImpl identityClient;
    private Map<MParticleBaseClientImpl.Endpoint, MPUrl> defaultUrls = new HashMap<MParticleBaseClientImpl.Endpoint, MPUrl>();
    String apiKey = mRandomUtils.getAlphaString(8);



    @Before
    public void beforeClass() throws InterruptedException, MalformedURLException {
        startMParticle(MParticleOptions.builder(mContext).credentials(apiKey, "secret"));
        setClients();
        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            defaultUrls.put(endpoint, mpClient.getUrl(endpoint, endpoint.name()));
        }
        MParticle.setInstance(null);
    }

    @After
    public void after() {
        MParticle.setInstance(null);
    }

    @Test
    public void testDefaultEndpoints() throws MalformedURLException, InterruptedException {
        MParticle.start(MParticleOptions.builder(mContext).credentials(apiKey, "s").build());
        setClients();
        assertEquals(NetworkOptionsManager.MP_URL, mpClient.getUrl(AUDIENCE).getAuthority());
        assertEquals(NetworkOptionsManager.MP_CONFIG_URL, mpClient.getUrl(CONFIG).getAuthority());
        assertEquals(NetworkOptionsManager.MP_URL, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(NetworkOptionsManager.MP_IDENTITY_URL, mpClient.getUrl(IDENTITY).getAuthority());
        String randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/v1/" + randIdentityPath, mpClient.getUrl(IDENTITY, randIdentityPath).getPath());

        assertEquals(NetworkOptionsManager.MP_URL, identityClient.getUrl(AUDIENCE).getAuthority());
        assertEquals(NetworkOptionsManager.MP_CONFIG_URL, identityClient.getUrl(CONFIG).getAuthority());
        assertEquals(NetworkOptionsManager.MP_URL, identityClient.getUrl(EVENTS).getAuthority());
        assertEquals(NetworkOptionsManager.MP_IDENTITY_URL, identityClient.getUrl(IDENTITY).getAuthority());
        randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/v1/" + randIdentityPath, identityClient.getUrl(IDENTITY, randIdentityPath).getPath());
    }

    @Test
    public void testRandomEndpoint() throws MalformedURLException {
        String identityUrl = mRandomUtils.getAlphaString(20);
        String configUrl = mRandomUtils.getAlphaString(20);
        String audienceUrl = mRandomUtils.getAlphaString(20);
        String eventsUrl = mRandomUtils.getAlphaString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials(apiKey, "secret")
                .networkOptions(NetworkOptions.builder()
                        .addDomainMapping(DomainMapping.audienceMapping(audienceUrl)
                                .build())
                        .addDomainMapping(DomainMapping.configMapping(configUrl)
                                .build())
                        .addDomainMapping(DomainMapping.identityMapping(identityUrl)
                                .build())
                        .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                        .build())
                .build();
        MParticle.start(options);
        setClients();
        assertEquals(audienceUrl, mpClient.getUrl(AUDIENCE).getAuthority());
        assertEquals(configUrl, mpClient.getUrl(CONFIG).getAuthority());
        assertEquals(eventsUrl, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(identityUrl, mpClient.getUrl(IDENTITY).getAuthority());
        String randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/v1/" + randIdentityPath, mpClient.getUrl(IDENTITY, randIdentityPath).getPath());

        assertEquals(audienceUrl, identityClient.getUrl(AUDIENCE).getAuthority());
        assertEquals(configUrl, identityClient.getUrl(CONFIG).getAuthority());
        assertEquals(eventsUrl, identityClient.getUrl(EVENTS).getAuthority());
        assertEquals(identityUrl, identityClient.getUrl(IDENTITY).getAuthority());
        randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/v1/" + randIdentityPath, identityClient.getUrl(IDENTITY, randIdentityPath).getPath());

        //test the that the Path is still the default one (make sure the overrideSubdirectory is not kicking in when it shouldn't)
        assertEquals(defaultUrls.get(AUDIENCE).getPath(), mpClient.getUrl(AUDIENCE).getPath());
        assertEquals(defaultUrls.get(CONFIG).getPath(), mpClient.getUrl(CONFIG).getPath());
        assertEquals(defaultUrls.get(EVENTS).getPath(), mpClient.getUrl(EVENTS).getPath());
        assertEquals(defaultUrls.get(IDENTITY).getPath(), mpClient.getUrl(IDENTITY, IDENTITY.name()).getPath());
    }

    @Test
    public void testOverrideSubdirectory() throws MalformedURLException {
        String identityUrl = mRandomUtils.getAlphaString(20);
        String configUrl = mRandomUtils.getAlphaString(20);
        String audienceUrl = mRandomUtils.getAlphaString(20);
        String eventsUrl = mRandomUtils.getAlphaString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials(apiKey, "secret")
                .networkOptions(NetworkOptions.builder()
                        .addDomainMapping(DomainMapping.audienceMapping(audienceUrl, true)
                                .build())
                        .addDomainMapping(DomainMapping.configMapping(configUrl, true)
                                .build())
                        .addDomainMapping(DomainMapping.identityMapping(identityUrl, true)
                                .build())
                        .addDomainMapping(DomainMapping.eventsMapping(eventsUrl, true).build())
                        .build())
                .build();
        MParticle.start(options);
        setClients();

        assertEquals(audienceUrl, mpClient.getUrl(AUDIENCE).getAuthority());
        assertEquals(configUrl, mpClient.getUrl(CONFIG).getAuthority());
        assertEquals(eventsUrl, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(identityUrl, mpClient.getUrl(IDENTITY).getAuthority());
        String randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/" + randIdentityPath, mpClient.getUrl(IDENTITY, randIdentityPath).getPath());

        //test the that the Path is still the default one (make sure the overrideSubdirectory is not kicking in when it shouldn't)
        assertEquals(defaultUrls.get(AUDIENCE).getPath(), mpClient.getUrl(AUDIENCE).getPath());

        String configPath = defaultUrls.get(CONFIG).getPath();
        configPath = configPath.substring(configPath.indexOf(apiKey) - 1);
        assertEquals(configPath, mpClient.getUrl(CONFIG).getPath());

        String eventsPath = defaultUrls.get(EVENTS).getPath();
        eventsPath = eventsPath.substring(eventsPath.indexOf(apiKey) - 1);
        assertEquals(eventsPath, mpClient.getUrl(EVENTS).getPath());

        String identityPath = defaultUrls.get(IDENTITY).getPath();
        identityPath = identityPath.substring(identityPath.indexOf(IDENTITY.name()) - 1);
        assertEquals(identityPath, mpClient.getUrl(IDENTITY, IDENTITY.name()).getPath());
    }

    /**
     * make sure that when you have "Events" DomainMapping it will, by default, apply to "Alias" endpoint.
     * This was the behavior we rolled out before the "Alias" DomainMapping was introduced, we need
     * to make sure it still works
     */
    @Test
    public void testEventsLegacyBehavior() throws MalformedURLException {
        String eventsUrl = mRandomUtils.getAlphaString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials(apiKey, "secret")
                .networkOptions(NetworkOptions.builder()
                        .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                        .build())
                .build();
        MParticle.start(options);
        setClients();

        assertEquals(eventsUrl, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(eventsUrl, mpClient.getUrl(ALIAS).getAuthority());
    }

    @Test
    public void testAliasOverrideEvents() throws MalformedURLException {
        String eventsUrl = mRandomUtils.getAlphaString(20);
        String aliasUrl = mRandomUtils.getAlphaString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials(apiKey, "secret")
                .networkOptions(NetworkOptions.builder()
                        .addDomainMapping(DomainMapping.eventsMapping(eventsUrl).build())
                        .addDomainMapping(DomainMapping.aliasMapping(aliasUrl).build())
                        .build())
                .build();
        MParticle.start(options);
        setClients();

        assertEquals(eventsUrl, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(aliasUrl, mpClient.getUrl(ALIAS).getAuthority());
    }

    @Test
    public void testEventsDoesntApplyAlias() throws MalformedURLException {
        String eventsUrl = mRandomUtils.getAlphaString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials(apiKey, "secret")
                .networkOptions(NetworkOptions.builder()
                        .addDomainMapping(DomainMapping.eventsMapping(eventsUrl, false, true).build())
                        .build())
                .build();
        MParticle.start(options);
        setClients();

        assertEquals(eventsUrl, mpClient.getUrl(EVENTS).getAuthority());
        assertEquals(NetworkOptionsManager.MP_URL, mpClient.getUrl(ALIAS).getAuthority());
    }

    private void setClients() {
        mpClient = (MParticleBaseClientImpl)AccessUtils.getApiClient();
        identityClient = com.mparticle.identity.AccessUtils.getIdentityApiClient();
    }
}
