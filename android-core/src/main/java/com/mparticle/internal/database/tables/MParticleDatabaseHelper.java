package com.mparticle.internal.database.tables;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.VisibleForTesting;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MParticleDatabaseHelper implements SQLiteOpenHelperWrapper {
    private final Context mContext;
    public static final int DB_VERSION = 10;
    private static String DB_NAME = "mparticle.db";

    public static String getDbName() {
        return DB_NAME;
    }

    @VisibleForTesting
    public static void setDbName(String name) {
        DB_NAME = name;
    }

    public MParticleDatabaseHelper(Context context) {
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SessionTable.CREATE_SESSIONS_DDL);
        db.execSQL(MessageTable.CREATE_MESSAGES_DDL);
        db.execSQL(UploadTable.CREATE_UPLOADS_DDL);
        db.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL);
        db.execSQL(ReportingTable.CREATE_REPORTING_DDL);
        db.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SessionTable.CREATE_SESSIONS_DDL);
        db.execSQL(MessageTable.CREATE_MESSAGES_DDL);
        db.execSQL(UploadTable.CREATE_UPLOADS_DDL);
        db.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL);
        db.execSQL(ReportingTable.CREATE_REPORTING_DDL);
        db.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL);
        try {
            if (oldVersion < 5) {
                upgradeUserAttributes(db);
            }
            if (oldVersion < 6) {
                upgradeSessionTable(db);
                upgradeReportingTable(db);
            }
            if (oldVersion < 7) {
                upgradeMpId(db);
                ConfigManager.setNeedsToMigrate(mContext, true);
            }
            if (oldVersion < 8) {
                removeGcmTable(db);
            }
            if (oldVersion < 9) {
                upgradeMessageTable(db);
            }
            if (oldVersion < 10) {
                upgradeUploadsTable(db);
            }
        } catch (Exception e) {
            Logger.warning("Exception while upgrading SQLite Database:\n" + e.getMessage() + "\nThis may have been caused by the database having been already upgraded");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SessionTable.SessionTableColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UploadTable.UploadTableColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ReportingTable.ReportingTableColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserAttributesTable.UserAttributesTableColumns.TABLE_NAME);
        onCreate(db);
    }

    private void upgradeSessionTable(SQLiteDatabase db) {
        db.execSQL(SessionTable.SESSION_ADD_APP_INFO_COLUMN);
        db.execSQL(SessionTable.SESSION_ADD_DEVICE_INFO_COLUMN);
    }

    private void upgradeReportingTable(SQLiteDatabase db) {
        db.execSQL(ReportingTable.REPORTING_ADD_SESSION_ID_COLUMN);
    }

    private void upgradeMessageTable(SQLiteDatabase db) {
        db.execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN);
        db.execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN);
    }

    private void upgradeMpId(SQLiteDatabase db) {
        final String currentMpId = String.valueOf(ConfigManager.getMpid(mContext));
        String updateStatement = "ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT \'%s\'";
        String[] tableNames = new String[]{
                ReportingTable.ReportingTableColumns.TABLE_NAME,
                SessionTable.SessionTableColumns.TABLE_NAME,
                UserAttributesTable.UserAttributesTableColumns.TABLE_NAME,
                BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME,
                MessageTable.MessageTableColumns.TABLE_NAME
        };
        for (String tableName : tableNames) {
            SQLiteStatement statement = db.compileStatement(String.format(updateStatement, tableName, MpIdDependentTable.MP_ID, currentMpId));
            statement.execute();
        }
    }

    private void upgradeUserAttributes(SQLiteDatabase db) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);

        String userAttrs = sharedPreferences.getString(Constants.PrefKeys.DEPRECATED_USER_ATTRS + ConfigManager.getInstance(mContext).getApiKey(), null);
        try {
            JSONObject userAttributes = new JSONObject(userAttrs);
            Iterator<String> iter = userAttributes.keys();
            ContentValues values;
            double time = System.currentTimeMillis();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    Object value = userAttributes.get(key);
                    String stringValue = null;
                    if (value != null) {
                        stringValue = value.toString();
                    }
                    values = new ContentValues();
                    values.put(UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_KEY, key);
                    values.put(UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_VALUE, stringValue);
                    values.put(UserAttributesTable.UserAttributesTableColumns.IS_LIST, false);
                    values.put(UserAttributesTable.UserAttributesTableColumns.CREATED_AT, time);
                    db.insert(UserAttributesTable.UserAttributesTableColumns.TABLE_NAME, null, values);
                } catch (JSONException e) {
                }
            }

        } catch (Exception e) {
        } finally {
            sharedPreferences.edit().remove(Constants.PrefKeys.DEPRECATED_USER_ATTRS + ConfigManager.getInstance(mContext).getApiKey()).apply();
        }
    }

    private void removeGcmTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS gcm_messages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void upgradeUploadsTable(SQLiteDatabase db) {
        db.execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN);

        // Insert current upload settings
        UploadSettings uploadSettings = ConfigManager.getInstance(mContext).getUploadSettings();
        ContentValues values = new ContentValues();
        values.put(UploadTable.UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson());
        db.update(UploadTable.UploadTableColumns.TABLE_NAME, values, null, null);
    }
}
