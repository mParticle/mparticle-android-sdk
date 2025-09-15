package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class SessionTable extends MpIdDependentTable {

    @Override
    public String getTableName() {
        return SessionTableColumns.TABLE_NAME;
    }

    public interface SessionTableColumns {
        String TABLE_NAME = "sessions";
        String SESSION_ID = "session_id";
        String API_KEY = "api_key";
        String START_TIME = "start_time";
        String END_TIME = "end_time";
        String SESSION_FOREGROUND_LENGTH = "session_length";
        String ATTRIBUTES = "attributes";
        /**
         * This column, previously unused as CFUUID, has been re-purposed for STATUS
         * to avoid a schema change.
         */
        String STATUS = "cfuuid";
        String APP_INFO = "app_info";
        String DEVICE_INFO = "device_info";
        String MP_ID = MpIdDependentTable.MP_ID;
    }

    /**
     * SessionStatus is used to determine if we've "closed" a session by
     * logging a session-end event. Sessions are kept in the database after
     * this, due to requiring device and application information at the time
     * of batch creation.
     **/
    public interface SessionStatus {
        String CLOSED = "1";
    }

    static final String SESSION_ADD_DEVICE_INFO_COLUMN = "ALTER TABLE " + SessionTableColumns.TABLE_NAME +
            " ADD COLUMN " + SessionTableColumns.DEVICE_INFO + " TEXT";

    static final String SESSION_ADD_APP_INFO_COLUMN = "ALTER TABLE " + SessionTableColumns.TABLE_NAME +
            " ADD COLUMN " + SessionTableColumns.APP_INFO + " TEXT";

    static final String CREATE_SESSIONS_DDL =
            "CREATE TABLE IF NOT EXISTS " + SessionTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SessionTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    SessionTableColumns.API_KEY + " STRING NOT NULL, " +
                    SessionTableColumns.START_TIME + " INTEGER NOT NULL," +
                    SessionTableColumns.END_TIME + " INTEGER NOT NULL," +
                    SessionTableColumns.SESSION_FOREGROUND_LENGTH + " INTEGER NOT NULL," +
                    SessionTableColumns.ATTRIBUTES + " TEXT, " +
                    SessionTableColumns.STATUS + " TEXT," +
                    SessionTableColumns.APP_INFO + " TEXT, " +
                    SessionTableColumns.DEVICE_INFO + " TEXT, " +
                    SessionTableColumns.MP_ID + " INTEGER" +
                    ");";
}
