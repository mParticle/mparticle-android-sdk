package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.junit.Test

class BreadcrumbTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(object : SQLiteOpenHelperWrapper {
            override fun onCreate(database: SQLiteDatabase) {
                database.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
    }

    companion object {
        const val old_CREATE_BREADCRUMBS_DDL =
            "CREATE TABLE IF NOT EXISTS " + BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                BreadcrumbTable.BreadcrumbTableColumns.SESSION_ID + " STRING NOT NULL, " +
                BreadcrumbTable.BreadcrumbTableColumns.API_KEY + " STRING NOT NULL, " +
                BreadcrumbTable.BreadcrumbTableColumns.MESSAGE + " TEXT, " +
                BreadcrumbTable.BreadcrumbTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                BreadcrumbTable.BreadcrumbTableColumns.CF_UUID + " TEXT" +
                ");"
    }
}
