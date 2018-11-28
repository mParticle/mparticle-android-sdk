package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.networking.IdentityRequest;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;

public final class IdentityApiStartTest extends BaseCleanInstallEachTest {

    @Test
    public void testInitialIdentitiesPresent() throws Exception {
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .userIdentities(identities)
                .build();
        startMParticle(MParticleOptions.builder(mContext)
                .identify(request));

        assertTrue(mServer.Requests().getIdentify().size() == 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), identities);

        MParticle.setInstance(null);
    }


    @Test
    public void testNoInitialIdentityNoStoredIdentity() throws Exception {
        startMParticle();

        assertEquals(mServer.Requests().getIdentify().size(), 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), new HashMap<MParticle.IdentityType, String>());
    }

    @Test
    public void testNoInitialIdentity() throws Exception {

        final Long currentMpid = ran.nextLong();
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        startMParticle();

        MParticle.getInstance().Internal().getConfigManager().setMpid(currentMpid, ran.nextBoolean());

        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            AccessUtils.setUserIdentity(entry.getValue(), entry.getKey(), currentMpid);
        }
        com.mparticle.internal.AccessUtils.awaitMessageHandler();

        mServer = MockServer.getNewInstance(mContext);
        startMParticle();

        assertEquals(mServer.Requests().getIdentify().size(), 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), identities);
    }

    private void assertIdentitiesMatch(Request request, Map<MParticle.IdentityType, String> identities) throws Exception {
        assertTrue(request instanceof IdentityRequest);
        IdentityRequest identityRequest = request.asIdentityRequest();
        assertNotNull(identityRequest);

        JSONObject knownIdentities = identityRequest.getBody().known_identities;
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