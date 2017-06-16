package com.mparticle.internal.database.tables.mp;

import android.content.Context;
import android.provider.BaseColumns;

import com.mparticle.internal.ConfigManager;

public class BreadcrumbTable {

    protected interface BreadcrumbTableColumns {
        String TABLE_NAME = "breadcrumbs";
        String SESSION_ID = "session_id";
        String API_KEY = "api_key";
        String MESSAGE = "message";
        String CREATED_AT = "breadcrumb_time";
        String CF_UUID = "cfuuid";
        String MP_ID = "mp_id";
    }

    static String getAddMpIdColumnString(String defaultValue) {
        return MParticleDatabaseHelper.addIntegerColumnString(BreadcrumbTableColumns.TABLE_NAME, BreadcrumbTableColumns.MP_ID, defaultValue);
    }

    static final String CREATE_BREADCRUMBS_DDL =
            "CREATE TABLE IF NOT EXISTS " + BreadcrumbTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    BreadcrumbTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    BreadcrumbTableColumns.API_KEY + " STRING NOT NULL, " +
                    BreadcrumbTableColumns.MESSAGE + " TEXT, " +
                    BreadcrumbTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    BreadcrumbTableColumns.CF_UUID + " TEXT, " +
                    BreadcrumbTableColumns.MP_ID + " INTEGER" +
                    ");";
}
