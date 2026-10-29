package com.mparticle.internal.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.listeners.InternalListenerManager

open class MPDatabaseImpl(database: SQLiteDatabase?) : MPDatabase {
    internal val sqLiteDatabase: SQLiteDatabase? = database

    override fun insert(table: String?, nullColumnHack: String?, contentValues: ContentValues?): Long {
        val row = sqLiteDatabase!!.insert(table, nullColumnHack, contentValues)
        if (InternalListenerManager.isEnabled) {
            if (row >= 0) {
                InternalListenerManager.listener.onEntityStored(row, table!!, contentValues)
            }
        }
        return row
    }

    override fun rawQuery(query: String?, vararg selectionArgs: String?): Cursor = sqLiteDatabase!!.rawQuery(query, selectionArgs)

    override fun query(
        table: String?,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?,
    ): Cursor {
        var queryColumns = columns
        if (InternalListenerManager.isEnabled) {
            queryColumns = getColumnsWithId(queryColumns)
        }
        val cursor = sqLiteDatabase!!.query(table, queryColumns, selection, selectionArgs, groupBy, having, orderBy, limit)
        if (InternalListenerManager.isEnabled) {
            val columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            if (columnIndex >= 0 && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    // Just a note, the Cursor is composited based on it's current position, not the entire object.
                    InternalListenerManager.listener.onCompositeObjects(table + cursor.getInt(columnIndex), cursor)
                    cursor.moveToNext()
                }
            }
            cursor.moveToFirst()
            cursor.move(-1)
        }
        return cursor
    }

    override fun query(
        table: String?,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
    ): Cursor {
        var queryColumns = columns
        if (InternalListenerManager.isEnabled) {
            queryColumns = getColumnsWithId(queryColumns)
        }
        val cursor = sqLiteDatabase!!.query(table, queryColumns, selection, selectionArgs, groupBy, having, orderBy)
        if (InternalListenerManager.isEnabled) {
            val columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            if (columnIndex >= 0 && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    InternalListenerManager.listener.onCompositeObjects(table + cursor.getInt(columnIndex), cursor)
                    cursor.moveToNext()
                }
            }
            cursor.moveToFirst()
            cursor.move(-1)
        }
        return cursor
    }

    private fun getColumnsWithId(columns: Array<String?>?): Array<String?>? {
        if (columns == null) {
            return columns
        }
        var found = false
        for (column in columns) {
            // A null column name throws NullPointerException here, as it always has.
            if (column!! == BaseColumns._ID) {
                found = true
            }
        }
        if (!found) {
            val list: MutableList<String?> = ArrayList(columns.asList())
            list.add(BaseColumns._ID)
            return list.toTypedArray()
        }
        return columns
    }

    override fun delete(table: String?, whereClause: String?, whereArgs: Array<String?>?): Int =
        sqLiteDatabase!!.delete(table, whereClause, whereArgs)

    override fun beginTransaction() {
        sqLiteDatabase!!.beginTransaction()
    }

    override fun setTransactionSuccessful() {
        sqLiteDatabase!!.setTransactionSuccessful()
    }

    override fun endTransaction() {
        sqLiteDatabase!!.endTransaction()
    }

    override fun update(tableName: String?, contentValues: ContentValues?, s: String?, strings: Array<String?>?): Int =
        sqLiteDatabase!!.update(tableName, contentValues, s, strings)
}
