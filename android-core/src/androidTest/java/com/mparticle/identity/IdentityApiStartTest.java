package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.ConfigManager;
import com.mparticle.networking.IdentityRequest;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

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

    /**
     * This asserts that when the SDK receives a new Push InstanceId in the background, it will send
     * a modify request with the background change when the SDK starts up, unless there is a pushRegistration
     * included in the startup object. Make sure the Push InstanceId logged in the background is deleted
     * after it is used in the modify() request
     */
    @Test
    public void testLogNotificationBackgroundTest() throws InterruptedException {
        startMParticle();
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)),
                new MockServer.RequestReceivedCallback() {
                    @Override
                    public void onRequestReceived(Request request) {
                        fail("should not have modify request for this user!");
                    }
                });
        MParticle.setInstance(null);

        assertNull(ConfigManager.getInstance(mContext).getPushInstanceId());
        final String instanceId = mRandomUtils.getAlphaNumericString(10);
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(mContext, instanceId, mRandomUtils.getAlphaNumericString(15));

        final Mutable<Boolean> called = new Mutable<Boolean>(false);
        CountDownLatch latch = new MPLatch(1);
        /**
         * This tests that a modify request is sent when the previous Push InstanceId is empty, with the value of "null"
         */
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                if (jsonObject.has("identity_changes")) {
                    try {
                        JSONArray identityChanges = jsonObject.getJSONArray("identity_changes");
                        assertEquals(1, identityChanges.length());
                        JSONObject identityChange = identityChanges.getJSONObject(0);

                        assertEquals("null", identityChange.getString("old_value"));
                        assertEquals(instanceId, identityChange.getString("new_value"));
                        assertEquals("push_token", identityChange.getString("identity_type"));
                        called.value = true;
                    } catch (JSONException jse) {
                        jse.toString();
                    }
                    return true;
                }
                return false;
            }
        }), latch);

        startMParticle();
        latch.await();
        assertTrue(called.value);

        MParticle.setInstance(null);
        called.value = false;

        final String newInstanceId = mRandomUtils.getAlphaNumericString(15);
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(mContext, newInstanceId, mRandomUtils.getAlphaNumericString(15));


        latch = new CountDownLatch(1);
        /**
         * tests that the modify request was made with the correct value for the instanceId set while
         * the SDK was stopped
         */
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(new MockServer.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                if (jsonObject.has("identity_changes")) {
                    try {
                        JSONArray identityChanges = jsonObject.getJSONArray("identity_changes");
                        assertEquals(1, identityChanges.length());
                        JSONObject identityChange = identityChanges.getJSONObject(0);

                        assertEquals(instanceId, identityChange.getString("old_value"));
                        assertEquals(newInstanceId, identityChange.getString("new_value"));
                        assertEquals("push_token", identityChange.getString("identity_type"));
                        called.value = true;
                    } catch (JSONException jse) {
                        jse.toString();
                    }
                    return true;
                }
                return false;
            }
        }), latch);

        startMParticle();
        latch.await();
        assertTrue(called.value);
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