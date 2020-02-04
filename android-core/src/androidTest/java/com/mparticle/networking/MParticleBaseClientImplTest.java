package com.mparticle.networking;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class MParticleBaseClientImplTest extends BaseCleanInstallEachTest {
    Map<MParticleBaseClientImpl.Endpoint, MPUrl> defaultUrls = new HashMap<MParticleBaseClientImpl.Endpoint, MPUrl>();
    String apiKey = mRandomUtils.getAlphaString(10);

    @Before
    public void before() throws InterruptedException, MalformedURLException {
        startMParticle(MParticleOptions.builder(mContext).credentials(apiKey, "secret"));
        MParticleBaseClientImpl baseClientImpl = (MParticleBaseClientImpl)AccessUtils.getApiClient();
        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            defaultUrls.put(endpoint, baseClientImpl.getUrl(endpoint, endpoint.name()));
        }
        MParticle.setInstance(null);
    }

    @Test
    public void testGetUrlForceDefaultOption() throws MalformedURLException {
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

        MParticleBaseClientImpl baseClientImpl = (MParticleBaseClientImpl)AccessUtils.getApiClient();

        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            MPUrl generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name(), true);
            assertEquals(defaultUrls.get(endpoint).toString(), generatedUrl.toString());
            assertTrue(generatedUrl == generatedUrl.getDefaultUrl());
        }

        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            MPUrl generatedUrl = baseClientImpl.getUrl(endpoint, endpoint.name(), false);
            assertNotEquals(defaultUrls.get(endpoint).toString(), generatedUrl.toString());
            assertFalse(generatedUrl == generatedUrl.getDefaultUrl());
            assertEquals(defaultUrls.get(endpoint).toString(), generatedUrl.getDefaultUrl().toString());
        }
    }





}
