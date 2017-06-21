package com.mparticle.internal.database.services.mp;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

import org.junit.BeforeClass;

public class BaseMPServiceTest {
    protected static SQLiteDatabase database;

    @BeforeClass
    public static void setup() {
        InstrumentationRegistry.getTargetContext().deleteDatabase(MParticleDatabaseHelper.DB_NAME);
        SQLiteOpenHelper openHelper = new BaseDatabase(new MParticleDatabaseHelper(InstrumentationRegistry.getContext()), MParticleDatabaseHelper.DB_NAME);
        database = openHelper.getWritableDatabase();
    }

    protected void clearDatabase() {
        setup();
    }

}
