package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;

public final class IdentityApiStartTest extends BaseCleanInstallEachTest {

    @Test
    public void testInitialIdentitiesPresentWithAndroidId() throws Exception {
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .userIdentities(identities)
                .build();
        startMParticle(MParticleOptions.builder(mContext)
                .androidIdEnabled(true)
                .identify(request));

        assertTrue(mServer.Requests().getIdentify().size() == 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), identities, true);
    }

    @Test
    public void testInitialIdentitiesPresentWithoutAndroidId() throws Exception {
        final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .userIdentities(identities)
                .build();
        startMParticle(MParticleOptions.builder(mContext)
                .androidIdEnabled(false)
                .identify(request));

        assertTrue(mServer.Requests().getIdentify().size() == 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), identities, false);
    }


    @Test
    public void testNoInitialIdentityNoStoredIdentity() throws Exception {
        startMParticle();

        assertEquals(mServer.Requests().getIdentify().size(), 1);
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), new HashMap<>(), false);
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
        assertIdentitiesMatch(mServer.Requests().getIdentify().get(0), identities, false);
    }

    /**
     * This asserts that when the SDK receives a new Push InstanceId in the background, it will send
     * a modify request with the background change when the SDK starts up, unless there is a pushRegistration
     * included in the startup object. Make sure the Push InstanceId logged in the background is deleted
     * after it is used in the modify() request
     */
    @Test
    public void testLogNotificationBackgroundTest() throws InterruptedException {
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

    private void assertIdentitiesMatch(Request request, Map<MParticle.IdentityType, String> identities, boolean androidIdEnabled) throws Exception {
        assertTrue(request instanceof IdentityRequest);
        IdentityRequest identityRequest = request.asIdentityRequest();
        assertNotNull(identityRequest);

        JSONObject knownIdentities = identityRequest.getBody().known_identities;
        assertNotNull(knownIdentities);

        if (androidIdEnabled) {
            assertNotNull(knownIdentities.remove("android_uuid"));
        } else {
            assertFalse(knownIdentities.has("android_uuid"));
        }
        assertNotNull(knownIdentities.remove("device_application_stamp"));

        assertEquals(knownIdentities.length(), identities.size());

        Iterator<String> keys = knownIdentities.keys();
        Map<MParticle.IdentityType, String> copy = new HashMap<MParticle.IdentityType, String>(identities);

        while (keys.hasNext()) {
            String key = keys.next();
            assertEquals(copy.get(MParticleIdentityClientImpl.getIdentityType(key)), knownIdentities.getString(key));
        }
    }

    /**
     * In this scenario, a logPushRegistration's modify request is made when the current MPID is 0. Previously
     * the method's modify request would failed when a valid MPID wasn't present, but currently we will
     * defer the request until a valid MPID is present.
     *
     * Additionally, this tests that if the logPushRegistration method is called multiple times (for whatever reason)
     * before a valid MPID is present, we will ignore the previous values, and only send the most recent request.
     * This would be good in a case where the device is offline for a period of time, and logPushNotification
     * request back up.
     * @throws InterruptedException
     */
    @Test
    public void testPushRegistrationModifyRequest() throws InterruptedException, JSONException {
        final Long startingMpid = ran.nextLong();
        mServer.setupHappyIdentify(startingMpid, 200);
        final Mutable<Boolean> logPushRegistrationCalled = new Mutable<Boolean>(false);
        final CountDownLatch identifyLatch = new MPLatch(1);
        final CountDownLatch modifyLatch = new MPLatch(1);

        MParticle.start(MParticleOptions.builder(mContext).credentials("key", "value").build());

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user, MParticleUser previousUser) {
                assertTrue(logPushRegistrationCalled.value);
                identifyLatch.countDown();
                MParticle.getInstance().Identity().removeIdentityStateListener(this);
            }
        });
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(startingMpid)), modifyLatch);

        String pushRegistration = null;
        for (int i = 0; i < 5; i++) {
            MParticle.getInstance().logPushRegistration(pushRegistration = mRandomUtils.getAlphaString(12),  "senderId");
        }
        logPushRegistrationCalled.value = true;

        identifyLatch.await();
        modifyLatch.await();

        List<Request> modifyRequests = mServer.Requests().getModify();
        assertEquals(1, modifyRequests.size());
        JSONObject body = modifyRequests.get(0).getBodyJson();
        JSONArray identityChanges = body.getJSONArray("identity_changes");
        assertEquals(1, identityChanges.length());
        JSONObject identityChange = identityChanges.getJSONObject(0);
        assertEquals(pushRegistration, identityChange.getString("new_value"));
        assertEquals("push_token", identityChange.getString("identity_type"));

        //make sure the mDeferredModifyPushRegistrationListener was successfully removed from the IdentityApi
        assertEquals(0, AccessUtils.getIdentityStateListeners().size());
    }

    @Test
    public void testOperatingSystemSetProperly() throws InterruptedException {
        final Mutable<Boolean> called = new Mutable<Boolean>(false);
        final CountDownLatch latch = new MPLatch(1);
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getIdentifyUrl()), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                assertEquals("fire", request.asIdentityRequest().getBody().clientSdk.platform);
                called.value = true;
                latch.countDown();
            }
        });
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                .build());
        latch.await();
        assertTrue(called.value);

        MParticle.setInstance(null);
        called.value = false;
        final CountDownLatch latch1 = new MPLatch(1);
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getIdentifyUrl()), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                assertEquals("fire", request.asIdentityRequest().getBody().clientSdk.platform);
                called.value = true;
                latch1.countDown();
            }
        });
        MParticle.start(MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                .build());
        latch1.await();
        assertTrue(called.value);
    }

    /**
     * This builds on the previous test. The common scenario where we send a modify() request
     * when a valid MPID is not present, is when a client sets a pushRegistration in MParticleOptions
     * on the applications initial install
     */
    @Test
    public void testPushRegistrationInMParticleOptions() {
        Exception ex = null;
        try {
            startMParticle(MParticleOptions
                    .builder(mContext)
                    .pushRegistration("instanceId", "senderId")
                    .environment(MParticle.Environment.Development));
            assertTrue(MPUtility.isDevEnv());
        } catch (Exception e) {
            ex = e;
        }
        assertNull(ex);
    }
}