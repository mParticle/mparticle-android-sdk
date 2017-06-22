package com.mparticle.internal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

public class DatabaseTables {


    private SQLiteOpenHelper mMParticleDatabase;
    private static DatabaseTables instance;

    public static DatabaseTables getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseTables(context);
        }
        return instance;
    }

    private DatabaseTables(Context context) {

        mMParticleDatabase = new MParticleDatabase(context);
    }

    public SQLiteDatabase getMParticleDatabase() {
        if (mMParticleDatabase != null) {
            return mMParticleDatabase.getWritableDatabase();
        }
        return null;
    }

    class MParticleDatabase extends AbstractDatabase {
        private Context context;

        public MParticleDatabase(Context context) {
            super(context, MParticleDatabaseHelper.DB_NAME, null, MParticleDatabaseHelper.DB_VERSION);
            this.context = context;
        }


        @Override
        SQLiteOpenHelperWrapper getSQLiteOpenHelperWrapper() {
            return new MParticleDatabaseHelper(context);
        }
    }

    abstract class AbstractDatabase extends SQLiteOpenHelper {
        SQLiteOpenHelperWrapper sqLiteOpenHelperWrapper;

        public AbstractDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            this.sqLiteOpenHelperWrapper = getSQLiteOpenHelperWrapper();
        }

        abstract SQLiteOpenHelperWrapper getSQLiteOpenHelperWrapper();

        @Override
        public void onCreate(SQLiteDatabase db) {
            sqLiteOpenHelperWrapper.onCreate(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            sqLiteOpenHelperWrapper.onUpgrade(db, oldVersion, newVersion);
        }
    }
}
