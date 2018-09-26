package com.mparticle.internal.database.tables.mp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MParticleDatabaseHelper implements SQLiteOpenHelperWrapper {
    private final Context mContext;
    public static final int DB_VERSION = 8;
    public static final String DB_NAME = "mparticle.db";

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
        if (oldVersion < 5) {
            upgradeUserAttributes(db);
        }
        if (oldVersion < 6) {
            upgradeSessionTable(db);
            upgradeReportingTable(db);
        }
        if (oldVersion < 7 ) {
            upgradeMpId(db);
            ConfigManager.setNeedsToMigrate(mContext, true);
        }
        if (oldVersion < 8) {
            removeGcmTable(db);
        }
    }

    private void upgradeSessionTable(SQLiteDatabase db) {
        db.execSQL(SessionTable.SESSION_ADD_APP_INFO_COLUMN);
        db.execSQL(SessionTable.SESSION_ADD_DEVICE_INFO_COLUMN);
    }

    private void upgradeReportingTable(SQLiteDatabase db) {
        db.execSQL(ReportingTable.REPORTING_ADD_SESSION_ID_COLUMN);
    }

    private void upgradeMpId(SQLiteDatabase db) {
        String currentMpId = String.valueOf(ConfigManager.getMpid(mContext));
        db.execSQL(ReportingTable.getAddMpIdColumnString(currentMpId));
        db.execSQL(SessionTable.getAddMpIdColumnString(currentMpId));
        db.execSQL(UserAttributesTable.getAddMpIdColumnString(currentMpId));
        db.execSQL(BreadcrumbTable.getAddMpIdColumnString(currentMpId));
        db.execSQL(MessageTable.getAddMpIdColumnString(currentMpId));
    }

    private void upgradeUserAttributes(SQLiteDatabase db) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String userAttrs = sharedPreferences.getString(Constants.PrefKeys.DEPRECATED_USER_ATTRS + MParticle.getInstance().Internal().getConfigManager().getApiKey(), null);
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
            sharedPreferences.edit().remove(Constants.PrefKeys.DEPRECATED_USER_ATTRS + MParticle.getInstance().Internal().getConfigManager().getApiKey()).apply();
        }
    }

    private void removeGcmTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS gcm_messages");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String addIntegerColumnString(String tableName, String columnName, String defaultValue) {
        return addColumnString(tableName, columnName, "INTEGER", defaultValue);
    }

    static String addColumnString(String tableName, String columnName, String type) {
        return addColumnString(tableName, columnName, type, null);
    }

    static String addColumnString(String tableName, String columnName, String type, String defaultValue) {
        StringBuilder builder = new StringBuilder(String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, type));
        if (!MPUtility.isEmpty(defaultValue)) {
            builder.append(String.format(" DEFAULT %s", defaultValue));
        }
        return builder.toString();
    }

}
