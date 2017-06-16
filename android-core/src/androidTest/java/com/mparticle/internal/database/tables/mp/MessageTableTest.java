package com.mparticle.internal.database.tables.mp;

import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class MessageTableTest extends BaseTableTest {

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
