package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class UploadTable {
    internal interface UploadTableColumns : BaseColumns {
        companion object {
            const val TABLE_NAME: String = "uploads"
            const val API_KEY: String = "api_key"
            const val MESSAGE: String = "message"
            const val CREATED_AT: String = "message_time"

            /**
             * This column, previously unused as CFUUID, has been re-purposed for REQUEST_TYPE
             * to avoid a schema change.
             */
            const val REQUEST_TYPE: String = "cfuuid"
            const val SESSION_ID: String = "session_id"
            const val UPLOAD_SETTINGS: String = "upload_settings"
        }
    }

    companion object {
        const val UPLOAD_REQUEST: String = "0"
        const val ALIAS_REQUEST: String = "1"

        internal const val CREATE_UPLOADS_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${UploadTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${UploadTableColumns.API_KEY} STRING NOT NULL,
                ${UploadTableColumns.MESSAGE} TEXT,
                ${UploadTableColumns.CREATED_AT} INTEGER NOT NULL,
                ${UploadTableColumns.REQUEST_TYPE} TEXT,
                ${UploadTableColumns.SESSION_ID} TEXT,
                ${UploadTableColumns.UPLOAD_SETTINGS} TEXT
            );
        """

        internal const val UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN: String = """
            ALTER TABLE ${UploadTableColumns.TABLE_NAME}
            ADD COLUMN ${UploadTableColumns.UPLOAD_SETTINGS} TEXT
        """
    }
}
