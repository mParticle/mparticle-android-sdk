package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(
            object : SQLiteOpenHelperWrapper {
                override fun onCreate(database: SQLiteDatabase) {
                    database.execSQL(UploadTable.CREATE_UPLOADS_DDL)
                }

                override fun onUpgrade(
                    database: SQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {
                    // do nothing
                }

                override fun onDowngrade(
                    database: SQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {}
            },
        )
    }
}
