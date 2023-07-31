package com.mparticle.internal.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.listeners.InternalListenerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MPDatabaseImpl implements MPDatabase {
    SQLiteDatabase sqLiteDatabase;

    public MPDatabaseImpl(SQLiteDatabase database) {
        this.sqLiteDatabase = database;
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues contentValues) {
        long row = sqLiteDatabase.insert(table, nullColumnHack, contentValues);
        if (InternalListenerManager.isEnabled()) {
            if (row >= 0) {
                InternalListenerManager.getListener().onEntityStored(row, table, contentValues);
            }
        }
        return row;
    }

    @Override
    public Cursor rawQuery(String query, String... selectionArgs) {
        return sqLiteDatabase.rawQuery(query, selectionArgs);
    }

    @Override
    public Cursor query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit) {
        if (InternalListenerManager.isEnabled()) {
            columns = getColumnsWithId(columns);
        }
        Cursor cursor = sqLiteDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        if (InternalListenerManager.isEnabled()) {
            int columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            if (columnIndex >= 0 && cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    //Just a note, the Cursor is composited based on it's current position, not the entire object.
                    InternalListenerManager.getListener().onCompositeObjects(table + cursor.getInt(columnIndex), cursor);
                    cursor.moveToNext();
                }
            }
            cursor.moveToFirst();
            cursor.move(-1);
        }
        return cursor;
    }

    @Override
    public Cursor query(String table, String[] columns, String selection,
                        String[] selectionArgs, String groupBy, String having,
                        String orderBy) {
        if (InternalListenerManager.isEnabled()) {
            columns = getColumnsWithId(columns);
        }
        Cursor cursor = sqLiteDatabase.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        if (InternalListenerManager.isEnabled()) {
            int columnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            if (columnIndex >= 0 && cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    InternalListenerManager.getListener().onCompositeObjects(table + cursor.getInt(columnIndex), cursor);
                    cursor.moveToNext();
                }
            }
            cursor.moveToFirst();
            cursor.move(-1);
        }
        return cursor;
    }

    private String[] getColumnsWithId(String[] columns) {
        if (columns == null) {
            return columns;
        }
        boolean found = false;
        for (String column : columns) {
            if (column.equals(BaseColumns._ID)) {
                found = true;
            }
        }
        if (!found) {
            List<String> list = new ArrayList<String>(Arrays.asList(columns));
            list.add(BaseColumns._ID);
            columns = list.toArray(new String[columns.length + 1]);
        }
        return columns;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return sqLiteDatabase.delete(table, whereClause, whereArgs);
    }

    @Override
    public void beginTransaction() {
        sqLiteDatabase.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        sqLiteDatabase.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        sqLiteDatabase.endTransaction();
    }

    @Override
    public int update(String tableName, ContentValues contentValues, String s, String[] strings) {
        return sqLiteDatabase.update(tableName, contentValues, s, strings);
    }

}
