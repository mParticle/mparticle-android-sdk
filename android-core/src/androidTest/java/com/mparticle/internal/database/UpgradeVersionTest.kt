package com.mparticle.internal.database

import android.database.sqlite.SQLiteDatabase
import android.location.Location
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.services.BreadcrumbService
import com.mparticle.internal.database.services.MessageService
import com.mparticle.internal.database.services.ReportingService
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.internal.database.services.SessionService
import com.mparticle.internal.database.services.UploadService
import com.mparticle.internal.database.services.UserAttributesService
import com.mparticle.internal.database.tables.BaseTableTest
import com.mparticle.internal.database.tables.BreadcrumbTableTest
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import com.mparticle.internal.database.tables.MessageTableTest
import com.mparticle.internal.database.tables.MpIdDependentTable
import com.mparticle.internal.database.tables.ReportingTableTest
import com.mparticle.internal.database.tables.SessionTableTest
import com.mparticle.internal.database.tables.UserAttributeTableTest
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.testing.context
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class UpgradeVersionTest : BaseTableTest() {
    private val helper = MParticleDatabaseHelper(context)

    protected interface FcmMessageTableColumns {
        companion object {
            const val CONTENT_ID = "content_id"
            const val CAMPAIGN_ID = "campaign_id"
            const val TABLE_NAME = "gcm_messages"
            const val PAYLOAD = "payload"
            const val CREATED_AT = "message_time"
            const val DISPLAYED_AT = "displayed_time"
            const val EXPIRATION = "expiration"
            const val BEHAVIOR = "behavior"
            const val APPSTATE = "appstate"
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDropFcmMessageTable() {
        // test to make sure it doesn't crash when there is an FcmMessages table to delete
        runTest(
            object : SQLiteOpenHelperWrapper {
                override fun onCreate(database: SQLiteDatabase) {
                    database.execSQL(CREATE_GCM_MSG_DDL)
                    helper.onCreate(database)
                }

                override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    helper.onUpgrade(database, oldVersion, newVersion)
                }

                override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    helper.onDowngrade(database, oldVersion, newVersion)
                }
            },
            7
        )
        deleteTestingDatabase()

        // test to make sure it doesn't crash when there is NO FcmMessages table to delete
        runTest(helper, 7)
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun testDowngradeTable() {

        // Open database and insert some values
        var baseDatabase =
            TestSQLiteOpenHelper(helper, BaseTableTest.DB_NAME, Int.MAX_VALUE)
        baseDatabase.onCreateLatch.await()
        var db = MPDatabaseImpl(baseDatabase.writableDatabase)
        val message: BaseMPMessage =
            BaseMPMessage.Builder("test").build(InternalSession(), Location("New York City"), 1)
        BreadcrumbService.insertBreadcrumb(db, context, message, "key", 1L)
        MessageService.insertMessage(db, "key", message, 1L, "id", 1)
        ReportingService.insertReportingMessage(
            db,
            ReportingTableTest.getRandomReportingMessage("123"),
            1L
        )
        SessionService.insertSession(db, message, "key", "", "", 1L)
        UploadService.insertAliasRequest(db, "key", JSONObject().put("key", "value"))
        UserAttributesService.insertAttribute(db, "key", "value", 1L, false, 1L)

        // test to make sure there are values in the database
        var databaseContents = mockingPlatforms.getDatabaseContents(db as MPDatabase)
        Assert.assertEquals(6, databaseContents.size)
        databaseContents.entries.forEach {
            Assert.assertEquals(
                it.key,
                1,
                it.value.size
            )
        }

        // reopen the database, make sure nothing happens on a normal install
        baseDatabase = TestSQLiteOpenHelper(helper, BaseTableTest.DB_NAME, Int.MAX_VALUE)
        db = MPDatabaseImpl(baseDatabase.writableDatabase)

        // test to make sure the values are still in the database
        databaseContents = mockingPlatforms.getDatabaseContents(db as MPDatabase)
        Assert.assertEquals(6, databaseContents.size.toLong())
        databaseContents.forEach {
            Assert.assertEquals(it.key, 1, it.value.size)
        }

        // downgrade the database
        baseDatabase = TestSQLiteOpenHelper(helper, BaseTableTest.DB_NAME, 1)
        baseDatabase.writableDatabase
        baseDatabase.onDowngradeLatch.await()
        db = MPDatabaseImpl(baseDatabase.writableDatabase)

        // test to make sure the values where delete and the database is empty
        databaseContents = mockingPlatforms.getDatabaseContents(db as MPDatabase)
        Assert.assertEquals(6, databaseContents.size)
        databaseContents.forEach {
            Assert.assertEquals(it.key, 0, it.value.size)
        }
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun testAddMpidColumns() {
        val sqLiteOpenHelperWrapper: SQLiteOpenHelperWrapper = object : SQLiteOpenHelperWrapper {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(com.mparticle.internal.database.tables.SessionTableTest.old_CREATE_SESSION_DDL)
                db.execSQL(MessageTableTest.old_no_mpid_CREATE_MESSAGES_DDL)
                db.execSQL(BreadcrumbTableTest.old_CREATE_BREADCRUMBS_DDL)
                db.execSQL(ReportingTableTest.old_CREATE_REPORTING_DDL)
                db.execSQL(UserAttributeTableTest.old_CREATE_USER_ATTRIBUTES_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                helper.onUpgrade(database, oldVersion, newVersion)
            }

            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                helper.onDowngrade(database, oldVersion, newVersion)
            }
        }
        var sqLiteOpenHelper =
            TestSQLiteOpenHelper(sqLiteOpenHelperWrapper, BaseTableTest.DB_NAME, 6)
        sqLiteOpenHelper.onCreateLatch.await()
        var databaseContents = mockingPlatforms.getDatabaseSchema(sqLiteOpenHelper.writableDatabase)
        databaseContents.forEach { Assert.assertFalse(it.value.contains(MpIdDependentTable.MP_ID)) }

        sqLiteOpenHelper =
            TestSQLiteOpenHelper(sqLiteOpenHelperWrapper, BaseTableTest.DB_NAME, 8)
        sqLiteOpenHelper.writableDatabase
        sqLiteOpenHelper.onUpgradeLatch.await()
        databaseContents = mockingPlatforms.getDatabaseSchema(sqLiteOpenHelper.writableDatabase)
        databaseContents.forEach {
            if (it.key == "uploads") {
                Assert.assertFalse(it.value.contains(MpIdDependentTable.MP_ID))
            } else {
                Assert.assertTrue(it.value.contains(MpIdDependentTable.MP_ID))
            }
        }
    }

    companion object {
        const val CREATE_GCM_MSG_DDL =
            "CREATE TABLE IF NOT EXISTS " + FcmMessageTableColumns.TABLE_NAME + " (" + FcmMessageTableColumns.CONTENT_ID +
                " INTEGER PRIMARY KEY, " +
                FcmMessageTableColumns.PAYLOAD + " TEXT NOT NULL, " +
                FcmMessageTableColumns.APPSTATE + " TEXT NOT NULL, " +
                FcmMessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                FcmMessageTableColumns.EXPIRATION + " INTEGER NOT NULL, " +
                FcmMessageTableColumns.BEHAVIOR + " INTEGER NOT NULL," +
                FcmMessageTableColumns.CAMPAIGN_ID + " TEXT NOT NULL, " +
                FcmMessageTableColumns.DISPLAYED_AT + " INTEGER NOT NULL " +
                ");"
    }
}
