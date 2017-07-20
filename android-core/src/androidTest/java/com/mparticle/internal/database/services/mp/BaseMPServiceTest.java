package com.mparticle.internal.database.services.mp;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.BaseCleanInstallEachTest;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

abstract public class BaseMPServiceTest extends BaseCleanInstallEachTest {
    protected static SQLiteDatabase database;

    @Override
    protected void beforeClass() throws Exception {

    }

    @CallSuper
    @Override
    protected void before() throws Exception {
        SQLiteOpenHelper openHelper = new BaseDatabase(new MParticleDatabaseHelper(InstrumentationRegistry.getContext()), MParticleDatabaseHelper.DB_NAME);
        database = openHelper.getWritableDatabase();
    }
}
