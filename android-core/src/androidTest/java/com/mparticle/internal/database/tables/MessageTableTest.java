package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.os.Message;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class MessageTableTest extends BaseTableTest {
    public static final String old_no_mpid_CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                    MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
                    MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
                    MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                    MessageTable.MessageTableColumns.CF_UUID + " TEXT" +
                    ");";

   private static final String old_no_dp_CREATE_MESSAGES_DDL = "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " +
    MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
    MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
    MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
    MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
    MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
    MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
    MessageTable.MessageTableColumns.CF_UUID + " TEXT, " +
    MessageTable.MessageTableColumns.MP_ID + " INTEGER " +
            ");";


    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(MessageTable.CREATE_MESSAGES_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                //do nothing
            }

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {

            }
        });
    }

    @Test
    public void addDataplanColumnsTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(old_no_dp_CREATE_MESSAGES_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN);
                database.execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN);
            }

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {

            }
        });
    }
}
