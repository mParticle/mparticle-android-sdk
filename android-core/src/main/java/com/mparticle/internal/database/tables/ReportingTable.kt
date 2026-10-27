package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class ReportingTable : MpIdDependentTable() {
    override fun getTableName(): String = ReportingTableColumns.TABLE_NAME

    internal interface ReportingTableColumns : BaseColumns {
        companion object {
            const val CREATED_AT: String = "report_time"
            const val MODULE_ID: String = "module_id"
            const val TABLE_NAME: String = "reporting"
            const val MESSAGE: String = "message"
            const val SESSION_ID: String = "session_id"
            const val MP_ID: String = MpIdDependentTable.MP_ID
        }
    }

    companion object {
        internal const val CREATE_REPORTING_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${ReportingTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${ReportingTableColumns.MODULE_ID} INTEGER NOT NULL,
                ${ReportingTableColumns.MESSAGE} TEXT NOT NULL,
                ${ReportingTableColumns.SESSION_ID} STRING NOT NULL,
                ${ReportingTableColumns.CREATED_AT} INTEGER NOT NULL,
                ${ReportingTableColumns.MP_ID} INTEGER
            );
        """

        internal const val REPORTING_ADD_SESSION_ID_COLUMN: String = """
            ALTER TABLE ${ReportingTableColumns.TABLE_NAME}
            ADD COLUMN ${ReportingTableColumns.SESSION_ID} STRING
        """
    }
}
