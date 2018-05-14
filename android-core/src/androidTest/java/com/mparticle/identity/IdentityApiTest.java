package com.mparticle.identity;

import android.os.Handler;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.identity.UserAliasHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.MParticleUtils;
import com.mparticle.testutils.Server;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public final class IdentityApiTest extends BaseCleanStartedEachTest {
    ConfigManager mConfigManager;
    Handler handler;
    long mpid1, mpid2, mpid3;

    @Before
    public void before() {
        mConfigManager = MParticle.getInstance().getConfigManager();
        handler = new Handler();
        mpid1 = new Random().nextLong();
        mpid2 = new Random().nextLong();
        mpid3 = new Random().nextLong();
    }

    /**
     * test that when we receive a new MParticleUser from and IdentityApi server call, the correct
     * MParticleUser object is passed to all the possible callbacks
     *  - IdentityStateListener
     *  - MParticleTask<IdentityApiResult>
     *  - MParticle.getInstance().Identity().getCurrentUser()
     */
    @Test
    public void testUserChangeCallbackAccuracy() throws JSONException, InterruptedException {
        final Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
        identities.put(MParticle.IdentityType.Facebook, "facebooker.me");
        identities.put(MParticle.IdentityType.Email, "tester@mparticle.gov");
        identities.put(MParticle.IdentityType.Google, "hello@googlemail.com");
        final Map<MParticle.IdentityType, String> identities2 = new HashMap<MParticle.IdentityType, String>();
        identities2.put(MParticle.IdentityType.CustomerId, "12345");
        identities2.put(MParticle.IdentityType.Microsoft, "microsoftUser");
        final Map<String, Object> userAttributes = new HashMap<String, Object>();
        userAttributes.put("field1", new JSONObject("{jsonField1:\"value\", json2:3}"));
        userAttributes.put("number2", "HelloWorld");
        userAttributes.put("third", 123);

        mServer
                .addConditionalLoginResponse(mStartingMpid, mpid1)
                .addConditionalLoginResponse(mpid1, mpid2);

        final boolean[] checked = new boolean[4];

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid1) {
                    try {
                        MParticleUtils.awaitSetUserAttribute();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    assertMParticleUserEquals(user, mpid1, identities, null);
                    checked[0] = true;
                }
                if (checked[0] && user.getId() == mpid2) {
                    try {
                        MParticleUtils.awaitSetUserAttribute();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    assertMParticleUserEquals(user, mpid2, identities2, userAttributes);
                    checked[1] = true;
                }
            }
        });

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser().userIdentities(identities).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().login(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertMParticleUserEquals(identityApiResult.getUser(), mpid1, identities, null);
                checked[2] = true;
                for (Map.Entry<String, Object> entry: userAttributes.entrySet()) {
                    MParticle.getInstance().Identity().getCurrentUser().setUserAttribute(entry.getKey(), entry.getValue());
                }
//                assertTrue(identityApiResult.getUser().setUserAttributes());
                try {
                    MParticleUtils.awaitSetUserAttribute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                IdentityApiRequest request1 = IdentityApiRequest.withEmptyUser().userIdentities(identities2).userAliasHandler(new UserAliasHandler() {
                    @Override
                    public void onUserAlias(MParticleUser previousUser, MParticleUser newUser) {
                        newUser.setUserAttributes(previousUser.getUserAttributes());
                    }
                }).build();
                MParticleTask<IdentityApiResult> result1 = MParticle.getInstance().Identity().login(request1);

                //test that change actually took place
                result1.addSuccessListener(new TaskSuccessListener() {
                    @Override
                    public void onSuccess(IdentityApiResult identityApiResult) {
                        assertMParticleUserEquals(identityApiResult.getUser(), mpid2, identities2, userAttributes);
                        checked[3] = true;
                    }
                });
            }
        });

        TestingUtils.checkAllBool(checked, 1, 20);
    }


    /**
     * happy case, tests that IdentityChangedListener works when added, and stays there
     *
     * @throws Exception
     */
    @Test
    public void testIdentityChangedListenerAdd() throws Exception {
        mServer
                .addConditionalIdentityResponse(mStartingMpid, mpid1)
                .addConditionalIdentityResponse(mpid1, mpid2);

        final boolean[] called = new boolean[2];
        Arrays.fill(called, false);

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(final MParticleUser user) {
                if (user != null && user.getId() == mpid1) {
                    called[0] = true;
                }
                if (called[0] && user.getId() == mpid2) {
                    called[1] = true;
                }
            }
        });

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser().build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
            }
        });

        MParticleUtils.awaitUploadRunnables();

        request = IdentityApiRequest.withEmptyUser().build();
        result = MParticle.getInstance().Identity().identify(request);

        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
            }
        });

        TestingUtils.checkAllBool(called, 1, 10);
    }

    @Test
    public void testAddMultipleIdentityStateListeners() throws Exception {
        mServer
                .addConditionalIdentityResponse(mStartingMpid, mpid1)
                .addConditionalIdentityResponse(mpid1, mpid2)
                .addConditionalIdentityResponse(mpid2, mpid3);

        final boolean[] called = new boolean[9];
        Arrays.fill(called, false);

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(final MParticleUser user) {
                if (user != null && mpid1 == user.getId()) {
                    called[0] = true;
                }
                if (called[0] && mpid2 == user.getId()) {
                    called[1] = true;
                }
                if (called[1] && mpid3 == user.getId()) {
                    called[2] = true;
                }
            }
        });

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(final MParticleUser user) {
                if (user != null && mpid1 == user.getId()) {
                    called[3] = true;
                }
                if (called[3] && mpid2 == user.getId()) {
                    called[4] = true;
                }
                if (called[4] && mpid3 == user.getId()) {
                    called[5] = true;
                }
            }
        });

        IdentityApiRequest request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid1)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
                called[6] = true;
            }
        });

        MParticleUtils.awaitUploadRunnables();

        request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid2)).build();
        result = MParticle.getInstance().Identity().identify(request);

        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
                called[7] = true;
            }
        });



        MParticleUtils.awaitUploadRunnables();

        request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid3)).build();
        result = MParticle.getInstance().Identity().identify(request);

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid3) {
                    called[8] = true;
                }
            }
        });

        MParticleUtils.awaitUploadRunnables();

        TestingUtils.checkAllBool(called, 1, 10);
    }

    @Test
    public void testRemoveIdentityStateListeners() throws Exception {
        mServer
                .addConditionalIdentityResponse(mStartingMpid, mpid1)
                .addConditionalIdentityResponse(mpid1, mpid2)
                .addConditionalIdentityResponse(mpid2, mpid3);

        final boolean[] called = new boolean[5];
        Arrays.fill(called, false);

        IdentityStateListener identityStateListener = new IdentityStateListener() {
            @Override
            public void onUserIdentified(final MParticleUser user) {
                if (mpid1 == user.getId()) {
                    called[0] = true;
                    return;
                }
                if (called[0] && mpid2 == user.getId()) {
                    called[1] = true;
                    return;
                }
                fail("IdentityStateListener failed to be removed");
            }
        };

        IdentityStateListener identityStateListener2 = new IdentityStateListener() {
            @Override
            public void onUserIdentified(final MParticleUser user) {
                if (mpid1 == user.getId()) {
                    called[2] = true;
                    return;
                }
                fail("IdentityStateListener failed to be removed");
            }
        };
        MParticle.getInstance().Identity().addIdentityStateListener(identityStateListener);
        MParticle.getInstance().Identity().addIdentityStateListener(identityStateListener2);

        IdentityApiRequest request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid1)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
                called[3] = true;
            }
        });

        MParticleUtils.awaitUploadRunnables();
        MParticleUtils.awaitStoreMessage();

        MParticle.getInstance().Identity().removeIdentityStateListener(identityStateListener2);

        request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid2)).build();
        result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
            }
        });

        MParticleUtils.awaitUploadRunnables();
        MParticleUtils.awaitStoreMessage();


        MParticle.getInstance().Identity().removeIdentityStateListener(identityStateListener);
        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid3) {
                    called[4] = true;
                }
            }
        });


        request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getUser(mpid3)).build();
        MParticle.getInstance().Identity().identify(request);

        MParticleUtils.awaitUploadRunnables();
        MParticleUtils.awaitStoreMessage();

        TestingUtils.checkAllBool(called, 1, 10);
    }


    /**
     *
     * @throws Exception
     */
    @Test
    public void testMigrationOfSameMpidOnRequest() throws Exception {

    }

    private void assertMParticleUserEquals(MParticleUser dto1, Long mpid, Map<MParticle.IdentityType, String> identityTypes, Map<String, Object> userAttributes) {
        assertTrue(dto1.getId() == mpid);
        if (userAttributes != null) {
            if (dto1.getUserAttributes() != null) {
                assertEquals(dto1.getUserAttributes().size(), userAttributes.size());
                for (Map.Entry<String, Object> entry : dto1.getUserAttributes().entrySet()) {
                    if (entry.getValue() == null) {
                        assertNull(userAttributes.get(entry.getKey()));
                    } else {
                        assertEquals(entry.getValue().toString(), userAttributes.get(entry.getKey()).toString());
                    }
                }
            }
        } else {
            assertEquals(dto1.getUserAttributes().size(), 0);
        }
        assertEquals(dto1.getUserIdentities().size(), identityTypes.size());
        for (Map.Entry<MParticle.IdentityType, String> entry : dto1.getUserIdentities().entrySet()) {
            assertEquals(entry.getValue(), identityTypes.get(entry.getKey()));
        }
    }

    @Test
    public void testGetUser() throws Exception {
        IdentityApi identity = MParticle.getInstance().Identity();
        assertNotNull(identity.getCurrentUser());
        MParticle.getInstance().getConfigManager().setMpid(mpid1);
        assertEquals(identity.getCurrentUser().getId(), mpid1);
        Map<String, Object> mpid1UserAttributes = new HashMap<String, Object>(RandomUtils.getInstance().getRandomAttributes(10));
        Map<MParticle.IdentityType, String> mpid1UserIdentites = RandomUtils.getInstance().getRandomUserIdentities();
        identity.getCurrentUser().setUserAttributes(mpid1UserAttributes);
        AccessUtils.setUserIdentities(mpid1UserIdentites, identity.getCurrentUser().getId());

        MParticle.getInstance().getConfigManager().setMpid(mpid2);
        Map<String, Object> mpid2UserAttributes = new HashMap<String, Object>(RandomUtils.getInstance().getRandomAttributes(10));
        Map<MParticle.IdentityType, String> mpid2UserIdentites = RandomUtils.getInstance().getRandomUserIdentities();
        identity.getCurrentUser().setUserAttributes(mpid2UserAttributes);
        AccessUtils.setUserIdentities(mpid2UserIdentites, identity.getCurrentUser().getId());

        MParticle.getInstance().getConfigManager().setMpid(mpid3);
        Map<String, Object> mpid3UserAttributes = new HashMap<String, Object>(RandomUtils.getInstance().getRandomAttributes(10));
        Map<MParticle.IdentityType, String> mpid3UserIdentities = RandomUtils.getInstance().getRandomUserIdentities();
        identity.getCurrentUser().setUserAttributes(mpid3UserAttributes);

        AccessUtils.setUserIdentities(mpid3UserIdentities, identity.getCurrentUser().getId());

        mpid1UserAttributes.remove(null);
        mpid2UserAttributes.remove(null);
        mpid3UserAttributes.remove(null);

        MParticleUtils.awaitSetUserAttribute();

        //should return null for mpid = 0
        assertNull(identity.getUser(0L));

        //should return an MParticleUser with the correct mpid, userIdentities, and userAttributes for
        //previously seen users
        assertMParticleUserEquals(identity.getUser(mpid1), mpid1, mpid1UserIdentites, mpid1UserAttributes);
        assertMParticleUserEquals(identity.getUser(mpid2), mpid2, mpid2UserIdentites, mpid2UserAttributes);
        assertMParticleUserEquals(identity.getUser(mpid3), mpid3, mpid3UserIdentities, mpid3UserAttributes);

        //should return null for unseen mpid's
        assertNull(identity.getUser(RandomUtils.getInstance().randomLong(Long.MIN_VALUE, Long.MAX_VALUE)));
        assertNull(identity.getUser(RandomUtils.getInstance().randomLong(Long.MIN_VALUE, Long.MAX_VALUE)));
        assertNull(identity.getUser(RandomUtils.getInstance().randomLong(Long.MIN_VALUE, Long.MAX_VALUE)));
    }

    /**
     * this simulates the scenerio in which you make a modify() call, but the current MParticleUser
     * changes between the time you build the request and the time you make the call
     */
    @Test
    public void testModifyConcurrentCalls() throws Exception {
        assertEquals(mStartingMpid, MParticle.getInstance().Identity().getCurrentUser().getId(), 0);

        Map<MParticle.IdentityType, String> userIdentities = RandomUtils.getInstance().getRandomUserIdentities();
        for (MParticle.IdentityType identity: userIdentities.keySet()) {
            AccessUtils.setUserIdentity(userIdentities.get(identity), identity, mStartingMpid);
        }
        IdentityApiRequest request = IdentityApiRequest.withUser(MParticle.getInstance().Identity().getCurrentUser()).customerId(RandomUtils.getInstance().getAlphaNumericString(24)).build();

        MParticle.getInstance().Identity().modify(request);
        //change the mpid;
        //behind the scenes, this call will take place before the (above) modfy request goes out, since
        //the modify request will be passed to a background thread before it is executed
        MParticle.getInstance().getConfigManager().setMpid(mpid2);


        mServer.waitForVerify(urlPathMatching(String.format("/v([0-9]*)/%s/modify", mStartingMpid)), new Server.JSONMatch() {
            @Override
            public boolean isMatch(JSONObject jsonObject) {
                if (jsonObject.has("identity_changes")) {
                    try {
                        return jsonObject.getJSONArray("identity_changes").length() == 1;
                    } catch (JSONException jse) {
                        jse.toString();
                    }
                }
                return false;
            }
        }, 20000);
    }
}

