package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class ReportingTableTest extends BaseTableTest {

    private static final String old_CREATE_REPORTING_DDL =
            "CREATE TABLE IF NOT EXISTS " + ReportingTable.ReportingTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ReportingTable.ReportingTableColumns.MODULE_ID + " INTEGER NOT NULL, " +
                    ReportingTable.ReportingTableColumns.MESSAGE + " TEXT NOT NULL, " +
                    ReportingTable.ReportingTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    ReportingTable.ReportingTableColumns.CREATED_AT + " INTEGER NOT NULL" +
                    ");";

    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(ReportingTable.CREATE_REPORTING_DDL);
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
                database.execSQL(old_CREATE_REPORTING_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(ReportingTable.getAddMpIdColumnString("1"));
            }
        });
    }
}
