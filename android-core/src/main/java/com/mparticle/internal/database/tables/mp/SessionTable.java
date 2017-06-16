package com.mparticle.internal.database.tables.mp;

import android.content.Context;
import android.provider.BaseColumns;

public class SessionTable {

    public interface SessionTableColumns {
        String TABLE_NAME = "sessions";
        String SESSION_ID = "session_id";
        String API_KEY = "api_key";
        String START_TIME = "start_time";
        String END_TIME = "end_time";
        String SESSION_FOREGROUND_LENGTH = "session_length";
        String ATTRIBUTES = "attributes";
        String CF_UUID = "cfuuid";
        String APP_INFO = "app_info";
        String DEVICE_INFO = "device_info";
        String MP_ID = "mp_id";
    }

    static final String SESSION_ADD_DEVICE_INFO_COLUMN = "ALTER TABLE " + SessionTableColumns.TABLE_NAME +
            " ADD COLUMN " + SessionTableColumns.DEVICE_INFO + " TEXT";

    static final String SESSION_ADD_APP_INFO_COLUMN = "ALTER TABLE " + SessionTableColumns.TABLE_NAME +
            " ADD COLUMN " + SessionTableColumns.APP_INFO + " TEXT";

    static String getAddMpIdColumnString(String defaultValue) {
        return MParticleDatabaseHelper.addIntegerColumnString(SessionTableColumns.TABLE_NAME, SessionTableColumns.MP_ID, defaultValue);
    }

    static final String CREATE_SESSIONS_DDL =
            "CREATE TABLE IF NOT EXISTS " + SessionTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SessionTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    SessionTableColumns.API_KEY + " STRING NOT NULL, " +
                    SessionTableColumns.START_TIME + " INTEGER NOT NULL," +
                    SessionTableColumns.END_TIME + " INTEGER NOT NULL," +
                    SessionTableColumns.SESSION_FOREGROUND_LENGTH + " INTEGER NOT NULL," +
                    SessionTableColumns.ATTRIBUTES + " TEXT, " +
                    SessionTableColumns.CF_UUID + " TEXT," +
                    SessionTableColumns.APP_INFO + " TEXT, " +
                    SessionTableColumns.DEVICE_INFO + " TEXT, " +
                    SessionTableColumns.MP_ID + " INTEGER" +
                    ");";
}
