package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class MessageTable extends MpIdDependentTable {

    public static final String ADD_DATAPLAN_VERSION_COLUMN = "ALTER TABLE " + MessageTableColumns.TABLE_NAME +
            " ADD COLUMN " + MessageTableColumns.DATAPLAN_VERSION + " NUMBER";
    public static final String ADD_DATAPLAN_ID_COLUMN = "ALTER TABLE " + MessageTableColumns.TABLE_NAME +
            " ADD COLUMN " + MessageTableColumns.DATAPLAN_ID + " TEXT";

    @Override
    public String getTableName() {
        return MessageTableColumns.TABLE_NAME;
    }

    public interface MessageTableColumns extends BaseColumns {
        String TABLE_NAME = "messages";
        String SESSION_ID = "session_id";
        String API_KEY = "api_key";
        String MESSAGE = "message";
        String STATUS = "upload_status";
        String CREATED_AT = "message_time";
        String MESSAGE_TYPE = "message_type";
        String CF_UUID = "cfuuid";
        String MP_ID = MpIdDependentTable.MP_ID;
        String DATAPLAN_VERSION = "dataplan_version";
        String DATAPLAN_ID = "dataplan_id";
    }

    static final String CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                    MessageTableColumns.MESSAGE + " TEXT, " +
                    MessageTableColumns.STATUS + " INTEGER, " +
                    MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                    MessageTableColumns.CF_UUID + " TEXT, " +
                    MessageTableColumns.MP_ID + " INTEGER, " +
                    MessageTableColumns.DATAPLAN_ID + " TEXT," +
                    MessageTableColumns.DATAPLAN_VERSION + " INTEGER" +
                    ");";

}
