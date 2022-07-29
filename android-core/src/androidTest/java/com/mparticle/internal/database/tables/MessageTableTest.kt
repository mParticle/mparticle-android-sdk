package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.junit.Test

class MessageTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(object : SQLiteOpenHelperWrapper {
            override fun onCreate(database: SQLiteDatabase) {
                database.execSQL(MessageTable.CREATE_MESSAGES_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // do nothing
            }

            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun addDataplanColumnsTest() {
        runTest(object : SQLiteOpenHelperWrapper {
            override fun onCreate(database: SQLiteDatabase) {
                database.execSQL(old_no_dp_CREATE_MESSAGES_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                database.execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)
                database.execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN)
            }

            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
    }

    companion object {
        const val old_no_mpid_CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
                MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
                MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                MessageTable.MessageTableColumns.CF_UUID + " TEXT" +
                ");"
        private const val old_no_dp_CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
                MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
                MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                MessageTable.MessageTableColumns.CF_UUID + " TEXT, " +
                MessageTable.MessageTableColumns.MP_ID + " INTEGER " +
                ");"
    }
}
