package com.mparticle.internal.database.services;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.DatabaseTables;

public class BaseDBManager {
    private DatabaseTables mDatabaseTables;
    protected Context mContext;

    public BaseDBManager(Context context, DatabaseTables databaseTables) {
        mContext = context;
        mDatabaseTables = databaseTables;
    }

    protected SQLiteDatabase getMParticleDatabase() {
        return mDatabaseTables.getMParticleDatabase();
    }

}
