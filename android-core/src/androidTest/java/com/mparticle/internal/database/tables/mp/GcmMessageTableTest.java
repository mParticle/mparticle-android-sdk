package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class GcmMessageTableTest extends BaseTableTest {
    private static final String old_CREATE_GCM_MSG_DDL =
            "CREATE TABLE IF NOT EXISTS " + GcmMessageTable.GcmMessageTableColumns.TABLE_NAME + " (" + GcmMessageTable.GcmMessageTableColumns.CONTENT_ID +
                    " INTEGER PRIMARY KEY, " +
                    GcmMessageTable.GcmMessageTableColumns.PAYLOAD + " TEXT NOT NULL, " +
                    GcmMessageTable.GcmMessageTableColumns.APPSTATE + " TEXT NOT NULL, " +
                    GcmMessageTable.GcmMessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    GcmMessageTable.GcmMessageTableColumns.EXPIRATION + " INTEGER NOT NULL, " +
                    GcmMessageTable.GcmMessageTableColumns.BEHAVIOR + " INTEGER NOT NULL," +
                    GcmMessageTable.GcmMessageTableColumns.CAMPAIGN_ID + " TEXT NOT NULL, " +
                    GcmMessageTable.GcmMessageTableColumns.DISPLAYED_AT + " INTEGER NOT NULL" +
                    ");";

    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(GcmMessageTable.CREATE_GCM_MSG_DDL);
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
                database.execSQL(old_CREATE_GCM_MSG_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(GcmMessageTable.getAddMpIdColumnString("1"));
            }
        });
    }
}
