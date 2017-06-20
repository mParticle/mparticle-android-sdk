package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class MessageTableTest extends BaseTableTest {
   private static final String old_CREATE_MESSAGES_DDL =
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
        });
    }

    @Test
    public void addMpIdColumnTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(old_CREATE_MESSAGES_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(MessageTable.getAddMpIdColumnString("1"));
            }
        });
    }
}
