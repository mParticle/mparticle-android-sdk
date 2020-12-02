package com.mparticle.testutils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

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
import com.mparticle.internal.UploadHandler;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.services.MessageService;
import com.mparticle.internal.database.services.SessionService;
import com.mparticle.networking.MPConnectionTestImpl;
import com.mparticle.networking.MockServer;
import com.mparticle.testutils.AndroidUtils.Mutable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    @Rule
    public CaptureFailingTestLogcat captureFailingTestLogcat = new CaptureFailingTestLogcat();

    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        final Handler testHandler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull final Throwable e) {
                if (t.getName().equals("mParticleMessageHandler") || t.getName().equals("mParticleUploadHandler")) {
                    testHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        });
    }

    @Before
    public void beforeImpl() throws Exception {
        Logger.setLogHandler(null);
        mContext = new ContextWrapper(InstrumentationRegistry.getInstrumentation().getContext()) {
            @Override
            public int checkCallingOrSelfPermission(String permission) {
                if (permission.equals("com.google.android.c2dm.permission.RECEIVE")) {
                    return PackageManager.PERMISSION_GRANTED;
                }
                return super.checkCallingOrSelfPermission(permission);
            }
        };
        MParticle.reset(mContext);

        mStartingMpid = new Random().nextLong();
        if (autoStartServer()) {
            mServer = MockServer.getNewInstance(mContext);
        }
        checkClean(3);
    }

    @After
    public void afterImpl() {
        MParticle.reset(mContext);
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

    public class CaptureFailingTestLogcat implements TestRule {
        private static final String LOGCAT_HEADER = "\n============== Logcat Output =============\n";
        private static final String STACKTRACE_HEADER = "\n============== Stacktrace ===============";
        private static final String MOCKSERVER_HEADER = "\n ============== Mock Server Requests ==========\n";
        private static final String ORIGINAL_CLASS_HEADER = "\nOriginal class: ";

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        base.evaluate();
                    } catch (Throwable throwable) {
                        if (throwable instanceof AssumptionViolatedException) {
                            throw throwable;
                        }
                        String message = getReleventLogsAfterTestStart(description.getMethodName());
                        StringBuilder requestReceivedBuilder = new StringBuilder();
                        if (mServer != null) {
                            for (MPConnectionTestImpl connection : mServer.Requests().requests) {
                                requestReceivedBuilder.append(connection.getURL().toString())
                                        .append("\n")
                                        .append(connection.getBody())
                                        .append("\n")
                                        .append(connection.getResponseCode())
                                        .append(connection.getResponseMessage())
                                        .append("\n");
                            }
                        } else {
                            requestReceivedBuilder.append("Mock Server not started");
                        }
                        message = throwable.getMessage() + ORIGINAL_CLASS_HEADER
                                + throwable.getClass().getName() + LOGCAT_HEADER
                                + message + MOCKSERVER_HEADER +
                                requestReceivedBuilder.toString() + STACKTRACE_HEADER;
                        Throwable modifiedThrowable = new Throwable(message);
                        modifiedThrowable.setStackTrace(throwable.getStackTrace());
                        throw modifiedThrowable;
                    }
                }
            };
        }

        private String getReleventLogsAfterTestStart(String testName) {
            String testStartMessage = "TestRunner: started: " + testName;
            String currentProcessId = Integer.toString(android.os.Process.myPid());
            Boolean isRecording = false;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = null;
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "threadtime"});
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(currentProcessId)) {
                        if (line.contains(testStartMessage)) {
                            isRecording = true;
                        }
                    }
                    if (isRecording) {
                        builder.append(line);
                        builder.append("\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Runtime.getRuntime().exec(new String[]{"logcat", "-b", "all", "-c"});
            } catch (IOException e) {
                e.printStackTrace();
            }
            return builder.toString();
        }
    }

    private boolean checkClean(int counter) {
        if (counter <= 0) {
            fail("Database clean failed");
        }
        int sessions = SessionService.getSessions(new MParticleDBManager(mContext).getDatabase()).getCount();
        int messages = MessageService.getMessagesForUpload(new MParticleDBManager(mContext).getDatabase()).size();
        if (sessions > 0 || messages > 0) {
            MParticle.reset(mContext);
            return checkClean(--counter);
        }
        return true;
    }
}
