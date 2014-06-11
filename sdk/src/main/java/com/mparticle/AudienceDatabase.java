package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* package-private */class AudienceDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "mparticle_audience.db";

    public interface AudienceTable {
        public final static String TABLE_NAME = "audiences";
        public final static String AUDIENCE_ID = "_id";
        public final static String NAME = "name";
        public final static String ENDPOINTS = "endpoint_ids";
    }

    private static final String CREATE_AUDIENCES_DDL =
            "CREATE TABLE IF NOT EXISTS " + AudienceTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY, " +
                    AudienceTable.NAME + " TEXT NOT NULL, " +
                    AudienceTable.ENDPOINTS + " TEXT " +
                    ");";

    public interface AudienceMembershipTable {
        public final static String TABLE_NAME = "audience_memberships";
        public final static String ID = "_id";
        public final static String AUDIENCE_ID = "audience_id";
        public final static String TIMESTAMP = "timestamp";
        public final static String MEMBERSHIP_ACTION = "action";
    }

    private static final String CREATE_AUDIENCE_MEMBERSHIP_DDL =
            "CREATE TABLE IF NOT EXISTS " + AudienceMembershipTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    AudienceMembershipTable.AUDIENCE_ID + " INTEGER NOT NULL, " +
                    AudienceMembershipTable.TIMESTAMP + " REAL NOT NULL, " +
                    AudienceMembershipTable.MEMBERSHIP_ACTION + " INTEGER NOT NULL, " +
                    " FOREIGN KEY ("+ AudienceMembershipTable.AUDIENCE_ID+") REFERENCES "+ AudienceTable.TABLE_NAME+" ("+ AudienceTable.AUDIENCE_ID+"));";




    AudienceDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_AUDIENCES_DDL);
        db.execSQL(CREATE_AUDIENCE_MEMBERSHIP_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CREATE_AUDIENCES_DDL);
        db.execSQL(CREATE_AUDIENCE_MEMBERSHIP_DDL);

    }

}
