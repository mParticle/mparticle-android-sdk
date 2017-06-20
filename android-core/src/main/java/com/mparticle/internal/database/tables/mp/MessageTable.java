package com.mparticle.internal.database.tables.mp;

import android.content.Context;
import android.provider.BaseColumns;

public class MessageTable {
    protected interface MessageTableColumns extends BaseColumns {
        String TABLE_NAME = "messages";
        String SESSION_ID = "session_id";
        String API_KEY = "api_key";
        String MESSAGE = "message";
        String STATUS = "upload_status";
        String CREATED_AT = "message_time";
        String MESSAGE_TYPE = "message_type";
        String CF_UUID = "cfuuid";
        String MP_ID = "mpid";
    }

    static String getAddMpIdColumnString(String defaultValue) {
        return MParticleDatabaseHelper.addIntegerColumnString(MessageTable.MessageTableColumns.TABLE_NAME, MessageTable.MessageTableColumns.MP_ID, defaultValue);
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
                    MessageTableColumns.MP_ID + " INTEGER" +
                    ");";

}
