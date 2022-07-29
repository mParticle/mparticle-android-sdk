package com.mparticle.internal.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.testing.FailureLatch

class TestSQLiteOpenHelper @JvmOverloads constructor(
    helper: SQLiteOpenHelperWrapper,
    databaseName: String?,
    version: Int = 1
) : SQLiteOpenHelper(
    InstrumentationRegistry.getInstrumentation().context, databaseName, null, version
) {
    var helper: SQLiteOpenHelperWrapper = helper
    var onCreateLatch = FailureLatch()
    var onUpgradeLatch = FailureLatch()
    var onDowngradeLatch = FailureLatch()
    override fun onCreate(db: SQLiteDatabase) {
        helper.onCreate(db)
        onCreateLatch.countDown()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        helper.onUpgrade(db, oldVersion, newVersion)
        onUpgradeLatch.countDown()
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        helper.onDowngrade(db, oldVersion, newVersion)
        onDowngradeLatch.countDown()
    }

    init {
        writableDatabase
    }
}
