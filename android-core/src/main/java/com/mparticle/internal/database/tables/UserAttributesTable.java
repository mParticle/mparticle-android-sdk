package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class UserAttributesTable extends MpIdDependentTable {

    @Override
    public String getTableName() {
        return UserAttributesTableColumns.TABLE_NAME;
    }

    protected interface UserAttributesTableColumns {
        String TABLE_NAME = "attributes";
        String ATTRIBUTE_KEY = "attribute_key";
        String ATTRIBUTE_VALUE = "attribute_value";
        String IS_LIST = "is_list";
        String CREATED_AT = "created_time";
        String MP_ID = MpIdDependentTable.MP_ID;
    }

    static final String CREATE_USER_ATTRIBUTES_DDL =
            "CREATE TABLE IF NOT EXISTS " + UserAttributesTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UserAttributesTableColumns.ATTRIBUTE_KEY + " COLLATE NOCASE NOT NULL, " +
                    UserAttributesTableColumns.ATTRIBUTE_VALUE + " TEXT, " +
                    UserAttributesTableColumns.IS_LIST + " INTEGER NOT NULL, " +
                    UserAttributesTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    UserAttributesTableColumns.MP_ID + " INTEGER" +
                    ");";
}
