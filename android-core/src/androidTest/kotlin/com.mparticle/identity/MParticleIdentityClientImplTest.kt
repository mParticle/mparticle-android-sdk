package com.mparticle.identity;

import android.os.Handler;
import android.util.MutableBoolean;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;
import com.mparticle.networking.MPConnection;
import com.mparticle.networking.MPConnectionTestImpl;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MParticleIdentityClientImplTest extends BaseCleanStartedEachTest {
    private ConfigManager mConfigManager;
    private MParticleIdentityClientImpl mApiClient;

    @Before
    public void before() throws Exception {
        mConfigManager = MParticle.getInstance().Internal().getConfigManager();
    }


    @Test
    public void testModifySameData() throws Exception {
        final CountDownLatch latch = new MPLatch(2);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fail("modify did not complete");
            }
        }, 10 * 1000);

        final AndroidUtils.Mutable<Boolean> called = new AndroidUtils.Mutable<Boolean>(false);
       MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build())
               .addSuccessListener(new TaskSuccessListener() {
                   @Override
                   public void onSuccess(IdentityApiResult result) {
                        latch.countDown();
                        MParticle.getInstance().Identity().modify(IdentityApiRequest.withEmptyUser().build())
                                .addSuccessListener(new TaskSuccessListener() {
                                    @Override
                                    public void onSuccess(IdentityApiResult result) {
                                        int currentModifyRequestCount = mServer.Requests().getModify().size();
                                        //make sure we made 1 or 0 modify requests. It could go either way for the first modify request,
                                        //it may have changes, it may not depending on state. The second request though, should not have
                                        //changes, and therefore it should not take place, so less than 2 requests is a good condition
                                        assertTrue(2 > currentModifyRequestCount);
                                        handler.removeCallbacks(null);
                                        called.value = true;
                                        latch.countDown();
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
        assertTrue(called.value);
    }

    @Test
    public void testIdentifyMessage() throws Exception {
        int iterations = 5;
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();
            final MutableBoolean checked = new MutableBoolean(false);
            final CountDownLatch latch = new CountDownLatch(1);
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(MPConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
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
                        checked.value = true;
                        setApiClient(null);
                        latch.countDown();
                    }
                }
            });

            mApiClient.identify(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
            latch.await();
            assertTrue(checked.value);
        }

    }

    @Test
    public void testLoginMessage() throws Exception {
        int iterations = 5;
        for (int i = 0; i < iterations; i++) {
            final CountDownLatch latch = new MPLatch(1);
            final MutableBoolean checked = new MutableBoolean(false);
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();

            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(MPConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
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
                        checked.value = true;
                        latch.countDown();
                    }
                }
            });
            mApiClient.login(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
            latch.await();
            assertTrue(checked.value);
        }
    }

    @Test
    public void testLogoutMessage() throws Exception {
        int iterations = 5;
        for (int i = 0; i < iterations; i++) {
            final Map<MParticle.IdentityType, String> userIdentities = mRandomUtils.getRandomUserIdentities();
            final CountDownLatch latch = new MPLatch(1);
            final MutableBoolean checked = new MutableBoolean(false);
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(MPConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
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
                        checked.value = true;
                        latch.countDown();
                    }
                }
            });

            mApiClient.logout(IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build());
            latch.await();
            assertTrue(checked.value);
        }
    }

    @Test
    public void testModifyMessage() throws Exception {
        int iterations = 5;
        for (int i = 1; i <= iterations; i++) {
            mConfigManager.setMpid(i, ran.nextBoolean());

            final Map<MParticle.IdentityType, String> oldUserIdentities = mRandomUtils.getRandomUserIdentities();
            final Map<MParticle.IdentityType, String> newUserIdentities = mRandomUtils.getRandomUserIdentities();

            ((MParticleUserImpl)MParticle.getInstance().Identity().getCurrentUser()).setUserIdentities(oldUserIdentities);

            final CountDownLatch latch = new MPLatch(1);
            final MutableBoolean checked = new MutableBoolean(false);
            setApiClient(new MockIdentityApiClient() {
                @Override
                public void makeUrlRequest(MPConnection connection, String payload, boolean mparticle) throws IOException, JSONException {
                        if (connection.getURL().toString().contains(MParticleIdentityClientImpl.MODIFY_PATH)) {
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
                            setApiClient(null);
                            checked.value = true;
                            latch.countDown();
                        }
                }
            });

            mApiClient.modify(IdentityApiRequest.withEmptyUser()
                    .userIdentities(newUserIdentities)
                    .build());
            latch.await();
            assertTrue(checked.value);
        }
    }

    private void setApiClient(final MockIdentityApiClient identityClient) {
        mApiClient = new MParticleIdentityClientImpl(mContext, mConfigManager, MParticle.OperatingSystem.ANDROID) {
            @Override
            public MPConnection makeUrlRequest(Endpoint endpoint, final MPConnection connection, String payload, boolean identity) throws IOException {
                try {
                    identityClient.makeUrlRequest(connection, payload, identity);
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
                ((MPConnectionTestImpl)connection).setResponseCode(202);
                return connection;
            }
        };
        MParticle.getInstance().Identity().setApiClient(mApiClient);
    }

    private void checkStaticsAndRemove(JSONObject knowIdentites) throws JSONException {
        if (knowIdentites.has(ANDROID_AAID)) {
            assertEquals(MPUtility.getAdIdInfo(mContext).id, knowIdentites.getString(ANDROID_AAID));
            knowIdentites.remove(ANDROID_AAID);
        } else {
            assertTrue(MPUtility.getAdIdInfo(mContext) == null || MPUtility.isEmpty(MPUtility.getAdIdInfo(mContext).id));
        }
        if (knowIdentites.has(ANDROID_UUID)) {
            assertEquals(MPUtility.getAndroidID(mContext), knowIdentites.getString(ANDROID_UUID));
            knowIdentites.remove(ANDROID_UUID);
        } else {
            assertTrue(MPUtility.isEmpty(MPUtility.getAndroidID(mContext)));
        }
        if (knowIdentites.has(PUSH_TOKEN)) {
            assertEquals(mConfigManager.getPushInstanceId(), knowIdentites.getString(PUSH_TOKEN));
            knowIdentites.remove(PUSH_TOKEN);
        } else {
            assertNull(mConfigManager.getPushInstanceId());
        }
        assertTrue(knowIdentites.has(DEVICE_APPLICATION_STAMP));
        assertEquals(mConfigManager.getDeviceApplicationStamp(), knowIdentites.get(DEVICE_APPLICATION_STAMP));
        knowIdentites.remove(DEVICE_APPLICATION_STAMP);
    }

    interface MockIdentityApiClient {
        void makeUrlRequest(MPConnection connection, String payload, boolean mparticle) throws IOException, JSONException;
    }
}
