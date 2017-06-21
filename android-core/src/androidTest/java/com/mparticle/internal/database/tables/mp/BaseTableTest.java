package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.BeforeClass;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseTableTest {
    private static final String DB_NAME = "test_database";
    CountDownLatch timer = new CountDownLatch(1);

    @BeforeClass
    public static void setup() {
//        Looper.prepare();
    }

    protected void runTest(SQLiteOpenHelperWrapper helper) throws InterruptedException {
        InstrumentationRegistry.getTargetContext().deleteDatabase(DB_NAME);
        SQLiteDatabase baseDatabase = new BaseDatabase(helper, DB_NAME, timer, 1).getWritableDatabase();
        timer.await(5, TimeUnit.SECONDS);
        baseDatabase = new BaseDatabase(helper, DB_NAME, timer, 2).getWritableDatabase();
        timer.await(5, TimeUnit.SECONDS);

    }
}
