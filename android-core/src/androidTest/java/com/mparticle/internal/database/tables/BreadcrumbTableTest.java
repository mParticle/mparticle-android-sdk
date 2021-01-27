package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class BreadcrumbTableTest extends BaseTableTest{
    public static final String old_CREATE_BREADCRUMBS_DDL =
            "CREATE TABLE IF NOT EXISTS " + BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    BreadcrumbTable.BreadcrumbTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    BreadcrumbTable.BreadcrumbTableColumns.API_KEY + " STRING NOT NULL, " +
                    BreadcrumbTable.BreadcrumbTableColumns.MESSAGE + " TEXT, " +
                    BreadcrumbTable.BreadcrumbTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    BreadcrumbTable.BreadcrumbTableColumns.CF_UUID + " TEXT" +
                    ");";

    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

            }

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {

            }
        });
    }
}
