package com.mparticle.internal.database

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.internal.database.tables.BaseTableTest
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import com.mparticle.internal.database.tables.MessageTable
import org.junit.Test

class UpgradeMessageTableTest : BaseTableTest() {
    private val helper = MParticleDatabaseHelper(mContext)

    @Test
    @Throws(InterruptedException::class)
    fun addMessageTableTest() {
        // test to make sure it doesn't crash when there is an FcmMessages table to delete
        runTest(
            object : SQLiteOpenHelperWrapper {
                override fun onCreate(database: SQLiteDatabase) {
                    database.execSQL(CREATE_MESSAGES_DDL)
                    helper.onCreate(database)
                }

                override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    helper.onUpgrade(database, oldVersion, newVersion)
                }

                override fun onDowngrade(
                    database: SQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    helper.onDowngrade(database, oldVersion, newVersion)
                }
            },
            9
        )
        deleteTestingDatabase()

        // test to make sure it doesn't crash when there is NO FcmMessages table to delete
        runTest(helper, 8)
    }

    companion object {
        const val CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
                MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
                MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                MessageTable.MessageTableColumns.CF_UUID + " TEXT, " +
                MessageTable.MessageTableColumns.MP_ID + " INTEGER" +
                ");"
    }
}
