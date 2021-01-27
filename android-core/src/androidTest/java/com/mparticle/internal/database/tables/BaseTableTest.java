package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.internal.database.TestSQLiteOpenHelper;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Before;

import com.mparticle.testutils.MPLatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseTableTest extends BaseCleanInstallEachTest {
    public static final String DB_NAME = "test_database";
    CountDownLatch onCreateLatch = new MPLatch(1);
    CountDownLatch onUpgradeLatch = new MPLatch(1);

    protected void runTest(SQLiteOpenHelperWrapper helper) throws InterruptedException {
        runTest(helper, 6);
    }

    protected void runTest(SQLiteOpenHelperWrapper helper, int oldVersion) throws InterruptedException {
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(DB_NAME);
        TestSQLiteOpenHelper openHelper = new TestSQLiteOpenHelper(helper, DB_NAME, oldVersion);
        openHelper.getWritableDatabase();
        openHelper.onCreateLatch.await();
        openHelper = new TestSQLiteOpenHelper(helper, DB_NAME, MParticleDatabaseHelper.DB_VERSION);
        openHelper.getWritableDatabase();
        if (oldVersion < MParticleDatabaseHelper.DB_VERSION) {
            openHelper.onUpgradeLatch.await();
        }
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(DB_NAME);
    }

    @Before
    public final void before() throws Exception {
        deleteTestingDatabase();
    }

    protected void deleteTestingDatabase() {
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(DB_NAME);
    }
}
