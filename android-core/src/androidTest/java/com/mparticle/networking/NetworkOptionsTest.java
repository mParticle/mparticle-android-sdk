package com.mparticle.networking;

import android.net.Uri;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.testutils.RandomUtils;

import org.junit.Test;

import java.net.MalformedURLException;

import static junit.framework.Assert.assertEquals;

public class NetworkOptionsTest extends BaseCleanInstallEachTest {
    private MParticleBaseClientImpl mpClient;
    private MParticleBaseClientImpl identityClient;


    @Test
    public void testDefaultEndpoints() throws MalformedURLException {
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        setClients();
        assertEquals(NetworkOptionsManager.MP_URL, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).getAuthority());
        assertEquals(NetworkOptionsManager.MP_CONFIG_URL, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).getAuthority());
        assertEquals(NetworkOptionsManager.MP_URL, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).getAuthority());
        assertEquals(NetworkOptionsManager.MP_IDENTITY_URL, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).getAuthority());
        String randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/" + randIdentityPath, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).getPath());

        assertEquals(NetworkOptionsManager.MP_URL, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).getAuthority());
        assertEquals(NetworkOptionsManager.MP_CONFIG_URL, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).getAuthority());
        assertEquals(NetworkOptionsManager.MP_URL, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).getAuthority());
        assertEquals(NetworkOptionsManager.MP_IDENTITY_URL, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).getAuthority());
        randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/" + randIdentityPath, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).getPath());
    }

    @Test
    public void testRandomEndpoint() throws MalformedURLException {
        String identityUrl = mRandomUtils.getAlphaNumericString(20);
        String configUrl = mRandomUtils.getAlphaNumericString(20);
        String audienceUrl = mRandomUtils.getAlphaNumericString(20);
        String eventsUrl = mRandomUtils.getAlphaNumericString(20);

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
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
        assertEquals(audienceUrl, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).getAuthority());
        assertEquals(configUrl, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).getAuthority());
        assertEquals(eventsUrl, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).getAuthority());
        assertEquals(identityUrl, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).getAuthority());
        String randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/" + randIdentityPath, mpClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).getPath());

        assertEquals(audienceUrl, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.AUDIENCE).getAuthority());
        assertEquals(configUrl, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.CONFIG).getAuthority());
        assertEquals(eventsUrl, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.EVENTS).getAuthority());
        assertEquals(identityUrl, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY).getAuthority());
        randIdentityPath = mRandomUtils.getAlphaString(10);
        assertEquals("/" + randIdentityPath, identityClient.getUrl(MParticleBaseClientImpl.Endpoint.IDENTITY, randIdentityPath).getPath());
    }

    private void setClients() {
        mpClient = (MParticleBaseClientImpl)AccessUtils.getApiClient();
        identityClient = (MParticleBaseClientImpl) com.mparticle.identity.AccessUtils.getIdentityApiClient();
    }
}
