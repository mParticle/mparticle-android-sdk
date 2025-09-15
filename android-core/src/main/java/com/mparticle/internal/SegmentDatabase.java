package com.mparticle.internal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Separate database dedicated to storing segment membership information.
 *
 * The reason it's separate is that a client/customer may want to query this at any time. Particularly for ad-display
 * it's crucial that those queries return as quickly as possible. Keeping this as a separate database allows for the SDK
 * to write/read from the primary database in parallel without worrying about getting in the way.
 */
/* package-private */class SegmentDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "mparticle_segment.db";

    interface SegmentTable {
        String TABLE_NAME = "segments";
        String SEGMENT_ID = "_id";
        String NAME = "name";
        String ENDPOINTS = "endpoint_ids";
    }

    private static final String CREATE_SEGMENT_DDL =
            "CREATE TABLE IF NOT EXISTS " + SegmentTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY, " +
                    SegmentTable.NAME + " TEXT NOT NULL, " +
                    SegmentTable.ENDPOINTS + " TEXT " +
                    ");";

    interface SegmentMembershipTable {
        String TABLE_NAME = "segment_memberships";
        String ID = "_id";
        String SEGMENT_ID = "segment_id";
        String TIMESTAMP = "timestamp";
        String MEMBERSHIP_ACTION = "action";
    }

    private static final String CREATE_SEGMENT_MEMBERSHIP_DDL =
            "CREATE TABLE IF NOT EXISTS " + SegmentMembershipTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SegmentMembershipTable.SEGMENT_ID + " INTEGER NOT NULL, " +
                    SegmentMembershipTable.TIMESTAMP + " REAL NOT NULL, " +
                    SegmentMembershipTable.MEMBERSHIP_ACTION + " INTEGER NOT NULL, " +
                    " FOREIGN KEY (" + SegmentMembershipTable.SEGMENT_ID + ") REFERENCES " + SegmentTable.TABLE_NAME + " (" + SegmentTable.SEGMENT_ID + "));";


    SegmentDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SEGMENT_DDL);
        db.execSQL(CREATE_SEGMENT_MEMBERSHIP_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CREATE_SEGMENT_DDL);
        db.execSQL(CREATE_SEGMENT_MEMBERSHIP_DDL);
    }

}
