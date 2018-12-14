package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class SessionTableTest extends BaseTableTest {
    private static final String old_CREATE_SESSION_DDL =
            "CREATE TABLE IF NOT EXISTS " + SessionTable.SessionTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SessionTable.SessionTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    SessionTable.SessionTableColumns.API_KEY + " STRING NOT NULL, " +
                    SessionTable.SessionTableColumns.START_TIME + " INTEGER NOT NULL," +
                    SessionTable.SessionTableColumns.END_TIME + " INTEGER NOT NULL," +
                    SessionTable.SessionTableColumns.SESSION_FOREGROUND_LENGTH + " INTEGER NOT NULL," +
                    SessionTable.SessionTableColumns.ATTRIBUTES + " TEXT, " +
                    SessionTable.SessionTableColumns.STATUS + " TEXT," +
                    SessionTable.SessionTableColumns.APP_INFO + " TEXT, " +
                    SessionTable.SessionTableColumns.DEVICE_INFO + " TEXT" +
                    ");";

    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(SessionTable.CREATE_SESSIONS_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

            }
        });
    }

    @Test
    public void addMpIdColumnTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(old_CREATE_SESSION_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(SessionTable.getAddMpIdColumnString("1"));
            }
        });
    }
}
