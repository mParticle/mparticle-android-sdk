package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class UploadTable {

    public static final String UPLOAD_REQUEST = "0";
    public static final String ALIAS_REQUEST = "1";

    protected interface UploadTableColumns extends BaseColumns {
        String TABLE_NAME = "uploads";
        String API_KEY = "api_key";
        String MESSAGE = "message";
        String CREATED_AT = "message_time";
        /**
         * This column, previously unused as CFUUID, has been re-purposed for REQUEST_TYPE
         * to avoid a schema change.
         */
        String REQUEST_TYPE = "cfuuid";
        String SESSION_ID = "session_id";
        String UPLOAD_SETTINGS = "upload_settings";
    }


    static final String CREATE_UPLOADS_DDL =
            "CREATE TABLE IF NOT EXISTS " + UploadTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UploadTableColumns.API_KEY + " STRING NOT NULL, " +
                    UploadTableColumns.MESSAGE + " TEXT, " +
                    UploadTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    UploadTableColumns.REQUEST_TYPE + " TEXT, " +
                    UploadTableColumns.SESSION_ID + " TEXT, " +
                    UploadTableColumns.UPLOAD_SETTINGS + " TEXT" +
                    ");";

    static final String UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN = "ALTER TABLE " + UploadTableColumns.TABLE_NAME +
            " ADD COLUMN " + UploadTableColumns.UPLOAD_SETTINGS + " TEXT";
}
