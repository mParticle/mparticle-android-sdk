package com.mparticle.internal.database.services;

import android.database.sqlite.SQLiteDatabase;

public interface SQLiteOpenHelperWrapper {

    void onCreate(SQLiteDatabase database);

    void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion);

    void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion);
}
