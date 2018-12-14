package com.mparticle.internal.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.junit.Test;

public class UserAttributeTableTest extends BaseTableTest {

    private static final String old_CREATE_USER_ATTRIBUTES_DDL =
            "CREATE TABLE IF NOT EXISTS " + UserAttributesTable.UserAttributesTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_KEY + " COLLATE NOCASE NOT NULL, " +
                    UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_VALUE + " TEXT, " +
                    UserAttributesTable.UserAttributesTableColumns.IS_LIST + " INTEGER NOT NULL, " +
                    UserAttributesTable.UserAttributesTableColumns.CREATED_AT + " INTEGER NOT NULL " +
                    ");";

    @Test
    public void createTableTest() throws InterruptedException {
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL);
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
                database.execSQL(old_CREATE_USER_ATTRIBUTES_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                database.execSQL(UserAttributesTable.getAddMpIdColumnString("1"));
            }
        });
    }


}
