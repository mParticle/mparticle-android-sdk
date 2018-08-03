package com.mparticle.identity;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.Server;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

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
        startMParticle(MParticleOptions.builder(mContext)
                .identify(request));

        assertTrue(mServer.getRequests().Identity().identify().size() == 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), identities);

        MParticle.setInstance(null);
    }


    @Test
    public void testNoInitialIdentityNoStoredIdentity() throws Exception {
        startMParticle();

        assertEquals(mServer.getRequests().Identity().identify().size(), 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), new HashMap<MParticle.IdentityType, String>());
    }

    @Test
    public void testNoInitialIdentity() throws Exception {

        final Long currentMpid = new Random().nextLong();
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        startMParticle();

        MParticle.getInstance().getConfigManager().setMpid(currentMpid);


        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            AccessUtils.setUserIdentity(entry.getValue(), entry.getKey(), currentMpid);
        }

        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")));
        mServer.reset(currentMpid);

        startMParticle();

        assertEquals(mServer.getRequests().Identity().identify().size(), 1);
        assertIdentitiesMatch(mServer.getRequests().Identity().identify().get(0).getRequest(), identities);
    }

    private void assertIdentitiesMatch(Request request, Map<MParticle.IdentityType, String> identities) throws Exception {
        JSONObject json = new JSONObject(request.getBodyAsString());

        assertNotNull(json);

        JSONObject knownIdentities = json.getJSONObject("known_identities");
        assertNotNull(knownIdentities);

        assertNotNull(knownIdentities.remove("android_uuid"));
        assertNotNull(knownIdentities.remove("device_application_stamp"));

        assertEquals(knownIdentities.length(), identities.size());

        Iterator<String> keys = knownIdentities.keys();
        Map<MParticle.IdentityType, String> copy = new HashMap<MParticle.IdentityType, String>(identities);

        while (keys.hasNext()) {
            String key = keys.next();
            assertEquals(copy.get(getIdentityTypeIgnoreCase(key)), knownIdentities.getString(key));
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