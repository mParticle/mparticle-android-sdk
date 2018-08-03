package com.mparticle;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.DatabaseTables;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MessageManager;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;
import com.mparticle.testutils.RandomUtils;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

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
        String randomName = RandomUtils.getInstance().getAlphaNumericString(RandomUtils.getInstance().randomInt(4, 64));
        MParticle.getInstance().setInstallReferrer(randomName);
        assertTrue(MParticle.getInstance().getInstallReferrer().equals(randomName));
    }

    /**
     * These tests are to make sure that we are not missing any instances of the InstallReferrer
     * being set at any of the entry points, without the corresponding installReferrerUpdated() calls
     * being made
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

        MParticle.getInstance().mKitManager = new KitFrameworkWrapper(mContext, null,null, null, null, true) {
            @Override
            public void installReferrerUpdated() {
                called[1] = true;
            }
        };

        //test when the InstallReferrer is set directly on the InstallReferrerHelper
        String installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(mContext, installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is set through the MParticle object in the public API
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        MParticle.getInstance().setInstallReferrer(installReferrer);

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is received via the ReferrerReceiver Receiver
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        ReferrerReceiver.setInstallReferrer(mContext, ReferrerReceiver.getMockInstallReferrerIntent(installReferrer));

        assertTrue(called[0]);
        assertTrue(called[1]);

        Arrays.fill(called, false);

        //test when it is received through the MPReceiver Receiver
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        new MPReceiver().onReceive(mContext, ReferrerReceiver.getMockInstallReferrerIntent(installReferrer));

        assertTrue(called[1]);
        assertTrue(called[0]);

        Arrays.fill(called, false);

        //just a sanity check, if Context is null, it should not set mark the InstallReferrer as updated
        installReferrer = RandomUtils.getInstance().getAlphaNumericString(10);
        InstallReferrerHelper.setInstallReferrer(null, installReferrer);

        org.junit.Assert.assertFalse(called[0]);
        org.junit.Assert.assertFalse(called[1]);
    }

    private void ensureSessionActive() {
        if (!MParticle.getInstance().isSessionActive()) {
            MParticle.getInstance().logEvent(TestingUtils.getInstance().getRandomMPEventSimple());
            assertTrue(MParticle.getInstance().isSessionActive());
        }
    }

    @Test
    public void testResetSync() throws JSONException, InterruptedException {
        testReset(new Runnable() {
            @Override
            public void run() {
                MParticle.reset(mContext);
            }
        });
    }

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

    @Test
    public void testResetIdentitySync() throws JSONException, InterruptedException {
        testResetIdentityCall(new Runnable() {
            @Override
            public void run() {
                MParticle.reset(mContext);
            }
        });
    }

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
     * Test that Identity calls in progress will exit gracefully, and not trigger any callbacks
     */
    public void testResetIdentityCall(Runnable resetRunnable) throws InterruptedException {
        final boolean[] called = new boolean[2];
        IdentityStateListener crashListener = new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                assertTrue(called[0]);
                throw new IllegalStateException("Should not be getting callbacks after reset");
            }
        };

        mServer.setupHappyIdentify(new Random().nextLong(), 100);
        MParticle.getInstance().Identity().addIdentityStateListener(crashListener);
        MParticle.getInstance().Identity().identify(IdentityApiRequest.withEmptyUser().build());

        resetRunnable.run();
        called[0] = true;
        mServer.waitForVerify(postRequestedFor(urlPathMatching("/v([0-9]*)/identify")));

        assertSDKGone();
    }

    private void testReset(Runnable resetRunnable) throws JSONException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            MParticle.getInstance().logEvent(TestingUtils.getInstance().getRandomMPEventRich());
        }
        Random ran = new Random();
        for (int i = 0; i < 10; i++) {
            MParticle.getInstance().getConfigManager().setMpid(ran.nextLong());
        }
        assertTrue(getData(DatabaseTables.getInstance(mContext).getMParticleDatabase().query("messages", null, null, null, null, null, null)).length() > 0);
        assertEquals(6, getAllTables().size());
        assertTrue(10 < MParticle.getInstance().getConfigManager().getMpids().size());

        //Set strict mode, so if we get any warning or error messages during the reset/restart phase,
        //it will throw an exception
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING);

        resetRunnable.run();

        assertSDKGone();

        //restart the SDK, to the point where the initial Identity call returns, make sure there are no errors on startup
        TestingUtils.setStrictMode(MParticle.LogLevel.WARNING, "Failed to get MParticle instance, getInstance() called prior to start().");
        beforeBase();
    }

    private void assertSDKGone() {
        //check post-reset state
        //should be 2 entries in default SharedPreferences (the install boolean and the original install time)
        //and 0 other SharedPreferences tables
        //make sure the 2 entries in default SharedPreferences are the correct values
        //0 tables should exist
        //then we call DatabaseTables.getInstance(Context).getMParticleDatabase, which should create the database,
        //and make sure it is created without an error message, and that all the tables are empty
        String sharedPrefsDirectory = mContext.getFilesDir().getPath().replace("files", "shared_prefs/");
        File[] files = new File(sharedPrefsDirectory).listFiles();
        for (File file : files) {
            String sharedPreferenceName = file.getPath().replace(sharedPrefsDirectory, "").replace(".xml", "");
            if (!sharedPreferenceName.equals("WebViewChromiumPrefs") && !sharedPreferenceName.equals("com.mparticle.test_preferences")) {
                fail();
            }
        }
        assertEquals(0, mContext.databaseList().length);
        try {
            for (String tableName: getAllTables()) {
                JSONArray data = getData(DatabaseTables.getInstance(mContext).getMParticleDatabase().query(tableName, null, null, null, null, null, null));
                if (data.length() > 0) {
                    assertEquals(0, data.length());
                }
            }
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    private List<String> getAllTables() throws JSONException {
        SQLiteDatabase database = DatabaseTables.getInstance(mContext).getMParticleDatabase();
        Cursor cursor = database.query("sqlite_master", null, "type = ?", new String[]{"table"}, null, null, null);
        cursor.moveToFirst();
        List<String> tableNames = new ArrayList<String>();
        try {
            while (!cursor.isAfterLast()) {
                String tableName = cursor.getString(cursor.getColumnIndex("name"));
                if (!"android_metadata".equals(tableName) && !"sqlite_sequence".equals(tableName)) {
                    tableNames.add(cursor.getString(cursor.getColumnIndex("name")));
                }
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return tableNames;
    }

    private JSONArray getData(Cursor cursor) throws JSONException {
        cursor.moveToFirst();
        JSONArray jsonArray = new JSONArray();
        try {
            while (!cursor.isAfterLast()) {
                JSONObject jsonObject = new JSONObject();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String columnName = cursor.getColumnName(i);
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_FLOAT:
                            jsonObject.put(columnName, cursor.getFloat(i));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            jsonObject.put(columnName, cursor.getInt(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            jsonObject.put(columnName, cursor.getString(i));
                    }
                }
                jsonArray.put(jsonObject);
                cursor.moveToNext();
            }
        }
        finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return jsonArray;
    }

}