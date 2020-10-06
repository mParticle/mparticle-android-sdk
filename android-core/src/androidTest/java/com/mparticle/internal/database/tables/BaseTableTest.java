package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Before;

import com.mparticle.testutils.MPLatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseTableTest extends BaseCleanInstallEachTest {
    public static final String DB_NAME = "test_database";
    CountDownLatch timer = new MPLatch(1);

    protected void runTest(SQLiteOpenHelperWrapper helper) throws InterruptedException {
        runTest(helper, 6);
    }

    protected void runTest(SQLiteOpenHelperWrapper helper, int oldVersion) throws InterruptedException {
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(DB_NAME);
        SQLiteDatabase baseDatabase = new BaseDatabase(helper, DB_NAME, timer, oldVersion).getWritableDatabase();
        timer.await(5, TimeUnit.SECONDS);
        baseDatabase.setVersion(MParticleDatabaseHelper.DB_VERSION);
        timer.await(5, TimeUnit.SECONDS);
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
