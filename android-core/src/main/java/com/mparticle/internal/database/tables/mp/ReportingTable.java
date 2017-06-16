package com.mparticle.internal.database.tables.mp;

import android.content.Context;
import android.provider.BaseColumns;

public class ReportingTable {

    protected interface ReportingTableColumns extends BaseColumns {
        String CREATED_AT = "report_time";
        String MODULE_ID = "module_id";
        String TABLE_NAME = "reporting";
        String MESSAGE = "message";
        String SESSION_ID = "session_id";
        String MP_ID = "mp_id";
    }

    static final String CREATE_REPORTING_DDL =
            "CREATE TABLE IF NOT EXISTS " + ReportingTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ReportingTableColumns.MODULE_ID + " INTEGER NOT NULL, " +
                    ReportingTableColumns.MESSAGE + " TEXT NOT NULL, " +
                    ReportingTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    ReportingTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    ReportingTableColumns.MP_ID + " INTEGER" +
                    ");";

    static final String REPORTING_ADD_SESSION_ID_COLUMN = "ALTER TABLE " + ReportingTableColumns.TABLE_NAME +
            " ADD COLUMN " + ReportingTableColumns.SESSION_ID + " STRING";

    static String getAddMpIdColumnString(String defaultValue) {
        return MParticleDatabaseHelper.addIntegerColumnString(ReportingTableColumns.TABLE_NAME, ReportingTableColumns.MP_ID, defaultValue);
    }

}
