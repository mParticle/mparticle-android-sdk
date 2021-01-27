package com.mparticle.internal.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.testutils.MPLatch;

import java.util.concurrent.CountDownLatch;

public class TestSQLiteOpenHelper extends SQLiteOpenHelper {
    SQLiteOpenHelperWrapper helper;
    public CountDownLatch onCreateLatch;
    public CountDownLatch onUpgradeLatch;
    public CountDownLatch onDowngradeLatch;

    public TestSQLiteOpenHelper(SQLiteOpenHelperWrapper helper, String databaseName) {
        this(helper, databaseName, 1);
    }

    public TestSQLiteOpenHelper(SQLiteOpenHelperWrapper helper, String databaseName, int version) {
        super(InstrumentationRegistry.getInstrumentation().getContext(), databaseName, null, version);
        this.helper = helper;
        this.onCreateLatch = new MPLatch(1);
        this.onUpgradeLatch = new MPLatch(1);
        this.onDowngradeLatch = new MPLatch(1);
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        helper.onCreate(db);
        onCreateLatch.countDown();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        helper.onUpgrade(db, oldVersion, newVersion);
        onUpgradeLatch.countDown();
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        helper.onDowngrade(db, oldVersion, newVersion);
        onDowngradeLatch.countDown();
    }
}