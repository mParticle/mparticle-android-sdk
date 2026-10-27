package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class UserAttributesTable : MpIdDependentTable() {
    override fun getTableName(): String = UserAttributesTableColumns.TABLE_NAME

    internal interface UserAttributesTableColumns {
        companion object {
            const val TABLE_NAME: String = "attributes"
            const val ATTRIBUTE_KEY: String = "attribute_key"
            const val ATTRIBUTE_VALUE: String = "attribute_value"
            const val IS_LIST: String = "is_list"
            const val CREATED_AT: String = "created_time"
            const val MP_ID: String = MpIdDependentTable.MP_ID
        }
    }

    companion object {
        internal const val CREATE_USER_ATTRIBUTES_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${UserAttributesTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${UserAttributesTableColumns.ATTRIBUTE_KEY} COLLATE NOCASE NOT NULL,
                ${UserAttributesTableColumns.ATTRIBUTE_VALUE} TEXT,
                ${UserAttributesTableColumns.IS_LIST} INTEGER NOT NULL,
                ${UserAttributesTableColumns.CREATED_AT} INTEGER NOT NULL,
                ${UserAttributesTableColumns.MP_ID} INTEGER
            );
        """
    }
}
