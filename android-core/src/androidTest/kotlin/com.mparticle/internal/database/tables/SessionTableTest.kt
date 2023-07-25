package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.junit.Test

class SessionTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(object : SQLiteOpenHelperWrapper {
            override fun onCreate(database: SQLiteDatabase) {
                database.execSQL(SessionTable.CREATE_SESSIONS_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
    }

    companion object {
        const val old_CREATE_SESSION_DDL =
            "CREATE TABLE IF NOT EXISTS " + SessionTable.SessionTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SessionTable.SessionTableColumns.SESSION_ID + " STRING NOT NULL, " +
                SessionTable.SessionTableColumns.API_KEY + " STRING NOT NULL, " +
                SessionTable.SessionTableColumns.START_TIME + " INTEGER NOT NULL," +
                SessionTable.SessionTableColumns.END_TIME + " INTEGER NOT NULL," +
                SessionTable.SessionTableColumns.SESSION_FOREGROUND_LENGTH + " INTEGER NOT NULL," +
                SessionTable.SessionTableColumns.ATTRIBUTES + " TEXT, " +
                SessionTable.SessionTableColumns.STATUS + " TEXT," +
                SessionTable.SessionTableColumns.APP_INFO + " TEXT, " +
                SessionTable.SessionTableColumns.DEVICE_INFO + " TEXT" +
                ");"
    }
}
