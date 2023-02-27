package com.mparticle.internal.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.testutils.MPLatch
import java.util.concurrent.CountDownLatch

class TestSQLiteOpenHelper @JvmOverloads constructor(
    var helper: SQLiteOpenHelperWrapper,
    databaseName: String?,
    version: Int = 1
) : SQLiteOpenHelper(
    InstrumentationRegistry.getInstrumentation().context,
    databaseName,
    null,
    version
) {
    @JvmField
    var onCreateLatch: CountDownLatch = MPLatch(1)

    @JvmField
    var onUpgradeLatch: CountDownLatch = MPLatch(1)

    @JvmField
    var onDowngradeLatch: CountDownLatch = MPLatch(1)

    init {
        writableDatabase
    }

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
}
