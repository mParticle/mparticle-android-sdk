package com.mparticle.identity;

import android.os.Handler;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.identity.AccessUtils.IdentityApiClient;
import com.mparticle.internal.ConfigManager;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.TestingUtils;
import com.mparticle.mock.MockContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class IdentityApiTest extends BaseCleanStartedEachTest {
    ConfigManager mConfigManager;
    Handler handler;
    long mpid1, mpid2, mpid3;

    @Override
    protected void beforeClass() {

    }

    @Override
    protected void before() {
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

        AccessUtils.setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse login(IdentityApiRequest request) throws Exception {
                if (mConfigManager.getMpid() == mStartingMpid) {
                    return getIdentityHttpResponse(mpid1);
                }

                if (mConfigManager.getMpid() == mpid1) {
                    return getIdentityHttpResponse(mpid2);
                }
                return null;
            }
        }, true);

        final boolean[] checked = new boolean[4];

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid1) {
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
                assertTrue(identityApiResult.getUser().setUserAttributes(userAttributes));
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

        AccessUtils.setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                if (mConfigManager.getMpid() == mStartingMpid) {
                    return getIdentityHttpResponse(mpid1);
                }
                if (mConfigManager.getMpid() == mpid1) {
                    return getIdentityHttpResponse(mpid2);
                }
                return null;
            }
        }, true);

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

        AccessUtils.setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                if (mConfigManager.getMpid() == mStartingMpid) {
                    return getIdentityHttpResponse(mpid1);
                }
                if (mConfigManager.getMpid() == mpid1) {
                    return getIdentityHttpResponse(mpid2);
                }
                if (mConfigManager.getMpid() == mpid2) {
                    return getIdentityHttpResponse(mpid3);
                }
                return null;
            }
        }, true);

        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
                called[6] = true;
            }
        });

        MParticleUtils.awaitUploadRunnables();


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid2, MParticle.getInstance().Identity().mUserDelegate)).build();
        result = MParticle.getInstance().Identity().identify(request);

        result.addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
                called[7] = true;
            }
        });



        MParticleUtils.awaitUploadRunnables();


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid3, MParticle.getInstance().Identity().mUserDelegate)).build();
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

        AccessUtils.setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                if (mConfigManager.getMpid() == mStartingMpid) {
                    return getIdentityHttpResponse(mpid1);
                }
                if (mConfigManager.getMpid() == mpid1) {
                    return getIdentityHttpResponse(mpid2);
                }
                if (mConfigManager.getMpid() == mpid2) {
                    return getIdentityHttpResponse(mpid3);
                }
                return null;
            }
        }, true);


        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
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

        request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid2, MParticle.getInstance().Identity().mUserDelegate)).build();
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


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(new MockContext(), mpid3, MParticle.getInstance().Identity().mUserDelegate)).build();
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

    private static IdentityHttpResponse getIdentityHttpResponse(long mpid) {
        return new IdentityHttpResponse(200, mpid, "context", new ArrayList<IdentityHttpResponse.Error>());
    }

    private void assertMParticleUserEquals(MParticleUser dto1, Long mpid, Map<MParticle.IdentityType, String> identityTypes, Map<String, Object> userAttributes) {
        assertTrue(dto1.getId() == mpid);
        if (userAttributes != null) {
            if (dto1.getUserAttributes() != null) {
                assertEquals(dto1.getUserAttributes().size(), userAttributes.size());
                for (Map.Entry<String, Object> entry : dto1.getUserAttributes().entrySet()) {
                    assertEquals(entry.getValue().toString(), userAttributes.get(entry.getKey()).toString());
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
}

