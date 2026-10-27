package com.mparticle.internal.database.tables

import android.provider.BaseColumns

open class BreadcrumbTable : MpIdDependentTable() {
    override fun getTableName(): String = BreadcrumbTableColumns.TABLE_NAME

    internal interface BreadcrumbTableColumns {
        companion object {
            const val TABLE_NAME: String = "breadcrumbs"
            const val SESSION_ID: String = "session_id"
            const val API_KEY: String = "api_key"
            const val MESSAGE: String = "message"
            const val CREATED_AT: String = "breadcrumb_time"
            const val CF_UUID: String = "cfuuid"
            const val MP_ID: String = MpIdDependentTable.MP_ID
        }
    }

    companion object {
        internal const val CREATE_BREADCRUMBS_DDL: String = """
            CREATE TABLE IF NOT EXISTS ${BreadcrumbTableColumns.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${BreadcrumbTableColumns.SESSION_ID} STRING NOT NULL,
                ${BreadcrumbTableColumns.API_KEY} STRING NOT NULL,
                ${BreadcrumbTableColumns.MESSAGE} TEXT,
                ${BreadcrumbTableColumns.CREATED_AT} INTEGER NOT NULL,
                ${BreadcrumbTableColumns.CF_UUID} TEXT,
                ${BreadcrumbTableColumns.MP_ID} INTEGER
            );
        """
    }
}
