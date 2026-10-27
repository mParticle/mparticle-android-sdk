package com.mparticle.internal.database

import android.content.ContentValues
import android.database.Cursor

interface MPDatabase {
    fun insert(table: String?, nullColumnHack: String?, contentValues: ContentValues?): Long

    fun query(
        table: String?,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?,
    ): Cursor

    fun query(
        table: String?,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
    ): Cursor

    fun rawQuery(query: String?, vararg selectionArgs: String?): Cursor

    fun delete(table: String?, whereClause: String?, whereArgs: Array<String?>?): Int

    fun beginTransaction()

    fun setTransactionSuccessful()

    fun endTransaction()

    fun update(tableName: String?, contentValues: ContentValues?, s: String?, strings: Array<String?>?): Int
}
