package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class MessageTable : MpIdDependentTable() {
    override fun getTableName(): String = MessageTableColumns.TABLE_NAME

    interface MessageTableColumns : BaseColumns {
        companion object {
            const val TABLE_NAME: String = "messages"
            const val SESSION_ID: String = "session_id"
            const val API_KEY: String = "api_key"
            const val MESSAGE: String = "message"
            const val STATUS: String = "upload_status"
            const val CREATED_AT: String = "message_time"
            const val MESSAGE_TYPE: String = "message_type"
            const val CF_UUID: String = "cfuuid"
            const val MP_ID: String = MpIdDependentTable.MP_ID
            const val DATAPLAN_VERSION: String = "dataplan_version"
            const val DATAPLAN_ID: String = "dataplan_id"
        }
    }

    companion object {
        const val ADD_DATAPLAN_VERSION_COLUMN: String = """
            ALTER TABLE ${MessageTableColumns.TABLE_NAME}
            ADD COLUMN ${MessageTableColumns.DATAPLAN_VERSION} NUMBER
        """
        const val ADD_DATAPLAN_ID_COLUMN: String = """
            ALTER TABLE ${MessageTableColumns.TABLE_NAME}
            ADD COLUMN ${MessageTableColumns.DATAPLAN_ID} TEXT
        """

        internal const val CREATE_MESSAGES_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${MessageTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${MessageTableColumns.SESSION_ID} STRING NOT NULL,
                ${MessageTableColumns.API_KEY} STRING NOT NULL,
                ${MessageTableColumns.MESSAGE} TEXT,
                ${MessageTableColumns.STATUS} INTEGER,
                ${MessageTableColumns.CREATED_AT} INTEGER NOT NULL,
                ${MessageTableColumns.MESSAGE_TYPE} TEXT,
                ${MessageTableColumns.CF_UUID} TEXT,
                ${MessageTableColumns.MP_ID} INTEGER,
                ${MessageTableColumns.DATAPLAN_ID} TEXT,
                ${MessageTableColumns.DATAPLAN_VERSION} INTEGER
            );
        """
    }
}
