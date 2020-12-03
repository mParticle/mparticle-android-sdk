package com.mparticle.internal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    Context mContext;
    SQLiteOpenHelperWrapper sqLiteOpenHelperWrapper;

    public DatabaseHelper(Context context) {
        super(context, MParticleDatabaseHelper.getDbName(), null, MParticleDatabaseHelper.DB_VERSION);
        mContext = context;
        this.sqLiteOpenHelperWrapper = getSQLiteOpenHelperWrapper();
    }

    protected SQLiteOpenHelperWrapper getSQLiteOpenHelperWrapper() {
        return new MParticleDatabaseHelper(mContext);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        sqLiteOpenHelperWrapper.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        sqLiteOpenHelperWrapper.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        sqLiteOpenHelperWrapper.onDowngrade(db, oldVersion, newVersion);
    }
}
