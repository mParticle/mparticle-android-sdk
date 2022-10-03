package com.mparticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MParticleJSInterface;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MParticleTest extends BaseCleanStartedEachTest {
    private String configResponse = "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[] }, \"pio\":30 }";

    @Test
    public void testEnsureSessionActive() {
        MParticle.getInstance().mAppStateManager.ensureActiveSession();
        ensureSessionActive();
    }

    @Test
    public void testEnsureSessionActiveAtStart() {
        assertFalse(MParticle.getInstance().isSessionActive());
    }

    @Test
    public void testSessionEndsOnOptOut() {
        MParticle.getInstance().mAppStateManager.ensureActiveSession();
        assertTrue(MParticle.getInstance().mAppStateManager.getSession().isActive());
        MParticle.getInstance().setOptOut(true);
        assertFalse(MParticle.getInstance().mAppStateManager.getSession().isActive());
    }

    @Test
    public void testSetInstallReferrer() {
        MParticle.getInstance().setInstallReferrer("foo install referrer");
        Assert.assertEquals("foo install referrer", MParticle.getInstance().getInstallReferrer());
    }

    @Test
    public void testInstallReferrerUpdate() {
        String randomName = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(4, 64));
        MParticle.getInstance().setInstallReferrer(randomName);
        assertTrue(MParticle.getInstance().getInstallReferrer().equals(randomName));
    }

    /**
     * These tests are to make sure that we are not missing any instances of the InstallReferrer
     * being set at any of the entry points, without the corresponding installReferrerUpdated() calls
     * being made.
     * @throws Exception
     */
    @Test
    public void testCalledUpdateInstallReferrer() throws Exception {
        final boolean[] called = new boolean[2];
        MParticle.getInstance().mMessageManager = new MessageManager(){
            @Override
            public void installReferrerUpdated() {
                called[0] = true;
            }
        };

        MParticle.getInstance().mKitManager = new KitFrameworkWrapper(mContext, null,null, null, true, null) {
            @Override
            public void installReferrerUpdated() {
                called[1] = true;
            }
        };

        //Test when the InstallReferrer is set directly on the InstallReferrerHelper.
        String installReferrer = mRandomUtils.getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(mContext, installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //Test when it is set through the MParticle object in the public API.
        installReferrer = mRandomUtils.getAlphaNumericString(10);
        MParticle.getInstance().setInstallReferrer(installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //Just a sanity check, if Context is null, it should not set mark the InstallReferrer as updated.
        installReferrer = mRandomUtils.getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(null, installReferrer);

        org.junit.Assert.assertFalse(called[0]);
        org.junit.Assert.assertFalse(called[1]);
    }

    @Test
    public void testRegisterWebView() throws JSONException, InterruptedException {
        MParticle.setInstance(null);
        final String token = mRandomUtils.getAlphaNumericString(15);
        mServer.setupConfigResponse(new JSONObject().put(ConfigManager.WORKSPACE_TOKEN, token).toString());
        startMParticle();
        final Map<String, Object> jsInterfaces = new HashMap<String, Object>();
        final MPLatch latch = new MPLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                WebView webView = new WebView(mContext) {

                    @Override
                    public void addJavascriptInterface(Object object, String name) {
                        jsInterfaces.put(name, object);
                    }
                };

                MParticle.getInstance().registerWebView(webView);
                assertTrue(jsInterfaces.get(MParticleJSInterface.INTERFACE_BASE_NAME + "_" + token+ "_v2") instanceof MParticleJSInterface);

                String clientToken = mRandomUtils.getAlphaNumericString(15);
                MParticle.getInstance().registerWebView(webView, clientToken);
                assertTrue(jsInterfaces.get(MParticleJSInterface.INTERFACE_BASE_NAME + "_" + clientToken + "_v2") instanceof MParticleJSInterface);
                latch.countDown();
            }
        });
        latch.await();
        assertEquals(2, jsInterfaces.size());
    }

    private void ensureSessionActive() {
        if (!MParticle.getInstance().isSessionActive()) {
            MParticle.getInstance().logEvent(TestingUtils.getInstance().getRandomMPEventSimple());
            assertTrue(MParticle.getInstance().isSessionActive());
        }
    }

    @OrchestratorOnly
    @Test
    public void testResetSync() throws JSONException, InterruptedException {
        testReset(new Runnable() {
            @Override
            public void run() {
                MParticle.reset(mContext);
            }
        });
    }

    @OrchestratorOnly
    @Test
    public void testResetAsync() throws JSONException, InterruptedException {
        testReset(new Runnable() {
            @Override
            public void run() {
                final CountDownLatch latch = new MPLatch(1);
                MParticle.reset(mContext, new MParticle.ResetListener() {
                    @Override
                    public void onReset() {
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @OrchestratorOnly
    @Test
    public void testResetIdentitySync() throws JSONException, InterruptedException {
        testResetIdentityCall(new Runnable() {
            @Override
            public void run() {
                MParticle.reset(mContext);
            }
        });
    }

    @OrchestratorOnly
    @Test
    public void testResetIdentityAsync() throws JSONException, InterruptedException {
        testResetIdentityCall(new Runnable() {
            @Override
            public void run() {
                final CountDownLatch latch = new MPLatch(1);
                MParticle.reset(mContext, new MParticle.ResetListener() {
                    @Override
                    public void onReset() {
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @OrchestratorOnly
    @Test
    public void testResetConfigCall() throws InterruptedException {
        mServer.setupConfigResponse(configResponse, 100);
        MParticle.getInstance().refreshConfiguration();
        MParticle.reset(mContext);
        //This sleep is here just to
        Thread.sleep(100);
        assertSDKGone();
    }


    /**
     * Test that Identity calls in progress will exit gracefully, and not trigger any callbacks.
     */
    public void testResetIdentityCall(Runnable resetRunnable) throws InterruptedException {
        final boolean[] called = new boolean[2];
        IdentityStateListener crashListener = new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user, MParticleUser previousUser) {
                assertTrue(called[0]);
                throw new IllegalStateException("Should not be getting callbacks after reset");
            }
        };

        mServer.setupHappyIdentify(ran.nextLong(), 100);
        MParticle.getInstance().Identity().addIdentityStateListener(crashListener);
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());

        called[0] = true;
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getIdentifyUrl()));

        resetRunnable.run();

        assertSDKGone();
    }

    @Test
    public void testPushEnabledApi() throws InterruptedException {
        String senderId = "senderId";
        startMParticle();
        MParticle.getInstance().Messaging().enablePushNotifications(senderId);
        String fetchedSenderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId();
        assertTrue(MParticle.getInstance().Internal().getConfigManager().isPushEnabled());

        assertEquals(senderId, fetchedSenderId);

        String otherSenderId = "senderIdLogPushRegistration";
        MParticle.getInstance().logPushRegistration("instanceId", otherSenderId);
        fetchedSenderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId();
        assertEquals(otherSenderId, fetchedSenderId);

        MParticle.getInstance().Messaging().disablePushNotifications();
        fetchedSenderId = MParticle.getInstance().Internal().getConfigManager().getPushSenderId();
        assertFalse(MParticle.getInstance().Internal().getConfigManager().isPushEnabled());
        assertNull(fetchedSenderId);
    }

    @Test
    public void testLogPushRegistrationModifyMessages() throws InterruptedException {
        PushRegistrationTest pushRegistrationTest = new PushRegistrationTest().setServer(mServer);
        pushRegistrationTest.setContext(mContext);
        for (final PushRegistrationTest.SetPush setPush: pushRegistrationTest.setPushes) {
            final PushRegistrationHelper.PushRegistration oldRegistration = new PushRegistrationHelper.PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            setPush.setPushRegistration(oldRegistration);
            final PushRegistrationHelper.PushRegistration newPushRegistration = new PushRegistrationHelper.PushRegistration(mRandomUtils.getAlphaNumericString(10), mRandomUtils.getAlphaNumericString(15));
            final CountDownLatch latch = new MPLatch(1);
            final AndroidUtils.Mutable<Boolean> received = new AndroidUtils.Mutable<Boolean>(false);
            mServer.waitForVerify(new Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(new MockServer.JSONMatch() {
                @Override
                public boolean isMatch(JSONObject jsonObject) {
                    if (jsonObject.has("identity_changes")) {
                        try {
                            JSONArray identityChanges = jsonObject.getJSONArray("identity_changes");
                            assertEquals(1, identityChanges.length());
                            JSONObject identityChange = identityChanges.getJSONObject(0);
                            String failureMessage = "When " + oldRegistration + " set with: " + setPush.getName();

                            //This is a wierd case. We might be setting the old pushRegistration with "logPushRegistration()",
                            //which will kick of its own modify request. We want to ignore this if this is the case.
                            if (identityChange.getString("new_value").equals(oldRegistration.instanceId)) {
                                return false;
                            }
                            assertEquals(failureMessage, oldRegistration.instanceId, identityChange.getString("old_value"));
                            assertEquals(failureMessage, newPushRegistration.instanceId, identityChange.getString("new_value"));
                            assertEquals(failureMessage, "push_token", identityChange.getString("identity_type"));
                        } catch (JSONException jse) {
                            jse.toString();
                        }
                        return true;
                    }
                    return false;
                }
            }), new MockServer.RequestReceivedCallback() {
                @Override
                public void onRequestReceived(Request request) {
                    received.value = true;
                    latch.countDown();
                }
            });
            MParticle.getInstance().logPushRegistration(newPushRegistration.instanceId, newPushRegistration.senderId);
            latch.await();
        }
    }

    @Test
    public void testSetLocation() {
        Location location = new Location("");
        MParticle.getInstance().setLocation(location);
        assertEquals(location, MParticle.getInstance().mMessageManager.getLocation());
        MParticle.getInstance().setLocation(null);
        assertNull(MParticle.getInstance().mMessageManager.getLocation());
    }

    private void testReset(Runnable resetRunnable) throws JSONException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            MParticle.getInstance().logEvent(TestingUtils.getInstance().getRandomMPEventRich());
        }
        for (int i = 0; i < 10; i++) {
            MParticle.getInstance().Internal().getConfigManager().setMpid(ran.nextLong(), ran.nextBoolean());
        }
        JSONObject databaseJson = getDatabaseContents(Collections.singletonList("messages"));
        assertTrue(databaseJson.getJSONArray("messages").length() > 0);
        assertEquals(6, getAllTables().size());
        assertTrue(10 < MParticle.getInstance().Internal().getConfigManager().getMpids().size());

        //Set strict mode, so if we get any warning or error messages during the reset/restart phase,
        //it will throw an exception.
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING);

        resetRunnable.run();
        assertSDKGone();

        //Restart the SDK, to the point where the initial Identity call returns, make sure there are no errors on startup.
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING, "Failed to get MParticle instance, getInstance() called prior to start().");
        beforeBase();
    }

    private void assertSDKGone() {
        //Check post-reset state:
        //should be 2 entries in default SharedPreferences (the install boolean and the original install time)
        //and 0 other SharedPreferences tables.
        //Make sure the 2 entries in default SharedPreferences are the correct values.
        //0 tables should exist.
        //Then we call DatabaseHelper.getInstance(Context).openDatabase, which should create the database,
        //and make sure it is created without an error message, and that all the tables are empty.
        String sharedPrefsDirectory = mContext.getFilesDir().getPath().replace("files", "shared_prefs/");
        File[] files = new File(sharedPrefsDirectory).listFiles();
        for (File file : files) {
            String sharedPreferenceName = file.getPath().replace(sharedPrefsDirectory, "").replace(".xml", "");
            if (!sharedPreferenceName.equals("WebViewChromiumPrefs") && !sharedPreferenceName.equals("com.mparticle.test_preferences")) {
                fail("SharedPreference file failed to clear:\n" + getSharedPrefsContents(sharedPreferenceName));
            }
        }
        assertEquals(0, mContext.databaseList().length);
        try {
            JSONObject databaseJson = getDatabaseContents();
            Iterator<String> keys = databaseJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                assertEquals(key, 0, databaseJson.getJSONArray(key).length());
            }
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    private String getSharedPrefsContents(String name) {
        try {
            SharedPreferences prefs = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
            return name + ":\n" + new JSONObject(prefs.getAll()).toString(4);
        } catch (JSONException e) {
            return "error printing SharedPrefs :/";
        }
    }
}