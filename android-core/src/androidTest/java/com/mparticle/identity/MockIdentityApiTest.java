package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.MParticleTask;
import com.mparticle.TaskSuccessListener;
import com.mparticle.internal.AccessUtils;
import com.mparticle.utils.AndroidUtils;
import com.mparticle.utils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MockIdentityApiTest {
    Context mContext;
    protected CountDownLatch lock = new CountDownLatch(1);
    Handler handler;
    int serverDelay = 0;
    int delayBetweenCalls = 1;
    long mpid1, mpid2, mpid3;

    @BeforeClass
    public static void setupClass() {
        Looper.prepare();
        AndroidUtils.getInstance().deleteDatabase();
    }

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        handler = new Handler();
        //this should reset it to a "Fresh Install" state..
        AccessUtils.clearMpId(mContext);
        MParticle.setInstance(null);
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
    public void testUserChangeCallbackAccurancy() throws JSONException, InterruptedException {
        Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
        identities.put(MParticle.IdentityType.Facebook, "facebooker.me");
        identities.put(MParticle.IdentityType.Email, "tester@mparticle.gov");
        identities.put(MParticle.IdentityType.Google, "hello@googlemail.com");
        Map<MParticle.IdentityType, String> identities2 = new HashMap<MParticle.IdentityType, String>();
        identities2.put(MParticle.IdentityType.CustomerId, "12345");
        identities2.put(MParticle.IdentityType.Microsoft, "microsoftUser");
        Map<String, Object> userAttributes = new HashMap<String, Object>();
        userAttributes.put("field1", new JSONObject("{jsonField1:\"value\", json2:3}"));
        userAttributes.put("number2", "HelloWorld");
        userAttributes.put("third", 123);

        final IdentityHttpResponse user1 = new IdentityHttpResponse(mpid1, identities);
        final IdentityHttpResponse user2 = new IdentityHttpResponse(mpid2, identities2, userAttributes);

        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().Identity().getCurrentUser());

        setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                lock.await(serverDelay, TimeUnit.SECONDS);
                if (request.getMpId().equals(mpid1)) {
                    return user1;
                }
                if (request.getMpId().equals(mpid2)) {
                    return user2;
                }
                return null;
            }
        });

        final boolean[] checked = new boolean[4];

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                try {
                    lock.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (user.getId() == user1.getMpId()) {
                    assertMParticleUserEquals(user, user1, false);
                    checked[0] = true;
                }
                if (checked[0] && user.getId() == user2.getMpId()) {
                    assertMParticleUserEquals(user, user2, true);
                    checked[1] = true;
                }
            }
        });

        // value passed in here is meaningless, except for the shouldCopyUserAttributes()...we are
        // going to override it with the custom implementation of MParticleIdentityApiClient anyways
        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertMParticleUserEquals(identityApiResult.getUser(), user1, false);
                checked[2] = true;
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);

        IdentityApiRequest request1 = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid2, MParticle.getInstance().Identity().mUserDelegate)).copyUserAttributes(true).build();
        MParticleTask<IdentityApiResult> result1 = MParticle.getInstance().Identity().identify(request1);

        //test that change actually took place
        result1.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertMParticleUserEquals(identityApiResult.getUser(), user2, true);
                checked[3] = true;
            }
        });

        TestingUtils.checkAllBool(checked, 1, 10);


    }

    private void assertMParticleUserEquals(MParticleUser dto1, IdentityHttpResponse dto2, boolean shouldCopyUserAttributes) {
        assertEquals(dto1.getId(), dto2.getMpId());
        if (shouldCopyUserAttributes) {
            if (dto1.getUserAttributes() != null) {
                assertEquals(dto1.getUserAttributes().size(), dto2.getUserAttributes().size());
                for (Map.Entry<String, Object> entry : dto1.getUserAttributes().entrySet()) {
                    assertEquals(entry.getValue().toString(), dto2.getUserAttributes().get(entry.getKey()).toString());
                }
            }
        } else {
            assertEquals(dto1.getUserAttributes().size(), 0);
        }
        assertEquals(dto1.getUserIdentities().size(), dto2.getIdentities().size());
        for (Map.Entry<MParticle.IdentityType, String> entry : dto1.getUserIdentities().entrySet()) {
            assertEquals(entry.getValue(), dto2.getIdentities().get(entry.getKey()));
        }
    }

    /**
     * happy case, tests that IdentityChangedListener works when added, and stays there
     *
     * @throws Exception
     */
    @Test
    public void testIdentityChangedListenerAdd() throws Exception {
        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().Identity().getCurrentUser());

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
                    lock.countDown();
                }
            }
        });

        setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                if (request.getMpId().equals(mpid1)) {
                    return new IdentityHttpResponse(mpid1, new HashMap<MParticle.IdentityType, String>());
                }
                if (request.getMpId().equals(mpid2)) {
                    return new IdentityHttpResponse(mpid2, new HashMap<MParticle.IdentityType, String>());
                }
                return null;
            }
        });

        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);

        request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid2, MParticle.getInstance().Identity().mUserDelegate)).build();
        result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
            }
        });

        TestingUtils.checkAllBool(called, 1, 10);
    }

    @Test
    public void testAddMultipleIdentityStateListeners() throws Exception {
        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().Identity().getCurrentUser());

        final boolean[] called = new boolean[7];
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

        setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                lock.await(serverDelay, TimeUnit.MILLISECONDS);
                if (request.getMpId().equals(mpid1)) {
                    return new IdentityHttpResponse(mpid1, new HashMap<MParticle.IdentityType, String>());
                }
                if (request.getMpId().equals(mpid2)) {
                    return new IdentityHttpResponse(mpid2, new HashMap<MParticle.IdentityType, String>());
                }
                if (request.getMpId().equals(mpid3)) {
                    return new IdentityHttpResponse(mpid3, new HashMap<MParticle.IdentityType, String>());
                }
                return null;
            }
        });

        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid2, MParticle.getInstance().Identity().mUserDelegate)).build();
        result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
            }
        });

        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid3) {
                    called[6] = true;
                }
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid3, MParticle.getInstance().Identity().mUserDelegate)).build();
        result = MParticle.getInstance().Identity().identify(request);


        lock.await(delayBetweenCalls, TimeUnit.SECONDS);

        TestingUtils.checkAllBool(called, 1, 10);
    }

    @Test
    public void testRemoveIdentityStateListeners() throws Exception {
        MParticleOptions options = MParticleOptions
                .builder(mContext)
                .credentials("key", "value")
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().Identity().getCurrentUser());

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
                if (user != null && mpid1 == user.getId()) {
                    called[2] = true;
                    return;
                }
                fail("IdentityStateListener failed to be removed");
            }
        };
        MParticle.getInstance().Identity().addIdentityStateListener(identityStateListener);
        MParticle.getInstance().Identity().addIdentityStateListener(identityStateListener2);

        setIdentityApiClient(new IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                lock.await(serverDelay, TimeUnit.MILLISECONDS);
                if (request.getMpId().equals(mpid1)) {
                    return new IdentityHttpResponse(mpid1, new HashMap<MParticle.IdentityType, String>());
                }
                if (request.getMpId().equals(mpid2)) {
                    return new IdentityHttpResponse(mpid2, new HashMap<MParticle.IdentityType, String>());
                }
                if (request.getMpId().equals(mpid3)) {
                    return new IdentityHttpResponse(mpid3, new HashMap<MParticle.IdentityType, String>());
                }
                return null;
            }
        });


        IdentityApiRequest request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid1, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticleTask<IdentityApiResult> result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid1);
                called[3] = true;
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);

        MParticle.getInstance().Identity().removeIdentityStateListener(identityStateListener2);

        request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid2, MParticle.getInstance().Identity().mUserDelegate)).build();
        result = MParticle.getInstance().Identity().identify(request);

        //test that change actually took place
        result.addSuccessListener(new TaskSuccessListener<IdentityApiResult>() {
            @Override
            public void onSuccess(IdentityApiResult identityApiResult) {
                assertEquals(identityApiResult.getUser().getId(), mpid2);
            }
        });

        lock.await(delayBetweenCalls, TimeUnit.SECONDS);


        MParticle.getInstance().Identity().removeIdentityStateListener(identityStateListener);
        MParticle.getInstance().Identity().addIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                if (user.getId() == mpid3) {
                    called[4] = true;
                }
            }
        });


        request = IdentityApiRequest.withUser(MParticleUser.getInstance(mpid3, MParticle.getInstance().Identity().mUserDelegate)).build();
        MParticle.getInstance().Identity().identify(request);

        TestingUtils.checkAllBool(called, 1, 10);
    }


    /**
     *
     * @throws Exception
     */
    @Test
    public void testMigrationOfSameMpidOnRequest() throws Exception {

    }


    private void setIdentityApiClient(MParticleIdentityClient client) {
        MParticle.getInstance().Identity().setApiClient(client);
    }

    class IdentityApiClient implements MParticleIdentityClient {

        @Override
        public IdentityHttpResponse login(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public IdentityHttpResponse logout(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
            return null;
        }

        @Override
        public Boolean modify(IdentityApiRequest request) throws Exception {
            return null;
        }
    }
}
