package com.mparticle.internal.database.tables;

import android.provider.BaseColumns;

public class ReportingTable extends MpIdDependentTable {

    @Override
    public String getTableName() {
        return ReportingTableColumns.TABLE_NAME;
    }

    protected interface ReportingTableColumns extends BaseColumns {
        String CREATED_AT = "report_time";
        String MODULE_ID = "module_id";
        String TABLE_NAME = "reporting";
        String MESSAGE = "message";
        String SESSION_ID = "session_id";
        String MP_ID = MpIdDependentTable.MP_ID;
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


}
