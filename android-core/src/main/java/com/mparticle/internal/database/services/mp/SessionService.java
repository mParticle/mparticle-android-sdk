package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.Constants;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.database.tables.mp.SessionTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mparticle.internal.database.tables.mp.SessionTable.SessionTableColumns.APP_INFO;
import static com.mparticle.internal.database.tables.mp.SessionTable.SessionTableColumns.TABLE_NAME;

public class SessionService extends SessionTable {
    public static String[] readyMessages = new String[]{Integer.toString(Constants.Status.UPLOADED)};

    public static int deleteSessions(SQLiteDatabase database, String currentSessionId){
        String[] selectionArgs = new String[]{currentSessionId};
        return database.delete(TABLE_NAME, SessionTableColumns.SESSION_ID + "!=? ", selectionArgs);
    }

    public static Cursor getSessions(SQLiteDatabase db) {
        return db.query(TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static void updateSessionEndTime(SQLiteDatabase db, String sessionId, long endTime, long sessionLength) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTableColumns.END_TIME, endTime);
        if (sessionLength > 0) {
            sessionValues.put(SessionTableColumns.SESSION_FOREGROUND_LENGTH, sessionLength);
        }
        String[] whereArgs = {sessionId};
        db.update(TABLE_NAME, sessionValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static void updateSessionAttributes(SQLiteDatabase db, String sessionId, String attributes) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTableColumns.ATTRIBUTES, attributes);
        String[] whereArgs = {sessionId};
        db.update(TABLE_NAME, sessionValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static void updateSessionStatus(SQLiteDatabase db, String sessionId, String status) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SessionTableColumns.STATUS, status);
        String[] whereArgs = {sessionId};
        db.update(SessionTableColumns.TABLE_NAME, contentValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }

    public static Cursor getSessionForSessionEndMessage(SQLiteDatabase db, String sessionId) throws JSONException {
        String[] selectionArgs = new String[]{sessionId};
        String[] sessionColumns = new String[]{SessionTableColumns.START_TIME, SessionTableColumns.END_TIME,
                SessionTableColumns.SESSION_FOREGROUND_LENGTH, SessionTableColumns.ATTRIBUTES};
        Cursor selectCursor = db.query(TABLE_NAME, sessionColumns, SessionTableColumns.SESSION_ID + "=? and " + SessionTableColumns.STATUS + " IS NULL",
                selectionArgs, null, null, null);
        return selectCursor;
    }

    public static List<String> getOrphanSessionIds(SQLiteDatabase db, String apiKey) {
        List<String> sessionIds = new ArrayList<String>();
        String[] selectionArgs = new String[]{apiKey};
        String[] sessionColumns = new String[]{SessionTableColumns.SESSION_ID};
        Cursor selectCursor = null;
        try {
            selectCursor = db.query(TABLE_NAME, sessionColumns,
                    SessionTableColumns.API_KEY + "= ? and " + SessionTableColumns.STATUS + " IS NULL",
                    selectionArgs, null, null, null);
            // NOTE: there should be at most one orphan per api key - but
            // process any that are found
            while (selectCursor.moveToNext()) {
                sessionIds.add(selectCursor.getString(0));
            }
            return sessionIds;
        }
        finally {
            if (selectCursor != null && !selectCursor.isClosed()) {
                selectCursor.close();
            }
        }
    }

    public static void insertSession(SQLiteDatabase db, BaseMPMessage message, String apiKey, String appInfo, String deviceInfo, long mpId) throws JSONException {
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

    public static List<JSONObject> processSessions(SQLiteDatabase database, HashMap<String, Map<Long, MessageBatch>> uploadMessagesBySession) {
        Cursor sessionCursor = null;
        List<JSONObject> deviceInfos = new ArrayList<JSONObject>();
        try {
            sessionCursor = SessionService.getSessions(database);
            while (sessionCursor.moveToNext()) {
                String sessionId = sessionCursor.getString(sessionCursor.getColumnIndex(SessionTableColumns.SESSION_ID));
                Map<Long, MessageBatch> batchMap = uploadMessagesBySession.get(sessionId);
                if (batchMap != null) {
                    try {
                        String appInfo = sessionCursor.getString(sessionCursor.getColumnIndex(APP_INFO));
                        JSONObject appInfoJson = new JSONObject(appInfo);
                        String deviceInfo = sessionCursor.getString(sessionCursor.getColumnIndex(SessionTableColumns.DEVICE_INFO));
                        JSONObject deviceInfoJson = new JSONObject(deviceInfo);
                        deviceInfos.add(deviceInfoJson);
                        for (MessageBatch batch: batchMap.values()) {
                            batch.setAppInfo(appInfoJson);
                            batch.setDeviceInfo(deviceInfoJson);
                        }
                    } catch (Exception e) {

                    }

                }
            }
        }
        finally {
            if (sessionCursor != null && !sessionCursor.isClosed()){
                sessionCursor.close();
            }
        }
        return deviceInfos;
    }

    public static void updateSessionInstallReferrer(SQLiteDatabase db, JSONObject appInfo, String sessionId) {
        ContentValues contentValues = new ContentValues();
                contentValues.put(APP_INFO, appInfo.toString());
                String[] whereArgs = {sessionId};
                db.update(TABLE_NAME, contentValues, SessionTableColumns.SESSION_ID + "=?", whereArgs);
    }
}
