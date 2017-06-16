package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import java.util.concurrent.CountDownLatch;

public class BaseDatabase extends SQLiteOpenHelper {
    static String DB_NAME = "atest_dateabsdase";
    SQLiteOpenHelperWrapper helper;
    CountDownLatch timer;

    BaseDatabase(SQLiteOpenHelperWrapper helper, CountDownLatch timer, int version) {
        super(InstrumentationRegistry.getTargetContext(), DB_NAME, null, version);
        this.helper = helper;
        this.timer = timer;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        helper.onCreate(db);
        timer.countDown();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        helper.onUpgrade(db, oldVersion, newVersion);
        timer.countDown();
    }
}