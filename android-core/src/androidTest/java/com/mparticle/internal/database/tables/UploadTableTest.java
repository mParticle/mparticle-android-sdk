package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class UploadTableTest extends BaseTableTest{
    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(UploadTable.CREATE_UPLOADS_DDL);
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
}
