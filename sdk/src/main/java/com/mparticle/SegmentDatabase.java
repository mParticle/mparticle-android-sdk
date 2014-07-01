package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* package-private */class SegmentDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "mparticle_segment.db";

    interface SegmentTable {
        public final static String TABLE_NAME = "segments";
        public final static String SEGMENT_ID = "_id";
        public final static String NAME = "name";
        public final static String ENDPOINTS = "endpoint_ids";
    }

    private static final String CREATE_SEGMENT_DDL =
            "CREATE TABLE IF NOT EXISTS " + SegmentTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY, " +
                    SegmentTable.NAME + " TEXT NOT NULL, " +
                    SegmentTable.ENDPOINTS + " TEXT " +
                    ");";

    interface SegmentMembershipTable {
        public final static String TABLE_NAME = "segment_memberships";
        public final static String ID = "_id";
        public final static String SEGMENT_ID = "segment_id";
        public final static String TIMESTAMP = "timestamp";
        public final static String MEMBERSHIP_ACTION = "action";
    }

    private static final String CREATE_SEGMENT_MEMBERSHIP_DDL =
            "CREATE TABLE IF NOT EXISTS " + SegmentMembershipTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SegmentMembershipTable.SEGMENT_ID + " INTEGER NOT NULL, " +
                    SegmentMembershipTable.TIMESTAMP + " REAL NOT NULL, " +
                    SegmentMembershipTable.MEMBERSHIP_ACTION + " INTEGER NOT NULL, " +
                    " FOREIGN KEY ("+ SegmentMembershipTable.SEGMENT_ID+") REFERENCES "+ SegmentTable.TABLE_NAME+" ("+ SegmentTable.SEGMENT_ID +"));";




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
