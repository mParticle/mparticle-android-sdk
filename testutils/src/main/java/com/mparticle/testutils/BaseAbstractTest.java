package com.mparticle.testutils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils.Mutable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class BaseAbstractTest {
    protected MockServer mServer;
    Activity activity = new Activity();
    protected Context mContext;
    protected Random ran = new Random();
    protected RandomUtils mRandomUtils = new RandomUtils();
    protected static Long mStartingMpid;


    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void beforeImpl() throws Exception {
        Logger.setLogHandler(null);
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mStartingMpid = new Random().nextLong();
        if (autoStartServer()) {
            mServer = MockServer.getNewInstance(mContext);
        }
    }

    protected void startMParticle() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext));
    }

    protected void startMParticle(MParticleOptions.Builder options) throws InterruptedException {
        MParticle.setInstance(null);
        final CountDownLatch latch = new MPLatch(1);
        BaseIdentityTask identityTask = com.mparticle.AccessUtils.getIdentityTask(options);
        final Mutable<Boolean> called = new Mutable<>(false);
        if (identityTask == null) {
            identityTask = new BaseIdentityTask();
        }
        identityTask.addFailureListener(new TaskFailureListener() {
            @Override
            public void onFailure(IdentityHttpResponse result) {
                fail(result.toString());
            }
        }).addSuccessListener(new TaskSuccessListener() {
            @Override
            public void onSuccess(IdentityApiResult result) {
                called.value = true;
                latch.countDown();
            }
        });

        options.identifyTask(identityTask);
        if (mServer == null) {
            mServer = MockServer.getNewInstance(mContext);
        }
        MParticle.start(com.mparticle.AccessUtils.setCredentialsIfEmpty(options).build());
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

    protected boolean autoStartServer() {
        return true;
    }

    public JSONObject getDatabaseContents() throws JSONException {
        return getDatabaseContents(new MParticleDBManager(mContext).getDatabase(), getAllTables());
    }

    public JSONObject getDatabaseContents(MPDatabase database) throws JSONException {
        return getDatabaseContents(database, getAllTables(database));
    }

    public JSONObject getDatabaseContents(List<String> tableNames) throws JSONException {
        return getDatabaseContents(new MParticleDBManager(mContext).getDatabase(), getAllTables());
    }

    public JSONObject getDatabaseContents(MPDatabase database, List<String> tableNames) throws JSONException {
        JSONObject databaseJson = new JSONObject();
        for (String tableName: tableNames) {
            JSONArray data = getData(database.query(tableName, null, null, null, null, null, null));
            databaseJson.put(tableName, data);
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
