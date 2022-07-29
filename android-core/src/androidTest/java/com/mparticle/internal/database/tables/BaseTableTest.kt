package com.mparticle.internal.database.tables

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.internal.database.TestSQLiteOpenHelper
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.testing.BaseTest
import org.junit.Before

open class BaseTableTest : BaseTest() {

    @Throws(InterruptedException::class)
    protected fun runTest(helper: SQLiteOpenHelperWrapper, oldVersion: Int = 6) {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
        var openHelper = TestSQLiteOpenHelper(helper, DB_NAME, oldVersion)
        openHelper.writableDatabase
        openHelper.onCreateLatch.await()
        openHelper = TestSQLiteOpenHelper(helper, DB_NAME, MParticleDatabaseHelper.DB_VERSION)
        openHelper.writableDatabase
        if (oldVersion < MParticleDatabaseHelper.DB_VERSION) {
            openHelper.onUpgradeLatch.await()
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
