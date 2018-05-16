package com.mparticle.identity;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.testutils.RandomUtils;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public final class IdentityApiStartTest extends BaseCleanInstallEachTest {
    RandomUtils mRandomUtils = RandomUtils.getInstance();

    @Test
    public void testInitialIdentitiesPresent() throws Exception {
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .userIdentities(identities)
                .build();
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .identify(request)
                .build();

        MParticle.start(options);

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")), 2000);

        assertTrue(mServer.getRequests().Identity().identify().size() == 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), identities);

        MParticle.setInstance(null);
    }


    @Test
    public void testNoInitialIdentityNoStoredIdentity() throws Exception {

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")), 2000);

        assertEquals(mServer.getRequests().Identity().identify().size(), 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), new HashMap<MParticle.IdentityType, String>());

    }

    @Test
    public void testNoInitialIdentity() throws Exception {

        final Long currentMpid = new Random().nextLong();
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);

        MParticle.getInstance().getConfigManager().setMpid(currentMpid);


        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            AccessUtils.setUserIdentity(entry.getValue(), entry.getKey(), currentMpid);
        }

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")), 2000);
        mServer.reset(currentMpid);

        MParticle.setInstance(null);

        options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();

        MParticle.start(options);

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")), 2000);

        assertEquals(mServer.getRequests().Identity().identify().size(), 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), identities);

    }

    private void assertIdentitiesMatch(LoggedRequest request, Map<MParticle.IdentityType, String> identities) throws Exception {
        String body = request.getBodyAsString();
        JSONObject json = new JSONObject(body);

        assertNotNull(json);

        JSONObject knownIdentities = json.getJSONObject("known_identities");
        assertNotNull(knownIdentities);

        assertNotNull(knownIdentities.remove("android_uuid"));
        assertNotNull(knownIdentities.remove("device_application_stamp"));

        assertEquals(knownIdentities.length(), identities.size());

        Iterator<String> keys = knownIdentities.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            assertEquals(identities.get(getIdentityTypeIgnoreCase(key)), knownIdentities.getString(key));
        }

    }

    private MParticle.IdentityType getIdentityTypeIgnoreCase(String value) {
        for (MParticle.IdentityType identityType: MParticle.IdentityType.values()) {
            if (value.toLowerCase().equals(identityType.name().toLowerCase())) {
                return identityType;
            }
        }
        fail(String.format("Could not find IdentityType equal to \"%s\"", value));
        return null;
    }
}