package com.mparticle.internal.database.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.tables.BreadcrumbTable;
import com.mparticle.internal.messages.BaseMPMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BreadcrumbService extends BreadcrumbTable {

    private static final String[] idColumns = {"_id"};

    public static int insertBreadcrumb(MPDatabase db, Context context, BaseMPMessage message, String apiKey, Long mpid) throws JSONException {
        if (message == null) {
            return -1;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(BreadcrumbTableColumns.MP_ID, mpid);
        contentValues.put(BreadcrumbTableColumns.API_KEY, apiKey);
        contentValues.put(BreadcrumbTableColumns.CREATED_AT, message.getLong(Constants.MessageKey.TIMESTAMP));
        contentValues.put(BreadcrumbTableColumns.SESSION_ID, message.getSessionId());
        contentValues.put(BreadcrumbTableColumns.MESSAGE, message.toString());

        db.insert(BreadcrumbTableColumns.TABLE_NAME, null, contentValues);
        Cursor cursor = db.query(BreadcrumbTableColumns.TABLE_NAME,
                idColumns,
                BreadcrumbTableColumns.MP_ID + " = ?",
                new String[]{String.valueOf(mpid)},
                null,
                null,
                " _id asc");
        if (cursor.moveToFirst()) {
            int minId = cursor.getInt(0);
            if (cursor.getCount() > ConfigManager.getBreadcrumbLimit(context, mpid)) {
                String[] limit = {String.valueOf(minId)};
                return db.delete(BreadcrumbTableColumns.TABLE_NAME, " _id = ?", limit);
            }
        }
        return -1;
    }

    private static final String[] breadcrumbColumns = {
            BreadcrumbTableColumns.CREATED_AT,
            BreadcrumbTableColumns.MESSAGE
    };

    /**
     * For testing only.
     */
    static int getBreadcrumbCount(MPDatabase db, Long mpid) {
        Cursor rawIds = null;

        try {
            rawIds = db.query(BreadcrumbTableColumns.TABLE_NAME,
                    null,
                    BreadcrumbTableColumns.MP_ID + " = ? ",
                    new String[]{String.valueOf(mpid)},
                    null,
                    null,
                    null);
            return rawIds.getCount();
        } finally {
            if (rawIds != null && !rawIds.isClosed()) {
                rawIds.close();
            }
        }
    }

    public static JSONArray getBreadcrumbs(MPDatabase db, Context context, Long mpid) throws JSONException {
        Cursor breadcrumbCursor = null;
        try {
            breadcrumbCursor = db.query(BreadcrumbTableColumns.TABLE_NAME,
                    breadcrumbColumns,
                    BreadcrumbTableColumns.MP_ID + " = ? ",
                    new String[]{String.valueOf(mpid)},
                    null,
                    null,
                    BreadcrumbTableColumns.CREATED_AT + " desc limit " + ConfigManager.getBreadcrumbLimit(context, mpid));

            if (breadcrumbCursor.getCount() > 0) {
                JSONArray breadcrumbs = new JSONArray();
                int breadcrumbIndex = breadcrumbCursor.getColumnIndexOrThrow(BreadcrumbTableColumns.MESSAGE);
                while (breadcrumbCursor.moveToNext()) {
                    JSONObject breadcrumbObject = new JSONObject(breadcrumbCursor.getString(breadcrumbIndex));
                    breadcrumbs.put(breadcrumbObject);
                }
                return breadcrumbs;
            }

        } catch (Exception e) {
            Logger.debug("Error while appending breadcrumbs: " + e.toString());
        } finally {
            if (breadcrumbCursor != null && !breadcrumbCursor.isClosed()) {
                breadcrumbCursor.close();
            }
        }
        return new JSONArray();
    }

    public static void deleteAll(MPDatabase db) {
        db.delete(BreadcrumbTableColumns.TABLE_NAME, null, null);
    }
}
