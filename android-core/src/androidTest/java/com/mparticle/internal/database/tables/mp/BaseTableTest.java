package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.mparticle.BaseCleanInstallEachTest;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseTableTest extends BaseCleanInstallEachTest {
    public static final String DB_NAME = "test_database";
    CountDownLatch timer = new CountDownLatch(1);



    protected void runTest(SQLiteOpenHelperWrapper helper) throws InterruptedException {
        SQLiteDatabase baseDatabase = new BaseDatabase(helper, DB_NAME, timer, 6).getWritableDatabase();
        timer.await(5, TimeUnit.SECONDS);
        baseDatabase = new BaseDatabase(helper, DB_NAME, timer, MParticleDatabaseHelper.DB_VERSION).getWritableDatabase();
        timer.await(5, TimeUnit.SECONDS);
        InstrumentationRegistry.getTargetContext().deleteDatabase(DB_NAME);
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {

    }
}
