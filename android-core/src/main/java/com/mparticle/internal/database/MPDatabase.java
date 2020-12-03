package com.mparticle.internal.database;

import android.content.ContentValues;
import android.database.Cursor;

public interface MPDatabase {

    long insert(String table, String nullColumnHack, ContentValues contentValues);

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy, String limit);

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy);

    Cursor rawQuery(String query, String... selectionArgs);

    int delete(String table, String whereClause, String[] whereArgs);

    void beginTransaction();

    void setTransactionSuccessful();

    void endTransaction();

    int update(String tableName, ContentValues contentValues, String s, String[] strings);
}
