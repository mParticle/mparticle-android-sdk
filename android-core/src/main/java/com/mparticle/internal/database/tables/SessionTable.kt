package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class SessionTable : MpIdDependentTable() {
    override fun getTableName(): String = SessionTableColumns.TABLE_NAME

    interface SessionTableColumns {
        companion object {
            const val TABLE_NAME: String = "sessions"
            const val SESSION_ID: String = "session_id"
            const val API_KEY: String = "api_key"
            const val START_TIME: String = "start_time"
            const val END_TIME: String = "end_time"
            const val SESSION_FOREGROUND_LENGTH: String = "session_length"
            const val ATTRIBUTES: String = "attributes"

            /**
             * This column, previously unused as CFUUID, has been re-purposed for STATUS
             * to avoid a schema change.
             */
            const val STATUS: String = "cfuuid"
            const val APP_INFO: String = "app_info"
            const val DEVICE_INFO: String = "device_info"
            const val MP_ID: String = MpIdDependentTable.MP_ID
        }
    }

    /**
     * SessionStatus is used to determine if we've "closed" a session by
     * logging a session-end event. Sessions are kept in the database after
     * this, due to requiring device and application information at the time
     * of batch creation.
     */
    interface SessionStatus {
        companion object {
            const val CLOSED: String = "1"
        }
    }

    companion object {
        internal const val SESSION_ADD_DEVICE_INFO_COLUMN: String = """
            ALTER TABLE ${SessionTableColumns.TABLE_NAME}
            ADD COLUMN ${SessionTableColumns.DEVICE_INFO} TEXT
        """

        internal const val SESSION_ADD_APP_INFO_COLUMN: String = """
            ALTER TABLE ${SessionTableColumns.TABLE_NAME}
            ADD COLUMN ${SessionTableColumns.APP_INFO} TEXT
        """

        internal const val CREATE_SESSIONS_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${SessionTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${SessionTableColumns.SESSION_ID} STRING NOT NULL,
                ${SessionTableColumns.API_KEY} STRING NOT NULL,
                ${SessionTableColumns.START_TIME} INTEGER NOT NULL,
                ${SessionTableColumns.END_TIME} INTEGER NOT NULL,
                ${SessionTableColumns.SESSION_FOREGROUND_LENGTH} INTEGER NOT NULL,
                ${SessionTableColumns.ATTRIBUTES} TEXT,
                ${SessionTableColumns.STATUS} TEXT,
                ${SessionTableColumns.APP_INFO} TEXT,
                ${SessionTableColumns.DEVICE_INFO} TEXT,
                ${SessionTableColumns.MP_ID} INTEGER
            );
        """
    }
}
