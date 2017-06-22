package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.database.tables.mp.BreadcrumbTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BreadcrumbService extends BreadcrumbTable {

    private static final String[] idColumns = {"_id"};

    public static void dbInsertBreadcrumb(SQLiteDatabase db, MPMessage message, String apiKey) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BreadcrumbTableColumns.API_KEY, apiKey);
        contentValues.put(BreadcrumbTableColumns.CREATED_AT, message.getLong(Constants.MessageKey.TIMESTAMP));
        contentValues.put(BreadcrumbTableColumns.SESSION_ID, message.getSessionId());
        contentValues.put(BreadcrumbTableColumns.MESSAGE, message.toString());


        db.insert(BreadcrumbTableColumns.TABLE_NAME, null, contentValues);
        Cursor cursor = db.query(BreadcrumbTableColumns.TABLE_NAME, idColumns, null, null, null, null, " _id desc limit 1");
        if (cursor.moveToFirst()) {
            int maxId = cursor.getInt(0);
            if (maxId > ConfigManager.getBreadcrumbLimit()) {
                String[] limit = {Integer.toString(maxId - ConfigManager.getBreadcrumbLimit())};
                db.delete(BreadcrumbTableColumns.TABLE_NAME, " _id < ?", limit);
            }
        }
    }

    private static final String[] breadcrumbColumns = {
            BreadcrumbTableColumns.CREATED_AT,
            BreadcrumbTableColumns.MESSAGE
    };

    public static void appendBreadcrumbs(SQLiteDatabase db, JSONObject message) throws JSONException {
        Cursor breadcrumbCursor = null;
        try {
            breadcrumbCursor = db.query(BreadcrumbTableColumns.TABLE_NAME,
                    breadcrumbColumns,
                    null,
                    null,
                    null,
                    null,
                    BreadcrumbTableColumns.CREATED_AT + " desc limit " + ConfigManager.getBreadcrumbLimit());

            if (breadcrumbCursor.getCount() > 0) {
                JSONArray breadcrumbs = new JSONArray();
                int breadcrumbIndex = breadcrumbCursor.getColumnIndex(BreadcrumbTableColumns.MESSAGE);
                while (breadcrumbCursor.moveToNext()) {
                    JSONObject breadcrumbObject = new JSONObject(breadcrumbCursor.getString(breadcrumbIndex));
                    breadcrumbs.put(breadcrumbObject);
                }
                message.put(Constants.MessageType.BREADCRUMB, breadcrumbs);
            }
        } catch (Exception e) {
            Logger.debug("Error while appending breadcrumbs: " + e.toString());
        } finally {
            if (breadcrumbCursor != null && !breadcrumbCursor.isClosed()) {
                breadcrumbCursor.close();
            }
        }
    }
}
