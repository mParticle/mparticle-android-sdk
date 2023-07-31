package com.mparticle.internal.database.tables

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.internal.database.TestSQLiteOpenHelper
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Before
import java.util.concurrent.CountDownLatch

open class BaseTableTest : BaseCleanInstallEachTest() {
    var onCreateLatch: CountDownLatch = MPLatch(1)
    var onUpgradeLatch: CountDownLatch = MPLatch(1)

    @Throws(InterruptedException::class)
    protected fun runTest(helper: SQLiteOpenHelperWrapper?, oldVersion: Int = 6) {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
        var openHelper = helper?.let { TestSQLiteOpenHelper(it, DB_NAME, oldVersion) }
        openHelper?.writableDatabase
        openHelper?.onCreateLatch?.await()
        openHelper =
            helper?.let { TestSQLiteOpenHelper(it, DB_NAME, MParticleDatabaseHelper.DB_VERSION) }
        openHelper?.writableDatabase
        if (oldVersion < MParticleDatabaseHelper.DB_VERSION) {
            openHelper?.onUpgradeLatch?.await()
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    @Before
    @Throws(Exception::class)
    fun before() {
        deleteTestingDatabase()
    }

    protected fun deleteTestingDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    companion object {
        const val DB_NAME = "test_database"
    }
}
