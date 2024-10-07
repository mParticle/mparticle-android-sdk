package com.mparticle.testutils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.MPDatabaseImpl;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.services.MessageService;
import com.mparticle.internal.database.services.SessionService;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils.Mutable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public abstract class BaseAbstractTest {
    protected MockServer mServer;
    Activity activity = new Activity();
    protected Context mContext;
    protected Random ran = new Random();
    protected RandomUtils mRandomUtils = new RandomUtils();
    protected static Long mStartingMpid;
    private static final String defaultDatabaseName = MParticleDatabaseHelper.getDbName();

    @Rule
    public CaptureLogcatOnFailingTest captureFailingTestLogcat = new CaptureLogcatOnFailingTest();

    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        Thread.setDefaultUncaughtExceptionHandler(new TestingUncaughtExceptionHandler());
    }

    @Before
    public void beforeImpl() throws Exception {
        if (useInMemoryDatabase()) {
            MParticleDatabaseHelper.setDbName(null);
        } else {
            MParticleDatabaseHelper.setDbName(defaultDatabaseName);
        }
        Logger.setLogHandler(null);
        mContext = new TestingContext(InstrumentationRegistry.getInstrumentation().getContext());
        clearStorage();

        mStartingMpid = new Random().nextLong();
        mServer = MockServer.getNewInstance(mContext);
        checkStorageCleared(3);
    }

    protected boolean useInMemoryDatabase() {
        return false;
    }

    protected void startMParticle() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext));
    }

    @SuppressLint("MParticleInitialization")
    protected void startMParticle(MParticleOptions.Builder optionsBuilder) throws InterruptedException {
        MParticle.setInstance(null);
        final CountDownLatch latch = new MPLatch(1);
        BaseIdentityTask identityTask = com.mparticle.AccessUtils.getIdentityTask(optionsBuilder);
        final Mutable<Boolean> called = new Mutable<>(false);
        if (identityTask == null) {
            identityTask = new BaseIdentityTask();
        }
        identityTask
                .addFailureListener(result -> fail(result.toString()))
                .addSuccessListener(result -> {
                    called.value = true;
                    latch.countDown();
                });

        optionsBuilder.identifyTask(identityTask);
        optionsBuilder = com.mparticle.AccessUtils.setCredentialsIfEmpty(optionsBuilder);
        MParticle.start(optionsBuilder.build());
        mServer.setupHappyIdentify(mStartingMpid);
        latch.await();
        assertTrue(called.value);
    }

    protected void goToBackground() {
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().Internal().getAppStateManager();
            //Need to set AppStateManager's Handler to be on the main looper, otherwise, it will not put the app in the background.
            AccessUtils.setAppStateManagerHandler(new Handler(Looper.getMainLooper()));
            if (appStateManager.isBackgrounded()) {
                appStateManager.onActivityResumed(activity);
            }
            appStateManager.onActivityPaused(activity);
        }
    }

    protected void goToForeground() {
        activity = new Activity();
        if (MParticle.getInstance() != null) {
            AppStateManager appStateManager = MParticle.getInstance().Internal().getAppStateManager();
            appStateManager.onActivityResumed(activity);
        }
    }

    protected void clearStorage() {
        com.mparticle.AccessUtils.reset(mContext, false, false);
        MPDatabase database = new MParticleDBManager(mContext).getDatabase();
        List<String> tableNames = getAllTables(database);
        for (String tableName : tableNames) {
            database.delete(tableName, "1 = 1", null);
        }
    }

    protected JSONObject getDatabaseContents() throws JSONException {
        return getDatabaseContents(new MParticleDBManager(mContext).getDatabase(), getAllTables());
    }

    protected JSONObject getDatabaseContents(MPDatabase database) throws JSONException {
        return getDatabaseContents(database, getAllTables(database));
    }

    protected JSONObject getDatabaseContents(List<String> tableNames) throws JSONException {
        return getDatabaseContents(new MParticleDBManager(mContext).getDatabase(), getAllTables());
    }

    protected JSONObject getDatabaseContents(MPDatabase database, List<String> tableNames) throws JSONException {
        JSONObject databaseJson = new JSONObject();
        for (String tableName : tableNames) {
            JSONArray data = getData(database.query(tableName, null, null, null, null, null, null));
            databaseJson.put(tableName, data);
        }
        return databaseJson;
    }

    protected JSONObject getDatabaseSchema(SQLiteDatabase database) throws JSONException {
        JSONObject databaseJson = new JSONObject();
        for (String tableName : getAllTables(new MPDatabaseImpl(database))) {
            Cursor cursor = database.query(tableName, null, null, null, null, null, null);
            JSONObject columnNames = new JSONObject();
            for (String columnName : cursor.getColumnNames()) {
                columnNames.put(columnName, true);
            }
            databaseJson.put(tableName, columnNames);
        }
        return databaseJson;
    }

    protected List<String> getAllTables() {
        return getAllTables(new MParticleDBManager(mContext).getDatabase());
    }

    protected List<String> getAllTables(MPDatabase database) {
        Cursor cursor = database.query("sqlite_master", null, "type = ?", new String[]{"table"}, null, null, null);
        cursor.moveToFirst();
        List<String> tableNames = new ArrayList<String>();
        try {
            while (!cursor.isAfterLast()) {
                String tableName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (!"android_metadata".equals(tableName) && !"sqlite_sequence".equals(tableName)) {
                    tableNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                }
                cursor.moveToNext();
            }
        } finally {
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
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return jsonArray;
    }

    protected void fetchConfig() {
        MParticle.getInstance().Internal().getMessageManager().refreshConfiguration();
    }

    protected void setCachedConfig(JSONObject config) throws JSONException {
        ConfigManager.getInstance(mContext).saveConfigJson(config, null, null, System.currentTimeMillis());
    }

    protected JSONObject getSimpleConfigWithKits() throws JSONException {
        return new JSONObject()
                .put("id", 12345)
                .put(
                        "eks",
                        new JSONArray()
                                .put(
                                        new JSONObject()
                                                .put("id", 1)
                                )
                );
    }

    private boolean checkStorageCleared(int counter) {
        if (counter <= 0) {
            fail("Database clean failed");
        }
        int sessions = SessionService.getSessions(new MParticleDBManager(mContext).getDatabase()).getCount();
        int messages = MessageService.getMessagesForUpload(new MParticleDBManager(mContext).getDatabase()).size();
        if (sessions > 0 || messages > 0) {
            clearStorage();
            return checkStorageCleared(--counter);
        }
        return true;
    }

    protected MParticleOptions emptyMParticleOptions(Context context) {
        return com.mparticle.AccessUtils.emptyMParticleOptions(context);
    }
}
