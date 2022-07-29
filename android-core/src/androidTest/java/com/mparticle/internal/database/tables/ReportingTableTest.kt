package com.mparticle.internal.database.tables

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.json.JSONObject
import org.junit.Test
import java.util.Random

class ReportingTableTest : BaseTableTest() {
    @Test
    @Throws(InterruptedException::class)
    fun createTableTest() {
        runTest(object : SQLiteOpenHelperWrapper {
            override fun onCreate(database: SQLiteDatabase) {
                database.execSQL(ReportingTable.CREATE_REPORTING_DDL)
            }

            override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
    }

    companion object {
        const val old_CREATE_REPORTING_DDL =
            "CREATE TABLE IF NOT EXISTS " + ReportingTable.ReportingTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ReportingTable.ReportingTableColumns.MODULE_ID + " INTEGER NOT NULL, " +
                ReportingTable.ReportingTableColumns.MESSAGE + " TEXT NOT NULL, " +
                ReportingTable.ReportingTableColumns.SESSION_ID + " STRING NOT NULL, " +
                ReportingTable.ReportingTableColumns.CREATED_AT + " INTEGER NOT NULL" +
                ");"

        fun getRandomReportingMessage(sessionId: String): JsonReportingMessage {
            val ran = Random()
            return object : JsonReportingMessage {
                var randomNumber = 0
                override fun setDevMode(development: Boolean) {
                    // do nothing
                }

                override fun getTimestamp(): Long {
                    return System.currentTimeMillis() - 100
                }

                override fun getModuleId(): Int {
                    return 1 // MParticle.ServiceProviders.APPBOY;
                }

                override fun toJson(): JSONObject {
                    return JSONObject()
                        .apply {
                            put("fieldOne", "a value")
                            put("fieldTwo", "another value")
                            put(
                                "a random Number",
                                if (randomNumber == -1) ran.nextInt().also {
                                    randomNumber = it
                                } else randomNumber
                            )
                        }
                }

                override fun getSessionId(): String {
                    return sessionId
                }

                override fun setSessionId(sessionId: String) {}
            }
        }
    }
}
