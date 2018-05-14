package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;
import com.mparticle.testutils.RandomUtils;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static com.mparticle.identity.MParticleIdentityClientImpl.ANDROID_AAID;
import static com.mparticle.identity.MParticleIdentityClientImpl.ANDROID_UUID;
import static com.mparticle.identity.MParticleIdentityClientImpl.DEVICE_APPLICATION_STAMP;
import static com.mparticle.identity.MParticleIdentityClientImpl.IDENTITY_CHANGES;
import static com.mparticle.identity.MParticleIdentityClientImpl.IDENTITY_TYPE;
import static com.mparticle.identity.MParticleIdentityClientImpl.NEW_VALUE;
import static com.mparticle.identity.MParticleIdentityClientImpl.OLD_VALUE;
import static com.mparticle.identity.MParticleIdentityClientImpl.PUSH_TOKEN;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MParticleIdentityClientImplTest extends BaseCleanStartedEachTest {
    private Context mContext;
    private ConfigManager mConfigManager;
    private MParticleIdentityClientImpl mApiClient;
    protected CountDownLatch lock = new CountDownLatch(1);

    private RandomUtils mRandomUtils = RandomUtils.getInstance();

    @Before
    public void before() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        mConfigManager = MParticle.getInstance().getConfigManager();
    }


    @Test
    public void testModifySameData() throws Exception {
        final int startingModifyRequestCount = mServer.getRequests().Identity().modify().size();
        final CountDownLatch latch = new CountDownLatch(2);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fail("modify did not complete");
            }
        }, 10 * 1000);

       MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build())
               .addSuccessListener(new TaskSuccessListener() {
                   @Override
                   public void onSuccess(IdentityApiResult result) {
                        latch.countDown();
                        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build())
                                .addSuccessListener(new TaskSuccessListener() {
                                    @Override
                                    public void onSuccess(IdentityApiResult result) {
                                        latch.countDown();
                                        int currentModifyRequestCount = mServer.getRequests().Identity().modify().size();
                                        //make sure we made 1 or 0 modify requests. It could go either way for the first modify request,
                                        //it may have changes, it may not depending on state. The second request though, should not have
                                        //changes, and therefore it should not take place, so less than 2 requests is a good condition
                                        assertTrue(startingModifyRequestCount + 2 > currentModifyRequestCount);
                                        handler.removeCallbacks(null);
                                    }
                                })
                                .addFailureListener(new TaskFailureListener() {
                                    @Override
                                    public void onFailure(IdentityHttpResponse result) {
                                        fail("task failed");
                                    }
                                });
                   }
               })
               .addFailureListener(new TaskFailureListener() {
                   @Override
                   public void onFailure(IdentityHttpResponse result) {
                        fail("task failed");
                   }
               });
        latch.await();
    }

    @Test
    public void testIdentifyMessage() throws Exception {
        int iterations = 20;
        final boolean[]checked = new boolean[iterations];
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();

            final int finalI = i;
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(HttpURLConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
                    if (connection.getURL().toString().contains("/identify")) {
                        JSONObject jsonObject = new JSONObject(payload);
                        JSONObject knownIdentities = jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES);
                        assertNotNull(knownIdentities);
                        checkStaticsAndRemove(knownIdentities);
                        if (knownIdentities.length() != userIdentities.size()) {
                            assertEquals(knownIdentities.length(), userIdentities.size());
                        }
                        for (Map.Entry<MParticle.IdentityType, String> identity : userIdentities.entrySet()) {
                            String value = knownIdentities.getString(mApiClient.getStringValue(identity.getKey()));
                            assertEquals(value, identity.getValue());
                        }
                        checked[finalI] = true;
                        setApiClient(null);
                        lock.countDown();
                    }
                }
            });

            mApiClient.identify(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
            lock.await();
        }
        TestingUtils.checkAllBool(checked, 1, 10);
    }

    @Test
    public void testLoginMessage() throws Exception {
        int iterations = 20;
        final boolean[]checked = new boolean[iterations];
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();

            final int finalI = i;
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(HttpURLConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
                    if (connection.getURL().toString().contains("/login")) {
                        JSONObject jsonObject = new JSONObject(payload);
                        JSONObject knownIdentities = jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES);
                        assertNotNull(knownIdentities);
                        checkStaticsAndRemove(knownIdentities);
                        assertEquals(knownIdentities.length(), userIdentities.size());
                        for (Map.Entry<MParticle.IdentityType, String> identity : userIdentities.entrySet()) {
                            String value = knownIdentities.getString(mApiClient.getStringValue(identity.getKey()));
                            assertEquals(value, identity.getValue());
                        }
                        checked[finalI] = true;
                    }
                }
            });
            mApiClient.login(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
        }
        TestingUtils.checkAllBool(checked, 1, 10);
    }

    @Test
    public void testLogoutMessage() throws Exception {
        int iterations = 20;
        final boolean[]checked = new boolean[iterations];
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();

            final int finalI = i;
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(HttpURLConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
                    if (connection.getURL().toString().contains("/logout")) {
                        JSONObject jsonObject = new JSONObject(payload);
                        JSONObject knownIdentities = jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES);
                        assertNotNull(knownIdentities);
                        checkStaticsAndRemove(knownIdentities);
                        assertEquals(knownIdentities.length(), userIdentities.size());
                        for (Map.Entry<MParticle.IdentityType, String> identity : userIdentities.entrySet()) {
                            String value = knownIdentities.getString(mApiClient.getStringValue(identity.getKey()));
                            assertEquals(value, identity.getValue());
                        }
                        checked[finalI] = true;
                    }
                }
            });

            mApiClient.logout(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
        }
        TestingUtils.checkAllBool(checked, 1, 10);
    }

    @Test
    public void testModifyMessage() throws Exception {
        mConfigManager.setMpid(new Random().nextLong());
        int iterations = 20;
        final boolean[]checked = new boolean[iterations];
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> oldUserIdentities = mRandomUtils.getRandomUserIdentities();
            final Map<MParticle.IdentityType, String> newUserIdentities = mRandomUtils.getRandomUserIdentities();

            AccessUtils.clearUserIdentities((MParticleUserImpl)MParticle.getInstance().Identity().getCurrentUser());
            ((MParticleUserImpl)MParticle.getInstance().Identity().getCurrentUser()).setUserIdentities(oldUserIdentities);

            final int finalI = i;
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(HttpURLConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
                        if (connection.getURL().toString().contains("/modify")) {
                            JSONObject jsonObject = new JSONObject(payload);
                            JSONArray changedIdentities = jsonObject.getJSONArray(IDENTITY_CHANGES);
                            for (int i = 0; i < changedIdentities.length(); i++) {
                                JSONObject changeJson = changedIdentities.getJSONObject(i);
                                Object newValue = changeJson.getString(NEW_VALUE);
                                Object oldValue = changeJson.getString(OLD_VALUE);
                                MParticle.IdentityType identityType = mApiClient.getIdentityType(changeJson.getString(IDENTITY_TYPE));
                                String nullString = JSONObject.NULL.toString();
                                if (oldUserIdentities.get(identityType) == null) {
                                    if(!oldValue.equals(JSONObject.NULL.toString())) {
                                        fail();
                                    }
                                } else {
                                    assertEquals(oldValue, oldUserIdentities.get(identityType));
                                }
                                if (newUserIdentities.get(identityType) == null) {
                                    if(!newValue.equals(nullString)) {
                                        fail();
                                    }
                                } else {
                                    assertEquals(newValue, newUserIdentities.get(identityType));
                                }
                            }
                            checked[finalI] = true;
                            setApiClient(null);
                        }
                }
            });

            mApiClient.modify(IdentityApiRequest.withEmptyUser()
                    .userIdentities(newUserIdentities)
                    .build());
        }
        TestingUtils.checkAllBool(checked, 1, 10);
    }

    private void setApiClient(final MockIdentityApiClient identityClient) {
        mApiClient = new MParticleIdentityClientImpl(mContext, mConfigManager) {
            @Override
            public HttpURLConnection makeUrlRequest(Endpoint endpoint, final HttpURLConnection connection, String payload, boolean identity) throws IOException {
                try {
                    identityClient.makeUrlRequest(connection, payload, identity);
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                return new HttpURLConnection(null) {

                    @Override
                    public void connect() throws IOException {}

                    @Override
                    public void disconnect() {}

                    @Override
                    public boolean usingProxy() {
                        return false;
                    }

                    @Override
                    public int getResponseCode() throws IOException {
                        return 202;
                    }
                };
            }
        };
        MParticle.getInstance().Identity().setApiClient(mApiClient);
    }

    private void checkStaticsAndRemove(JSONObject knowIdentites) throws JSONException {
        if (knowIdentites.has(ANDROID_AAID)) {
            assertEquals(MPUtility.getGoogleAdIdInfo(mContext).id, knowIdentites.getString(ANDROID_AAID));
            knowIdentites.remove(ANDROID_AAID);
        } else {
            assertTrue(MPUtility.getGoogleAdIdInfo(mContext) == null || MPUtility.isEmpty(MPUtility.getGoogleAdIdInfo(mContext).id));
        }
        if (knowIdentites.has(ANDROID_UUID)) {
            assertEquals(MPUtility.getAndroidID(mContext), knowIdentites.getString(ANDROID_UUID));
            knowIdentites.remove(ANDROID_UUID);
        } else {
            assertTrue(MPUtility.isEmpty(MPUtility.getAndroidID(mContext)));
        }
        if (knowIdentites.has(PUSH_TOKEN)) {
            assertEquals(mConfigManager.getPushToken(), knowIdentites.getString(PUSH_TOKEN));
            knowIdentites.remove(PUSH_TOKEN);
        } else {
            assertNull(mConfigManager.getPushToken());
        }
        assertTrue(knowIdentites.has(DEVICE_APPLICATION_STAMP));
        assertEquals(mConfigManager.getDeviceApplicationStamp(), knowIdentites.get(DEVICE_APPLICATION_STAMP));
        knowIdentites.remove(DEVICE_APPLICATION_STAMP);
    }

    interface MockIdentityApiClient {
        void makeUrlRequest(HttpURLConnection connection, String payload, boolean mparticle) throws IOException, JSONException;
    }
}
