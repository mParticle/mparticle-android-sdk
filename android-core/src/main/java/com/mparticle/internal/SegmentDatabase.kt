package com.mparticle.internal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Separate database dedicated to storing segment membership information.
 *
 * The reason it's separate is that a client/customer may want to query this at any time. Particularly for ad-display
 * it's crucial that those queries return as quickly as possible. Keeping this as a separate database allows for the SDK
 * to write/read from the primary database in parallel without worrying about getting in the way.
 */
internal class SegmentDatabase(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    internal interface SegmentTable {
        companion object {
            const val TABLE_NAME: String = "segments"
            const val SEGMENT_ID: String = "_id"
            const val NAME: String = "name"
            const val ENDPOINTS: String = "endpoint_ids"
        }
    }

    internal interface SegmentMembershipTable {
        companion object {
            const val TABLE_NAME: String = "segment_memberships"
            const val ID: String = "_id"
            const val SEGMENT_ID: String = "segment_id"
            const val TIMESTAMP: String = "timestamp"
            const val MEMBERSHIP_ACTION: String = "action"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SEGMENT_DDL)
        db.execSQL(CREATE_SEGMENT_MEMBERSHIP_DDL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(CREATE_SEGMENT_DDL)
        db.execSQL(CREATE_SEGMENT_MEMBERSHIP_DDL)
    }

    companion object {
        private const val DB_VERSION: Int = 1
        private const val DB_NAME: String = "mparticle_segment.db"

        private val CREATE_SEGMENT_DDL: String =
            """
            CREATE TABLE IF NOT EXISTS ${SegmentTable.TABLE_NAME} (
                _id INTEGER PRIMARY KEY,
                ${SegmentTable.NAME} TEXT NOT NULL,
                ${SegmentTable.ENDPOINTS} TEXT
            );
            """.trimIndent()

        private val CREATE_SEGMENT_MEMBERSHIP_DDL: String =
            """
            CREATE TABLE IF NOT EXISTS ${SegmentMembershipTable.TABLE_NAME} (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                ${SegmentMembershipTable.SEGMENT_ID} INTEGER NOT NULL,
                ${SegmentMembershipTable.TIMESTAMP} REAL NOT NULL,
                ${SegmentMembershipTable.MEMBERSHIP_ACTION} INTEGER NOT NULL,
                FOREIGN KEY (${SegmentMembershipTable.SEGMENT_ID}) REFERENCES ${SegmentTable.TABLE_NAME} (${SegmentTable.SEGMENT_ID})
            );
            """.trimIndent()
    }
}
