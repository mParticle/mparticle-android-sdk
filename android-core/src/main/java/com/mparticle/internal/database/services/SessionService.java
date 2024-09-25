package com.mparticle.internal.database.services;

import static com.mparticle.internal.database.tables.SessionTable.SessionTableColumns.APP_INFO;
import static com.mparticle.internal.database.tables.SessionTable.SessionTableColumns.SESSION_ID;
import static com.mparticle.internal.database.tables.SessionTable.SessionTableColumns.TABLE_NAME;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.mparticle.internal.BatchId;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.tables.BreadcrumbTable;
import com.mparticle.internal.database.tables.SessionTable;
import com.mparticle.internal.messages.BaseMPMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionService extends SessionTable {
    public static String[] readyMessages = new String[]{Integer.toString(Constants.Status.UPLOADED)};

    public static int deleteSessions(MPDatabase database, String currentSessionId) {
        String[] selectionArgs = new String[]{currentSessionId};
        return database.delete(TABLE_NAME, SessionTableColumns.SESSION_ID + "!=? ", selectionArgs);
    }

    /**
     * delete Session entries with session_id that are not a part of the Set
     *
     * @param database
     * @param exceptSessionIds the session_id value for message that SHOULD NOT be deleted
     * @return the number of Session entries deleted
     */
    public static int deleteSessions(MPDatabase database, Set<String> exceptSessionIds) {
        List<String> idsWrapped = new ArrayList<String>();
        for (String id : exceptSessionIds) {
            idsWrapped.add("'" + id + "'");
        }
        String idArgString = TextUtils.join(",", idsWrapped.toArray());
        return database.delete(TABLE_NAME, SESSION_ID + " NOT IN (" + idArgString + ")", null);
    }

    public static Cursor getSessions(MPDatabase db) {
        return db.query(TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static void updateSessionEndTime(MPDatabase db, String sessionId, long endTime, long sessionLength) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTableColumns.END_TIME, endTime);
        if (sessionLength > 0) {
            sessionValues.put(SessionTableColumns.SESSION_FOREGROUND_LENGTH, sessionLength);
        }
        String[] whereArgs = {sessionId};
        db.update(TABLE_NAME, sessionValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static void updateSessionAttributes(MPDatabase db, String sessionId, String attributes) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTableColumns.ATTRIBUTES, attributes);
        String[] whereArgs = {sessionId};
        db.update(TABLE_NAME, sessionValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static void updateSessionStatus(MPDatabase db, String sessionId, String status) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SessionTableColumns.STATUS, status);
        String[] whereArgs = {sessionId};
        db.update(SessionTableColumns.TABLE_NAME, contentValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static Cursor getSessionForSessionEndMessage(MPDatabase db, String sessionId) throws JSONException {
        String[] selectionArgs = new String[]{sessionId};
        String[] sessionColumns = new String[]{SessionTableColumns.START_TIME, SessionTableColumns.END_TIME,
                SessionTableColumns.SESSION_FOREGROUND_LENGTH, SessionTableColumns.ATTRIBUTES};
        Cursor selectCursor = db.query(TABLE_NAME, sessionColumns, SessionTableColumns.SESSION_ID + "=? and " + SessionTableColumns.STATUS + " IS NULL",
                selectionArgs, null, null, null);
        return selectCursor;
    }

    public static List<String> getOrphanSessionIds(MPDatabase db, String apiKey) {
        List<String> sessionIds = new ArrayList<String>();
        String[] selectionArgs = new String[]{apiKey};
        String[] sessionColumns = new String[]{SessionTableColumns.SESSION_ID};
        Cursor selectCursor = null;
        try {
            selectCursor = db.query(TABLE_NAME, sessionColumns,
                    SessionTableColumns.API_KEY + "= ? and " + SessionTableColumns.STATUS + " IS NULL",
                    selectionArgs, null, null, null);
            // NOTE: There should be at most one orphan per API key - but
            // process any that are found.
            while (selectCursor.moveToNext()) {
                sessionIds.add(selectCursor.getString(0));
            }
            return sessionIds;
        } finally {
            if (selectCursor != null && !selectCursor.isClosed()) {
                selectCursor.close();
            }
        }
    }

    public static void insertSession(MPDatabase db, BaseMPMessage message, String apiKey, String appInfo, String deviceInfo, long mpId) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SessionTableColumns.MP_ID, mpId);
        contentValues.put(SessionTableColumns.API_KEY, apiKey);
        contentValues.put(SessionTableColumns.SESSION_ID, message.getSessionId());
        contentValues.put(SessionTableColumns.START_TIME, message.getLong(Constants.MessageKey.TIMESTAMP));
        contentValues.put(SessionTableColumns.END_TIME, message.getLong(Constants.MessageKey.TIMESTAMP));
        contentValues.put(SessionTableColumns.SESSION_FOREGROUND_LENGTH, 0);
        contentValues.put(APP_INFO, appInfo);
        contentValues.put(SessionTableColumns.DEVICE_INFO, deviceInfo);
        db.insert(TABLE_NAME, null, contentValues);
    }

    public static List<JSONObject> processSessions(MPDatabase database, HashMap<BatchId, MessageBatch> uploadMessagesByBatchId) {
        Cursor sessionCursor = null;
        List<JSONObject> deviceInfos = new ArrayList<JSONObject>();
        Map<String, List<MessageBatch>> batchesBySessionId = flattenBySessionId(uploadMessagesByBatchId);
        try {
            sessionCursor = SessionService.getSessions(database);
            while (sessionCursor.moveToNext()) {
                String sessionId = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(SessionTableColumns.SESSION_ID));
                List<MessageBatch> batchList = batchesBySessionId.get(sessionId);
                if (batchList != null) {
                    try {
                        String appInfo = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(APP_INFO));
                        JSONObject appInfoJson = new JSONObject(appInfo);
                        String deviceInfo = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(SessionTableColumns.DEVICE_INFO));
                        JSONObject deviceInfoJson = new JSONObject(deviceInfo);
                        deviceInfos.add(deviceInfoJson);
                        for (MessageBatch batch : batchList) {
                            batch.setAppInfo(appInfoJson);
                            batch.setDeviceInfo(deviceInfoJson);
                        }
                    } catch (Exception e) {

                    }

                }
            }
        } finally {
            if (sessionCursor != null && !sessionCursor.isClosed()) {
                sessionCursor.close();
            }
        }
        return deviceInfos;
    }

    public static void updateSessionInstallReferrer(MPDatabase db, JSONObject appInfo, String sessionId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_INFO, appInfo.toString());
        String[] whereArgs = {sessionId};
        db.update(TABLE_NAME, contentValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    static Map<String, List<MessageBatch>> flattenBySessionId(Map<BatchId, MessageBatch> uploadMessagesByBatchId) {
        Map<String, List<MessageBatch>> uploadMessagesBySessionId = new HashMap<String, List<MessageBatch>>();
        for (Map.Entry<BatchId, MessageBatch> batchEntry : uploadMessagesByBatchId.entrySet()) {
            String sessionId = batchEntry.getKey().getSessionId();
            List<MessageBatch> messageBatches = uploadMessagesBySessionId.get(sessionId);
            if (messageBatches == null) {
                messageBatches = new ArrayList<MessageBatch>();
                uploadMessagesBySessionId.put(sessionId, messageBatches);
            }
            messageBatches.add(batchEntry.getValue());
        }
        return uploadMessagesBySessionId;
    }

    public static void deleteAll(MPDatabase db) {
        db.delete(SessionTableColumns.TABLE_NAME, null, null);
    }
}
