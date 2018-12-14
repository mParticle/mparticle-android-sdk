package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class UploadTable {

    protected interface UploadTableColumns extends BaseColumns {
        String TABLE_NAME = "uploads";
        String API_KEY = "api_key";
        String MESSAGE = "message";
        String CREATED_AT = "message_time";
        String CF_UUID = "cfuuid";
        String SESSION_ID = "session_id";
    }

    static final String CREATE_UPLOADS_DDL =
            "CREATE TABLE IF NOT EXISTS " + UploadTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UploadTableColumns.API_KEY + " STRING NOT NULL, " +
                    UploadTableColumns.MESSAGE + " TEXT, " +
                    UploadTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    UploadTableColumns.CF_UUID + " TEXT, " +
                    UploadTableColumns.SESSION_ID + " TEXT" +
                    ");";
}
