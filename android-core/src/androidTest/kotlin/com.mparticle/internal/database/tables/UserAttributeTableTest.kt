package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserAttributeTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(
            object : SQLiteOpenHelperWrapper {
                override fun onCreate(database: SQLiteDatabase) {
                    database.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL)
                }

                override fun onUpgrade(
                    database: SQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {}

                override fun onDowngrade(
                    database: SQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {}
            },
        )
    }

    companion object {
        const val OLD_CREATE_USER_ATTRIBUTES_DDL =
            "CREATE TABLE IF NOT EXISTS " + UserAttributesTable.UserAttributesTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_KEY + " COLLATE NOCASE NOT NULL, " +
                UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_VALUE + " TEXT, " +
                UserAttributesTable.UserAttributesTableColumns.IS_LIST + " INTEGER NOT NULL, " +
                UserAttributesTable.UserAttributesTableColumns.CREATED_AT + " INTEGER NOT NULL " +
                ");"
    }
}
